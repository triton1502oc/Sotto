package com.amh.sotto.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class EmergencyTileServiceTest {

    @Test
    fun `extra key is defined correctly for opening partner mode`() {
        assertEquals("com.amh.sotto.extra.OPEN_PARTNER_MODE", EmergencyTileService.EXTRA_OPEN_PARTNER_MODE)
    }

    @Test
    fun `service class is instantiable`() {
        val service = EmergencyTileService()
        assertNotNull(service)
    }
}
