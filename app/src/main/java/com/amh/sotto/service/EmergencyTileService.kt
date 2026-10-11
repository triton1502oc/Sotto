package com.amh.sotto.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.amh.sotto.MainActivity
import com.amh.sotto.R
import com.amh.sotto.util.UsabilityTracker

/**
 * Quick Settings Tile enabling <3s direct access to Partner Mode / Crisis Safety Card
 * directly from the Android notification shade.
 */
class EmergencyTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = getString(R.string.tile_partner_mode)
            contentDescription = getString(R.string.tile_partner_mode)
            icon = android.graphics.drawable.Icon.createWithResource(this@EmergencyTileService, R.drawable.ic_qs_sotto_partner)
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        UsabilityTracker.recordActivation(this, "emergency_tile")

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_PARTNER_MODE, true)
        }

        val launchAction = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }

        if (isLocked) {
            unlockAndRun {
                launchAction()
            }
        } else {
            launchAction()
        }
    }

    companion object {
        const val EXTRA_OPEN_PARTNER_MODE = "com.amh.sotto.extra.OPEN_PARTNER_MODE"
    }
}
