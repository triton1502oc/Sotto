package com.amh.sotto.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.amh.sotto.R
import java.io.File
import java.io.FileOutputStream

/**
 * Specification for rendering and sharing a high-contrast Sotto AAC visual card.
 */
data class ShareableCardSpec(
    val primaryText: String,
    val secondaryText: String? = null,
    val badgeLabel: String? = null,
    val isEmergency: Boolean = false,
    val footerBrandText: String = DEFAULT_BRAND_FOOTER
) {
    companion object {
        const val DEFAULT_BRAND_FOOTER = "Sotto • Assistive Communication"
    }
}

/**
 * Renders a clean, zero-distraction 1080x1080 PNG card with subtle Sotto icon branding
 * and shares it via Android's native Share Sheet (Intent.ACTION_SEND) with a plain-text caption.
 */
object CardShareHelper {

    private const val CANVAS_SIZE = 1080
    private const val OUTER_MARGIN = 48f
    private const val CARD_CORNER_RADIUS = 48f
    private const val INNER_PADDING_HORIZONTAL = 80f
    private const val SHARED_DIR_NAME = "shared_cards"
    private const val SHARED_FILE_NAME = "sotto_card.png"

    // Low-stimulus, high-contrast dark palette
    private const val COLOR_OUTER_BG = 0xFF0D0F12.toInt()
    private const val COLOR_CARD_BG = 0xFF161920.toInt()
    private const val COLOR_CARD_BORDER = 0xFF2A303C.toInt()
    private const val COLOR_EMERGENCY_BG = 0xFF2A1215.toInt()
    private const val COLOR_EMERGENCY_BORDER = 0xFFFF5252.toInt()
    private const val COLOR_EMERGENCY_BADGE_BG = 0x33FF5252
    private const val COLOR_STANDARD_BADGE_BG = 0xFF232834.toInt()
    private const val COLOR_PRIMARY_TEXT = 0xFFF3F6FA.toInt()
    private const val COLOR_EMERGENCY_TEXT = 0xFFFFEBEE.toInt()
    private const val COLOR_SECONDARY_TEXT = 0xFF90CAF9.toInt()
    private const val COLOR_DIVIDER = 0xFF262C36.toInt()
    private const val COLOR_FOOTER_TEXT = 0xFF8B95A5.toInt()
    private const val COLOR_BRAND_BADGE_BG = 0xFF222733.toInt()

    /**
     * Builds the accessible plain-text caption attached as [Intent.EXTRA_TEXT] alongside the card image.
     */
    fun buildShareCaption(spec: ShareableCardSpec): String {
        val primary = spec.primaryText.trim()
        if (primary.isEmpty()) return ""

        val sb = StringBuilder()
        if (!spec.badgeLabel.isNullOrBlank()) {
            val prefix = if (spec.isEmergency) "🛡️ ${spec.badgeLabel.trim()}" else spec.badgeLabel.trim()
            sb.append("[").append(prefix).append("]\n")
        }
        sb.append(primary)
        val secondary = spec.secondaryText?.trim().orEmpty()
        if (secondary.isNotEmpty() && !secondary.equals(primary, ignoreCase = true)) {
            sb.append("\n\n").append(secondary)
        }
        return sb.toString()
    }

    /**
     * Selects an optimal primary font size (in px on a 1080x1080 canvas) based on phrase length.
     */
    internal fun computePrimaryTextSizePx(textLength: Int, hasSecondary: Boolean): Float {
        val base = when {
            textLength <= 25 -> 68f
            textLength <= 60 -> 56f
            textLength <= 120 -> 46f
            textLength <= 200 -> 38f
            else -> 32f
        }
        return if (hasSecondary) (base * 0.9f).coerceAtLeast(30f) else base
    }

