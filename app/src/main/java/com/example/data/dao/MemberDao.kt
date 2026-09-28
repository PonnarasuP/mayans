package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Member
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members ORDER BY name ASC")
    fun getAllMembers(): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveMembers(): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE id = :id LIMIT 1")
    fun getMemberById(id: Long): Flow<Member?>

    @Query("SELECT * FROM members WHERE id = :id LIMIT 1")
    suspend fun getMemberByIdSync(id: Long): Member?

    @Query("SELECT COUNT(*) FROM members WHERE isActive = 1")
    fun getActiveMemberCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Member): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<Member>)

    @Query("SELECT * FROM members ORDER BY name ASC")
    suspend fun getAllMembersSync(): List<Member>

    @Query("DELETE FROM members WHERE id = :id")
    suspend fun deleteMemberById(id: Long)

    @Update
    suspend fun updateMember(member: Member)

    @Delete
    suspend fun deleteMember(member: Member)
}
