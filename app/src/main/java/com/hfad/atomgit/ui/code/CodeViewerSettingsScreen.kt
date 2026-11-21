package com.hfad.atomgit.ui.code

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hfad.atomgit.data.model.CodeFontSizeSteps
import com.hfad.atomgit.ui.components.CODE_PREVIEW_SAMPLE
import com.hfad.atomgit.ui.components.CodeBlock
import com.hfad.atomgit.ui.profile.Pressable
import com.hfad.atomgit.ui.theme.Ink
import com.hfad.atomgit.ui.theme.InkMuted
import com.hfad.atomgit.ui.theme.Paper
import kotlin.math.roundToInt

@Composable
fun CodeViewerSettingsScreen(
    onBack: () -> Unit,
    onOpenSample: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CodeViewerSettingsViewModel = hiltViewModel(),
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val pageBackground = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(pageBackground)
            .testTag("screen-code-viewer-settings"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("code-settings-back")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "返回",
                )
            }
            Text(
                text = "代码",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    CodePreferenceSwitch(
                        title = "显示行号",
                        checked = preferences.showLineNumbers,
                        onCheckedChange = viewModel::setShowLineNumbers,
                        testTag = "code-pref-line-numbers",
                    )
                    CodePreferenceSwitch(
                        title = "始终使用深色主题",
                        checked = preferences.alwaysDarkTheme,
                        onCheckedChange = viewModel::setAlwaysDarkTheme,
                        testTag = "code-pref-always-dark",
                    )
                    CodePreferenceSwitch(
                        title = "覆盖系统字体大小",
                        checked = preferences.overrideSystemFontSize,
                        onCheckedChange = viewModel::setOverrideSystemFontSize,
                        testTag = "code-pref-override-font",
                        showDivider = !preferences.overrideSystemFontSize,
                    )
                    if (preferences.overrideSystemFontSize) {
                        CodeFontSizeSlider(
                            fontSizeSp = preferences.resolvedFontSizeSp,
                            onFontSizeSpChange = viewModel::setFontSizeSp,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    CodePreferenceSwitch(
                        title = "换行",
                        checked = preferences.wordWrap,
                        onCheckedChange = viewModel::setWordWrap,
                        testTag = "code-pref-word-wrap",
                        showDivider = false,
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
            ) {
                Pressable(
                    onClick = onOpenSample,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("code-settings-open-sample"),
                ) {
                    Text(
                        text = "打开示例代码",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "预览",
                        modifier = Modifier.padding(
                            start = 16.dp,
                            top = 12.dp,
                            end = 16.dp,
                            bottom = 8.dp,
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                    ) {
                        CodeBlock(
                            code = CODE_PREVIEW_SAMPLE,
                            preferences = preferences,
                            path = "Sample.kt",
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("code-settings-preview"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CodePreferenceSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    showDivider: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        val accent = MaterialTheme.colorScheme.primary
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Paper,
                checkedTrackColor = accent,
                checkedBorderColor = accent,
                uncheckedThumbColor = Paper,
                uncheckedTrackColor = Color(0xFF9E9E9E),
                uncheckedBorderColor = InkMuted,
                uncheckedIconColor = InkMuted,
            ),
        )
    }
    if (showDivider) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CodeFontSizeSlider(
    fontSizeSp: Int,
    onFontSizeSpChange: (Int) -> Unit,
) {
    val stepIndex = CodeFontSizeSteps.indexOf(fontSizeSp).coerceAtLeast(0)
    val accent = MaterialTheme.colorScheme.primary
    val sliderColors = SliderDefaults.colors(
        thumbColor = accent,
        activeTrackColor = accent,
        inactiveTrackColor = Color(0xFFBDBDBD),
        activeTickColor = Color.Transparent,
        inactiveTickColor = Color.Transparent,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
            .testTag("code-pref-font-size"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "字体大小",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${fontSizeSp}sp",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "A",
                fontSize = 12.sp,
                color = InkMuted,
                modifier = Modifier.padding(end = 8.dp),
            )
            Slider(
                value = stepIndex.toFloat(),
                onValueChange = { value ->
                    val index = value.roundToInt().coerceIn(0, CodeFontSizeSteps.lastIndex)
                    onFontSizeSpChange(CodeFontSizeSteps[index])
                },
                valueRange = 0f..CodeFontSizeSteps.lastIndex.toFloat(),
                steps = (CodeFontSizeSteps.size - 2).coerceAtLeast(0),
                colors = sliderColors,
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .shadow(2.dp, CircleShape)
                            .border(2.dp, Paper, CircleShape)
                            .background(accent, CircleShape),
                    )
                },
                track = { state ->
                    SliderDefaults.Track(
                        sliderState = state,
                        modifier = Modifier.height(6.dp),
                        colors = sliderColors,
                        // 不画档位小点，只保留吸附，避免边端蓝点
                        drawStopIndicator = null,
                        thumbTrackGapSize = 0.dp,
                        trackInsideCornerSize = 0.dp,
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("code-pref-font-size-slider"),
            )
            Text(
                text = "A",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
