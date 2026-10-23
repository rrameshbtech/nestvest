package com.nestvest.expense.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nestvest.R

internal object ExpenseStyle {
    val Ink = Color(0xFF202521)
    val Muted = Color(0xFF626A62)
    val Paper = Color(0xFFFAFBF7)
    val Line = Color(0xFFDCE1D8)
    val Green = Color(0xFF3D5844)
    val Pale = Color(0xFFE7EEE2)
    val Note = Color(0xFFF4F7F1)
    val NoteBorder = Color(0xFFCBD5C5)
    val ControlBorder = Color(0xFFB8C1B2)
    val Error = Color(0xFF793E2D)
    val ButtonShape = RoundedCornerShape(8.dp)
}

@Composable
internal fun ExpenseScaffold(
    footer: @Composable ColumnScope.() -> Unit,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(
        primary = ExpenseStyle.Green, onPrimary = Color.White,
        primaryContainer = ExpenseStyle.Pale, onPrimaryContainer = ExpenseStyle.Ink,
        background = ExpenseStyle.Paper, onBackground = ExpenseStyle.Ink,
        surface = ExpenseStyle.Paper, onSurface = ExpenseStyle.Ink,
        surfaceVariant = ExpenseStyle.Pale, onSurfaceVariant = ExpenseStyle.Muted,
        outline = ExpenseStyle.ControlBorder, outlineVariant = ExpenseStyle.Line,
        error = ExpenseStyle.Error,
    )) {
        Surface(color = ExpenseStyle.Paper, contentColor = ExpenseStyle.Ink) {
            Column(Modifier.fillMaxSize().imePadding().testTag("expense-screen")) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp)
                    .heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExpenseGlyph(ExpenseSymbol.HOME, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.app_name), fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    if (actions != null) actions() else ExpenseLocalCue()
                }
                HorizontalDivider(color = ExpenseStyle.Line)
                Column(Modifier.weight(1f).fillMaxWidth(), content = content)
                Column(Modifier.fillMaxWidth().testTag("expense-footer")
                    .background(ExpenseStyle.Paper).padding(horizontal = 22.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, content = footer)
            }
        }
    }
}

@Composable
internal fun ExpenseLocalCue() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Canvas(Modifier.size(6.dp)) { drawCircle(ExpenseStyle.Green) }
        Text(stringResource(R.string.expense_on_device), fontSize = 10.sp, color = ExpenseStyle.Green)
    }
}

@Composable
internal fun ExpenseEyebrow(text: String) {
    Text(text, fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.4.sp, color = ExpenseStyle.Muted)
}

@Composable
internal fun ExpenseHeading(text: String) {
    Text(text, fontSize = 29.sp, lineHeight = 33.sp, letterSpacing = (-1).sp,
        fontWeight = FontWeight.Medium)
}

@Composable
internal fun ExpenseFootnote(text: String) {
    Text(text, fontSize = 11.sp, lineHeight = 16.sp, color = ExpenseStyle.Muted,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
}

@Composable
internal fun ExpensePrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, shape = ExpenseStyle.ButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = ExpenseStyle.Ink, contentColor = Color.White),
        modifier = Modifier.fillMaxWidth().heightIn(min = 49.dp)) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun ExpenseSecondaryButton(text: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(onClick = onClick, enabled = enabled, shape = ExpenseStyle.ButtonShape,
        border = BorderStroke(1.dp, ExpenseStyle.ControlBorder),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseStyle.Ink),
        modifier = modifier.heightIn(min = 48.dp)) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

internal enum class ExpenseSymbol { HOME, NEEDS, WANTS, CULTURE, UNEXPECTED, RECEIPT, PLUS, ARROW, DELETE, LOCK }

