package pro.jayeshseth.bugrush.ui

import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.jayeshseth.bugrush.game.actors.BossVoxelActor
import pro.jayeshseth.bugrush.game.actors.BountyBeaconVoxelActor
import pro.jayeshseth.bugrush.game.actors.BugHazardActor
import pro.jayeshseth.bugrush.game.actors.BugType
import pro.jayeshseth.bugrush.game.actors.CompanionDroneVoxelActor
import pro.jayeshseth.bugrush.game.actors.CrateVoxelActor
import pro.jayeshseth.bugrush.game.actors.ExplosiveBarrelVoxelActor
import pro.jayeshseth.bugrush.game.actors.LaserFenceVoxelActor
import pro.jayeshseth.bugrush.game.actors.OverclockConduitVoxelActor
import pro.jayeshseth.bugrush.game.actors.PlayerArchetype
import pro.jayeshseth.bugrush.game.actors.PlayerVoxelActor
import pro.jayeshseth.bugrush.game.actors.TriviaShardVoxelActor
import pro.jayeshseth.bugrush.game.actors.TurretVoxelActor
import pro.jayeshseth.bugrush.game.renderer.IsometricProjection
import pro.jayeshseth.bugrush.game.renderer.VoxelCuboid
import pro.jayeshseth.bugrush.trivia.PowerUpType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ======================================================================================
// REUSABLE HIGH-CONTRAST UI ATOMS
// ======================================================================================

@Composable
fun KeycapBadge(
    text: String,
    accentColor: Color = Color(0xFF00E5FF)
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0A0E14))
            .border(1.5.dp, accentColor.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = accentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ArcadeTheme.pixelHeader
        )
    }
}

@Composable
fun CalloutCard(
    title: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF101724))
            .border(1.5.dp, accentColor.copy(alpha = 0.40f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            color = accentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            fontFamily = ArcadeTheme.pixelHeader
        )
        content()
    }
}

/**
 * 3D Hologram Pedestal Canvas that auto-centers cuboids and rotates smoothly 360°.
 * Uses dynamic rotated-depth painter's sorting along camera sightline.
 */
@Composable
fun HoloPedestalCanvas(
    cuboids: List<VoxelCuboid>,
    cameraAngleRad: Float,
    accentColor: Color,
    zoom: Float = 1.3f,
    modifier: Modifier = Modifier.fillMaxWidth().height(360.dp)
) {
    if (cuboids.isEmpty()) {
        Box(modifier = modifier.background(Color(0xFF080D14)))
        return
    }

    var minX = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    var minZ = Float.MAX_VALUE
    var maxZ = -Float.MAX_VALUE

    for (c in cuboids) {
        val hx = c.sizeX * 0.5f
        val hy = c.sizeY * 0.5f
        if (c.x - hx < minX) minX = c.x - hx
        if (c.x + hx > maxX) maxX = c.x + hx
        if (c.y - hy < minY) minY = c.y - hy
        if (c.y + hy > maxY) maxY = c.y + hy
        if (c.z < minZ) minZ = c.z
        if (c.z + c.sizeZ > maxZ) maxZ = c.z + c.sizeZ
    }

    val modelCenterX = (minX + maxX) * 0.5f
    val modelCenterY = (minY + maxY) * 0.5f

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF080D14))
            .border(1.5.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp))
    ) {
        val screenCenterX = size.width * 0.5f
        val screenCenterY = size.height * 0.60f

        // Holographic emitter rings on floor
        drawCircle(
            color = accentColor.copy(alpha = 0.08f),
            radius = size.width * 0.44f,
            center = Offset(screenCenterX, screenCenterY + 28f)
        )
        drawCircle(
            color = accentColor.copy(alpha = 0.28f),
            radius = size.width * 0.34f,
            center = Offset(screenCenterX, screenCenterY + 28f),
            style = Stroke(width = 1.5f)
        )
        drawCircle(
            color = accentColor.copy(alpha = 0.50f),
            radius = size.width * 0.22f,
            center = Offset(screenCenterX, screenCenterY + 28f),
            style = Stroke(width = 1.2f)
        )

        // Rotate depth painter's order along active camera sightline
        val cosA = cos(cameraAngleRad)
        val sinA = sin(cameraAngleRad)
        val sorted = cuboids.sortedBy { c ->
            if (c.isFloor) -100_000f
            else {
                val relX = c.x - modelCenterX
                val relY = c.y - modelCenterY
                val rotX = relX * cosA - relY * sinA
                val rotY = relX * sinA + relY * cosA
                rotX + rotY + c.z * 0.8f
            }
        }

        for (cuboid in sorted) {
            IsometricProjection.drawCuboid(
                scope = this,
                cuboid = cuboid,
                cameraCenterX = modelCenterX,
                cameraCenterY = modelCenterY,
                screenCenterX = screenCenterX,
                screenCenterY = screenCenterY,
                zoom = zoom,
                strokeColor = Color(0x33000000),
                strokeWidth = 1.2f,
                viewportWidth = size.width,
                viewportHeight = size.height,
                cameraAngleRad = cameraAngleRad
            )
        }
    }
}

