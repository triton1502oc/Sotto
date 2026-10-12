package com.amh.sotto.ui.pip

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amh.sotto.R
import com.amh.sotto.theme.SottoColors

@Composable
fun PipCommunicatorScreen(
    lastSpokenText: String?,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: App name & live speaking/ready badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.app_name).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSpeaking) MaterialTheme.colorScheme.primaryContainer else SottoColors.SuccessContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(if (isSpeaking) R.drawable.ic_volume_up else R.drawable.ic_check_bold),
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = if (isSpeaking) MaterialTheme.colorScheme.onPrimaryContainer else SottoColors.SuccessText
                        )
                        Text(
                            text = if (isSpeaking) stringResource(R.string.pip_status_speaking) else stringResource(R.string.pip_status_ready),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSpeaking) MaterialTheme.colorScheme.onPrimaryContainer else SottoColors.SuccessText
                        )
                    }
                }
            }

            // Center: High-contrast last spoken phrase
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = lastSpokenText ?: stringResource(R.string.pip_status_ready),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (lastSpokenText != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bottom Guide: System RemoteActions indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PipActionGuideBadge(iconRes = R.drawable.ic_check_bold, label = stringResource(R.string.pip_action_yes))
                PipActionGuideBadge(iconRes = R.drawable.ic_close_bold, label = stringResource(R.string.pip_action_no))
                PipActionGuideBadge(iconRes = R.drawable.ic_hourglass, label = stringResource(R.string.pip_action_wait))
            }
        }
    }
}

@Composable
private fun PipActionGuideBadge(
    iconRes: Int,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
