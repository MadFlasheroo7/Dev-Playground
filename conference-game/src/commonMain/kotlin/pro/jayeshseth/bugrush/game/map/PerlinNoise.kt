package pro.jayeshseth.bugrush.game.map

import kotlin.math.floor
import kotlin.random.Random

/**
 * Fast 2D Perlin and Fractal Brownian Motion (fBm) noise generator.
 * Scrambled with a unique session seed to produce procedural maps each playthrough.
 */
class PerlinNoise(seed: Long = Random.nextLong()) {

    private val p = IntArray(512)

    init {
        val perm = IntArray(256) { it }
        val rnd = Random(seed)
        // Fisher-Yates shuffle
        for (i in 255 downTo 1) {
            val j = rnd.nextInt(i + 1)
            val tmp = perm[i]
            perm[i] = perm[j]
            perm[j] = tmp
        }
        for (i in 0 until 512) {
            p[i] = perm[i and 255]
        }
    }

    private fun fade(t: Float): Float = t * t * t * (t * (t * 6f - 15f) + 10f)

    private fun lerp(t: Float, a: Float, b: Float): Float = a + t * (b - a)

    private fun grad(hash: Int, x: Float, y: Float): Float {
        val h = hash and 7
        val u = if (h < 4) x else y
        val v = if (h < 4) y else x
        return (if ((h and 1) == 0) u else -u) + (if ((h and 2) == 0) v else -v)
    }

    /**
     * Standard 2D Perlin noise returning value roughly between -1.0f and 1.0f.
     */
    fun noise2D(x: Float, y: Float): Float {
        val xi = floor(x).toInt() and 255
        val yi = floor(y).toInt() and 255

        val xf = x - floor(x)
        val yf = y - floor(y)

        val u = fade(xf)
        val v = fade(yf)

        val aa = p[p[xi] + yi]
        val ab = p[p[xi] + yi + 1]
        val ba = p[p[xi + 1] + yi]
        val bb = p[p[xi + 1] + yi + 1]

        val x1 = lerp(u, grad(aa, xf, yf), grad(ba, xf - 1f, yf))
        val x2 = lerp(u, grad(ab, xf, yf - 1f), grad(bb, xf - 1f, yf - 1f))

        return lerp(v, x1, x2)
    }

    /**
     * Multi-octave Fractal Brownian Motion (fBm) normalized to [0.0f, 1.0f].
     */
    fun fbm2D(
        x: Float,
        y: Float,
        octaves: Int = 3,
        lacunarity: Float = 2.0f,
        gain: Float = 0.5f
    ): Float {
        var total = 0f
        var frequency = 1f
        var amplitude = 1f
        var maxValue = 0f

        for (i in 0 until octaves) {
            total += noise2D(x * frequency, y * frequency) * amplitude
            maxValue += amplitude
            amplitude *= gain
            frequency *= lacunarity
        }

        val normalized = (total / maxValue) * 0.5f + 0.5f
        return normalized.coerceIn(0f, 1f)
    }
}
