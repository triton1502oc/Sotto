package com.amh.sotto.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardShareHelperTest {

    @Test
    fun `buildShareCaption with primary text only returns primary text`() {
        val spec = ShareableCardSpec(primaryText = "I need water")
        val caption = CardShareHelper.buildShareCaption(spec)
        assertEquals("I need water", caption)
    }

    @Test
    fun `buildShareCaption with primary and secondary returns bilingual text`() {
        val spec = ShareableCardSpec(
            primaryText = "I need water",
            secondaryText = "Saya butuh air"
        )
        val caption = CardShareHelper.buildShareCaption(spec)
        assertEquals("I need water\n\nSaya butuh air", caption)
    }

    @Test
    fun `buildShareCaption deduplicates when secondary text equals primary text case-insensitively`() {
        val spec = ShareableCardSpec(
            primaryText = "Help",
            secondaryText = "help"
        )
        val caption = CardShareHelper.buildShareCaption(spec)
        assertEquals("Help", caption)
    }

    @Test
    fun `buildShareCaption prefixes Emergency with shield icon when isEmergency is true`() {
        val spec = ShareableCardSpec(
            primaryText = "Call ambulance immediately",
            badgeLabel = "Emergency",
            isEmergency = true
        )
        val caption = CardShareHelper.buildShareCaption(spec)
        assertEquals("[🛡️ Emergency]\nCall ambulance immediately", caption)
    }

    @Test
    fun `buildShareCaption prefixes Partner Exchange when isEmergency is false`() {
        val spec = ShareableCardSpec(
            primaryText = "Are you comfortable right now?",
            badgeLabel = "Partner Exchange",
            isEmergency = false
        )
        val caption = CardShareHelper.buildShareCaption(spec)
        assertEquals("[Partner Exchange]\nAre you comfortable right now?", caption)
    }

    @Test
    fun `buildShareCaption returns empty string when primaryText is blank`() {
        val spec = ShareableCardSpec(
            primaryText = "   ",
            secondaryText = "Saya butuh bantuan",
            badgeLabel = "Emergency",
            isEmergency = true
        )
        val caption = CardShareHelper.buildShareCaption(spec)
        assertEquals("", caption)
    }

    @Test
    fun `computePrimaryTextSizePx scales font size by phrase length and secondary text presence`() {
        val sizeShort = CardShareHelper.computePrimaryTextSizePx(textLength = 15, hasSecondary = false)
        val sizeMedium = CardShareHelper.computePrimaryTextSizePx(textLength = 45, hasSecondary = false)
        val sizeMediumLong = CardShareHelper.computePrimaryTextSizePx(textLength = 90, hasSecondary = false)
        val sizeLong = CardShareHelper.computePrimaryTextSizePx(textLength = 150, hasSecondary = false)
        val sizeVeryLong = CardShareHelper.computePrimaryTextSizePx(textLength = 220, hasSecondary = false)

        assertEquals(68f, sizeShort, 0.01f)
        assertEquals(56f, sizeMedium, 0.01f)
        assertEquals(46f, sizeMediumLong, 0.01f)
        assertEquals(38f, sizeLong, 0.01f)
        assertEquals(32f, sizeVeryLong, 0.01f)

        // Monotonic decrease with length
        assertTrue(sizeShort > sizeMedium)
        assertTrue(sizeMedium > sizeMediumLong)
        assertTrue(sizeMediumLong > sizeLong)
        assertTrue(sizeLong > sizeVeryLong)

        // Scales down when secondary text is present
        val sizeShortWithSecondary = CardShareHelper.computePrimaryTextSizePx(textLength = 15, hasSecondary = true)
        val sizeMediumWithSecondary = CardShareHelper.computePrimaryTextSizePx(textLength = 45, hasSecondary = true)
        val sizeVeryLongWithSecondary = CardShareHelper.computePrimaryTextSizePx(textLength = 220, hasSecondary = true)

        assertEquals(68f * 0.9f, sizeShortWithSecondary, 0.01f)
        assertEquals(56f * 0.9f, sizeMediumWithSecondary, 0.01f)
        assertTrue(sizeShortWithSecondary < sizeShort)
        assertTrue(sizeMediumWithSecondary < sizeMedium)
        assertEquals(30f, sizeVeryLongWithSecondary, 0.01f) // Coerced to 30f minimum
    }
}
