package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.MemberWithContribution

@Composable
fun RecordPaymentDialog(
    item: MemberWithContribution,
    monthYear: String,
    isAdminMode: Boolean,
    defaultAmount: Double,
    onDismiss: () -> Unit,
    onSubmitUpi: (memberId: Long, amount: Double, utr: String) -> Unit,
    onSubmitCash: (memberId: Long, amount: Double, note: String) -> Unit,
    onAdminDirectRecord: (memberId: Long, amount: Double, method: String, ref: String, remarks: String) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("UPI") } // "UPI" or "CASH"
    var amountText by remember { mutableStateOf(defaultAmount.toInt().toString()) }
    var refText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Record Contribution",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${item.member.name} • Month: $monthYear",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select Payment Mode:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedMethod == "UPI",
                        onClick = { selectedMethod = "UPI" },
                        label = { Text("UPI Online") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.testTag("method_upi_chip")
                    )

                    FilterChip(
                        selected = selectedMethod == "CASH",
                        onClick = { selectedMethod = "CASH" },
                        label = { Text("Cash Payment") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Money,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.testTag("method_cash_chip")
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Contribution Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input")
                )

                if (selectedMethod == "UPI") {
                    OutlinedTextField(
                        value = refText,
                        onValueChange = { refText = it },
                        label = { Text("UPI UTR / Transaction ID") },
                        placeholder = { Text("e.g. 260904812345") },
                        singleLine = true,
                        supportingText = { Text("From GPay, PhonePe, Paytm, etc.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upi_ref_input")
                    )
                } else {
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Cash Details / Handover Note") },
                        placeholder = { Text("e.g. Handed cash to Mayan at welfare meeting") },
                        maxLines = 2,
                        supportingText = {
                            if (isAdminMode) {
                                Text("Admin mode: will be instantly marked as verified.")
                            } else {
                                Text("Will be submitted for Admin manual verification.")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cash_note_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: defaultAmount
                    if (isAdminMode) {
                        onAdminDirectRecord(
                            item.member.id,
                            amount,
                            selectedMethod,
                            if (selectedMethod == "UPI") refText else "Cash Verified by Admin",
                            if (selectedMethod == "UPI") "Direct UPI log" else noteText
                        )
                    } else {
                        if (selectedMethod == "UPI") {
                            onSubmitUpi(item.member.id, amount, refText)
                        } else {
                            onSubmitCash(item.member.id, amount, noteText)
                        }
                    }
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_record_payment_button")
            ) {
                Text(
                    if (isAdminMode) "Verify & Save"
                    else if (selectedMethod == "CASH") "Submit Cash for Verification"
                    else "Confirm UPI Payment"
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
