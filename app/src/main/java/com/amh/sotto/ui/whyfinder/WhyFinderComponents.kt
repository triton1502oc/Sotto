package com.amh.sotto.ui.whyfinder

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amh.sotto.R
import com.amh.sotto.data.BodyRegions
import com.amh.sotto.theme.SottoColors

@Composable
internal fun BodyPartSelectionGrid(
    onSelectPart: (String) -> Unit,
    onNotSure: () -> Unit,
    onStop: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(BodyRegions.ALL) { regionKey ->
                Surface(
                    onClick = { onSelectPart(regionKey) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getBodyRegionLabel(regionKey, context),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onNotSure,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.why_finder_btn_not_sure))
            }
            Button(
                onClick = onStop,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SottoColors.NeutralDarkContainer)
            ) {
                Text(stringResource(R.string.why_finder_btn_stop))
            }
        }
    }
}

@Composable
internal fun IntensityScaleRow(
    onSelectLevel: (Int) -> Unit,
    onNotSure: () -> Unit,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val colors = listOf(
                SottoColors.IntensityMild,
                SottoColors.IntensityLight,
                SottoColors.IntensityModerate,
                SottoColors.IntensityHigh,
                SottoColors.IntensitySevere
            )
            for (level in 1..5) {
                Button(
                    onClick = { onSelectLevel(level) },
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors[level - 1])
                ) {
                    Text(
                        text = "$level",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onNotSure,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.why_finder_btn_not_sure))
            }
            Button(
                onClick = onStop,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SottoColors.NeutralDarkContainer)
            ) {
                Text(stringResource(R.string.why_finder_btn_stop))
            }
        }
    }
}

fun getBodyRegionLabel(regionKey: String, context: Context): String {
    return when (regionKey) {
        BodyRegions.HEAD -> context.getString(R.string.body_region_head)
        BodyRegions.EYES -> context.getString(R.string.body_region_eyes)
        BodyRegions.EARS -> context.getString(R.string.body_region_ears)
        BodyRegions.TEETH -> context.getString(R.string.body_region_teeth)
        BodyRegions.THROAT -> context.getString(R.string.body_region_throat)
        BodyRegions.CHEST -> context.getString(R.string.body_region_chest)
        BodyRegions.STOMACH -> context.getString(R.string.body_region_stomach)
        BodyRegions.BACK -> context.getString(R.string.body_region_back)
        BodyRegions.ARMS_HANDS -> context.getString(R.string.body_region_arms_hands)
        BodyRegions.LEGS_FEET -> context.getString(R.string.body_region_legs_feet)
        BodyRegions.SKIN -> context.getString(R.string.body_region_skin)
        else -> regionKey
    }
}
