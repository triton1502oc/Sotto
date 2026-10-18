package com.amh.sotto.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.amh.sotto.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Renders branded Sotto QR code deck PNG images for sharing over WhatsApp/chat apps
 * and decodes QR code payloads 100% offline from PNG/JPEG images or screenshots.
 */
object QRCodeImageHelper {

    private const val CANVAS_SIZE = 1080
    private const val OUTER_MARGIN = 48f
    private const val CARD_CORNER_RADIUS = 48f
    private const val SHARED_QR_FILE_NAME = "sotto_qr_deck.png"
    private const val MAX_DECODE_DIMENSION = 1600

    private const val COLOR_OUTER_BG = 0xFF0D0F12.toInt()
    private const val COLOR_CARD_BG = 0xFF161920.toInt()
    private const val COLOR_CARD_BORDER = 0xFF2A303C.toInt()
    private const val COLOR_PRIMARY_TEXT = 0xFFF3F6FA.toInt()
    private const val COLOR_SECONDARY_TEXT = 0xFF90CAF9.toInt()
    private const val COLOR_MUTED_TEXT = 0xFF8B95A5.toInt()

    /**
     * Generates a raw square QR code [Bitmap] from UTF-8 [content].
     */
    fun generateQRCodeBitmap(content: String, size: Int = 600): Bitmap? {
        if (content.isBlank() || size <= 0) return null
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L,
                EncodeHintType.MARGIN to 1
            )
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val width = matrix.width
            val height = matrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Decodes a QR code string from an in-memory [Bitmap] 100% offline using ZXing.
     * Supports both clean QR cards and full-screen phone screenshots.
     */
    fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return null

        return try {
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            decodeQrFromPixels(pixels, width, height)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Pure JVM-testable helper that decodes a QR code from ARGB [pixels].
     */
    internal fun decodeQrFromPixels(pixels: IntArray, width: Int, height: Int): String? {
        if (width <= 0 || height <= 0 || pixels.size < width * height) return null
        val source = RGBLuminanceSource(width, height, pixels)
        val hints = mapOf(
            DecodeHintType.TRY_HARDER to java.lang.Boolean.TRUE,
            DecodeHintType.CHARACTER_SET to "UTF-8",
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)
        )
        val reader = QRCodeReader()

        val candidates = listOf(
            BinaryBitmap(HybridBinarizer(source)),
            BinaryBitmap(GlobalHistogramBinarizer(source)),
            BinaryBitmap(HybridBinarizer(source.invert()))
        )

        for (candidate in candidates) {
            try {
                val result = reader.decode(candidate, hints)
                val text = result.text?.trim()
                if (!text.isNullOrEmpty()) {
                    return text
                }
            } catch (_: Exception) {
                reader.reset()
            }
        }
        return null
    }

