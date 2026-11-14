package pro.jayeshseth.devcontrols.ui.visualfx

import pro.jayeshseth.devcontrols.ui.visualfx.qr.PureQrEncoder
import pro.jayeshseth.devcontrols.ui.visualfx.qr.QrEcLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PureQrEncoderTest {

    @Test
    fun testEncodeGoogleCom() {
        val matrix = PureQrEncoder.encode("google.com", QrEcLevel.M)
        // "google.com" is 10 bytes -> Version 1 (21x21 modules)
        assertEquals(1, matrix.version)
        assertEquals(21, matrix.size)

        // Check top-left finder pattern center is dark
        assertTrue(matrix.isDark(3, 3))
        // Check separator is white
        assertTrue(!matrix.isDark(0, 7))
        assertTrue(!matrix.isDark(7, 0))

        // Check finder identification
        assertTrue(matrix.isFinderPattern(0, 0))
        assertTrue(matrix.isFinderPattern(6, 6))
        assertTrue(matrix.isFinderPattern(0, 20))
        assertTrue(matrix.isFinderPattern(20, 0))
        assertTrue(!matrix.isFinderPattern(10, 10))

        // Total dark modules in a valid QR code should be reasonable (typically 20% to 70%)
        var darkCount = 0
        for (r in 0 until matrix.size) {
            for (c in 0 until matrix.size) {
                if (matrix.isDark(r, c)) darkCount++
            }
        }
        val total = matrix.size * matrix.size
        assertTrue(darkCount in 80..total)
    }

    @Test
    fun testEncodeLongerUrl() {
        val matrix = PureQrEncoder.encode("https://github.com/jayeshseth", QrEcLevel.M)
        assertTrue(matrix.version >= 2)
        assertTrue(matrix.size >= 25)
    }
}