@Composable
internal fun ExpenseGlyph(symbol: ExpenseSymbol, modifier: Modifier = Modifier,
    color: Color = ExpenseStyle.Green) {
    Canvas(modifier.size(20.dp)) {
        withTransform({ scale(size.width / 24f, size.height / 24f, Offset.Zero) }) {
            fun line(x: Float, y: Float, endX: Float, endY: Float) =
                drawLine(color, Offset(x, y), Offset(endX, endY), strokeWidth = 1.7f)
            val path = Path()
            when (symbol) {
                ExpenseSymbol.HOME -> {
                    path.moveTo(3f, 11f); path.lineTo(12f, 3f); path.lineTo(21f, 11f)
                    path.moveTo(6f, 9f); path.lineTo(6f, 21f); path.lineTo(18f, 21f); path.lineTo(18f, 9f)
                }
                ExpenseSymbol.NEEDS -> {
                    path.moveTo(5f, 8f); path.lineTo(19f, 8f); path.lineTo(20f, 21f)
                    path.lineTo(4f, 21f); path.close()
                    path.moveTo(9f, 9f); path.lineTo(9f, 5f); path.quadraticTo(12f, 1f, 15f, 5f); path.lineTo(15f, 9f)
                }
                ExpenseSymbol.WANTS -> {
                    path.moveTo(12f, 21f); path.cubicTo(8f, 17f, 2f, 12f, 3f, 7f)
                    path.cubicTo(4f, 2f, 10f, 2f, 12f, 7f)
                    path.cubicTo(14f, 2f, 20f, 2f, 21f, 7f)
                    path.cubicTo(22f, 12f, 16f, 17f, 12f, 21f)
                }
                ExpenseSymbol.CULTURE -> {
                    path.moveTo(12f, 6f); path.quadraticTo(8f, 3f, 3f, 5f); path.lineTo(3f, 19f)
                    path.quadraticTo(8f, 17f, 12f, 20f); path.quadraticTo(16f, 17f, 21f, 19f)
                    path.lineTo(21f, 5f); path.quadraticTo(16f, 3f, 12f, 6f); path.lineTo(12f, 20f)
                }
                ExpenseSymbol.UNEXPECTED -> {
                    path.moveTo(14f, 2f); path.lineTo(5f, 14f); path.lineTo(11f, 14f)
                    path.lineTo(10f, 22f); path.lineTo(19f, 10f); path.lineTo(13f, 10f); path.close()
                }
                ExpenseSymbol.RECEIPT -> {
                    path.moveTo(5f, 3f); path.lineTo(19f, 3f); path.lineTo(19f, 21f)
                    path.lineTo(15f, 19f); path.lineTo(12f, 21f); path.lineTo(9f, 19f); path.lineTo(5f, 21f); path.close()
                    line(8f, 8f, 16f, 8f); line(8f, 12f, 16f, 12f)
                }
                ExpenseSymbol.PLUS -> { line(4f, 12f, 20f, 12f); line(12f, 4f, 12f, 20f) }
                ExpenseSymbol.ARROW -> {
                    line(4f, 12f, 20f, 12f)
                    path.moveTo(12f, 5f); path.lineTo(20f, 12f); path.lineTo(12f, 19f)
                }
                ExpenseSymbol.DELETE -> {
                    path.moveTo(9f, 5f); path.lineTo(21f, 5f); path.lineTo(21f, 19f); path.lineTo(9f, 19f)
                    path.lineTo(2f, 12f); path.close()
                    line(12f, 9f, 18f, 15f); line(18f, 9f, 12f, 15f)
                }
                ExpenseSymbol.LOCK -> {
                    path.moveTo(5f, 10f); path.lineTo(19f, 10f); path.lineTo(19f, 21f)
                    path.lineTo(5f, 21f); path.close()
                    path.moveTo(8f, 10f); path.lineTo(8f, 6f)
                    path.cubicTo(8f, 1f, 16f, 1f, 16f, 6f); path.lineTo(16f, 10f)
                }
            }
            drawPath(path, color, style = Stroke(width = 1.7f))
        }
    }
}
