package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class StrokePath(
    val points: List<Pair<Float, Float>>,
    val color: Color = Color.Black,
    val strokeWidth: Float = 4f
)

@Composable
fun SignaturePad(
    title: String,
    subtitle: String? = null,
    onSignatureChanged: (Bitmap?) -> Unit,
    modifier: Modifier = Modifier
) {
    val strokes = remember { mutableStateListOf<StrokePath>() }
    val currentPoints = remember { mutableStateListOf<Pair<Float, Float>>() }

    fun updateBitmapExport() {
        if (strokes.isEmpty()) {
            onSignatureChanged(null)
            return
        }
        val width = 300
        val height = 150
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(15, 76, 76)
            strokeWidth = 5f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        for (stroke in strokes) {
            if (stroke.points.size > 1) {
                val path = AndroidPath()
                val scaleX = width / 300f
                val scaleY = height / 150f
                path.moveTo(stroke.points[0].first * scaleX, stroke.points[0].second * scaleY)
                for (i in 1 until stroke.points.size) {
                    path.lineTo(stroke.points[i].first * scaleX, stroke.points[i].second * scaleY)
                }
                canvas.drawPath(path, paint)
            }
        }
        onSignatureChanged(bitmap)
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                IconButton(
                    onClick = {
                        if (strokes.isNotEmpty()) {
                            strokes.removeAt(strokes.lastIndex)
                            updateBitmapExport()
                        }
                    },
                    enabled = strokes.isNotEmpty(),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = "Undo Goresan",
                        tint = if (strokes.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(
                    onClick = {
                        strokes.clear()
                        currentPoints.clear()
                        updateBitmapExport()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Hapus Tanda Tangan",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFFAFAFA))
                .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPoints.clear()
                            currentPoints.add(offset.x to offset.y)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentPoints.add(change.position.x to change.position.y)
                        },
                        onDragEnd = {
                            if (currentPoints.isNotEmpty()) {
                                strokes.add(StrokePath(points = currentPoints.toList()))
                                currentPoints.clear()
                                updateBitmapExport()
                            }
                        },
                        onDragCancel = {
                            currentPoints.clear()
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                // Draw existing strokes
                for (stroke in strokes) {
                    if (stroke.points.size > 1) {
                        val path = Path()
                        path.moveTo(stroke.points[0].first, stroke.points[0].second)
                        for (i in 1 until stroke.points.size) {
                            path.lineTo(stroke.points[i].first, stroke.points[i].second)
                        }
                        drawPath(
                            path = path,
                            color = Color(0xFF0F4C4C),
                            style = Stroke(width = stroke.strokeWidth)
                        )
                    }
                }

                // Draw stroke currently being drawn
                if (currentPoints.size > 1) {
                    val currentPath = Path()
                    currentPath.moveTo(currentPoints[0].first, currentPoints[0].second)
                    for (i in 1 until currentPoints.size) {
                        currentPath.lineTo(currentPoints[i].first, currentPoints[i].second)
                    }
                    drawPath(
                        path = currentPath,
                        color = Color(0xFF00796B),
                        style = Stroke(width = 4f)
                    )
                }
            }

            if (strokes.isEmpty() && currentPoints.isEmpty()) {
                Text(
                    text = "Goreskan tanda tangan di sini...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
