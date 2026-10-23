package com.nestvest.profile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nestvest.R
import com.nestvest.profile.domain.AppearanceCatalog
import com.nestvest.profile.ui.theme.Ink
import com.nestvest.profile.ui.theme.SoftSage

fun avatarLabel(id: String?): Int = when (id) {
    "leaf" -> R.string.avatar_leaf
    "home" -> R.string.avatar_home
    "sun" -> R.string.avatar_sun
    else -> R.string.appearance_default
}

fun colorLabel(id: String?): Int = when (id) {
    "sage" -> R.string.color_sage
    "sand" -> R.string.color_sand
    "sky" -> R.string.color_sky
    else -> R.string.appearance_default
}

@Composable
fun AppearancePreview(name: String, avatar: String?, color: String?) {
    val background = when (color) {
        "sand" -> Color(0xFFEADFC9)
        "sky" -> Color(0xFFD4DFE4)
        else -> SoftSage
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = CircleShape, color = background, contentColor = Ink) {
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                if (avatar == null || avatar !in AppearanceCatalog.avatars) {
                    Text(name.take(1), style = MaterialTheme.typography.headlineMedium)
                } else Canvas(Modifier.size(32.dp)) {
                    val stroke = Stroke(width = 2.dp.toPx())
                    when (avatar) {
                        "leaf" -> {
                            drawOval(Ink, topLeft = Offset(size.width * .15f, 0f),
                                size = Size(size.width * .7f, size.height), style = stroke)
                            drawLine(Ink, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = stroke.width)
                        }
                        "home" -> {
                            drawLine(Ink, Offset(0f, size.height / 2), Offset(size.width / 2, 0f), stroke.width)
                            drawLine(Ink, Offset(size.width / 2, 0f), Offset(size.width, size.height / 2), stroke.width)
                            drawRect(Ink, Offset(size.width * .2f, size.height * .4f),
                                Size(size.width * .6f, size.height * .6f), style = stroke)
                        }
                        "sun" -> {
                            drawCircle(Ink, radius = size.minDimension * .3f, style = stroke)
                            for (i in 0..7) {
                                val angle = i * Math.PI / 4
                                val direction = Offset(kotlin.math.cos(angle).toFloat(), kotlin.math.sin(angle).toFloat())
                                drawLine(Ink, center + direction * size.minDimension * .4f,
                                    center + direction * size.minDimension * .5f, stroke.width)
                            }
                        }
                    }
                }
            }
        }
        Column {
            if (avatar != null) Text(stringResource(avatarLabel(avatar)))
            if (color != null) Text(stringResource(colorLabel(color)))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAppearanceSheet(name: String, selection: AppearanceSelection, accept: (ProfileIntent) -> Unit) {
    ModalBottomSheet(onDismissRequest = { accept(ProfileIntent.DismissAppearance) }) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.appearance_title), style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.appearance_intro))
            AppearancePreview(name, selection.avatarId, selection.colorId)
            Text(stringResource(R.string.appearance_avatars))
            for (id in listOf(null) + AppearanceCatalog.avatars) {
                AppearanceOption(stringResource(avatarLabel(id)), id == selection.avatarId) {
                    accept(ProfileIntent.AvatarChanged(id))
                }
            }
            Text(stringResource(R.string.appearance_colors))
            for (id in listOf(null) + AppearanceCatalog.colors) {
                AppearanceOption(stringResource(colorLabel(id)), id == selection.colorId) {
                    accept(ProfileIntent.ColorChanged(id))
                }
            }
            Button(onClick = { accept(ProfileIntent.ApplyAppearance) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.appearance_apply)) }
            TextButton(onClick = { accept(ProfileIntent.SkipAppearance) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.appearance_skip)) }
        }
    }
}

@Composable
private fun AppearanceOption(label: String, selected: Boolean, choose: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
        .selectable(selected = selected, role = Role.RadioButton, onClick = choose),
        verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = null)
        Text(label)
    }
}