// ======================================================================================
// MODULAR SLIDE CONTENT COMPOSABLES (EDIT TEXT DIRECTLY HERE!)
// ======================================================================================

/**
 * Slide 1: Operator controls, movement thrusters, dash invulnerability, and weapon arsenal.
 */
@Composable
fun Slide1_OperatorAndWeaponsContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "🤖 OPERATOR & WEAPON ARSENAL",
                color = Color(0xFF00E5FF),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            KeycapBadge("[SPEC-01: COMBAT]", Color(0xFF00E5FF))
        }

        CalloutCard(title = "🔍 WHAT IS IT?", accentColor = Color(0xFF00E5FF)) {
            Text(
                text = "You are the conference Operator deployed into the live Kotlin runtime arena. Your objective is to squash rogue memory leaks, deadlock bugs, and anomalies within 120 seconds.",
                color = Color(0xFFF8FAFC),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium
            )
        }

        CalloutCard(title = "🕹️ MOVEMENT & COMBAT CONTROLS", accentColor = Color(0xFF00E5FF)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Omnidirectional Thrust:", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        KeycapBadge("W"); KeycapBadge("A"); KeycapBadge("S"); KeycapBadge("D")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mouse Aim & Continuous Fire:", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        KeycapBadge("MOUSE AIM"); KeycapBadge("LMB / SPACE")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kinetic Dash (Invulnerable):", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        KeycapBadge("RMB"); KeycapBadge("C"); KeycapBadge("E")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cycle Weapon Arsenal:", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    KeycapBadge("CTRL", Color(0xFF00E5FF))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Camera Rotation & Zoom:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        KeycapBadge("TAB/R"); KeycapBadge("Z/O"); KeycapBadge("M (MUTE)")
                    }
                }
            }
        }

        CalloutCard(title = "🔫 WEAPON ARSENAL (CYCLE WITH [CTRL])", accentColor = Color(0xFF38BDF8)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "• Rapid Byte Blaster: High-precision twin-plasma bolts, rapid cyclic rate (210ms).",
                    color = Color(0xFFF8FAFC),
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )
                Text(
                    text = "• Scatter Shotgun: 5-pellet wide spread cone; devastating point-blank swarm shredder.",
                    color = Color(0xFFF8FAFC),
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )
                Text(
                    text = "• Heavy Railgun: High-voltage piercing hyper-beam; penetrates armor and entire bug columns.",
                    color = Color(0xFFF8FAFC),
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )
                Text(
                    text = "• Plasma Flak Cannon: 5-way radial spread cannon; ultimate crowd saturation.",
                    color = Color(0xFFF8FAFC),
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )
            }
        }

        CalloutCard(title = "⚡ TACTICAL PRO TIP", accentColor = Color(0xFFFFD600)) {
            Text(
                text = "DASHING GRANTS INVULNERABILITY FRAMES! When surrounded by bug swarms, do not retreat—dash directly THROUGH enemies to dodge contact damage while stamina auto-recharges.",
                color = Color(0xFFFFE082),
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Slide 2: Power-Up buffs and in-world Overclock Power Conduits.
 */
@Composable
fun Slide2_PowerUpsAndConduitsContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "⚡ POWER-UPS & POWER CONDUITS",
                color = Color(0xFF00E676),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            KeycapBadge("[SPEC-02: BOOSTERS]", Color(0xFF00E676))
        }

        CalloutCard(title = "🔍 WHAT ARE THEY?", accentColor = Color(0xFF00E676)) {
            Text(
                text = "Powerful temporary state overclocks acquired by breaking data crates, answering Kotlin trivia shards, or stepping directly onto in-world Overclock Power Conduits.",
                color = Color(0xFFF8FAFC),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium
            )
        }

        CalloutCard(title = "💠 OVERCLOCK CONDUITS (ON THE MAP)", accentColor = Color(0xFF00E676)) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "• Glowing hexagonal power stations placed in each district.",
                    color = Color(0xFFF8FAFC),
                    fontSize = 14.sp
                )
                Text(
                    text = "• Step onto a charged conduit to instantly gain 6 seconds of Hyper-Fire and Hyper-Speed!",
                    color = Color(0xFFA7FFEB),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• Conduits enter a 25-second recharge cooldown after use.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
            }
        }

        CalloutCard(title = "🧪 THE 4 POWER-UP BUFFS", accentColor = Color(0xFF69F0AE)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "🧲 Speed Magnet (12s): Double sprint speed + massive gem suction field pulling distant items.",
                    color = Color(0xFFF8FAFC),
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )
                Text(
                    text = "🛡️ Freeze Shield (10s): Emits cryo freeze waves locking all bugs in ice + full damage immunity.",
                    color = Color(0xFFF8FAFC),
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )
                Text(
                    text = "⚡ Overcharge God Mode (15s): Golden invincible aura + supercharged hyper-blaster with 2x damage.",
                    color = Color(0xFFF8FAFC),
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )
                Text(
                    text = "💣 Mega Frenzy Nuke (15s): Screen-wide EMP shockwave vaporizing all active bugs + locks score to 5x!",
                    color = Color(0xFFF8FAFC),
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )
            }
        }

        CalloutCard(title = "⚡ TACTICAL PRO TIP", accentColor = Color(0xFFFFD600)) {
            Text(
                text = "TIME YOUR CONDUIT RUNS! When an elite swarm arrives, dash onto the district Overclock Conduit to turn the tide immediately without expending defensive cooldowns.",
                color = Color(0xFFFFE082),
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Slide 3: Trivia Crates and Roguelite Version Perks.
 */
@Composable
fun Slide3_TriviaCratesAndPerksContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "🤖 TRIVIA CRATES & VERSION PERKS",
                color = Color(0xFF7F52FF),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            KeycapBadge("[SPEC-03: PERKS]", Color(0xFF7F52FF))
        }

        CalloutCard(title = "🔍 HOW TRIVIA CRATES WORK", accentColor = Color(0xFF7F52FF)) {
            Text(
                text = "Shoot wooden Data Crates placed across districts to uncover glowing Kotlin Trivia Shards. Touch a shard to open a rapid 3-option Kotlin question. Answering correctly awards permanent Version Perks!",
                color = Color(0xFFF8FAFC),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium
            )
        }

        CalloutCard(title = "🛠️ THE 4 ROGUELITE VERSION PERKS", accentColor = Color(0xFFB388FF)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "🤖 Garbage Collector Drone: Autonomous companion that orbits you, suctions distant energy gems, and auto-fires bytecode at bugs every 600ms.",
                    color = Color(0xFFF8FAFC),
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )
                Text(
                    text = "🗼 Coroutines Defense Turret: Automatically deploys a stationary rapid blaster sentry whenever you dash. Lasts 8s and targets any bug within 380 units.",
                    color = Color(0xFFF8FAFC),
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )
                Text(
                    text = "⚡ JIT Compiler: Permanent +35% projectile velocity and +35% weapon fire rate boost across all weapons.",
                    color = Color(0xFFF8FAFC),
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )
                Text(
                    text = "🛡️ Refactor Firewall: Recharging energy bubble that completely absorbs a fatal hit. Automatically recharges every 20 seconds.",
                    color = Color(0xFFF8FAFC),
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )
            }
        }

        CalloutCard(title = "⚡ TACTICAL PRO TIP", accentColor = Color(0xFF00E676)) {
            Text(
                text = "BREAK CRATES EARLY! Securing the GC Drone and Refactor Firewall in the first 30 seconds accelerates gem accumulation and guarantees survivability against late-stage swarm rushes!",
                color = Color(0xFFA7FFEB),
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Slide 4: Tactical map hazards, explosive barrels, laser fences, and bounty beacons.
 */
@Composable
fun Slide4_MapHazardsAndBountiesContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "💥 MAP HAZARDS & BOUNTY BEACONS",
                color = Color(0xFFFFD600),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            KeycapBadge("[HAZARD-04: ARENA]", Color(0xFFFFD600))
        }

        CalloutCard(title = "💥 VOLATILE BYTE BARRELS", accentColor = Color(0xFFFFD600)) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "• Yellow/black hazard striped canisters with pulsing crimson detonator caps.",
                    color = Color(0xFFF8FAFC),
                    fontSize = 14.sp
                )
                Text(
                    text = "• Shoot with 2 projectile hits to detonate a 140-unit AoE blast dealing 500 damage to all bugs in radius and 10 damage to the Titan Boss!",
                    color = Color(0xFFFFD180),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        CalloutCard(title = "⚡ DYNAMIC LASER FENCES", accentColor = Color(0xFFFF5252)) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "• Pulsing laser barrier gates across highway intersections (4s ON / 2s OFF).",
                    color = Color(0xFFF8FAFC),
                    fontSize = 14.sp
                )
                Text(
                    text = "• Instantly vaporizes any bugs that step into the beam while active!",
                    color = Color(0xFFFF8A80),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        CalloutCard(title = "🎯 DISTRICT BOUNTY BEACONS", accentColor = Color(0xFF00E5FF)) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "• Interactive terminal beacons located in North, East, South, and West districts.",
                    color = Color(0xFFF8FAFC),
                    fontSize = 14.sp
                )
                Text(
                    text = "• Touch to trigger a 15-second Frenzy Challenge ('PURGE 8 BUGS IN 15s') for +5000 bonus PTS and multiplier boost!",
                    color = Color(0xFF80D8FF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        CalloutCard(title = "⚡ TACTICAL PRO TIP", accentColor = Color(0xFF00E676)) {
            Text(
                text = "HERD SWARMS INTO HAZARDS! Do not waste ammo on dense packs or armored Deadlocks—lead the swarm into an active Laser Fence or detonate a Byte Barrel right as they cluster around it!",
                color = Color(0xFFA7FFEB),
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Slide 5: Bug Swarm factions, OutOfMemoryError Titan Boss, and game loop scoring.
 */
@Composable
fun Slide5_BugsBossAndScoringContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "👾 SWARM ANOMALIES & TITAN BOSS",
                color = Color(0xFFFF5252),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            KeycapBadge("[THREAT-05: BOSS]", Color(0xFFFF5252))
        }

        CalloutCard(title = "👾 BUG SWARM FACTIONS", accentColor = Color(0xFFFF5252)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("• Red Bug (1 HP): Rapid direct swarm rushers.", color = Color(0xFFF8FAFC), fontSize = 13.5.sp)
                Text("• Memory Leak (2 HP): Toxic green bugs that leave damaging slime trails.", color = Color(0xFFF8FAFC), fontSize = 13.5.sp)
                Text("• Swarmer: Agile mini-bugs that hunt in aggressive fast packs.", color = Color(0xFFF8FAFC), fontSize = 13.5.sp)
                Text("• Phantom: Phased stealth bugs that cloak and flicker unexpectedly.", color = Color(0xFFF8FAFC), fontSize = 13.5.sp)
                Text(
                    text = "• Deadlock Beetle (3 HP): Heavy cyan armor plates DEFLECT frontal fire! Flank their rear weak point or pierce with Heavy Railgun!",
                    color = Color(0xFFFF8A80),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        CalloutCard(title = "👹 TITAN BOSS & PHASE 2 KERNEL PANIC", accentColor = Color(0xFFFF1744)) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "• The OutOfMemoryError Titan Boss has 35 HP.",
                    color = Color(0xFFF8FAFC),
                    fontSize = 14.sp
                )
                Text(
                    text = "• At <= 40% HP (<= 14 HP), emergency KERNEL PANIC red sirens sound, summoning minion bug swarms and unleashing sweeping 5-laser barrages!",
                    color = Color(0xFFFF8A80),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        CalloutCard(title = "⏱️ 120s MATCH, OVERCLOCK & 5x STREAKS", accentColor = Color(0xFF00E5FF)) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "• Squashing quota bugs triggers Instant System Overclock Tier Ascension with score bonuses.",
                    color = Color(0xFFF8FAFC),
                    fontSize = 14.sp
                )
                Text(
                    text = "• Continuously chain green energy gems to lock in 5x MAX OVERDRIVE multiplier!",
                    color = Color(0xFF80D8FF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        CalloutCard(title = "⚡ TACTICAL PRO TIP", accentColor = Color(0xFFFFD600)) {
            Text(
                text = "DODGE THE 5-LASER BARRAGE! In Phase 2 Titan combat, circle the boss diagonally. When red laser warning beams appear, dash perpendicular to slip between the beams unharmed!",
                color = Color(0xFFFFE082),
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ======================================================================================
// MAIN MISSION BRIEFING OVERLAY COMPOSABLE
// ======================================================================================

@Composable
fun MissionBriefingOverlay(
    isPausedMode: Boolean = false,
    currentSlide: Int? = null,
    onNextSlide: (() -> Unit)? = null,
    onPrevSlide: (() -> Unit)? = null,
    onSelectSlide: ((Int) -> Unit)? = null,
    onDeploy: () -> Unit,
    onSkipAll: () -> Unit = onDeploy
) {
    var internalSlide by remember { mutableStateOf(0) }
    val activeSlide = (currentSlide ?: internalSlide).coerceIn(0, 4)

    val handleNext = {
        if (onNextSlide != null) {
            onNextSlide()
        } else {
            if (activeSlide < 4) internalSlide++ else onDeploy()
        }
    }
    val handlePrev = {
        if (onPrevSlide != null) {
            onPrevSlide()
        } else {
            if (activeSlide > 0) internalSlide--
        }
    }
    val handleSelect = { slide: Int ->
        val clamped = slide.coerceIn(0, 4)
        if (onSelectSlide != null) {
            onSelectSlide(clamped)
        } else {
            internalSlide = clamped
        }
    }

    val focusRequester = remember { FocusRequester() }
    val infiniteTransition = rememberInfiniteTransition(label = "briefing_anim")

    // Continuous 360° rotation angle for live 3D voxel models
    val cameraAngleRad by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "camera_rotation"
    )

    val deployButtonGlow by infiniteTransition.animateColor(
        initialValue = Color(0xFF00E5FF),
        targetValue = Color(0xFF00E676),
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "deploy_glow"
    )

    val deployButtonScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(750, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "deploy_scale"
    )

    // Sub-tab selectors for slides with multiple previewable models
    var slide2SubTab by remember { mutableStateOf(0) } // Slide 2: 0=GC Drone, 1=Turret, 2=Crate
    var slide3SubTab by remember { mutableStateOf(0) } // Slide 3: 0=Byte Barrel, 1=Laser Fence, 2=Bounty Beacon
    var slide4SubTab by remember { mutableStateOf(0) } // Slide 4: 0=Deadlock, 1=Titan Boss

    val playerModel = remember { PlayerVoxelActor(0f, 0f, PlayerArchetype.CYBER_CODER) }
    val conduitModel = remember { OverclockConduitVoxelActor(0f, 0f, 0f, PowerUpType.SPEED_MAGNET) }
    val droneModel = remember { CompanionDroneVoxelActor(0f, 0f) }
    val turretModel = remember { TurretVoxelActor(0f, 0f) }
    val crateModel = remember { CrateVoxelActor(0f, 0f, containsTriviaShard = true) }
    val shardModel = remember { TriviaShardVoxelActor(0f, 0f, tier = 2) }
    val barrelModel = remember { ExplosiveBarrelVoxelActor(0f, 0f, 0f) }
    val laserModel = remember { LaserFenceVoxelActor(-35f, 0f, 35f, 0f) }
    val beaconModel = remember { BountyBeaconVoxelActor(0f, 0f, 0f, districtName = "NORTH DATA VAULT") }
    val deadlockModel = remember { BugHazardActor(0f, 0f, headingAngle = 0f, bugType = BugType.DEADLOCK) }
    val bossModel = remember { BossVoxelActor(0f, 0f, 0f) }

    val isInspectionMode = LocalInspectionMode.current
    LaunchedEffect(Unit) {
        if (!isInspectionMode) {
            focusRequester.requestFocus()
            var lastTime = androidx.compose.runtime.withFrameMillis { it }
            while (true) {
                androidx.compose.runtime.withFrameMillis { frameTime ->
                    val dt = (frameTime - lastTime).toInt().coerceIn(1, 100)
                    lastTime = frameTime
                    barrelModel.update(dt)
                    laserModel.update(dt)
                    bossModel.update(dt)
                    conduitModel.update(dt)
                    beaconModel.update(dt)
                    shardModel.update(dt)
                    droneModel.orbitAngle += dt * 0.003f
                }
            }
        }
    }

    val slideAccentColor = when (activeSlide) {
        0 -> Color(0xFF00E5FF) // Slide 0: Cyan
        1 -> Color(0xFF00E676) // Slide 1: Emerald Green
        2 -> Color(0xFF7F52FF) // Slide 2: Royal Purple
        3 -> Color(0xFFFFD600) // Slide 3: Amber
        else -> Color(0xFFFF1744) // Slide 4: Crimson
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF50A0E14))
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionRight, Key.D -> {
                            handleNext()
                            true
                        }
                        Key.DirectionLeft, Key.A -> {
                            handlePrev()
                            true
                        }
                        Key.Spacebar, Key.Enter -> {
                            onDeploy()
                            true
                        }
                        Key.Escape, Key.H, Key.F1 -> {
                            onSkipAll()
                            true
                        }
                        Key.One -> { handleSelect(0); true }
                        Key.Two -> { handleSelect(1); true }
                        Key.Three -> { handleSelect(2); true }
                        Key.Four -> { handleSelect(3); true }
                        Key.Five -> { handleSelect(4); true }
                        else -> false
                    }
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 16.dp)
                .widthIn(max = 1380.dp)
                .fillMaxWidth(0.98f)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF141C2A))
                .border(2.5.dp, slideAccentColor.copy(alpha = 0.85f), RoundedCornerShape(26.dp))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(slideAccentColor)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isPausedMode) "SYSTEM PAUSED • OPERATOR TACTICAL MANUAL" else "MISSION INITIALIZATION • VISUAL FIELD CODEX",
                        color = slideAccentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ArcadeTheme.pixelHeader,
                        letterSpacing = 1.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KeycapBadge("CHAPTER ${activeSlide + 1} OF 5", slideAccentColor)
                    Text(
                        text = "v2.5 EXPANDED EDITION",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ArcadeTheme.terminalBody
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isPausedMode) "OPERATOR FIELD MANUAL" else "INTERACTIVE MISSION BRIEFING",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = ArcadeTheme.pixelHeader,
                letterSpacing = 1.5.sp
            )

            Text(
                text = "ROTATE & INSPECT 3D ASSETS • HARNESS BUFFS • UNLOCK ROGUELITE PERKS • CONQUER TITANS",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ArcadeTheme.terminalBody,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Main Split Showcase: Left 3D Pedestal (~43%) | Right Callout Cards (~57%)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // LEFT SIDE: 3D Isometric Rotating Pedestal
                Column(
                    modifier = Modifier
                        .weight(0.43f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF0F1726), Color(0xFF090E17))
                            )
                        )
                        .border(1.5.dp, slideAccentColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pedestal Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(slideAccentColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "3D HOLO-FEED",
                                color = slideAccentColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "360° LIVE ROTATION",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Model Canvas & Variant Specifics
                    when (activeSlide) {
                        0 -> {
                            // Slide 0: Cyber-Coder Player Model
                            HoloPedestalCanvas(
                                cuboids = playerModel.emitVoxelCuboids(),
                                cameraAngleRad = cameraAngleRad,
                                accentColor = Color(0xFF00E5FF),
                                zoom = 1.55f
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "CYBER-CODER OPERATOR",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "KOTLIN SPECIALIST • RAPID BLASTER • KINETIC DASH",
                                color = Color(0xFF00E5FF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        1 -> {
                            // Slide 1: Overclock Power Conduit
                            HoloPedestalCanvas(
                                cuboids = conduitModel.emitVoxelCuboids(),
                                cameraAngleRad = cameraAngleRad,
                                accentColor = Color(0xFF00E676),
                                zoom = 1.45f
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "OVERCLOCK POWER CONDUIT",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "HEX CONDUIT STATION • STEP ON FOR 6s HYPER-CHARGE",
                                color = Color(0xFF00E676),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        2 -> {
                            // Slide 2: Roguelite Perks & Crates (Sub-tabs: Drone, Turret, Crate)
                            when (slide2SubTab) {
                                0 -> {
                                    HoloPedestalCanvas(
                                        cuboids = droneModel.emitVoxelCuboids(),
                                        cameraAngleRad = cameraAngleRad,
                                        accentColor = Color(0xFF7F52FF),
                                        zoom = 2.1f
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "GARBAGE COLLECTOR DRONE",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "ORBITING COMPANION • AUTO-SUCTIONS GEMS & SHOOTS BUGS",
                                        color = Color(0xFFB388FF),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                1 -> {
                                    HoloPedestalCanvas(
                                        cuboids = turretModel.emitVoxelCuboids(),
                                        cameraAngleRad = cameraAngleRad,
                                        accentColor = Color(0xFF00E5FF),
                                        zoom = 1.8f
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "COROUTINES DEFENSE TURRET",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "AUTO-DEPLOYED ON DASH • 8s SENTRY COVERING 380f",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                else -> {
                                    val crateCuboids = crateModel.emitVoxelCuboids() + shardModel.emitVoxelCuboids().map { it.copy(x = it.x + 30f) }
                                    HoloPedestalCanvas(
                                        cuboids = crateCuboids,
                                        cameraAngleRad = cameraAngleRad,
                                        accentColor = Color(0xFFFFD600),
                                        zoom = 1.55f
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "DATA CRATE & TRIVIA SHARD",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "SHOOT CRATES TO REVEAL SHARDS • ANSWER TRIVIA FOR PERKS",
                                        color = Color(0xFFFFD600),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            // Interactive Sub-tabs for Slide 2
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("🤖 GC DRONE", "🗼 TURRET", "📦 CRATE").forEachIndexed { index, title ->
                                    val isSelected = slide2SubTab == index
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Color(0xFF7F52FF) else Color(0xFF1E293B))
                                            .border(1.5.dp, if (isSelected) Color(0xFFB388FF) else Color(0xFF334155), RoundedCornerShape(10.dp))
                                            .clickable { slide2SubTab = index }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = title,
                                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        3 -> {
                            // Slide 3: Map Hazards (Sub-tabs: Byte Barrel, Laser Fence, Bounty Beacon)
                            when (slide3SubTab) {
                                0 -> {
                                    HoloPedestalCanvas(
                                        cuboids = barrelModel.emitVoxelCuboids(),
                                        cameraAngleRad = cameraAngleRad,
                                        accentColor = Color(0xFFFFD600),
                                        zoom = 2.0f
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "VOLATILE BYTE BARREL",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "140f SHOCKWAVE • 500 AOE DAMAGE • 2-HIT TRIGGER",
                                        color = Color(0xFFFFD600),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                1 -> {
                                    HoloPedestalCanvas(
                                        cuboids = laserModel.emitVoxelCuboids(),
                                        cameraAngleRad = cameraAngleRad,
                                        accentColor = Color(0xFFFF1744),
                                        zoom = 1.3f
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "DYNAMIC LASER FENCE",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "ROAD CHOKE POINT • 4s ACTIVE / 2s OFF • INSTANT VAPORIZE",
                                        color = Color(0xFFFF5252),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                else -> {
                                    HoloPedestalCanvas(
                                        cuboids = beaconModel.emitVoxelCuboids(),
                                        cameraAngleRad = cameraAngleRad,
                                        accentColor = Color(0xFF00E5FF),
                                        zoom = 1.35f
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "DISTRICT BOUNTY BEACON",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "OUTER DISTRICT TERMINAL • 15s PURGE TRIAL • +5000 PTS",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            // Interactive Sub-tabs for Slide 3
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("💥 BARREL", "⚡ LASER", "🎯 BOUNTY").forEachIndexed { index, title ->
                                    val isSelected = slide3SubTab == index
                                    val color = when (index) {
                                        0 -> Color(0xFFFFD600)
                                        1 -> Color(0xFFFF1744)
                                        else -> Color(0xFF00E5FF)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) color else Color(0xFF1E293B))
                                            .border(1.5.dp, if (isSelected) Color.White else Color(0xFF334155), RoundedCornerShape(10.dp))
                                            .clickable { slide3SubTab = index }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = title,
                                            color = if (isSelected) (if (index == 0) Color.Black else Color.White) else Color(0xFFCBD5E1),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        4 -> {
                            // Slide 4: Bug Swarms & Boss (Sub-tabs: Deadlock Beetle, Titan Boss)
                            when (slide4SubTab) {
                                0 -> {
                                    HoloPedestalCanvas(
                                        cuboids = deadlockModel.emitVoxelCuboids(),
                                        cameraAngleRad = cameraAngleRad,
                                        accentColor = Color(0xFFB388FF),
                                        zoom = 1.9f
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "DEADLOCK ARMORED BEETLE",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "3 HP • DEFLECTS FRONTAL SHOTS • FLANK REAR WEAK POINT",
                                        color = Color(0xFFB388FF),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                else -> {
                                    HoloPedestalCanvas(
                                        cuboids = bossModel.emitVoxelCuboids(),
                                        cameraAngleRad = cameraAngleRad,
                                        accentColor = Color(0xFFFF1744),
                                        zoom = 0.88f
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "OUT-OF-MEMORY TITAN",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "35 HP • PHASE 2 KERNEL PANIC SIREN & 5-LASER BARRAGE",
                                        color = Color(0xFFFF5252),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            // Interactive Sub-tabs for Slide 4
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (slide4SubTab == 0) Color(0xFF7F52FF) else Color(0xFF1E293B))
                                        .border(1.5.dp, if (slide4SubTab == 0) Color(0xFFB388FF) else Color(0xFF334155), RoundedCornerShape(10.dp))
                                        .clickable { slide4SubTab = 0 }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "👾 DEADLOCK BEETLE",
                                        color = if (slide4SubTab == 0) Color.White else Color(0xFFCBD5E1),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (slide4SubTab == 1) Color(0xFFFF1744) else Color(0xFF1E293B))
                                        .border(1.5.dp, if (slide4SubTab == 1) Color.White else Color(0xFF334155), RoundedCornerShape(10.dp))
                                        .clickable { slide4SubTab = 1 }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "👹 TITAN BOSS",
                                        color = if (slide4SubTab == 1) Color.White else Color(0xFFCBD5E1),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // RIGHT SIDE: Modular Content Columns
                Box(
                    modifier = Modifier.weight(0.57f)
                ) {
                    when (activeSlide) {
                        0 -> Slide1_OperatorAndWeaponsContent()
                        1 -> Slide2_PowerUpsAndConduitsContent()
                        2 -> Slide3_TriviaCratesAndPerksContent()
                        3 -> Slide4_MapHazardsAndBountiesContent()
                        4 -> Slide5_BugsBossAndScoringContent()
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Navigation Bar: Prev | Indicator Pills | Next/Deploy | Skip All
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0E1420))
                    .border(1.2.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // PREV BUTTON
                val prevEnabled = activeSlide > 0
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (prevEnabled) Color(0xFF1E293B) else Color(0xFF141A24))
                        .border(
                            1.dp,
                            if (prevEnabled) Color(0xFF37474F) else Color(0xFF212833),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(enabled = prevEnabled) { handlePrev() }
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "❮ PREV SLIDE",
                            color = if (prevEnabled) Color.White else Color(0xFF546E7A),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        KeycapBadge("A / ◄", if (prevEnabled) Color(0xFF90A4AE) else Color(0xFF455A64))
                    }
                }

                // PAGER INDICATOR PILLS (● ○ ○ ○ ○)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val slideTitles = listOf(
                        "1. WEAPONS",
                        "2. POWER-UPS",
                        "3. PERKS & CRATES",
                        "4. MAP HAZARDS",
                        "5. BUGS & BOSS"
                    )
                    for (i in 0..4) {
                        val isSelected = activeSlide == i
                        val pillWidth by animateDpAsState(
                            targetValue = if (isSelected) 148.dp else 30.dp,
                            label = "pill_width"
                        )
                        val pillColor by animateColorAsState(
                            targetValue = if (isSelected) slideAccentColor else Color(0xFF263238),
                            label = "pill_color"
                        )

                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .width(pillWidth)
                                .clip(RoundedCornerShape(16.dp))
                                .background(pillColor)
                                .clickable { handleSelect(i) }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Text(
                                    text = slideTitles[i],
                                    color = if (activeSlide == 1 || activeSlide == 3) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // NEXT / DEPLOY & SKIP BUTTONS
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Next / Final Slide Deploy Button
                    if (activeSlide < 4) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .border(1.5.dp, slideAccentColor, RoundedCornerShape(12.dp))
                                .clickable { handleNext() }
                                .padding(horizontal = 18.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "NEXT SLIDE ❯",
                                    color = slideAccentColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                KeycapBadge("D / ►", slideAccentColor)
                            }
                        }
                    } else {
                        // Slide 4 Deploy Operator
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = deployButtonScale
                                    scaleY = deployButtonScale
                                }
                                .clip(RoundedCornerShape(12.dp))
                                .background(deployButtonGlow)
                                .border(1.5.dp, Color.White, RoundedCornerShape(12.dp))
                                .clickable { onDeploy() }
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isPausedMode) "RESUME MISSION [ENTER]" else "DEPLOY OPERATOR ❯",
                                    color = Color(0xFF0A0E14),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                KeycapBadge("ENTER", Color(0xFF0A0E14))
                            }
                        }
                    }

                    // Persistent SKIP ALL / PLAY NOW Bright Green Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF00E676))
                            .border(1.5.dp, Color(0xFFB9F6CA), RoundedCornerShape(12.dp))
                            .clickable { onSkipAll() }
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isPausedMode) "BACK TO RUN" else "⚡ SKIP ALL / PLAY NOW",
                                color = Color(0xFF0A0E14),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            KeycapBadge("SPACE", Color(0xFF0A0E14))
                        }
                    }
                }
            }
        }
    }
}

// ======================================================================================
// ISOLATED DESIGN-TIME PREVIEWS FOR DIRECT MANUAL EDITING IN IDE
// ======================================================================================

@Preview
@Composable
fun Preview_Slide1_Weapons() {
    Box(
        modifier = Modifier
            .background(Color(0xFF141C2A))
            .padding(24.dp)
            .width(650.dp)
    ) {
        Slide1_OperatorAndWeaponsContent()
    }
}

@Preview
@Composable
fun Preview_Slide2_PowerUps() {
    Box(
        modifier = Modifier
            .background(Color(0xFF141C2A))
            .padding(24.dp)
            .width(650.dp)
    ) {
        Slide2_PowerUpsAndConduitsContent()
    }
}

@Preview
@Composable
fun Preview_Slide3_Perks() {
    Box(
        modifier = Modifier
            .background(Color(0xFF141C2A))
            .padding(24.dp)
            .width(650.dp)
    ) {
        Slide3_TriviaCratesAndPerksContent()
    }
}

@Preview
@Composable
fun Preview_Slide4_Hazards() {
    Box(
        modifier = Modifier
            .background(Color(0xFF141C2A))
            .padding(24.dp)
            .width(650.dp)
    ) {
        Slide4_MapHazardsAndBountiesContent()
    }
}

@Preview
@Composable
fun Preview_Slide5_BugsAndBoss() {
    Box(
        modifier = Modifier
            .background(Color(0xFF141C2A))
            .padding(24.dp)
            .width(650.dp)
    ) {
        Slide5_BugsBossAndScoringContent()
    }
}

@Preview
@Composable
fun Preview_FullBriefing_Slide1() {
    MissionBriefingOverlay(
        isPausedMode = false,
        currentSlide = 0,
        onDeploy = {},
        onSkipAll = {}
    )
}

@Preview
@Composable
fun Preview_FullBriefing_Slide2() {
    MissionBriefingOverlay(
        isPausedMode = false,
        currentSlide = 1,
        onDeploy = {},
        onSkipAll = {}
    )
}

@Preview
@Composable
fun Preview_FullBriefing_Slide3() {
    MissionBriefingOverlay(
        isPausedMode = false,
        currentSlide = 2,
        onDeploy = {},
        onSkipAll = {}
    )
}

@Preview
@Composable
fun Preview_FullBriefing_Slide4() {
    MissionBriefingOverlay(
        isPausedMode = false,
        currentSlide = 3,
        onDeploy = {},
        onSkipAll = {}
    )
}

@Preview
@Composable
fun Preview_FullBriefing_Slide5() {
    MissionBriefingOverlay(
        isPausedMode = false,
        currentSlide = 4,
        onDeploy = {},
        onSkipAll = {}
    )
}

@Preview
@Composable
fun Preview_FullBriefing_Paused() {
    MissionBriefingOverlay(
        isPausedMode = true,
        currentSlide = 1,
        onDeploy = {},
        onSkipAll = {}
    )
}
