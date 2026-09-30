package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.model.AppConfig
import com.example.data.model.AuditLog
import com.example.data.model.Contribution
import com.example.data.model.Member
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class CloudSyncState {
    CONNECTED,
    SYNCING,
    SYNCED,
    OFFLINE_CACHE,
    SETUP_REQUIRED,
    PERMISSION_REQUIRED
}

class FirestoreSyncManager(
    private val context: Context,
    private val db: AppDatabase
) {
    private val TAG = "FirestoreSyncManager"

    private val _syncState = MutableStateFlow(CloudSyncState.OFFLINE_CACHE)
    val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()

    private val _statusMessage = MutableStateFlow("Initializing centralized database...")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow<Long?>(null)
    val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

    private val _isRealtimeActive = MutableStateFlow(false)
    val isRealtimeActive: StateFlow<Boolean> = _isRealtimeActive.asStateFlow()

    private var firestore: FirebaseFirestore? = null
    private val listenerRegistrations = mutableListOf<ListenerRegistration>()

    private val memberDao = db.memberDao()
    private val contributionDao = db.contributionDao()
    private val configDao = db.appConfigDao()
    private val auditLogDao = db.auditLogDao()

    init {
        setupFirestore()
    }

    private fun setupFirestore() {
        try {
            val app = if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseApp.getInstance()
            } else {
                try {
                    FirebaseApp.initializeApp(context)
                } catch (e: Exception) {
                    Log.w(TAG, "Default FirebaseApp not initialized: ${e.message}")
                    null
                }
            }

            if (app != null) {
                // Attempt anonymous auth if enabled in Firebase Console (satisfies request.auth != null)
                try {
                    val auth = FirebaseAuth.getInstance(app)
                    if (auth.currentUser == null) {
                        auth.signInAnonymously()
                            .addOnSuccessListener {
                                Log.d(TAG, "Authenticated anonymously with Firebase: ${it.user?.uid}")
                            }
                            .addOnFailureListener { e ->
                                Log.d(TAG, "Anonymous auth not enabled in Firebase Console: ${e.message}")
                            }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "FirebaseAuth initialization note: ${e.message}")
                }

                val fs = FirebaseFirestore.getInstance(app)
                val settings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                fs.firestoreSettings = settings
                firestore = fs
                _syncState.value = CloudSyncState.CONNECTED
                _statusMessage.value = "Connected to Firebase Cloud Firestore"
            } else {
                _syncState.value = CloudSyncState.SETUP_REQUIRED
                _statusMessage.value = "Centralized cloud ready: Waiting for google-services.json credentials"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firestore: ${e.message}", e)
            _syncState.value = CloudSyncState.SETUP_REQUIRED
            _statusMessage.value = "Offline cache active (Firebase project setup needed)"
        }
    }

    private fun handleListenerError(error: FirebaseFirestoreException?, collectionName: String) {
        if (error == null) return
        val isPermissionDenied = error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                error.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true

        if (isPermissionDenied) {
            Log.w(
                TAG,
                "$collectionName listener: Firestore PERMISSION_DENIED. Firebase Console Security Rules require read/write access. Falling back safely to local Room SQLite database."
            )
            _syncState.value = CloudSyncState.PERMISSION_REQUIRED
            _statusMessage.value = "Firestore Rules require permissions in Firebase Console. Local offline database active."
            // Stop listeners to prevent repeated permission errors
            stopRealtimeSync()
        } else {
            Log.w(TAG, "$collectionName listener warning: ${error.message}")
            _syncState.value = CloudSyncState.OFFLINE_CACHE
            _statusMessage.value = "Cloud offline (${error.code}). Local database active."
        }
    }

    fun startRealtimeSync(scope: CoroutineScope) {
        val fs = firestore
        if (fs == null) {
            Log.d(TAG, "Cannot start realtime sync: Firestore is null")
            return
        }

        // Cancel any existing listeners
        stopRealtimeSync()

        try {
            // 1. Members Collection Listener
            val membersReg = fs.collection("members")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleListenerError(error, "Members")
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val members = snapshot.documents.mapNotNull { docToMember(it) }
                                if (members.isNotEmpty()) {
                                    memberDao.insertMembers(members)
                                    _lastSyncTimestamp.value = System.currentTimeMillis()
                                    _syncState.value = CloudSyncState.SYNCED
                                    _statusMessage.value = "Synchronized ${members.size} members from Cloud"
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to merge cloud members: ${e.message}")
                            }
                        }
                    }
                }
            listenerRegistrations.add(membersReg)

            // 2. Contributions Collection Listener
            val contribsReg = fs.collection("contributions")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleListenerError(error, "Contributions")
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val contributions = snapshot.documents.mapNotNull { docToContribution(it) }
                                if (contributions.isNotEmpty()) {
                                    contributionDao.insertContributions(contributions)
                                    _lastSyncTimestamp.value = System.currentTimeMillis()
                                    _syncState.value = CloudSyncState.SYNCED
                                    _statusMessage.value = "Synchronized ${contributions.size} contributions from Cloud"
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to merge cloud contributions: ${e.message}")
                            }
                        }
                    }
                }
            listenerRegistrations.add(contribsReg)

            // 3. App Config Listener (Admin Only can write, all can read)
            val configReg = fs.collection("app_config").document("settings")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleListenerError(error, "Config")
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val cloudConfig = docToAppConfig(snapshot)
                                if (cloudConfig != null) {
                                    configDao.insertOrUpdate(cloudConfig)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to merge cloud config: ${e.message}")
                            }
                        }
                    }
                }
            listenerRegistrations.add(configReg)

            // 4. Audit Logs Listener
            val logsReg = fs.collection("audit_logs")
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleListenerError(error, "Audit logs")
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val logs = snapshot.documents.mapNotNull { docToAuditLog(it) }
                                if (logs.isNotEmpty()) {
                                    auditLogDao.insertLogs(logs)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to merge cloud audit logs: ${e.message}")
                            }
                        }
                    }
                }
            // 5. Payment Reminders Listener (Cross-device push notifications, member only sees their own)
            val syncStartTime = System.currentTimeMillis() - (60 * 1000)
            val remindersReg = fs.collection("payment_reminders")
                .whereGreaterThan("timestamp", syncStartTime)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Reminders listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        for (change in snapshot.documentChanges) {
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                val doc = change.document
                                val targetMemberId = doc.getLong("memberId")
                                val targetMemberName = doc.getString("memberName") ?: "Member"
                                val monthYear = doc.getString("monthYear") ?: ""
                                val amount = doc.getDouble("amount") ?: 0.0

                                if (com.example.util.NotificationHelper.shouldShowNotificationForMember(context, targetMemberId, targetMemberName)) {
                                    com.example.util.NotificationHelper.sendContributionReminderNotification(
                                        context = context,
                                        memberName = targetMemberName,
                                        monthYear = monthYear,
                                        amount = amount,
                                        targetMemberId = targetMemberId,
                                        notificationId = com.example.util.NotificationHelper.NOTIFICATION_ID_BASE + (targetMemberId?.toInt() ?: 1)
                                    )
                                }
                            }
                        }
                    }
                }
            listenerRegistrations.add(remindersReg)

            _isRealtimeActive.value = true
            _syncState.value = CloudSyncState.CONNECTED
            _statusMessage.value = "Realtime Central Database Active"
            Log.d(TAG, "Realtime Firestore sync listeners registered successfully")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start realtime sync: ${e.message}")
            _isRealtimeActive.value = false
        }
    }

    fun stopRealtimeSync() {
        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()
        _isRealtimeActive.value = false
    }

    suspend fun pushMember(member: Member) {
        val fs = firestore ?: return
        withContext(Dispatchers.IO) {
            try {
                val data = hashMapOf(
                    "id" to member.id,
                    "name" to member.name,
                    "phone" to member.phone,
                    "email" to member.email,
                    "role" to member.role,
                    "joinDate" to member.joinDate,
                    "isActive" to member.isActive,
                    "notes" to member.notes,
                    "updatedAt" to System.currentTimeMillis()
                )
                fs.collection("members").document(member.id.toString())
                    .set(data, SetOptions.merge())
                    .await()
                _lastSyncTimestamp.value = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to push member ${member.id} to cloud: ${e.message}")
            }
        }
    }

    suspend fun deleteMemberFromCloud(memberId: Long) {
        val fs = firestore ?: return
        withContext(Dispatchers.IO) {
            try {
                fs.collection("members").document(memberId.toString())
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete member $memberId from cloud: ${e.message}")
            }
        }
    }

    suspend fun pushContribution(contribution: Contribution) {
        val fs = firestore ?: return
        val docId = "${contribution.memberId}_${contribution.monthYear}"
        withContext(Dispatchers.IO) {
            try {
                val data = hashMapOf(
                    "id" to contribution.id,
                    "memberId" to contribution.memberId,
                    "monthYear" to contribution.monthYear,
                    "amount" to contribution.amount,
                    "status" to contribution.status,
                    "paymentMethod" to contribution.paymentMethod,
                    "transactionRef" to contribution.transactionRef,
                    "paidDate" to contribution.paidDate,
                    "verifiedByAdmin" to contribution.verifiedByAdmin,
                    "verifiedDate" to contribution.verifiedDate,
                    "remarks" to contribution.remarks,
                    "updatedAt" to System.currentTimeMillis()
                )
                fs.collection("contributions").document(docId)
                    .set(data, SetOptions.merge())
                    .await()
                _lastSyncTimestamp.value = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to push contribution $docId to cloud: ${e.message}")
            }
        }
    }

    suspend fun pushConfig(config: AppConfig, isAdmin: Boolean) {
        val fs = firestore ?: return
        if (!isAdmin) {
            Log.w(TAG, "Security reject: Non-admin attempted to push config to cloud database")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val data = hashMapOf(
                    "id" to config.id,
                    "upiId" to config.upiId,
                    "upiName" to config.upiName,
                    "monthlyAmount" to config.monthlyAmount,
                    "adminPin" to config.adminPin,
                    "fundTitle" to config.fundTitle,
                    "contactPhone" to config.contactPhone,
                    "reminderDayOfMonth" to config.reminderDayOfMonth,
                    "autoNotifyMissed" to config.autoNotifyMissed,
                    "adminName" to config.adminName,
                    "adminEmail" to config.adminEmail,
                    "updatedAt" to System.currentTimeMillis()
                )
                fs.collection("app_config").document("settings")
                    .set(data, SetOptions.merge())
                    .await()
                _lastSyncTimestamp.value = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to push config to cloud: ${e.message}")
            }
        }
    }

    suspend fun pushAuditLog(log: AuditLog) {
        val fs = firestore ?: return
        val docId = "${log.id}_${log.timestamp}"
        withContext(Dispatchers.IO) {
            try {
                val data = hashMapOf(
                    "id" to log.id,
                    "timestamp" to log.timestamp,
                    "action" to log.action,
                    "description" to log.description,
                    "performedByRole" to log.performedByRole
                )
                fs.collection("audit_logs").document(docId)
                    .set(data, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to push audit log to cloud: ${e.message}")
            }
        }
    }

    suspend fun pushPaymentReminder(
        memberId: Long,
        memberName: String,
        monthYear: String,
        amount: Double
    ) {
        val fs = firestore ?: return
        withContext(Dispatchers.IO) {
            try {
                val data = hashMapOf(
                    "id" to java.util.UUID.randomUUID().toString(),
                    "memberId" to memberId,
                    "memberName" to memberName,
                    "monthYear" to monthYear,
                    "amount" to amount,
                    "timestamp" to System.currentTimeMillis()
                )
                fs.collection("payment_reminders").add(data).await()
                Log.d(TAG, "Pushed payment reminder for member $memberName ($memberId)")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to push payment reminder: ${e.message}")
            }
        }
    }

    suspend fun syncAllLocalToCloud(): Pair<Boolean, String> {
        val fs = firestore
        if (fs == null) {
            return Pair(false, "Firebase not configured. Please supply google-services.json to connect cloud database.")
        }

        return withContext(Dispatchers.IO) {
            try {
                _syncState.value = CloudSyncState.SYNCING
                _statusMessage.value = "Uploading all members & transactions to Cloud Firestore..."

                val members = memberDao.getAllMembersSync()
                val contributions = contributionDao.getAllContributionsSync()
                val config = configDao.getConfigSync()
                val logs = auditLogDao.getAllLogsSync()

                // Batch upload members
                for (m in members) {
                    val mData = hashMapOf(
                        "id" to m.id,
                        "name" to m.name,
                        "phone" to m.phone,
                        "email" to m.email,
                        "role" to m.role,
                        "joinDate" to m.joinDate,
                        "isActive" to m.isActive,
                        "notes" to m.notes,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    fs.collection("members").document(m.id.toString()).set(mData, SetOptions.merge()).await()
                }

                // Batch upload contributions
                for (c in contributions) {
                    val docId = "${c.memberId}_${c.monthYear}"
                    val cData = hashMapOf(
                        "id" to c.id,
                        "memberId" to c.memberId,
                        "monthYear" to c.monthYear,
                        "amount" to c.amount,
                        "status" to c.status,
                        "paymentMethod" to c.paymentMethod,
                        "transactionRef" to c.transactionRef,
                        "paidDate" to c.paidDate,
                        "verifiedByAdmin" to c.verifiedByAdmin,
                        "verifiedDate" to c.verifiedDate,
                        "remarks" to c.remarks,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    fs.collection("contributions").document(docId).set(cData, SetOptions.merge()).await()
                }

                // Upload config
                if (config != null) {
                    val cfgData = hashMapOf(
                        "id" to config.id,
                        "upiId" to config.upiId,
                        "upiName" to config.upiName,
                        "monthlyAmount" to config.monthlyAmount,
                        "adminPin" to config.adminPin,
                        "fundTitle" to config.fundTitle,
                        "contactPhone" to config.contactPhone,
                        "reminderDayOfMonth" to config.reminderDayOfMonth,
                        "autoNotifyMissed" to config.autoNotifyMissed,
                        "adminName" to config.adminName,
                        "adminEmail" to config.adminEmail,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    fs.collection("app_config").document("settings").set(cfgData, SetOptions.merge()).await()
                }

                // Upload recent audit logs
                for (l in logs.take(50)) {
                    val lData = hashMapOf(
                        "id" to l.id,
                        "timestamp" to l.timestamp,
                        "action" to l.action,
                        "description" to l.description,
                        "performedByRole" to l.performedByRole
                    )
                    fs.collection("audit_logs").document("${l.id}_${l.timestamp}").set(lData, SetOptions.merge()).await()
                }

                _lastSyncTimestamp.value = System.currentTimeMillis()
                _syncState.value = CloudSyncState.SYNCED
                _statusMessage.value = "Synced ${members.size} members, ${contributions.size} contributions to Cloud Database."
                Pair(true, "Successfully uploaded ${members.size} members and ${contributions.size} records to Central Firestore!")
            } catch (e: Exception) {
                val isPerm = (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) ||
                        e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
                if (isPerm) {
                    Log.w(TAG, "Sync failed: PERMISSION_DENIED. Firestore rules require write permission.")
                    _syncState.value = CloudSyncState.PERMISSION_REQUIRED
                    _statusMessage.value = "Firestore permission required. Update Rules in Firebase Console."
                    Pair(false, "Permission Denied: Configure Firestore Rules in your Firebase Console (see Settings for guide)")
                } else {
                    Log.w(TAG, "Error in syncAllLocalToCloud: ${e.message}")
                    _syncState.value = CloudSyncState.OFFLINE_CACHE
                    _statusMessage.value = "Sync failed: ${e.localizedMessage ?: "Unknown error"}"
                    Pair(false, "Cloud sync failed: ${e.localizedMessage ?: "Check connection"}")
                }
            }
        }
    }

    suspend fun fetchAllFromCloud(): Pair<Boolean, String> {
        val fs = firestore
        if (fs == null) {
            return Pair(false, "Firebase not configured.")
        }

        return withContext(Dispatchers.IO) {
            try {
                _syncState.value = CloudSyncState.SYNCING
                _statusMessage.value = "Downloading centralized records from Cloud Firestore..."

                val membersSnap = fs.collection("members").get().await()
                val members = membersSnap.documents.mapNotNull { docToMember(it) }
                if (members.isNotEmpty()) {
                    memberDao.insertMembers(members)
                }

                val contribsSnap = fs.collection("contributions").get().await()
                val contribs = contribsSnap.documents.mapNotNull { docToContribution(it) }
                if (contribs.isNotEmpty()) {
                    contributionDao.insertContributions(contribs)
                }

                val configSnap = fs.collection("app_config").document("settings").get().await()
                val cloudCfg = docToAppConfig(configSnap)
                if (cloudCfg != null) {
                    configDao.insertOrUpdate(cloudCfg)
                }

                _lastSyncTimestamp.value = System.currentTimeMillis()
                _syncState.value = CloudSyncState.SYNCED
                _statusMessage.value = "Downloaded ${members.size} members and ${contribs.size} contributions from Cloud."
                Pair(true, "Successfully pulled ${members.size} members and ${contribs.size} records from Cloud!")
            } catch (e: Exception) {
                val isPerm = (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) ||
                        e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
                if (isPerm) {
                    Log.w(TAG, "Fetch failed: PERMISSION_DENIED. Firestore rules require read permission.")
                    _syncState.value = CloudSyncState.PERMISSION_REQUIRED
                    _statusMessage.value = "Firestore permission required. Update Rules in Firebase Console."
                    Pair(false, "Permission Denied: Configure Firestore Rules in your Firebase Console (see Settings for guide)")
                } else {
                    Log.w(TAG, "Error in fetchAllFromCloud: ${e.message}")
                    _syncState.value = CloudSyncState.OFFLINE_CACHE
                    Pair(false, "Download failed: ${e.localizedMessage ?: "Check connection"}")
                }
            }
        }
    }

    private fun docToMember(doc: DocumentSnapshot): Member? {
        return try {
            val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return null
            val name = doc.getString("name") ?: "Member #$id"
            val phone = doc.getString("phone") ?: ""
            val email = doc.getString("email") ?: ""
            val role = doc.getString("role") ?: "MEMBER"
            val joinDate = doc.getLong("joinDate") ?: System.currentTimeMillis()
            val isActive = doc.getBoolean("isActive") ?: true
            val notes = doc.getString("notes") ?: ""
            Member(
                id = id,
                name = name,
                phone = phone,
                email = email,
                role = role,
                joinDate = joinDate,
                isActive = isActive,
                notes = notes
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun docToContribution(doc: DocumentSnapshot): Contribution? {
        return try {
            val id = doc.getLong("id") ?: 0L
            val memberId = doc.getLong("memberId") ?: return null
            val monthYear = doc.getString("monthYear") ?: return null
            val amount = doc.getDouble("amount") ?: 0.0
            val status = doc.getString("status") ?: Contribution.STATUS_PENDING
            val method = doc.getString("paymentMethod") ?: Contribution.METHOD_NONE
            val ref = doc.getString("transactionRef") ?: ""
            val paidDate = doc.getLong("paidDate")
            val verified = doc.getBoolean("verifiedByAdmin") ?: false
            val verifiedDate = doc.getLong("verifiedDate")
            val remarks = doc.getString("remarks") ?: ""
            Contribution(
                id = id,
                memberId = memberId,
                monthYear = monthYear,
                amount = amount,
                status = status,
                paymentMethod = method,
                transactionRef = ref,
                paidDate = paidDate,
                verifiedByAdmin = verified,
                verifiedDate = verifiedDate,
                remarks = remarks
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun docToAppConfig(doc: DocumentSnapshot): AppConfig? {
        return try {
            if (!doc.exists()) return null
            AppConfig(
                id = 1,
                upiId = doc.getString("upiId") ?: "",
                upiName = doc.getString("upiName") ?: "Welfare Fund",
                monthlyAmount = doc.getDouble("monthlyAmount") ?: 0.0,
                adminPin = doc.getString("adminPin") ?: "170588",
                fundTitle = doc.getString("fundTitle") ?: "Welfare Fund",
                contactPhone = doc.getString("contactPhone") ?: "",
                reminderDayOfMonth = (doc.getLong("reminderDayOfMonth") ?: 1L).toInt(),
                autoNotifyMissed = doc.getBoolean("autoNotifyMissed") ?: true,
                adminName = doc.getString("adminName") ?: "Admin",
                adminEmail = doc.getString("adminEmail") ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun docToAuditLog(doc: DocumentSnapshot): AuditLog? {
        return try {
            val id = doc.getLong("id") ?: 0L
            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
            val action = doc.getString("action") ?: "ACTIVITY"
            val description = doc.getString("description") ?: ""
            val performedByRole = doc.getString("performedByRole") ?: "MEMBER"
            AuditLog(
                id = id,
                timestamp = timestamp,
                action = action,
                description = description,
                performedByRole = performedByRole
            )
        } catch (e: Exception) {
            null
        }
    }
}
