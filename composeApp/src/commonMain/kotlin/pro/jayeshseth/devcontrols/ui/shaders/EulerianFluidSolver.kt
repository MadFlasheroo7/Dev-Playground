package pro.jayeshseth.devcontrols.ui.shaders

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * High-performance 2D Eulerian Incompressible Navier-Stokes Fluid Solver.
 * Implements Jos Stam's Stable Fluids with:
 * - Multi-pass ping-pong buffering
 * - Vorticity Confinement (Fedkiw et al.)
 * - Incompressibility projection via Jacobi Poisson relaxation (Helmholtz-Hodge)
 * - Semi-Lagrangian advection for velocity and multi-channel RGB dye
 * - Thermal buoyancy & convection
 * - Solid obstacle boundary conditions (von Kármán vortex streets)
 * - Lagrangian streak tracer particle advection
 */
class EulerianFluidSolver(
    val width: Int = 72,
    val height: Int = 108
) {
    val size: Int = width * height

    // Velocity fields: current and ping-pong scratch
    var vx = FloatArray(size)
    var vy = FloatArray(size)
    var vx0 = FloatArray(size)
    var vy0 = FloatArray(size)

    // Pressure & Divergence fields
    var p = FloatArray(size)
    var p0 = FloatArray(size)
    var div = FloatArray(size)

    // Vorticity field (curl of velocity)
    var vort = FloatArray(size)

    // Multi-channel RGB Dye fields: current and ping-pong
    var dyeR = FloatArray(size)
    var dyeG = FloatArray(size)
    var dyeB = FloatArray(size)
    var dyeR0 = FloatArray(size)
    var dyeG0 = FloatArray(size)
    var dyeB0 = FloatArray(size)

    // Solid obstacle mask: true = solid wall / obstacle
    val solid = BooleanArray(size)

    // Lagrangian Tracer Particles
    class Tracer(
        var x: Float,
        var y: Float,
        var prevX: Float,
        var prevY: Float,
        var life: Float,
        var maxLife: Float,
        var r: Float,
        var g: Float,
        var b: Float
    )

    val tracerCount = 3500
    val tracers = Array(tracerCount) {
        val rx = Random.nextFloat() * (width - 2) + 1f
        val ry = Random.nextFloat() * (height - 2) + 1f
        Tracer(
            x = rx,
            y = ry,
            prevX = rx,
            prevY = ry,
            life = Random.nextFloat() * 100f,
            maxLife = 100f + Random.nextFloat() * 150f,
            r = 0.2f, g = 0.8f, b = 1.0f
        )
    }

    init {
        // Build initial solid borders
        for (x in 0 until width) {
            solid[idx(x, 0)] = true
            solid[idx(x, height - 1)] = true
        }
        for (y in 0 until height) {
            solid[idx(0, y)] = true
            solid[idx(width - 1, y)] = true
        }
    }

    inline fun idx(x: Int, y: Int): Int = y * width + x

    fun reset() {
        vx.fill(0f); vy.fill(0f); vx0.fill(0f); vy0.fill(0f)
        p.fill(0f); p0.fill(0f); div.fill(0f); vort.fill(0f)
        dyeR.fill(0f); dyeG.fill(0f); dyeB.fill(0f)
        dyeR0.fill(0f); dyeG0.fill(0f); dyeB0.fill(0f)
        for (t in tracers) respawnTracer(t)
    }

    fun setCircularObstacle(cx: Float, cy: Float, radius: Float, active: Boolean) {
        val rSq = radius * radius
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val dx = x - cx
                val dy = y - cy
                if (dx * dx + dy * dy <= rSq) {
                    solid[idx(x, y)] = active
                    if (active) {
                        vx[idx(x, y)] = 0f
                        vy[idx(x, y)] = 0f
                        dyeR[idx(x, y)] = 0f
                        dyeG[idx(x, y)] = 0f
                        dyeB[idx(x, y)] = 0f
                    }
                }
            }
        }
    }

    fun clearObstacles() {
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                solid[idx(x, y)] = false
            }
        }
    }

    fun addImpulse(
        gx: Float,
        gy: Float,
        fvx: Float,
        fvy: Float,
        r: Float,
        g: Float,
        b: Float,
        radius: Float = 3.5f
    ) {
        val radInt = radius.toInt() + 1
        val rSq = radius * radius
        val ix = gx.toInt()
        val iy = gy.toInt()

        for (dy in -radInt..radInt) {
            val y = iy + dy
            if (y < 1 || y >= height - 1) continue
            for (dx in -radInt..radInt) {
                val x = ix + dx
                if (x < 1 || x >= width - 1) continue
                val distSq = (dx * dx + dy * dy).toFloat()
                if (distSq <= rSq) {
                    val factor = (1f - distSq / rSq).coerceIn(0f, 1f)
                    val i = idx(x, y)
                    if (solid[i]) continue

                    vx[i] += fvx * factor
                    vy[i] += fvy * factor

                    dyeR[i] = (dyeR[i] + r * factor * 1.5f).coerceAtMost(3.0f)
                    dyeG[i] = (dyeG[i] + g * factor * 1.5f).coerceAtMost(3.0f)
                    dyeB[i] = (dyeB[i] + b * factor * 1.5f).coerceAtMost(3.0f)
                }
            }
        }
    }

    fun injectContinuousJet(
        gx: Float,
        gy: Float,
        fvx: Float,
        fvy: Float,
        r: Float,
        g: Float,
        b: Float
    ) {
        addImpulse(gx, gy, fvx, fvy, r, g, b, radius = 2.8f)
    }

    // -------------------------------------------------------------------------
    // THE 5-PASS EULERIAN SOLVER STEP
    // -------------------------------------------------------------------------

    fun step(
        dt: Float,
        viscosity: Float = 0.0001f,
        vorticityEps: Float = 2.2f,
        buoyancy: Float = 0.35f,
        jacobiIters: Int = 18,
        dyeDissipation: Float = 0.993f,
        velocityDissipation: Float = 0.997f
    ) {
        // Pass 1: Thermal Buoyancy & External body forces
        if (abs(buoyancy) > 1e-4f) {
            applyBuoyancy(dt, buoyancy)
        }

        // Pass 2: Vorticity Confinement (Restores turbulent micro-swirls)
        if (vorticityEps > 0.01f) {
            applyVorticityConfinement(dt, vorticityEps)
        }

        // Pass 3: Viscous Diffusion (if viscosity > 0)
        if (viscosity > 1e-5f) {
            diffuseVelocity(dt, viscosity, 6)
        }

        // Pass 4: Incompressibility Projection (Helmholtz-Hodge: div -> Poisson Jacobi -> subtract grad p)
        projectPressure(jacobiIters)

        // Pass 5: Semi-Lagrangian Advection (Velocity & Multi-channel RGB Dye)
        advectVelocity(dt, velocityDissipation)
        advectDye(dt, dyeDissipation)

        // Pass 5b: Clean up divergence post-advection
        projectPressure(jacobiIters / 2)

        // Pass 6: Advect Lagrangian Streak Tracers
        updateTracers(dt)
    }

    private fun applyBuoyancy(dt: Float, buoyancyStrength: Float) {
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val i = idx(x, y)
                if (solid[i]) continue
                val totalDye = dyeR[i] * 0.3f + dyeG[i] * 0.59f + dyeB[i] * 0.11f
                // Hot dye rises (negative Y in screen coords)
                vy[i] -= totalDye * buoyancyStrength * dt * 25f
            }
        }
    }

    private fun applyVorticityConfinement(dt: Float, eps: Float) {
        // 1. Calculate curl (vorticity)
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val i = idx(x, y)
                if (solid[i]) {
                    vort[i] = 0f
                    continue
                }
                val duydx = 0.5f * (vy[idx(x + 1, y)] - vy[idx(x - 1, y)])
                val duxdy = 0.5f * (vx[idx(x, y + 1)] - vx[idx(x, y - 1)])
                vort[i] = duydx - duxdy
            }
        }

        // 2. Calculate gradient of vorticity magnitude and apply restorative torque
        for (y in 2 until height - 2) {
            for (x in 2 until width - 2) {
                val i = idx(x, y)
                if (solid[i]) continue

                val dx = 0.5f * (abs(vort[idx(x + 1, y)]) - abs(vort[idx(x - 1, y)]))
                val dy = 0.5f * (abs(vort[idx(x, y + 1)]) - abs(vort[idx(x, y - 1)]))
                val len = sqrt(dx * dx + dy * dy) + 1e-5f

                val nx = dx / len
                val ny = dy / len
                val omega = vort[i]

                // Vorticity force: F = eps * dt * (N x omega)
                vx[i] += ny * omega * eps * dt
                vy[i] -= nx * omega * eps * dt
            }
        }
    }

    private fun diffuseVelocity(dt: Float, visc: Float, iters: Int) {
        val a = dt * visc * width * height
        val c = 1f + 4f * a

        // Copy current velocity to vx0, vy0
        vx.copyInto(vx0)
        vy.copyInto(vy0)

        for (k in 0 until iters) {
            for (y in 1 until height - 1) {
                for (x in 1 until width - 1) {
                    val i = idx(x, y)
                    if (solid[i]) continue
                    vx[i] = (vx0[i] + a * (vx[idx(x - 1, y)] + vx[idx(x + 1, y)] + vx[idx(x, y - 1)] + vx[idx(x, y + 1)])) / c
                    vy[i] = (vy0[i] + a * (vy[idx(x - 1, y)] + vy[idx(x + 1, y)] + vy[idx(x, y - 1)] + vy[idx(x, y + 1)])) / c
                }
            }
            applyVelocityBoundary()
        }
    }

    private fun projectPressure(iters: Int) {
        // Step 1: Calculate Divergence field
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val i = idx(x, y)
                if (solid[i]) {
                    div[i] = 0f
                    p[i] = 0f
                    continue
                }
                val dux = vx[idx(x + 1, y)] - vx[idx(x - 1, y)]
                val duy = vy[idx(x, y + 1)] - vy[idx(x, y - 1)]
                div[i] = -0.5f * (dux + duy)
                p[i] = 0f
            }
        }

        // Step 2: Jacobi Relaxation Poisson Solver for Pressure
        for (k in 0 until iters) {
            p.copyInto(p0)
            for (y in 1 until height - 1) {
                for (x in 1 until width - 1) {
                    val i = idx(x, y)
                    if (solid[i]) continue

                    val pL = if (solid[idx(x - 1, y)]) p0[i] else p0[idx(x - 1, y)]
                    val pR = if (solid[idx(x + 1, y)]) p0[i] else p0[idx(x + 1, y)]
                    val pT = if (solid[idx(x, y - 1)]) p0[i] else p0[idx(x, y - 1)]
                    val pB = if (solid[idx(x, y + 1)]) p0[i] else p0[idx(x, y + 1)]

                    p[i] = 0.25f * (pL + pR + pT + pB + div[i])
                }
            }
        }

        // Step 3: Subtract Pressure Gradient from Velocity (Enforce Incompressibility)
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val i = idx(x, y)
                if (solid[i]) continue

                val pL = if (solid[idx(x - 1, y)]) p[i] else p[idx(x - 1, y)]
                val pR = if (solid[idx(x + 1, y)]) p[i] else p[idx(x + 1, y)]
                val pT = if (solid[idx(x, y - 1)]) p[i] else p[idx(x, y - 1)]
                val pB = if (solid[idx(x, y + 1)]) p[i] else p[idx(x, y + 1)]

                vx[i] -= 0.5f * (pR - pL)
                vy[i] -= 0.5f * (pB - pT)
            }
        }

        applyVelocityBoundary()
    }

    private fun advectVelocity(dt: Float, dissipation: Float) {
        vx.copyInto(vx0)
        vy.copyInto(vy0)

        val dt0x = dt * width
        val dt0y = dt * height

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val i = idx(x, y)
                if (solid[i]) {
                    vx[i] = 0f
                    vy[i] = 0f
                    continue
                }

                // Backtrack position along streamline
                val px = (x - dt0x * vx0[i]).coerceIn(0.5f, width - 1.5f)
                val py = (y - dt0y * vy0[i]).coerceIn(0.5f, height - 1.5f)

                vx[i] = sampleBilinear(vx0, px, py) * dissipation
                vy[i] = sampleBilinear(vy0, px, py) * dissipation
            }
        }
        applyVelocityBoundary()
    }

    private fun advectDye(dt: Float, dissipation: Float) {
        dyeR.copyInto(dyeR0)
        dyeG.copyInto(dyeG0)
        dyeB.copyInto(dyeB0)

        val dt0x = dt * width
        val dt0y = dt * height

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val i = idx(x, y)
                if (solid[i]) {
                    dyeR[i] = 0f
                    dyeG[i] = 0f
                    dyeB[i] = 0f
                    continue
                }

                val px = (x - dt0x * vx[i]).coerceIn(0.5f, width - 1.5f)
                val py = (y - dt0y * vy[i]).coerceIn(0.5f, height - 1.5f)

                dyeR[i] = sampleBilinear(dyeR0, px, py) * dissipation
                dyeG[i] = sampleBilinear(dyeG0, px, py) * dissipation
                dyeB[i] = sampleBilinear(dyeB0, px, py) * dissipation
            }
        }
    }

    private fun applyVelocityBoundary() {
        for (y in 0 until height) {
            vx[idx(0, y)] = 0f
            vx[idx(width - 1, y)] = 0f
            vy[idx(0, y)] = 0f
            vy[idx(width - 1, y)] = 0f
        }
        for (x in 0 until width) {
            vx[idx(x, 0)] = 0f
            vx[idx(x, height - 1)] = 0f
            vy[idx(x, 0)] = 0f
            vy[idx(x, height - 1)] = 0f
        }
        // Also zero out internal solid obstacles
        for (i in 0 until size) {
            if (solid[i]) {
                vx[i] = 0f
                vy[i] = 0f
            }
        }
    }

    fun sampleBilinear(f: FloatArray, x: Float, y: Float): Float {
        val x0 = x.toInt(); val y0 = y.toInt()
        val x1 = (x0 + 1).coerceAtMost(width - 1)
        val y1 = (y0 + 1).coerceAtMost(height - 1)

        val sx = x - x0
        val sy = y - y0

        val f00 = f[idx(x0, y0)]
        val f10 = f[idx(x1, y0)]
        val f01 = f[idx(x0, y1)]
        val f11 = f[idx(x1, y1)]

        return (f00 * (1f - sx) + f10 * sx) * (1f - sy) +
               (f01 * (1f - sx) + f11 * sx) * sy
    }

    // -------------------------------------------------------------------------
    // LAGRANGIAN TRACER PARTICLES
    // -------------------------------------------------------------------------

    private fun updateTracers(dt: Float) {
        val dtScaleX = dt * width * 1.5f
        val dtScaleY = dt * height * 1.5f

        for (t in tracers) {
            t.life += 1f
            if (t.life >= t.maxLife) {
                respawnTracer(t)
                continue
            }

            t.prevX = t.x
            t.prevY = t.y

            val curVx = sampleBilinear(vx, t.x, t.y)
            val curVy = sampleBilinear(vy, t.x, t.y)

            t.x += curVx * dtScaleX
            t.y += curVy * dtScaleY

            // Boundaries & obstacles
            val ix = t.x.toInt()
            val iy = t.y.toInt()
            if (ix < 1 || ix >= width - 1 || iy < 1 || iy >= height - 1 || solid[idx(ix, iy)]) {
                respawnTracer(t)
                continue
            }

            // Adopt local dye color
            val localR = sampleBilinear(dyeR, t.x, t.y)
            val localG = sampleBilinear(dyeG, t.x, t.y)
            val localB = sampleBilinear(dyeB, t.x, t.y)
            val dyeMag = localR + localG + localB
            if (dyeMag > 0.05f) {
                t.r = localR / dyeMag
                t.g = localG / dyeMag
                t.b = localB / dyeMag
            }
        }
    }

    private fun respawnTracer(t: Tracer) {
        var attempts = 0
        while (attempts < 10) {
            val rx = Random.nextFloat() * (width - 4) + 2f
            val ry = Random.nextFloat() * (height - 4) + 2f
            val i = idx(rx.toInt(), ry.toInt())
            if (!solid[i]) {
                t.x = rx; t.y = ry
                t.prevX = rx; t.prevY = ry
                t.life = 0f
                t.maxLife = 80f + Random.nextFloat() * 120f
                return
            }
            attempts++
        }
        t.x = width / 2f
        t.y = height / 2f
        t.prevX = t.x
        t.prevY = t.y
        t.life = 0f
    }
}