    /**
     * Renders the [ShareableCardSpec] into a 1080x1080 [Bitmap] with subtle Sotto icon branding.
     */
    fun renderCardBitmap(context: Context, spec: ShareableCardSpec): Bitmap {
        val bitmap = Bitmap.createBitmap(CANVAS_SIZE, CANVAS_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Outer background
        canvas.drawColor(COLOR_OUTER_BG)

        // 2. Card container surface & border
        val cardRect = RectF(
            OUTER_MARGIN,
            OUTER_MARGIN,
            CANVAS_SIZE - OUTER_MARGIN,
            CANVAS_SIZE - OUTER_MARGIN
        )
        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (spec.isEmergency) COLOR_EMERGENCY_BG else COLOR_CARD_BG
        }
        canvas.drawRoundRect(cardRect, CARD_CORNER_RADIUS, CARD_CORNER_RADIUS, cardBgPaint)

        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = if (spec.isEmergency) 5f else 3f
            color = if (spec.isEmergency) COLOR_EMERGENCY_BORDER else COLOR_CARD_BORDER
        }
        canvas.drawRoundRect(cardRect, CARD_CORNER_RADIUS, CARD_CORNER_RADIUS, cardBorderPaint)

        // 3. Optional Top Badge (Emergency or Partner Mode)
        var contentTop = cardRect.top + 64f
        if (!spec.badgeLabel.isNullOrBlank()) {
            val badgeText = if (spec.isEmergency) {
                "🛡️  ${spec.badgeLabel.trim().uppercase()}"
            } else {
                spec.badgeLabel.trim().uppercase()
            }
            val badgeTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (spec.isEmergency) COLOR_EMERGENCY_BORDER else COLOR_SECONDARY_TEXT
                textSize = 26f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                letterSpacing = 0.06f
            }
            val textWidth = badgeTextPaint.measureText(badgeText)
            val badgePaddingH = 28f
            val badgeHeight = 54f
            val badgeWidth = textWidth + badgePaddingH * 2
            val badgeLeft = (CANVAS_SIZE - badgeWidth) / 2f
            val badgeRect = RectF(badgeLeft, contentTop, badgeLeft + badgeWidth, contentTop + badgeHeight)

