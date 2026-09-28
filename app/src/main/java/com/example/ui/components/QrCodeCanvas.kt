package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.util.UpiHelper

@Composable
fun QrCodeCanvas(
    content: String,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    qrColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    val matrix = remember(content) {
        UpiHelper.generateDeterministicQrMatrix(content, size = 25)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size - 16.dp)) {
            val cellWidth = this.size.width / matrix.size
            val cellHeight = this.size.height / matrix.size

            for (r in matrix.indices) {
                for (c in matrix[r].indices) {
                    if (matrix[r][c]) {
                        drawRect(
                            color = qrColor,
                            topLeft = Offset(c * cellWidth, r * cellHeight),
                            size = Size(cellWidth, cellHeight)
                        )
                    }
                }
            }
        }
    }
}
