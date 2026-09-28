package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object UpiHelper {

    fun buildUpiUri(
        upiId: String,
        name: String,
        amount: Double,
        note: String
    ): Uri {
        return Uri.parse(
            "upi://pay"
        ).buildUpon()
            .appendQueryParameter("pa", upiId)
            .appendQueryParameter("pn", name)
            .appendQueryParameter("am", String.format(java.util.Locale.US, "%.2f", amount))
            .appendQueryParameter("cu", "INR")
            .appendQueryParameter("tn", note)
            .build()
    }

    fun launchUpiPayment(
        context: Context,
        upiId: String,
        name: String,
        amount: Double,
        note: String
    ): Boolean {
        val upiUri = buildUpiUri(upiId, name, amount, note)
        val intent = Intent(Intent.ACTION_VIEW, upiUri)
        val chooser = Intent.createChooser(intent, "Pay ₹${amount.toInt()} with UPI")

        return try {
            context.startActivity(chooser)
            true
        } catch (_: Exception) {
            copyToClipboard(context, upiId, "UPI ID")
            Toast.makeText(
                context,
                "No UPI app found. UPI ID copied to clipboard: $upiId",
                Toast.LENGTH_LONG
            ).show()
            false
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Text") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied: $text", Toast.LENGTH_SHORT).show()
    }

    /**
     * Generates a 25x25 boolean matrix resembling a QR Code for rendering on Canvas.
     * Features standard finder patterns in 3 corners, timing strips, and deterministic encoded bits.
     */
    fun generateDeterministicQrMatrix(content: String, size: Int = 25): Array<BooleanArray> {
        val matrix = Array(size) { BooleanArray(size) { false } }

        fun drawFinder(r0: Int, c0: Int) {
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isInner = r in 2..4 && c in 2..4
                    matrix[r0 + r][c0 + c] = isBorder || isInner
                }
            }
        }

        // 3 Corner Finders
        drawFinder(0, 0)
        drawFinder(0, size - 7)
        drawFinder(size - 7, 0)

        // Timing patterns
        for (i in 7 until size - 7) {
            matrix[6][i] = (i % 2 == 0)
            matrix[i][6] = (i % 2 == 0)
        }

        // Fill remaining data cells deterministically using hash
        var hash = content.hashCode()
        for (r in 0 until size) {
            for (c in 0 until size) {
                // Skip finders and separators
                val inTopLeft = r < 8 && c < 8
                val inTopRight = r < 8 && c >= size - 8
                val inBottomLeft = r >= size - 8 && c < 8
                val isTiming = (r == 6) || (c == 6)

                if (!inTopLeft && !inTopRight && !inBottomLeft && !isTiming) {
                    hash = (hash * 31 + (r * 17) + (c * 23)) xor (r + c)
                    matrix[r][c] = (hash and 1) == 1
                }
            }
        }

        return matrix
    }
}