    /**
     * Decodes a QR code string from a content [Uri] (e.g. gallery image picker or WhatsApp share).
     */
    fun decodeQrFromUri(context: Context, uri: Uri): String? {
        return try {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            val rawW = boundsOptions.outWidth
            val rawH = boundsOptions.outHeight
            if (rawW <= 0 || rawH <= 0) return null

            var sampleSize = 1
            while ((rawW / sampleSize) > MAX_DECODE_DIMENSION || (rawH / sampleSize) > MAX_DECODE_DIMENSION) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            try {
                decodeQrFromBitmap(bitmap)
            } finally {
                bitmap.recycle()
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Renders a 1080x1080 dark-neutral card containing the QR code inside a crisp white quiet-zone
     * container and the subtle Sotto brand icon footer.
     */
    fun renderBrandedQrCardBitmap(
        context: Context,
        qrBitmap: Bitmap,
        title: String,
        subtitle: String,
        payloadLabel: String,
        footerBrandText: String
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(CANVAS_SIZE, CANVAS_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(COLOR_OUTER_BG)

        val cardRect = RectF(
            OUTER_MARGIN,
            OUTER_MARGIN,
            CANVAS_SIZE - OUTER_MARGIN,
            CANVAS_SIZE - OUTER_MARGIN
        )
        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = COLOR_CARD_BG
        }
        canvas.drawRoundRect(cardRect, CARD_CORNER_RADIUS, CARD_CORNER_RADIUS, cardBgPaint)

        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = COLOR_CARD_BORDER
        }
        canvas.drawRoundRect(cardRect, CARD_CORNER_RADIUS, CARD_CORNER_RADIUS, cardBorderPaint)

        // Header Title & Subtitle
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY_TEXT
            textSize = 42f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val titleY = cardRect.top + 82f
        canvas.drawText(title, CANVAS_SIZE / 2f, titleY, titlePaint)

        var qrTop = titleY + 34f
        if (subtitle.isNotBlank()) {
            val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_SECONDARY_TEXT
                textSize = 26f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val maxSubWidth = (cardRect.width() - 120f).toInt()
            val subLayout = StaticLayout.Builder
                .obtain(subtitle, 0, subtitle.length, subPaint, maxSubWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setMaxLines(2)
                .setIncludePad(false)
                .build()
            canvas.save()
            canvas.translate((CANVAS_SIZE - maxSubWidth) / 2f, titleY + 20f)
            subLayout.draw(canvas)
            canvas.restore()
            qrTop = titleY + 20f + subLayout.height + 28f
        }

        // Footer Branding
        val footerBottom = cardRect.bottom - 40f
        val iconSize = 44
        val footerTop = footerBottom - iconSize
        CardShareHelper.drawSottoBrandFooter(
            context = context,
            canvas = canvas,
            centerX = CANVAS_SIZE / 2f,
            topY = footerTop,
            iconSize = iconSize,
            brandText = footerBrandText.ifBlank { ShareableCardSpec.DEFAULT_BRAND_FOOTER }
        )

        // White quiet-zone QR box centered between header and footer
        val availableVertical = (footerTop - 52f) - qrTop
        val qrBoxSize = availableVertical.coerceIn(480f, 640f)
        val qrBoxLeft = (CANVAS_SIZE - qrBoxSize) / 2f
        val qrBoxTop = qrTop + ((availableVertical - qrBoxSize) / 2f).coerceAtLeast(0f)
        val qrBoxRect = RectF(qrBoxLeft, qrBoxTop, qrBoxLeft + qrBoxSize, qrBoxTop + qrBoxSize)

        val whiteBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        canvas.drawRoundRect(qrBoxRect, 32f, 32f, whiteBoxPaint)

        val qrInset = 28
        val dstRect = Rect(
            (qrBoxRect.left + qrInset).toInt(),
            (qrBoxRect.top + qrInset).toInt(),
            (qrBoxRect.right - qrInset).toInt(),
            (qrBoxRect.bottom - qrInset).toInt()
        )
        canvas.drawBitmap(qrBitmap, null, dstRect, null)

        // Payload size label between QR box and brand footer
        if (payloadLabel.isNotBlank()) {
            val payloadPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_MUTED_TEXT
                textSize = 22f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(payloadLabel, CANVAS_SIZE / 2f, qrBoxRect.bottom + 34f, payloadPaint)
        }

        return bitmap
    }

    /**
     * Renders and shares the branded QR code card image via Android's native Share Sheet.
     */
    fun shareQrDeckImage(
        context: Context,
        qrBitmap: Bitmap,
        title: String,
        subtitle: String,
        payloadLabel: String
    ) {
        UsabilityTracker.startIntent("share_qr_image", "qr_dialog")
        try {
            val brandFooter = context.getString(R.string.share_card_brand_footer)
            val brandedBitmap = renderBrandedQrCardBitmap(
                context = context,
                qrBitmap = qrBitmap,
                title = title,
                subtitle = subtitle,
                payloadLabel = payloadLabel,
                footerBrandText = brandFooter
            )
            val caption = context.getString(R.string.share_qr_caption)
            val chooserTitle = context.getString(R.string.share_qr_chooser_title)
            CardShareHelper.shareImage(
                context = context,
                bitmap = brandedBitmap,
                fileName = SHARED_QR_FILE_NAME,
                chooserTitle = chooserTitle,
                caption = caption,
                clipLabel = title
            )

            UsabilityTracker.recordActivation(context, "qr_image_shared")
            UsabilityTracker.endIntent(context, "share_qr_image", outcome = "shared", contextTag = "qr_dialog")
        } catch (_: Exception) {
            UsabilityTracker.endIntent(
                context,
                "share_qr_image",
                outcome = "aborted",
                frictionTag = "share_error",
                contextTag = "qr_dialog"
            )
        }
    }
}
