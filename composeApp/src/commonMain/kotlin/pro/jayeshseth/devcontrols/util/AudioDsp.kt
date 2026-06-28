package pro.jayeshseth.devcontrols.util

import kotlin.math.cos
import kotlin.math.sin

/** In-place iterative radix-2 Cooley-Tukey FFT. [re].size must be a power of two. */
fun fft(re: FloatArray, im: FloatArray) {
    val n = re.size
    var j = 0
    for (i in 1 until n) {
        var bit = n shr 1
        while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
        j = j or bit
        if (i < j) {
            val tr = re[i]; re[i] = re[j]; re[j] = tr
            val ti = im[i]; im[i] = im[j]; im[j] = ti
        }
    }
    var len = 2
    while (len <= n) {
        val ang = -2.0 * kotlin.math.PI / len
        val wr = cos(ang).toFloat(); val wi = sin(ang).toFloat()
        var i = 0
        while (i < n) {
            var curR = 1f; var curI = 0f
            for (k in 0 until len / 2) {
                val aR = re[i + k]; val aI = im[i + k]
                val bR = re[i + k + len / 2]; val bI = im[i + k + len / 2]
                val tR = bR * curR - bI * curI; val tI = bR * curI + bI * curR
                re[i + k] = aR + tR; im[i + k] = aI + tI
                re[i + k + len / 2] = aR - tR; im[i + k + len / 2] = aI - tI
                val nR = curR * wr - curI * wi; curI = curR * wi + curI * wr; curR = nR
            }
            i += len
        }
        len = len shl 1
    }
}
