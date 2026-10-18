package com.amh.sotto.util

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class QRCodeImageHelperTest {

    private fun generateQrPixels(
        content: String,
        width: Int = 300,
        height: Int = 300,
        margin: Int = 4
    ): IntArray {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to margin
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints)
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }
        return pixels
    }

    @Test
    fun `decodeQrFromPixels round-trips UTF-8 multilingual payload with emoji and Indonesian text`() {
        val samplePayload = """{"v":2,"title":"Bantuan & Medis 🚨","phrases":[{"text":"Saya butuh bantuan","trans":"I need help"}]}"""
        val width = 320
        val height = 320
        val pixels = generateQrPixels(samplePayload, width, height)

        val decoded = QRCodeImageHelper.decodeQrFromPixels(pixels, width, height)
        assertNotNull(decoded)
        assertEquals(samplePayload, decoded)
    }

    @Test
    fun `decodeQrFromPixels decodes inverted QR code via fallback binarizer`() {
        val samplePayload = "SOTTO_INVERTED_DECK_TEST_PAYLOAD"
        val width = 256
        val height = 256
        val normalPixels = generateQrPixels(samplePayload, width, height)

        // Invert pixels: dark becomes white, white becomes dark
        val invertedPixels = IntArray(width * height)
        for (i in normalPixels.indices) {
            invertedPixels[i] = if (normalPixels[i] == 0xFF000000.toInt()) {
                0xFFFFFFFF.toInt()
            } else {
                0xFF000000.toInt()
            }
        }

        val decoded = QRCodeImageHelper.decodeQrFromPixels(invertedPixels, width, height)
        assertNotNull(decoded)
        assertEquals(samplePayload, decoded)
    }

    @Test
    fun `decodeQrFromPixels decodes QR code embedded inside larger screenshot buffer`() {
        val payload = "SOTTO_SCREENSHOT_EMBEDDED_PAYLOAD_12345"
        val qrSize = 300
        val qrPixels = generateQrPixels(payload, qrSize, qrSize, margin = 4)

        val canvasW = 600
        val canvasH = 800
        val canvasPixels = IntArray(canvasW * canvasH)

        // Fill with dark canvas background (e.g. Sotto dark theme card)
        val darkBg = 0xFF161920.toInt()
        canvasPixels.fill(darkBg)

        // Embed the QR centered on the canvas
        val startX = (canvasW - qrSize) / 2
        val startY = (canvasH - qrSize) / 2

        for (y in 0 until qrSize) {
            for (x in 0 until qrSize) {
                val canvasIndex = (startY + y) * canvasW + (startX + x)
                val qrIndex = y * qrSize + x
                canvasPixels[canvasIndex] = qrPixels[qrIndex]
            }
        }

        val decoded = QRCodeImageHelper.decodeQrFromPixels(canvasPixels, canvasW, canvasH)
        assertNotNull(decoded)
        assertEquals(payload, decoded)
    }

    @Test
    fun `decodeQrFromPixels returns null for blank or uniform pixels`() {
        val size = 200
        val whitePixels = IntArray(size * size) { 0xFFFFFFFF.toInt() }
        assertNull(QRCodeImageHelper.decodeQrFromPixels(whitePixels, size, size))

        val blackPixels = IntArray(size * size) { 0xFF000000.toInt() }
        assertNull(QRCodeImageHelper.decodeQrFromPixels(blackPixels, size, size))
    }

    @Test
    fun `decodeQrFromPixels returns null for invalid dimensions or undersized buffers`() {
        val validPixels = IntArray(100) { 0xFFFFFFFF.toInt() }

        // Width or height <= 0
        assertNull(QRCodeImageHelper.decodeQrFromPixels(validPixels, 0, 10))
        assertNull(QRCodeImageHelper.decodeQrFromPixels(validPixels, 10, 0))
        assertNull(QRCodeImageHelper.decodeQrFromPixels(validPixels, -1, 10))

        // Buffer smaller than width * height
        assertNull(QRCodeImageHelper.decodeQrFromPixels(validPixels, 20, 20)) // needs 400 pixels
    }
}