            val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = if (spec.isEmergency) COLOR_EMERGENCY_BADGE_BG else COLOR_STANDARD_BADGE_BG
            }
            canvas.drawRoundRect(badgeRect, 18f, 18f, badgeBgPaint)

            val fontMetrics = badgeTextPaint.fontMetrics
            val textBaseline = badgeRect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText(badgeText, badgeLeft + badgePaddingH, textBaseline, badgeTextPaint)

            contentTop = badgeRect.bottom + 36f
        }

        // 4. Footer Branding (Sotto Icon + Brand Label at bottom of card)
        val footerBottom = cardRect.bottom - 44f
        val iconSize = 44
        val footerHeight = iconSize.toFloat()
        val footerTop = footerBottom - footerHeight
        drawSottoBrandFooter(
            context = context,
            canvas = canvas,
            centerX = CANVAS_SIZE / 2f,
            topY = footerTop,
            iconSize = iconSize,
            brandText = spec.footerBrandText.ifBlank { ShareableCardSpec.DEFAULT_BRAND_FOOTER }
        )

        val contentBottom = footerTop - 40f
        val availableHeight = (contentBottom - contentTop).coerceAtLeast(200f)
        val contentWidth = (cardRect.width() - INNER_PADDING_HORIZONTAL * 2).toInt()

        // 5. Primary & Secondary Text Layouts
        val primaryText = spec.primaryText.trim()
        val secondaryText = spec.secondaryText?.trim()?.takeIf {
            it.isNotEmpty() && !it.equals(primaryText, ignoreCase = true)
        }

        val primaryPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (spec.isEmergency) COLOR_EMERGENCY_TEXT else COLOR_PRIMARY_TEXT
            textSize = computePrimaryTextSizePx(primaryText.length, secondaryText != null)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val primaryLayout = StaticLayout.Builder
            .obtain(primaryText, 0, primaryText.length, primaryPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(6f, 1.12f)
            .setIncludePad(false)
            .build()

        val secondaryPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SECONDARY_TEXT
            textSize = (primaryPaint.textSize * 0.68f).coerceIn(28f, 42f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val secondaryLayout = secondaryText?.let { sec ->
            StaticLayout.Builder
                .obtain(sec, 0, sec.length, secondaryPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(4f, 1.1f)
                .setIncludePad(false)
                .build()
        }

        val dividerGap = if (secondaryLayout != null) 56f else 0f
        val totalTextHeight = primaryLayout.height + dividerGap + (secondaryLayout?.height ?: 0)
        val startY = contentTop + ((availableHeight - totalTextHeight) / 2f).coerceAtLeast(0f)
        val textLeft = cardRect.left + INNER_PADDING_HORIZONTAL

        canvas.save()
        canvas.translate(textLeft, startY)
        primaryLayout.draw(canvas)
        canvas.restore()

        if (secondaryLayout != null) {
            val dividerY = startY + primaryLayout.height + (dividerGap / 2f)
            val dividerHalfWidth = 140f
            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_DIVIDER
                strokeWidth = 3f
            }
            canvas.drawLine(
                (CANVAS_SIZE / 2f) - dividerHalfWidth,
                dividerY,
                (CANVAS_SIZE / 2f) + dividerHalfWidth,
                dividerY,
                dividerPaint
            )

            canvas.save()
            canvas.translate(textLeft, startY + primaryLayout.height + dividerGap)
            secondaryLayout.draw(canvas)
            canvas.restore()
        }

        return bitmap
    }

    internal fun drawSottoBrandFooter(
        context: Context,
        canvas: Canvas,
        centerX: Float,
        topY: Float,
        iconSize: Int,
        brandText: String
    ) {
        val footerTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_FOOTER_TEXT
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            letterSpacing = 0.02f
        }
        val textWidth = footerTextPaint.measureText(brandText)
        val gap = 14f
        val totalWidth = iconSize + gap + textWidth
        val startX = centerX - (totalWidth / 2f)

        // Draw authentic Sotto launcher icon (sage-green squircle with speech bubble & 5 voice equalizer bars)
        val badgeRect = RectF(startX, topY, startX + iconSize, topY + iconSize)
        val launcherDrawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
            ?: ContextCompat.getDrawable(context, R.drawable.ic_qs_sotto_partner)
        if (launcherDrawable != null) {
            launcherDrawable.setBounds(
                startX.toInt(),
                topY.toInt(),
                (startX + iconSize).toInt(),
                (topY + iconSize).toInt()
            )
            launcherDrawable.draw(canvas)
        } else {
            // Fallback acoustic bars motif if drawable unavailable in test environment
            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_SECONDARY_TEXT
                strokeWidth = 3f
                strokeCap = Paint.Cap.ROUND
            }
            val cx = badgeRect.centerX()
            val cy = badgeRect.centerY()
            canvas.drawLine(cx - 7f, cy - 5f, cx - 7f, cy + 5f, barPaint)
            canvas.drawLine(cx, cy - 9f, cx, cy + 9f, barPaint)
            canvas.drawLine(cx + 7f, cy - 5f, cx + 7f, cy + 5f, barPaint)
        }

        val fm = footerTextPaint.fontMetrics
        val baseline = badgeRect.centerY() - (fm.ascent + fm.descent) / 2f
        canvas.drawText(brandText, startX + iconSize + gap, baseline, footerTextPaint)
    }

    /**
     * Renders an image bitmap to temporary cache and opens the native Android Share Sheet.
     */
    fun shareImage(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        chooserTitle: String,
        caption: String? = null,
        clipLabel: String = ""
    ) {
        val sharedDir = File(context.cacheDir, SHARED_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
        val imageFile = File(sharedDir, fileName)
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
        bitmap.recycle()

        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, imageFile)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            if (!caption.isNullOrBlank()) {
                putExtra(Intent.EXTRA_TEXT, caption)
            }
            clipData = ClipData.newRawUri(clipLabel.take(40), contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }

    /**
     * Shares a rendered card via Android's native share sheet.
     */
    fun shareCard(
        context: Context,
        spec: ShareableCardSpec,
        sourceTag: String
    ) {
        if (spec.primaryText.isBlank()) return

        UsabilityTracker.startIntent("share_card", sourceTag)
        try {
            val bitmap = renderCardBitmap(context, spec)
            val caption = buildShareCaption(spec)
            val chooserTitle = context.getString(R.string.share_card_chooser_title)
            shareImage(
                context = context,
                bitmap = bitmap,
                fileName = SHARED_FILE_NAME,
                chooserTitle = chooserTitle,
                caption = caption,
                clipLabel = spec.primaryText
            )

            UsabilityTracker.recordActivation(context, "card_shared")
            UsabilityTracker.endIntent(context, "share_card", outcome = "shared", contextTag = sourceTag)
        } catch (e: Exception) {
            UsabilityTracker.endIntent(
                context,
                "share_card",
                outcome = "aborted",
                frictionTag = "share_error",
                contextTag = sourceTag
            )
        }
    }
}
