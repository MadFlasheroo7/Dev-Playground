package pro.jayeshseth.bugrush.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.jayeshseth.bugrush.game.actors.BossVoxelActor
import pro.jayeshseth.bugrush.game.actors.BugHazardActor
import pro.jayeshseth.bugrush.game.actors.BugType
import pro.jayeshseth.bugrush.game.actors.CompanionDroneVoxelActor
import pro.jayeshseth.bugrush.game.actors.DataRelayTowerActor
import pro.jayeshseth.bugrush.game.actors.PlayerArchetype
import pro.jayeshseth.bugrush.game.actors.PlayerVoxelActor
import pro.jayeshseth.bugrush.game.actors.TechBarricadeActor
import pro.jayeshseth.bugrush.game.actors.TurretVoxelActor
import pro.jayeshseth.bugrush.game.renderer.IsometricProjection
import pro.jayeshseth.bugrush.game.renderer.VoxelCuboid
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Renders any list of 3D VoxelCuboids in an isometric viewport inside a Compose Canvas.
 * Automatically centers the model around its 3D bounding box center.
 */
@Composable
fun VoxelActorCanvas(
    cuboids: List<VoxelCuboid>,
    modifier: Modifier = Modifier.size(180.dp),
    zoom: Float = 1.0f,
    cameraAngleRad: Float = 0f,
    backgroundColor: Color = Color(0xFF101722)
) {
    if (cuboids.isEmpty()) {
        Box(modifier = modifier.background(backgroundColor))
        return
    }

    // Compute bounding box to auto-center the voxel model in the frame
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
    val modelCenterZ = (minZ + maxZ) * 0.5f

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .border(1.dp, Color(0xFF263345), RoundedCornerShape(14.dp))
    ) {
        val screenCenterX = size.width * 0.5f
        // Offset Y slightly upward to visually center characters on screen
        val screenCenterY = size.height * 0.58f

        // Draw cuboids back-to-front by painter's depth (accounting for camera angle if rotated)
        val sorted = if (cameraAngleRad != 0f) {
            val cosA = kotlin.math.cos(cameraAngleRad)
            val sinA = kotlin.math.sin(cameraAngleRad)
            cuboids.sortedBy { c ->
                if (c.isFloor) -100_000f
                else {
                    val relX = c.x - modelCenterX
                    val relY = c.y - modelCenterY
                    val rotX = relX * cosA - relY * sinA
                    val rotY = relX * sinA + relY * cosA
                    rotX + rotY + c.z * 0.8f
                }
            }
        } else {
            cuboids.sortedBy { it.depth }
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

@Composable
fun CharacterCard(
    title: String,
    subtitle: String,
    cuboids: List<VoxelCuboid>,
    zoom: Float = 1.0f
) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161F2E))
            .border(1.dp, Color(0xFF334764), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VoxelActorCanvas(
            cuboids = cuboids,
            modifier = Modifier.size(176.dp),
            zoom = zoom
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            color = Color(0xFFECEFF1),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            color = Color(0xFF00E5FF),
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

// ========================================================
// INDIVIDUAL CHARACTER & PROP PREVIEWS
// ========================================================

@Preview
@Composable
fun PlayerCyberCoderPreview() {
    val actor = PlayerVoxelActor(0f, 0f, PlayerArchetype.CYBER_CODER).apply {
        headingX = 0f
        headingY = 1f
    }
    VoxelActorCanvas(actor.emitVoxelCuboids(), zoom = 1.6f)
}

@Preview
@Composable
fun PlayerKotlinBotPreview() {
    val actor = PlayerVoxelActor(0f, 0f, PlayerArchetype.KOTLIN_BOT).apply {
        headingX = 0f
        headingY = 1f
    }
    VoxelActorCanvas(actor.emitVoxelCuboids(), zoom = 1.6f)
}

@Preview
@Composable
fun PlayerArchDevPreview() {
    val actor = PlayerVoxelActor(0f, 0f, PlayerArchetype.ARCH_DEV).apply {
        headingX = 0f
        headingY = 1f
    }
    VoxelActorCanvas(actor.emitVoxelCuboids(), zoom = 1.5f)
}

@Preview
@Composable
fun BugNormalPreview() {
    val bug = BugHazardActor(0f, 0f, bugType = BugType.NORMAL)
    VoxelActorCanvas(bug.emitVoxelCuboids(), zoom = 2.2f)
}

@Preview
@Composable
fun BugMemoryLeakPreview() {
    val bug = BugHazardActor(0f, 0f, bugType = BugType.MEMORY_LEAK)
    VoxelActorCanvas(bug.emitVoxelCuboids(), zoom = 2.0f)
}

@Preview
@Composable
fun BugSwarmerPreview() {
    val bug = BugHazardActor(0f, 0f, bugType = BugType.SWARMER)
    VoxelActorCanvas(bug.emitVoxelCuboids(), zoom = 2.6f)
}

@Preview
@Composable
fun BugDeadlockPreview() {
    val bug = BugHazardActor(0f, 0f, bugType = BugType.DEADLOCK)
    VoxelActorCanvas(bug.emitVoxelCuboids(), zoom = 1.8f)
}

@Preview
@Composable
fun BossDreadnoughtPreview() {
    val boss = BossVoxelActor(0f, 0f)
    VoxelActorCanvas(boss.emitVoxelCuboids(), zoom = 0.85f)
}

@Preview
@Composable
fun CompanionDronePreview() {
    val drone = CompanionDroneVoxelActor(0f, 0f)
    VoxelActorCanvas(drone.emitVoxelCuboids(), zoom = 2.2f)
}

@Preview
@Composable
fun TurretPreview() {
    val turret = TurretVoxelActor(0f, 0f)
    VoxelActorCanvas(turret.emitVoxelCuboids(), zoom = 2.2f)
}

@Preview
@Composable
fun DataRelayTowerPreview() {
    val tower = DataRelayTowerActor(0f, 0f)
    VoxelActorCanvas(tower.emitVoxelCuboids(), zoom = 0.65f)
}

@Preview
@Composable
fun TechBarricadePreview() {
    val barricade = TechBarricadeActor(0f, 0f, isHorizontal = true)
    VoxelActorCanvas(barricade.emitVoxelCuboids(), zoom = 1.6f)
}

// ========================================================
// COMPLETE CHARACTER & PROP STUDIO GALLERY
// ========================================================

@OptIn(ExperimentalLayoutApi::class)
@Preview
@Composable
fun CharacterAndPropStudioGalleryPreview() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "KUBRIKO 3D VOXEL CHARACTER & PROP STUDIO",
            color = Color(0xFF00E5FF),
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Live preview of all procedural voxel models rendered via Compose DrawScope",
            color = Color(0xFF90A4AE),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Playable Archetypes
        Text(
            text = "— PLAYABLE HERO ARCHETYPES —",
            color = Color(0xFFFFD600),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CharacterCard(
                title = "Cyber-Coder",
                subtitle = "Blaster & Jetpack",
                cuboids = PlayerVoxelActor(0f, 0f, PlayerArchetype.CYBER_CODER).emitVoxelCuboids(),
                zoom = 1.6f
            )
            CharacterCard(
                title = "Kotlin Bot",
                subtitle = "Hover Boosters",
                cuboids = PlayerVoxelActor(0f, 0f, PlayerArchetype.KOTLIN_BOT).emitVoxelCuboids(),
                zoom = 1.6f
            )
            CharacterCard(
                title = "Arch-Dev",
                subtitle = "Railgun & Shield",
                cuboids = PlayerVoxelActor(0f, 0f, PlayerArchetype.ARCH_DEV).emitVoxelCuboids(),
                zoom = 1.5f
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bug Enemies
        Text(
            text = "— BUG HAZARDS & DREADNOUGHT BOSS —",
            color = Color(0xFFFF5252),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CharacterCard(
                title = "Normal Bug",
                subtitle = "Crawling Drone",
                cuboids = BugHazardActor(0f, 0f, bugType = BugType.NORMAL).emitVoxelCuboids(),
                zoom = 2.2f
            )
            CharacterCard(
                title = "Memory Leak",
                subtitle = "Toxic Slime Spitter",
                cuboids = BugHazardActor(0f, 0f, bugType = BugType.MEMORY_LEAK).emitVoxelCuboids(),
                zoom = 2.0f
            )
            CharacterCard(
                title = "Swarmer Bug",
                subtitle = "Fast Interceptor",
                cuboids = BugHazardActor(0f, 0f, bugType = BugType.SWARMER).emitVoxelCuboids(),
                zoom = 2.5f
            )
            CharacterCard(
                title = "Deadlock Tank",
                subtitle = "Armored Crusher",
                cuboids = BugHazardActor(0f, 0f, bugType = BugType.DEADLOCK).emitVoxelCuboids(),
                zoom = 1.8f
            )
            CharacterCard(
                title = "Kernel Panic Boss",
                subtitle = "Mega Dreadnought",
                cuboids = BossVoxelActor(0f, 0f).emitVoxelCuboids(),
                zoom = 0.85f
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Perks & Architecture
        Text(
            text = "— PERKS, TOWERS & BARRICADES —",
            color = Color(0xFF00E676),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CharacterCard(
                title = "Collector Drone",
                subtitle = "Companion Perk",
                cuboids = CompanionDroneVoxelActor(0f, 0f).emitVoxelCuboids(),
                zoom = 2.2f
            )
            CharacterCard(
                title = "Coroutines Turret",
                subtitle = "Stationary Defense",
                cuboids = TurretVoxelActor(0f, 0f).emitVoxelCuboids(),
                zoom = 2.2f
            )
            CharacterCard(
                title = "Data Relay Tower",
                subtitle = "Sector Comms Spire",
                cuboids = DataRelayTowerActor(0f, 0f).emitVoxelCuboids(),
                zoom = 0.65f
            )
            CharacterCard(
                title = "Tech Barricade",
                subtitle = "Hazard Cover Barrier",
                cuboids = TechBarricadeActor(0f, 0f, isHorizontal = true).emitVoxelCuboids(),
                zoom = 1.6f
            )
        }
    }
}
