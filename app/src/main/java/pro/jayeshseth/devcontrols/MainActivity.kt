package pro.jayeshseth.devcontrols

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.data.AdbEndpointDiscoverer
import pro.jayeshseth.devcontrols.data.DevSettingsRepository
import pro.jayeshseth.devcontrols.data.KioskPrefs
import pro.jayeshseth.devcontrols.kiosk.AnimationScales
import pro.jayeshseth.devcontrols.kiosk.KioskAccessibilityService
import pro.jayeshseth.devcontrols.kiosk.KioskNotification
import pro.jayeshseth.devcontrols.ui.AdbConsoleScreen
import pro.jayeshseth.devcontrols.ui.A11yCloakScreen
import pro.jayeshseth.devcontrols.ui.A11yDetectionScreen
import pro.jayeshseth.devcontrols.ui.AdbDetectionScreen
import pro.jayeshseth.devcontrols.ui.DisplayLabScreen
import pro.jayeshseth.devcontrols.ui.GestureLabScreen
import pro.jayeshseth.devcontrols.ui.AudioReactiveLabScreen
import pro.jayeshseth.devcontrols.ui.CustomLayoutLabScreen
import pro.jayeshseth.devcontrols.ui.DrawingLabScreen
import pro.jayeshseth.devcontrols.ui.DynamicColorLabScreen
import pro.jayeshseth.devcontrols.ui.GradientLabScreen
import pro.jayeshseth.devcontrols.ui.NestedScrollLabScreen
import pro.jayeshseth.devcontrols.ui.AccordionLabScreen
import pro.jayeshseth.devcontrols.ui.AnimatedBordersLabScreen
import pro.jayeshseth.devcontrols.ui.BarometerLabScreen
import pro.jayeshseth.devcontrols.ui.BleScannerLabScreen
import pro.jayeshseth.devcontrols.ui.BottomSheetLabScreen
import pro.jayeshseth.devcontrols.ui.CompassLabScreen
import pro.jayeshseth.devcontrols.ui.LightMeterLabScreen
import pro.jayeshseth.devcontrols.ui.NfcStatusLabScreen
import pro.jayeshseth.devcontrols.ui.QrToolsLabScreen
import pro.jayeshseth.devcontrols.ui.StepCounterLabScreen
import pro.jayeshseth.devcontrols.ui.PinPadLabScreen
import pro.jayeshseth.devcontrols.ui.PullRefreshLabScreen
import pro.jayeshseth.devcontrols.ui.RatingBarLabScreen
import pro.jayeshseth.devcontrols.ui.SegmentedControlLabScreen
import pro.jayeshseth.devcontrols.ui.SpeedDialFabLabScreen
import pro.jayeshseth.devcontrols.ui.StepperWizardLabScreen
import pro.jayeshseth.devcontrols.ui.TimelineLabScreen
import pro.jayeshseth.devcontrols.ui.CameraFilterLabScreen
import pro.jayeshseth.devcontrols.ui.ChartsLabScreen
import pro.jayeshseth.devcontrols.ui.CircularRevealThemeScreen
import pro.jayeshseth.devcontrols.ui.ColorPickerLabScreen
import pro.jayeshseth.devcontrols.ui.BoidsLabScreen
import pro.jayeshseth.devcontrols.ui.ConfettiLabScreen
import pro.jayeshseth.devcontrols.ui.DoublePendulumLabScreen
import pro.jayeshseth.devcontrols.ui.DragDropLabScreen
import pro.jayeshseth.devcontrols.ui.FallingSandLabScreen
import pro.jayeshseth.devcontrols.ui.FlipClockLabScreen
import pro.jayeshseth.devcontrols.ui.FractalTreeLabScreen
import pro.jayeshseth.devcontrols.ui.GameOfLifeLabScreen
import pro.jayeshseth.devcontrols.ui.GlassmorphismLabScreen
import pro.jayeshseth.devcontrols.ui.MandelbrotLabScreen
import pro.jayeshseth.devcontrols.ui.MazeLabScreen
import pro.jayeshseth.devcontrols.ui.MeshGalleryScreen
import pro.jayeshseth.devcontrols.ui.MetronomeLabScreen
import pro.jayeshseth.devcontrols.ui.NBodyLabScreen
import pro.jayeshseth.devcontrols.ui.OscilloscopeLabScreen
import pro.jayeshseth.devcontrols.ui.PathfindingLabScreen
import pro.jayeshseth.devcontrols.ui.PointCloudGlobeLabScreen
import pro.jayeshseth.devcontrols.ui.shaders.DissolveLabScreen
import pro.jayeshseth.devcontrols.ui.shaders.HalftoneLabScreen
import pro.jayeshseth.devcontrols.ui.shaders.RaymarchSdfLabScreen
import pro.jayeshseth.devcontrols.ui.shaders.RippleShaderLabScreen
import pro.jayeshseth.devcontrols.ui.shaders.VoronoiLabScreen
import pro.jayeshseth.devcontrols.ui.PagerTransformLabScreen
import pro.jayeshseth.devcontrols.ui.ParticleLabScreen
import pro.jayeshseth.devcontrols.ui.PathMorphLabScreen
import pro.jayeshseth.devcontrols.ui.PhysicsMotionLabScreen
import pro.jayeshseth.devcontrols.ui.RenderEffectLabScreen
import pro.jayeshseth.devcontrols.ui.ReorderListLabScreen
import pro.jayeshseth.devcontrols.ui.ScratchRevealLabScreen
import pro.jayeshseth.devcontrols.ui.ShaderEditorLabScreen
import pro.jayeshseth.devcontrols.ui.ShimmerLabScreen
import pro.jayeshseth.devcontrols.ui.SpotlightLabScreen
import pro.jayeshseth.devcontrols.ui.SpringChainLabScreen
import pro.jayeshseth.devcontrols.ui.SwipeCardLabScreen
import pro.jayeshseth.devcontrols.ui.SynthKeyboardLabScreen
import pro.jayeshseth.devcontrols.ui.TextFxLabScreen
import pro.jayeshseth.devcontrols.ui.SensorParallaxLabScreen
import pro.jayeshseth.devcontrols.ui.ShapeMorphLabScreen
import pro.jayeshseth.devcontrols.ui.SocialShareScreen
import pro.jayeshseth.devcontrols.ui.SortingVisualizerScreen
import pro.jayeshseth.devcontrols.ui.SoftBodyLabScreen
import pro.jayeshseth.devcontrols.ui.SpirographLabScreen
import pro.jayeshseth.devcontrols.ui.StarfieldLabScreen
import pro.jayeshseth.devcontrols.ui.TouchInternalsLabScreen
import pro.jayeshseth.devcontrols.ui.Transform3DLabScreen
import pro.jayeshseth.devcontrols.ui.WindowSizeLabScreen
import pro.jayeshseth.devcontrols.ui.WireframeLabScreen
import pro.jayeshseth.devcontrols.ui.Game2048Screen
import pro.jayeshseth.devcontrols.ui.SnakeGameScreen
import pro.jayeshseth.devcontrols.ui.BreakoutGameScreen
import pro.jayeshseth.devcontrols.ui.TetrisLiteScreen
import pro.jayeshseth.devcontrols.ui.MemoryMatchScreen
import pro.jayeshseth.devcontrols.ui.MinesweeperScreen
import pro.jayeshseth.devcontrols.ui.KaleidoscopeLabScreen
import pro.jayeshseth.devcontrols.ui.MatrixRainLabScreen
import pro.jayeshseth.devcontrols.ui.FireworksLabScreen
import pro.jayeshseth.devcontrols.ui.PlasmaLabScreen
import pro.jayeshseth.devcontrols.ui.WeatherLabScreen
import pro.jayeshseth.devcontrols.ui.PixelArtLabScreen
import pro.jayeshseth.devcontrols.ui.NetworkProxyLabScreen
import pro.jayeshseth.devcontrols.ui.SettingsEditorLabScreen
import pro.jayeshseth.devcontrols.ui.InputAutomationLabScreen
import pro.jayeshseth.devcontrols.ui.FeatureFlagsLabScreen
import pro.jayeshseth.devcontrols.ui.PackageExplorerLabScreen
import pro.jayeshseth.devcontrols.ui.TorchStrobeLabScreen
import pro.jayeshseth.devcontrols.ui.SpectrumLabScreen
import pro.jayeshseth.devcontrols.ui.TunerLabScreen
import pro.jayeshseth.devcontrols.ui.DecibelMeterLabScreen
import pro.jayeshseth.devcontrols.ui.SensorsDashboardLabScreen
import pro.jayeshseth.devcontrols.ui.BubbleLevelLabScreen
import pro.jayeshseth.devcontrols.ui.MetalDetectorLabScreen
import pro.jayeshseth.devcontrols.ui.BarcodeScannerLabScreen
import pro.jayeshseth.devcontrols.ui.GnssDashboardLabScreen
import pro.jayeshseth.devcontrols.ui.WifiScannerLabScreen
import pro.jayeshseth.devcontrols.ui.TtsSttLabScreen
import pro.jayeshseth.devcontrols.ui.NfcReaderLabScreen
import pro.jayeshseth.devcontrols.ui.MediaVisualizerLabScreen
import pro.jayeshseth.devcontrols.ui.FbmNoiseLabScreen
import pro.jayeshseth.devcontrols.ui.MetaballsLabScreen
import pro.jayeshseth.devcontrols.ui.TruchetLabScreen
import pro.jayeshseth.devcontrols.ui.GlitchLabScreen
import pro.jayeshseth.devcontrols.ui.EdgeDetectLabScreen
import pro.jayeshseth.devcontrols.ui.DitherLabScreen
import pro.jayeshseth.devcontrols.ui.ReactionDiffusionLabScreen
import pro.jayeshseth.devcontrols.ui.BloomLabScreen
import pro.jayeshseth.devcontrols.ui.PipLabScreen
import pro.jayeshseth.devcontrols.ui.ImmersiveLabScreen
import pro.jayeshseth.devcontrols.ui.OrientationLabScreen
import pro.jayeshseth.devcontrols.ui.CutoutInsetsLabScreen
import pro.jayeshseth.devcontrols.ui.ImeInsetsLabScreen
import pro.jayeshseth.devcontrols.ui.GestureExclusionLabScreen
import pro.jayeshseth.devcontrols.ui.FloatingBubbleLabScreen
import pro.jayeshseth.devcontrols.ui.CalendarLabScreen
import pro.jayeshseth.devcontrols.ui.WheelPickerLabScreen
import pro.jayeshseth.devcontrols.ui.TagInputLabScreen
import pro.jayeshseth.devcontrols.ui.TreeBrowserLabScreen
import pro.jayeshseth.devcontrols.ui.DataTableLabScreen
import pro.jayeshseth.devcontrols.ui.KanbanLabScreen
import pro.jayeshseth.devcontrols.ui.SignaturePadLabScreen
import pro.jayeshseth.devcontrols.ui.ProgressGalleryLabScreen
import pro.jayeshseth.devcontrols.ui.CoachMarkLabScreen
import pro.jayeshseth.devcontrols.ui.SharedElementLabScreen
import pro.jayeshseth.devcontrols.ui.ParallaxHeaderLabScreen
import pro.jayeshseth.devcontrols.ui.AnimatedContentGalleryScreen
import pro.jayeshseth.devcontrols.ui.CarouselLabScreen
import pro.jayeshseth.devcontrols.ui.StaggeredRevealLabScreen
import pro.jayeshseth.devcontrols.ui.PullToDismissLabScreen
import pro.jayeshseth.devcontrols.ui.PageCurlLabScreen
import pro.jayeshseth.devcontrols.ui.PathKeyframePlayerScreen
import pro.jayeshseth.devcontrols.ui.ConvexHullLabScreen
import pro.jayeshseth.devcontrols.ui.DelaunayLabScreen
import pro.jayeshseth.devcontrols.ui.LSystemLabScreen
import pro.jayeshseth.devcontrols.ui.NQueensLabScreen
import pro.jayeshseth.devcontrols.ui.TspLabScreen
import pro.jayeshseth.devcontrols.ui.MarchingSquaresLabScreen
import pro.jayeshseth.devcontrols.ui.QuadtreeLabScreen
import pro.jayeshseth.devcontrols.ui.PerlinTerrainLabScreen
import pro.jayeshseth.devcontrols.ui.ClothLabScreen
import pro.jayeshseth.devcontrols.ui.BilliardsLabScreen
import pro.jayeshseth.devcontrols.ui.GaltonBoardLabScreen
import pro.jayeshseth.devcontrols.ui.NewtonsCradleLabScreen
import pro.jayeshseth.devcontrols.ui.OrbitSandboxLabScreen
import pro.jayeshseth.devcontrols.ui.ChargedParticlesLabScreen
import pro.jayeshseth.devcontrols.ui.MarbleRunLabScreen
import pro.jayeshseth.devcontrols.ui.FluidLabScreen
import pro.jayeshseth.devcontrols.ui.SolidsLabScreen
import pro.jayeshseth.devcontrols.ui.Heightmap3DLabScreen
import pro.jayeshseth.devcontrols.ui.FunctionPlotterLabScreen
import pro.jayeshseth.devcontrols.ui.PointCloudMorphLabScreen
import pro.jayeshseth.devcontrols.ui.RaycasterLabScreen
import pro.jayeshseth.devcontrols.ui.ObjModelLabScreen
import pro.jayeshseth.devcontrols.ui.MandalaPadLabScreen
import pro.jayeshseth.devcontrols.ui.FlowFieldLabScreen
import pro.jayeshseth.devcontrols.ui.CirclePackingLabScreen
import pro.jayeshseth.devcontrols.ui.LightningLabScreen
import pro.jayeshseth.devcontrols.ui.ClockGalleryLabScreen
import pro.jayeshseth.devcontrols.ui.TurtleReplLabScreen
import pro.jayeshseth.devcontrols.ui.DoodlePhysicsLabScreen
import pro.jayeshseth.devcontrols.ui.ml.HandLandmarkScreen
import pro.jayeshseth.devcontrols.ui.ml.PoseLandmarkScreen
import pro.jayeshseth.devcontrols.ui.ml.ImageClassificationScreen
import pro.jayeshseth.devcontrols.ui.camera.LensExplorerScreen
import pro.jayeshseth.devcontrols.ui.camera.ProModeScreen
import pro.jayeshseth.devcontrols.ui.camera.ExposureToolsScreen
import pro.jayeshseth.devcontrols.ui.camera.VideoRecorderScreen
import pro.jayeshseth.devcontrols.ui.camera.RawCaptureScreen
import pro.jayeshseth.devcontrols.ui.camera.ExtensionsScreen
import pro.jayeshseth.devcontrols.ui.camera.HighFrameRateScreen
import pro.jayeshseth.devcontrols.ui.camera.TimelapseScreen
import pro.jayeshseth.devcontrols.ui.camera.MotionCamScreen
import pro.jayeshseth.devcontrols.ui.camera.DocScannerScreen
import pro.jayeshseth.devcontrols.ui.camera.CameraGalleryScreen
import pro.jayeshseth.devcontrols.ui.visualfx.DynamicIslandScreen
import pro.jayeshseth.devcontrols.ui.visualfx.LiquidSwipeScreen
import pro.jayeshseth.devcontrols.ui.visualfx.ParticleTextScreen
import pro.jayeshseth.devcontrols.ui.visualfx.MorphingIconsScreen
import pro.jayeshseth.devcontrols.ui.visualfx.GooeyNavBarScreen
import pro.jayeshseth.devcontrols.ui.visualfx.MorphingSheetScreen
import pro.jayeshseth.devcontrols.ui.visualfx.FrostedGlassScreen
import pro.jayeshseth.devcontrols.ui.visualfx.PaletteThemingScreen
import pro.jayeshseth.devcontrols.ui.visualfx.HolographicCardScreen
import pro.jayeshseth.devcontrols.ui.visualfx.TouchRippleWaterScreen
import pro.jayeshseth.devcontrols.ui.visualfx.MagnifierLensScreen
import pro.jayeshseth.devcontrols.ui.visualfx.AuroraScreen
import pro.jayeshseth.devcontrols.ui.visualfx.CausticsScreen
import pro.jayeshseth.devcontrols.ui.visualfx.SoapBubbleScreen
import pro.jayeshseth.devcontrols.ui.visualfx.CrtScreen
import pro.jayeshseth.devcontrols.ui.visualfx.PixelateScreen
import pro.jayeshseth.devcontrols.ui.visualfx.HeatHazeScreen
import pro.jayeshseth.devcontrols.ui.visualfx.BlackHoleScreen
import pro.jayeshseth.devcontrols.ui.visualfx.NeonSignScreen
import pro.jayeshseth.devcontrols.ui.visualfx.ConicGlowBorderScreen
import pro.jayeshseth.devcontrols.ui.visualfx.LiquidWaveFillScreen
import pro.jayeshseth.devcontrols.ui.visualfx.DepthParallaxScreen
import pro.jayeshseth.devcontrols.ui.visualfx.InkSpreadScreen
import pro.jayeshseth.devcontrols.ui.visualfx.VoronoiShatterScreen
import pro.jayeshseth.devcontrols.ui.visualfx.SmokeFluidScreen
import pro.jayeshseth.devcontrols.ui.visualfx.SpeedometerScreen
import pro.jayeshseth.devcontrols.ui.privacy.FingerprintAuditorScreen
import pro.jayeshseth.devcontrols.ui.privacy.HashingScreen
import pro.jayeshseth.devcontrols.ui.privacy.CryptoPlaygroundScreen
import pro.jayeshseth.devcontrols.ui.privacy.PasswordGeneratorScreen
import pro.jayeshseth.devcontrols.ui.privacy.BiometricLockScreen
import pro.jayeshseth.devcontrols.ui.privacy.ClipboardMonitorScreen
import pro.jayeshseth.devcontrols.ui.net.LocalServerScreen
import pro.jayeshseth.devcontrols.ui.net.NetworkInfoScreen
import pro.jayeshseth.devcontrols.ui.net.PingScanScreen
import pro.jayeshseth.devcontrols.ui.net.NsdDiscoveryScreen
import pro.jayeshseth.devcontrols.ui.net.BlePeripheralScreen
import pro.jayeshseth.devcontrols.ui.notif.NotificationStylesScreen
import pro.jayeshseth.devcontrols.ui.notif.NotificationActionsScreen
import pro.jayeshseth.devcontrols.ui.notif.NotificationProgressScreen
import pro.jayeshseth.devcontrols.ui.devtools.JsonFormatterScreen
import pro.jayeshseth.devcontrols.ui.devtools.EncoderScreen
import pro.jayeshseth.devcontrols.ui.devtools.RegexTesterScreen
import pro.jayeshseth.devcontrols.ui.devtools.JwtDecoderScreen
import pro.jayeshseth.devcontrols.ui.devtools.GeneratorsScreen
import pro.jayeshseth.devcontrols.ui.timedate.WorldClockScreen
import pro.jayeshseth.devcontrols.ui.timedate.StopwatchScreen
import pro.jayeshseth.devcontrols.ui.timedate.PomodoroScreen
import pro.jayeshseth.devcontrols.ui.toys.BubbleWrapScreen
import pro.jayeshseth.devcontrols.ui.toys.DecisionWheelScreen
import pro.jayeshseth.devcontrols.ui.toys.Magic8BallScreen
import pro.jayeshseth.devcontrols.ui.toys.WhiteNoiseScreen
import pro.jayeshseth.devcontrols.ui.audiolab.DrumMachineScreen
import pro.jayeshseth.devcontrols.ui.audiolab.ThereminScreen
import pro.jayeshseth.devcontrols.ui.audiolab.ToneGeneratorScreen
import pro.jayeshseth.devcontrols.ui.audiolab.ClapWhistleScreen
import pro.jayeshseth.devcontrols.ui.threed.Components3DScreen
import pro.jayeshseth.devcontrols.ui.threed.ShaderComponents3DScreen
import pro.jayeshseth.devcontrols.ui.maps.CoordinateToolsScreen
import pro.jayeshseth.devcontrols.ui.maps.SunMoonScreen
import pro.jayeshseth.devcontrols.ui.maps.CompassNavigatorScreen
import pro.jayeshseth.devcontrols.ui.maps.GpsTrackerScreen
import pro.jayeshseth.devcontrols.ui.SystemBarSheetScreen
import pro.jayeshseth.devcontrols.ui.WindowFxLabScreen
import pro.jayeshseth.devcontrols.ui.AdbcScreen
import pro.jayeshseth.devcontrols.ui.AllowListScreen
import pro.jayeshseth.devcontrols.ui.ForceDarkScreen
import pro.jayeshseth.devcontrols.ui.HapticsLabScreen
import pro.jayeshseth.devcontrols.ui.HideDevScreen
import pro.jayeshseth.devcontrols.ui.InfoDumpScreen
import pro.jayeshseth.devcontrols.ui.PermissionManagerScreen
import pro.jayeshseth.devcontrols.ui.PredictiveBackPlaygroundScreen
import pro.jayeshseth.devcontrols.ui.UsbInspectorScreen
import pro.jayeshseth.devcontrols.ui.FocusCallsSection
import pro.jayeshseth.devcontrols.ui.KioskTargetPickerScreen
import pro.jayeshseth.devcontrols.ui.theme.DevControlsTheme
import pro.jayeshseth.devcontrols.util.ForegroundAppDetector
import pro.jayeshseth.devcontrols.util.PermissionUtils
import pro.jayeshseth.devcontrols.util.WifiUtils
import pro.jayeshseth.devcontrols.widget.DevControlsWidgetReceiver
import pro.jayeshseth.devcontrols.widget.ToggleActionCallback

/**
 * The single activity. In-app navigation is a flat [Route] enum rendered one screen
 * at a time (no Navigation library — the route set is small and flat). `MAIN` is the
 * status panel + the column of feature-entry buttons; every other route is one
 * feature screen with a Back callback to `MAIN`. On launch it requests
 * `POST_NOTIFICATIONS` and reads the permission/appop state shown in the status
 * panel and used to gate Kiosk start. Feature→requirement mapping: see README.md.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val openKioskPicker = intent?.getBooleanExtra(
            ToggleActionCallback.EXTRA_OPEN_KIOSK_PICKER, false
        ) ?: false
        setContent {
            DevControlsTheme {
                // Ask for POST_NOTIFICATIONS on first launch (API 33+). Needed for
                // the pairing/kiosk/call notifications to actually post.
                val notifLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* result handled by re-checking PermissionUtils elsewhere */ }
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= 33 &&
                        !PermissionUtils.hasPostNotifications(this@MainActivity)
                    ) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                var route by remember {
                    mutableStateOf(if (openKioskPicker) Route.KIOSK_PICKER else Route.MAIN)
                }
                // Hoisted so they survive navigating into a lab and back.
                val mainScroll = rememberScrollState()
                val expandedCats = remember { androidx.compose.runtime.mutableStateMapOf<String, Boolean>() }
                // System back / gesture returns to the main list instead of exiting.
                androidx.activity.compose.BackHandler(enabled = route != Route.MAIN) { route = Route.MAIN }
                when (route) {
                    Route.MAIN -> androidx.compose.runtime.CompositionLocalProvider(LocalExpandedCats provides expandedCats) {
                        DevControlsScreen(
                        mainScroll = mainScroll,
                        onNavigate = { route = it }
                    )
                    }
                    Route.ALLOWLIST -> AllowListScreen(onBack = { route = Route.MAIN })
                    Route.KIOSK_PICKER -> KioskTargetPickerScreen(onBack = { route = Route.MAIN })
                    Route.ADB_CONSOLE -> AdbConsoleScreen(onBack = { route = Route.MAIN })
                    Route.ADB_C -> AdbcScreen(onBack = { route = Route.MAIN })
                    Route.ADB_PM -> PermissionManagerScreen(onBack = { route = Route.MAIN })
                    Route.HIDE_DEV -> HideDevScreen(onBack = { route = Route.MAIN })
                    Route.HAPTICS -> HapticsLabScreen(onBack = { route = Route.MAIN })
                    Route.USB_INSPECTOR -> UsbInspectorScreen(onBack = { route = Route.MAIN })
                    Route.PREDICTIVE_BACK -> PredictiveBackPlaygroundScreen(onBack = { route = Route.MAIN })
                    Route.FORCE_DARK -> ForceDarkScreen(onBack = { route = Route.MAIN })
                    Route.INFO_DUMP -> InfoDumpScreen(onBack = { route = Route.MAIN })
                    Route.ADB_DETECTION -> AdbDetectionScreen(onBack = { route = Route.MAIN })
                    Route.A11Y_DETECTION -> A11yDetectionScreen(onBack = { route = Route.MAIN })
                    Route.A11Y_CLOAK -> A11yCloakScreen(onBack = { route = Route.MAIN })
                    Route.GESTURE_LAB -> GestureLabScreen(onBack = { route = Route.MAIN })
                    Route.SYSTEM_BAR_SHEET -> SystemBarSheetScreen(onBack = { route = Route.MAIN })
                    Route.WINDOW_FX -> WindowFxLabScreen(onBack = { route = Route.MAIN })
                    Route.DISPLAY_LAB -> DisplayLabScreen(onBack = { route = Route.MAIN })
                    Route.GRADIENT_LAB -> GradientLabScreen(onBack = { route = Route.MAIN })
                    Route.SOCIAL_SHARE -> SocialShareScreen(onBack = { route = Route.MAIN })
                    Route.SHADER_EDITOR -> ShaderEditorLabScreen(onBack = { route = Route.MAIN })
                    Route.RENDER_EFFECT -> RenderEffectLabScreen(onBack = { route = Route.MAIN })
                    Route.TRANSFORM_3D -> Transform3DLabScreen(onBack = { route = Route.MAIN })
                    Route.CUSTOM_LAYOUT -> CustomLayoutLabScreen(onBack = { route = Route.MAIN })
                    Route.PHYSICS_MOTION -> PhysicsMotionLabScreen(onBack = { route = Route.MAIN })
                    Route.NESTED_SCROLL -> NestedScrollLabScreen(onBack = { route = Route.MAIN })
                    Route.TOUCH_INTERNALS -> TouchInternalsLabScreen(onBack = { route = Route.MAIN })
                    Route.WINDOW_SIZE -> WindowSizeLabScreen(onBack = { route = Route.MAIN })
                    Route.DYNAMIC_COLOR -> DynamicColorLabScreen(onBack = { route = Route.MAIN })
                    Route.AUDIO_REACTIVE -> AudioReactiveLabScreen(onBack = { route = Route.MAIN })
                    Route.SENSOR_PARALLAX -> SensorParallaxLabScreen(onBack = { route = Route.MAIN })
                    Route.SHAPE_MORPH -> ShapeMorphLabScreen(onBack = { route = Route.MAIN })
                    Route.PATH_MORPH -> PathMorphLabScreen(onBack = { route = Route.MAIN })
                    Route.PAGER_TRANSFORM -> PagerTransformLabScreen(onBack = { route = Route.MAIN })
                    Route.REORDER_LIST -> ReorderListLabScreen(onBack = { route = Route.MAIN })
                    Route.SWIPE_CARD -> SwipeCardLabScreen(onBack = { route = Route.MAIN })
                    Route.DRAWING -> DrawingLabScreen(onBack = { route = Route.MAIN })
                    Route.PARTICLES -> ParticleLabScreen(onBack = { route = Route.MAIN })
                    Route.CONFETTI -> ConfettiLabScreen(onBack = { route = Route.MAIN })
                    Route.SCRATCH -> ScratchRevealLabScreen(onBack = { route = Route.MAIN })
                    Route.TEXT_FX -> TextFxLabScreen(onBack = { route = Route.MAIN })
                    Route.CIRCULAR_REVEAL -> CircularRevealThemeScreen(onBack = { route = Route.MAIN })
                    Route.CHARTS -> ChartsLabScreen(onBack = { route = Route.MAIN })
                    Route.COLOR_PICKER -> ColorPickerLabScreen(onBack = { route = Route.MAIN })
                    Route.GAME_OF_LIFE -> GameOfLifeLabScreen(onBack = { route = Route.MAIN })
                    Route.GLASSMORPHISM -> GlassmorphismLabScreen(onBack = { route = Route.MAIN })
                    Route.SHIMMER -> ShimmerLabScreen(onBack = { route = Route.MAIN })
                    Route.ANIMATED_BORDERS -> AnimatedBordersLabScreen(onBack = { route = Route.MAIN })
                    Route.SPOTLIGHT -> SpotlightLabScreen(onBack = { route = Route.MAIN })
                    Route.SPRING_CHAIN -> SpringChainLabScreen(onBack = { route = Route.MAIN })
                    Route.FLIP_CLOCK -> FlipClockLabScreen(onBack = { route = Route.MAIN })
                    Route.MANDELBROT -> MandelbrotLabScreen(onBack = { route = Route.MAIN })
                    Route.CAMERA_FILTER -> CameraFilterLabScreen(onBack = { route = Route.MAIN })
                    Route.DRAG_DROP -> DragDropLabScreen(onBack = { route = Route.MAIN })
                    Route.MESH_GALLERY -> MeshGalleryScreen(onBack = { route = Route.MAIN })
                    Route.RAYMARCH -> RaymarchSdfLabScreen(onBack = { route = Route.MAIN })
                    Route.RIPPLE -> RippleShaderLabScreen(onBack = { route = Route.MAIN })
                    Route.VORONOI -> VoronoiLabScreen(onBack = { route = Route.MAIN })
                    Route.HALFTONE -> HalftoneLabScreen(onBack = { route = Route.MAIN })
                    Route.DISSOLVE -> DissolveLabScreen(onBack = { route = Route.MAIN })
                    Route.SORTING -> SortingVisualizerScreen(onBack = { route = Route.MAIN })
                    Route.PATHFINDING -> PathfindingLabScreen(onBack = { route = Route.MAIN })
                    Route.MAZE -> MazeLabScreen(onBack = { route = Route.MAIN })
                    Route.FRACTAL_TREE -> FractalTreeLabScreen(onBack = { route = Route.MAIN })
                    Route.SPIROGRAPH -> SpirographLabScreen(onBack = { route = Route.MAIN })
                    Route.DOUBLE_PENDULUM -> DoublePendulumLabScreen(onBack = { route = Route.MAIN })
                    Route.NBODY -> NBodyLabScreen(onBack = { route = Route.MAIN })
                    Route.BOIDS -> BoidsLabScreen(onBack = { route = Route.MAIN })
                    Route.FALLING_SAND -> FallingSandLabScreen(onBack = { route = Route.MAIN })
                    Route.SOFT_BODY -> SoftBodyLabScreen(onBack = { route = Route.MAIN })
                    Route.WIREFRAME -> WireframeLabScreen(onBack = { route = Route.MAIN })
                    Route.GLOBE -> PointCloudGlobeLabScreen(onBack = { route = Route.MAIN })
                    Route.STARFIELD -> StarfieldLabScreen(onBack = { route = Route.MAIN })
                    Route.SYNTH -> SynthKeyboardLabScreen(onBack = { route = Route.MAIN })
                    Route.OSCILLOSCOPE -> OscilloscopeLabScreen(onBack = { route = Route.MAIN })
                    Route.METRONOME -> MetronomeLabScreen(onBack = { route = Route.MAIN })
                    Route.SEGMENTED -> SegmentedControlLabScreen(onBack = { route = Route.MAIN })
                    Route.SPEED_DIAL -> SpeedDialFabLabScreen(onBack = { route = Route.MAIN })
                    Route.BOTTOM_SHEET -> BottomSheetLabScreen(onBack = { route = Route.MAIN })
                    Route.STEPPER -> StepperWizardLabScreen(onBack = { route = Route.MAIN })
                    Route.TIMELINE -> TimelineLabScreen(onBack = { route = Route.MAIN })
                    Route.RATING -> RatingBarLabScreen(onBack = { route = Route.MAIN })
                    Route.PULL_REFRESH -> PullRefreshLabScreen(onBack = { route = Route.MAIN })
                    Route.ACCORDION -> AccordionLabScreen(onBack = { route = Route.MAIN })
                    Route.PIN_PAD -> PinPadLabScreen(onBack = { route = Route.MAIN })
                    Route.COMPASS -> CompassLabScreen(onBack = { route = Route.MAIN })
                    Route.LIGHT_METER -> LightMeterLabScreen(onBack = { route = Route.MAIN })
                    Route.BAROMETER -> BarometerLabScreen(onBack = { route = Route.MAIN })
                    Route.STEP_COUNTER -> StepCounterLabScreen(onBack = { route = Route.MAIN })
                    Route.BLE -> BleScannerLabScreen(onBack = { route = Route.MAIN })
                    Route.QR -> QrToolsLabScreen(onBack = { route = Route.MAIN })
                    Route.NFC -> NfcStatusLabScreen(onBack = { route = Route.MAIN })
                    Route.GAME_2048 -> Game2048Screen(onBack = { route = Route.MAIN })
                    Route.SNAKE -> SnakeGameScreen(onBack = { route = Route.MAIN })
                    Route.BREAKOUT -> BreakoutGameScreen(onBack = { route = Route.MAIN })
                    Route.TETRIS -> TetrisLiteScreen(onBack = { route = Route.MAIN })
                    Route.MEMORY_MATCH -> MemoryMatchScreen(onBack = { route = Route.MAIN })
                    Route.MINESWEEPER -> MinesweeperScreen(onBack = { route = Route.MAIN })
                    Route.KALEIDOSCOPE -> KaleidoscopeLabScreen(onBack = { route = Route.MAIN })
                    Route.MATRIX_RAIN -> MatrixRainLabScreen(onBack = { route = Route.MAIN })
                    Route.FIREWORKS -> FireworksLabScreen(onBack = { route = Route.MAIN })
                    Route.PLASMA -> PlasmaLabScreen(onBack = { route = Route.MAIN })
                    Route.WEATHER -> WeatherLabScreen(onBack = { route = Route.MAIN })
                    Route.PIXEL_ART -> PixelArtLabScreen(onBack = { route = Route.MAIN })
                    Route.NETWORK_PROXY -> NetworkProxyLabScreen(onBack = { route = Route.MAIN })
                    Route.SETTINGS_EDITOR -> SettingsEditorLabScreen(onBack = { route = Route.MAIN })
                    Route.INPUT_AUTOMATION -> InputAutomationLabScreen(onBack = { route = Route.MAIN })
                    Route.FEATURE_FLAGS -> FeatureFlagsLabScreen(onBack = { route = Route.MAIN })
                    Route.PACKAGE_EXPLORER -> PackageExplorerLabScreen(onBack = { route = Route.MAIN })
                    Route.TORCH_STROBE -> TorchStrobeLabScreen(onBack = { route = Route.MAIN })
                    Route.SPECTRUM -> SpectrumLabScreen(onBack = { route = Route.MAIN })
                    Route.TUNER -> TunerLabScreen(onBack = { route = Route.MAIN })
                    Route.DECIBEL -> DecibelMeterLabScreen(onBack = { route = Route.MAIN })
                    Route.SENSORS_DASH -> SensorsDashboardLabScreen(onBack = { route = Route.MAIN })
                    Route.BUBBLE_LEVEL -> BubbleLevelLabScreen(onBack = { route = Route.MAIN })
                    Route.METAL_DETECTOR -> MetalDetectorLabScreen(onBack = { route = Route.MAIN })
                    Route.BARCODE_SCANNER -> BarcodeScannerLabScreen(onBack = { route = Route.MAIN })
                    Route.GNSS -> GnssDashboardLabScreen(onBack = { route = Route.MAIN })
                    Route.WIFI_SCANNER -> WifiScannerLabScreen(onBack = { route = Route.MAIN })
                    Route.TTS_STT -> TtsSttLabScreen(onBack = { route = Route.MAIN })
                    Route.NFC_READER -> NfcReaderLabScreen(onBack = { route = Route.MAIN })
                    Route.MEDIA_VIZ -> MediaVisualizerLabScreen(onBack = { route = Route.MAIN })
                    Route.FBM -> FbmNoiseLabScreen(onBack = { route = Route.MAIN })
                    Route.METABALLS -> MetaballsLabScreen(onBack = { route = Route.MAIN })
                    Route.TRUCHET -> TruchetLabScreen(onBack = { route = Route.MAIN })
                    Route.GLITCH -> GlitchLabScreen(onBack = { route = Route.MAIN })
                    Route.EDGE_DETECT -> EdgeDetectLabScreen(onBack = { route = Route.MAIN })
                    Route.DITHER -> DitherLabScreen(onBack = { route = Route.MAIN })
                    Route.REACTION_DIFFUSION -> ReactionDiffusionLabScreen(onBack = { route = Route.MAIN })
                    Route.BLOOM -> BloomLabScreen(onBack = { route = Route.MAIN })
                    Route.PIP -> PipLabScreen(onBack = { route = Route.MAIN })
                    Route.IMMERSIVE -> ImmersiveLabScreen(onBack = { route = Route.MAIN })
                    Route.ORIENTATION -> OrientationLabScreen(onBack = { route = Route.MAIN })
                    Route.CUTOUT_INSETS -> CutoutInsetsLabScreen(onBack = { route = Route.MAIN })
                    Route.IME_INSETS -> ImeInsetsLabScreen(onBack = { route = Route.MAIN })
                    Route.GESTURE_EXCLUSION -> GestureExclusionLabScreen(onBack = { route = Route.MAIN })
                    Route.FLOATING_BUBBLE -> FloatingBubbleLabScreen(onBack = { route = Route.MAIN })
                    Route.CALENDAR -> CalendarLabScreen(onBack = { route = Route.MAIN })
                    Route.WHEEL_PICKER -> WheelPickerLabScreen(onBack = { route = Route.MAIN })
                    Route.TAG_INPUT -> TagInputLabScreen(onBack = { route = Route.MAIN })
                    Route.TREE_BROWSER -> TreeBrowserLabScreen(onBack = { route = Route.MAIN })
                    Route.DATA_TABLE -> DataTableLabScreen(onBack = { route = Route.MAIN })
                    Route.KANBAN -> KanbanLabScreen(onBack = { route = Route.MAIN })
                    Route.SIGNATURE -> SignaturePadLabScreen(onBack = { route = Route.MAIN })
                    Route.PROGRESS_GALLERY -> ProgressGalleryLabScreen(onBack = { route = Route.MAIN })
                    Route.COACH_MARK -> CoachMarkLabScreen(onBack = { route = Route.MAIN })
                    Route.SHARED_ELEMENT -> SharedElementLabScreen(onBack = { route = Route.MAIN })
                    Route.PARALLAX_HEADER -> ParallaxHeaderLabScreen(onBack = { route = Route.MAIN })
                    Route.ANIMATED_CONTENT -> AnimatedContentGalleryScreen(onBack = { route = Route.MAIN })
                    Route.CAROUSEL -> CarouselLabScreen(onBack = { route = Route.MAIN })
                    Route.STAGGERED -> StaggeredRevealLabScreen(onBack = { route = Route.MAIN })
                    Route.PULL_DISMISS -> PullToDismissLabScreen(onBack = { route = Route.MAIN })
                    Route.PAGE_CURL -> PageCurlLabScreen(onBack = { route = Route.MAIN })
                    Route.PATH_PLAYER -> PathKeyframePlayerScreen(onBack = { route = Route.MAIN })
                    Route.CONVEX_HULL -> ConvexHullLabScreen(onBack = { route = Route.MAIN })
                    Route.DELAUNAY -> DelaunayLabScreen(onBack = { route = Route.MAIN })
                    Route.LSYSTEM -> LSystemLabScreen(onBack = { route = Route.MAIN })
                    Route.NQUEENS -> NQueensLabScreen(onBack = { route = Route.MAIN })
                    Route.TSP -> TspLabScreen(onBack = { route = Route.MAIN })
                    Route.MARCHING_SQUARES -> MarchingSquaresLabScreen(onBack = { route = Route.MAIN })
                    Route.QUADTREE -> QuadtreeLabScreen(onBack = { route = Route.MAIN })
                    Route.PERLIN_TERRAIN -> PerlinTerrainLabScreen(onBack = { route = Route.MAIN })
                    Route.CLOTH -> ClothLabScreen(onBack = { route = Route.MAIN })
                    Route.BILLIARDS -> BilliardsLabScreen(onBack = { route = Route.MAIN })
                    Route.GALTON -> GaltonBoardLabScreen(onBack = { route = Route.MAIN })
                    Route.NEWTONS_CRADLE -> NewtonsCradleLabScreen(onBack = { route = Route.MAIN })
                    Route.ORBIT_SANDBOX -> OrbitSandboxLabScreen(onBack = { route = Route.MAIN })
                    Route.CHARGED -> ChargedParticlesLabScreen(onBack = { route = Route.MAIN })
                    Route.MARBLE_RUN -> MarbleRunLabScreen(onBack = { route = Route.MAIN })
                    Route.FLUID -> FluidLabScreen(onBack = { route = Route.MAIN })
                    Route.SOLIDS -> SolidsLabScreen(onBack = { route = Route.MAIN })
                    Route.HEIGHTMAP3D -> Heightmap3DLabScreen(onBack = { route = Route.MAIN })
                    Route.FUNCTION_PLOTTER -> FunctionPlotterLabScreen(onBack = { route = Route.MAIN })
                    Route.POINT_CLOUD_MORPH -> PointCloudMorphLabScreen(onBack = { route = Route.MAIN })
                    Route.RAYCASTER -> RaycasterLabScreen(onBack = { route = Route.MAIN })
                    Route.OBJ_MODEL -> ObjModelLabScreen(onBack = { route = Route.MAIN })
                    Route.MANDALA -> MandalaPadLabScreen(onBack = { route = Route.MAIN })
                    Route.FLOW_FIELD -> FlowFieldLabScreen(onBack = { route = Route.MAIN })
                    Route.CIRCLE_PACKING -> CirclePackingLabScreen(onBack = { route = Route.MAIN })
                    Route.LIGHTNING -> LightningLabScreen(onBack = { route = Route.MAIN })
                    Route.CLOCK_GALLERY -> ClockGalleryLabScreen(onBack = { route = Route.MAIN })
                    Route.TURTLE -> TurtleReplLabScreen(onBack = { route = Route.MAIN })
                    Route.DOODLE_PHYSICS -> DoodlePhysicsLabScreen(onBack = { route = Route.MAIN })
                    Route.HAND_LANDMARK -> HandLandmarkScreen(onBack = { route = Route.MAIN })
                    Route.POSE_LANDMARK -> PoseLandmarkScreen(onBack = { route = Route.MAIN })
                    Route.IMAGE_CLASSIFY -> ImageClassificationScreen(onBack = { route = Route.MAIN })
                    Route.LENS_EXPLORER -> LensExplorerScreen(onBack = { route = Route.MAIN })
                    Route.PRO_MODE -> ProModeScreen(onBack = { route = Route.MAIN })
                    Route.EXPOSURE_TOOLS -> ExposureToolsScreen(onBack = { route = Route.MAIN })
                    Route.VIDEO_RECORDER -> VideoRecorderScreen(onBack = { route = Route.MAIN })
                    Route.RAW_CAPTURE -> RawCaptureScreen(onBack = { route = Route.MAIN })
                    Route.CAM_EXTENSIONS -> ExtensionsScreen(onBack = { route = Route.MAIN })
                    Route.HIGH_FPS -> HighFrameRateScreen(onBack = { route = Route.MAIN })
                    Route.TIMELAPSE -> TimelapseScreen(onBack = { route = Route.MAIN })
                    Route.MOTION_CAM -> MotionCamScreen(onBack = { route = Route.MAIN })
                    Route.DOC_SCANNER -> DocScannerScreen(onBack = { route = Route.MAIN })
                    Route.CAMERA_GALLERY -> CameraGalleryScreen(onBack = { route = Route.MAIN })
                    Route.DYNAMIC_ISLAND -> DynamicIslandScreen(onBack = { route = Route.MAIN })
                    Route.LIQUID_SWIPE -> LiquidSwipeScreen(onBack = { route = Route.MAIN })
                    Route.PARTICLE_TEXT -> ParticleTextScreen(onBack = { route = Route.MAIN })
                    Route.MORPHING_ICONS -> MorphingIconsScreen(onBack = { route = Route.MAIN })
                    Route.GOOEY_NAV -> GooeyNavBarScreen(onBack = { route = Route.MAIN })
                    Route.MORPHING_SHEET -> MorphingSheetScreen(onBack = { route = Route.MAIN })
                    Route.FROSTED_GLASS -> FrostedGlassScreen(onBack = { route = Route.MAIN })
                    Route.PALETTE_THEMING -> PaletteThemingScreen(onBack = { route = Route.MAIN })
                    Route.HOLOGRAPHIC -> HolographicCardScreen(onBack = { route = Route.MAIN })
                    Route.TOUCH_RIPPLE -> TouchRippleWaterScreen(onBack = { route = Route.MAIN })
                    Route.MAGNIFIER -> MagnifierLensScreen(onBack = { route = Route.MAIN })
                    Route.AURORA -> AuroraScreen(onBack = { route = Route.MAIN })
                    Route.CAUSTICS -> CausticsScreen(onBack = { route = Route.MAIN })
                    Route.SOAP_BUBBLE -> SoapBubbleScreen(onBack = { route = Route.MAIN })
                    Route.CRT -> CrtScreen(onBack = { route = Route.MAIN })
                    Route.PIXELATE -> PixelateScreen(onBack = { route = Route.MAIN })
                    Route.HEAT_HAZE -> HeatHazeScreen(onBack = { route = Route.MAIN })
                    Route.BLACK_HOLE -> BlackHoleScreen(onBack = { route = Route.MAIN })
                    Route.NEON_SIGN -> NeonSignScreen(onBack = { route = Route.MAIN })
                    Route.CONIC_GLOW -> ConicGlowBorderScreen(onBack = { route = Route.MAIN })
                    Route.WAVE_FILL -> LiquidWaveFillScreen(onBack = { route = Route.MAIN })
                    Route.DEPTH_PARALLAX -> DepthParallaxScreen(onBack = { route = Route.MAIN })
                    Route.INK_SPREAD -> InkSpreadScreen(onBack = { route = Route.MAIN })
                    Route.VORONOI_SHATTER -> VoronoiShatterScreen(onBack = { route = Route.MAIN })
                    Route.SMOKE_FLUID -> SmokeFluidScreen(onBack = { route = Route.MAIN })
                    Route.SPEEDOMETER -> SpeedometerScreen(onBack = { route = Route.MAIN })
                    Route.FINGERPRINT_AUDIT -> FingerprintAuditorScreen(onBack = { route = Route.MAIN })
                    Route.HASHING -> HashingScreen(onBack = { route = Route.MAIN })
                    Route.CRYPTO_PLAY -> CryptoPlaygroundScreen(onBack = { route = Route.MAIN })
                    Route.PASSWORD_GEN -> PasswordGeneratorScreen(onBack = { route = Route.MAIN })
                    Route.BIOMETRIC_LOCK -> BiometricLockScreen(onBack = { route = Route.MAIN })
                    Route.CLIPBOARD_MON -> ClipboardMonitorScreen(onBack = { route = Route.MAIN })
                    Route.LOCAL_SERVER -> LocalServerScreen(onBack = { route = Route.MAIN })
                    Route.NETWORK_INFO -> NetworkInfoScreen(onBack = { route = Route.MAIN })
                    Route.PING_SCAN -> PingScanScreen(onBack = { route = Route.MAIN })
                    Route.NSD_DISCOVERY -> NsdDiscoveryScreen(onBack = { route = Route.MAIN })
                    Route.BLE_PERIPHERAL -> BlePeripheralScreen(onBack = { route = Route.MAIN })
                    Route.NOTIF_STYLES -> NotificationStylesScreen(onBack = { route = Route.MAIN })
                    Route.NOTIF_ACTIONS -> NotificationActionsScreen(onBack = { route = Route.MAIN })
                    Route.NOTIF_PROGRESS -> NotificationProgressScreen(onBack = { route = Route.MAIN })
                    Route.JSON_FMT -> JsonFormatterScreen(onBack = { route = Route.MAIN })
                    Route.ENCODER -> EncoderScreen(onBack = { route = Route.MAIN })
                    Route.REGEX_TEST -> RegexTesterScreen(onBack = { route = Route.MAIN })
                    Route.JWT_DECODE -> JwtDecoderScreen(onBack = { route = Route.MAIN })
                    Route.GENERATORS -> GeneratorsScreen(onBack = { route = Route.MAIN })
                    Route.WORLD_CLOCK -> WorldClockScreen(onBack = { route = Route.MAIN })
                    Route.STOPWATCH -> StopwatchScreen(onBack = { route = Route.MAIN })
                    Route.POMODORO -> PomodoroScreen(onBack = { route = Route.MAIN })
                    Route.BUBBLE_WRAP -> BubbleWrapScreen(onBack = { route = Route.MAIN })
                    Route.DECISION_WHEEL -> DecisionWheelScreen(onBack = { route = Route.MAIN })
                    Route.MAGIC8 -> Magic8BallScreen(onBack = { route = Route.MAIN })
                    Route.WHITE_NOISE -> WhiteNoiseScreen(onBack = { route = Route.MAIN })
                    Route.DRUM_MACHINE -> DrumMachineScreen(onBack = { route = Route.MAIN })
                    Route.THEREMIN -> ThereminScreen(onBack = { route = Route.MAIN })
                    Route.TONE_GEN -> ToneGeneratorScreen(onBack = { route = Route.MAIN })
                    Route.CLAP_WHISTLE -> ClapWhistleScreen(onBack = { route = Route.MAIN })
                    Route.COMPONENTS_3D -> Components3DScreen(onBack = { route = Route.MAIN })
                    Route.SHADER_3D -> ShaderComponents3DScreen(onBack = { route = Route.MAIN })
                    Route.COORD_TOOLS -> CoordinateToolsScreen(onBack = { route = Route.MAIN })
                    Route.SUN_MOON -> SunMoonScreen(onBack = { route = Route.MAIN })
                    Route.COMPASS_NAV -> CompassNavigatorScreen(onBack = { route = Route.MAIN })
                    Route.GPS_TRACKER -> GpsTrackerScreen(onBack = { route = Route.MAIN })
                }
            }
        }
    }
}

private enum class Route { MAIN, ALLOWLIST, KIOSK_PICKER, ADB_CONSOLE, ADB_C, ADB_PM, HIDE_DEV, HAPTICS, USB_INSPECTOR, PREDICTIVE_BACK, FORCE_DARK, INFO_DUMP, ADB_DETECTION, A11Y_DETECTION, A11Y_CLOAK, GESTURE_LAB, SYSTEM_BAR_SHEET, WINDOW_FX, DISPLAY_LAB, GRADIENT_LAB, SOCIAL_SHARE, SHADER_EDITOR, RENDER_EFFECT, TRANSFORM_3D, CUSTOM_LAYOUT, PHYSICS_MOTION, NESTED_SCROLL, TOUCH_INTERNALS, WINDOW_SIZE, DYNAMIC_COLOR, AUDIO_REACTIVE, SENSOR_PARALLAX, SHAPE_MORPH, PATH_MORPH, PAGER_TRANSFORM, REORDER_LIST, SWIPE_CARD, DRAWING, PARTICLES, CONFETTI, SCRATCH, TEXT_FX, CIRCULAR_REVEAL, CHARTS, COLOR_PICKER, GAME_OF_LIFE, GLASSMORPHISM, SHIMMER, ANIMATED_BORDERS, SPOTLIGHT, SPRING_CHAIN, FLIP_CLOCK, MANDELBROT, CAMERA_FILTER, DRAG_DROP, MESH_GALLERY, RAYMARCH, RIPPLE, VORONOI, HALFTONE, DISSOLVE, SORTING, PATHFINDING, MAZE, FRACTAL_TREE, SPIROGRAPH, DOUBLE_PENDULUM, NBODY, BOIDS, FALLING_SAND, SOFT_BODY, WIREFRAME, GLOBE, STARFIELD, SYNTH, OSCILLOSCOPE, METRONOME, SEGMENTED, SPEED_DIAL, BOTTOM_SHEET, STEPPER, TIMELINE, RATING, PULL_REFRESH, ACCORDION, PIN_PAD, COMPASS, LIGHT_METER, BAROMETER, STEP_COUNTER, BLE, QR, NFC, GAME_2048, SNAKE, BREAKOUT, TETRIS, MEMORY_MATCH, MINESWEEPER, KALEIDOSCOPE, MATRIX_RAIN, FIREWORKS, PLASMA, WEATHER, PIXEL_ART, NETWORK_PROXY, SETTINGS_EDITOR, INPUT_AUTOMATION, FEATURE_FLAGS, PACKAGE_EXPLORER, TORCH_STROBE, SPECTRUM, TUNER, DECIBEL, SENSORS_DASH, BUBBLE_LEVEL, METAL_DETECTOR, BARCODE_SCANNER, GNSS, WIFI_SCANNER, TTS_STT, NFC_READER, MEDIA_VIZ, FBM, METABALLS, TRUCHET, GLITCH, EDGE_DETECT, DITHER, REACTION_DIFFUSION, BLOOM, PIP, IMMERSIVE, ORIENTATION, CUTOUT_INSETS, IME_INSETS, GESTURE_EXCLUSION, FLOATING_BUBBLE, CALENDAR, WHEEL_PICKER, TAG_INPUT, TREE_BROWSER, DATA_TABLE, KANBAN, SIGNATURE, PROGRESS_GALLERY, COACH_MARK, SHARED_ELEMENT, PARALLAX_HEADER, ANIMATED_CONTENT, CAROUSEL, STAGGERED, PULL_DISMISS, PAGE_CURL, PATH_PLAYER, CONVEX_HULL, DELAUNAY, LSYSTEM, NQUEENS, TSP, MARCHING_SQUARES, QUADTREE, PERLIN_TERRAIN, CLOTH, BILLIARDS, GALTON, NEWTONS_CRADLE, ORBIT_SANDBOX, CHARGED, MARBLE_RUN, FLUID, SOLIDS, HEIGHTMAP3D, FUNCTION_PLOTTER, POINT_CLOUD_MORPH, RAYCASTER, OBJ_MODEL, MANDALA, FLOW_FIELD, CIRCLE_PACKING, LIGHTNING, CLOCK_GALLERY, TURTLE, DOODLE_PHYSICS, HAND_LANDMARK, POSE_LANDMARK, IMAGE_CLASSIFY, LENS_EXPLORER, PRO_MODE, EXPOSURE_TOOLS, VIDEO_RECORDER, RAW_CAPTURE, CAM_EXTENSIONS, HIGH_FPS, TIMELAPSE, MOTION_CAM, DOC_SCANNER, CAMERA_GALLERY, DYNAMIC_ISLAND, LIQUID_SWIPE, PARTICLE_TEXT, MORPHING_ICONS, GOOEY_NAV, MORPHING_SHEET, FROSTED_GLASS, PALETTE_THEMING, HOLOGRAPHIC, TOUCH_RIPPLE, MAGNIFIER, AURORA, CAUSTICS, SOAP_BUBBLE, CRT, PIXELATE, HEAT_HAZE, BLACK_HOLE, NEON_SIGN, CONIC_GLOW, WAVE_FILL, DEPTH_PARALLAX, INK_SPREAD, VORONOI_SHATTER, SMOKE_FLUID, SPEEDOMETER, FINGERPRINT_AUDIT, HASHING, CRYPTO_PLAY, PASSWORD_GEN, BIOMETRIC_LOCK, CLIPBOARD_MON, LOCAL_SERVER, NETWORK_INFO, PING_SCAN, NSD_DISCOVERY, BLE_PERIPHERAL, NOTIF_STYLES, NOTIF_ACTIONS, NOTIF_PROGRESS, JSON_FMT, ENCODER, REGEX_TEST, JWT_DECODE, GENERATORS, WORLD_CLOCK, STOPWATCH, POMODORO, BUBBLE_WRAP, DECISION_WHEEL, MAGIC8, WHITE_NOISE, DRUM_MACHINE, THEREMIN, TONE_GEN, CLAP_WHISTLE, COMPONENTS_3D, SHADER_3D, COORD_TOOLS, SUN_MOON, COMPASS_NAV, GPS_TRACKER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevControlsScreen(
    mainScroll: androidx.compose.foundation.ScrollState,
    onNavigate: (Route) -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { DevSettingsRepository.get(context) }
    val discoverer = remember { AdbEndpointDiscoverer.get(context) }
    val kioskPrefs = remember { KioskPrefs.get(context) }
    val scope = rememberCoroutineScope()

    val devOpts by repo.developerOptionsEnabled.collectAsState(initial = repo.currentDeveloperOptions())
    val usb by repo.usbDebuggingEnabled.collectAsState(initial = repo.currentUsbDebugging())
    val wireless by repo.wirelessDebuggingEnabled.collectAsState(initial = repo.currentWirelessDebugging())
    val never by repo.neverTimeoutEnabled.collectAsState(initial = repo.currentNeverTimeout())
    val kioskTargetLabel by kioskPrefs.targetLabel.collectAsState(initial = null)
    val kioskTargetPkg by kioskPrefs.targetPackage.collectAsState(initial = null)
    val kioskActive by kioskPrefs.kioskActive.collectAsState(initial = false)
    val fastAnimations by kioskPrefs.fastAnimations.collectAsState(initial = false)
    val endpoint by discoverer.endpoint.collectAsState(initial = null)

    val hasSecure = PermissionUtils.hasWriteSecureSettings(context)
    val hasSystem = PermissionUtils.hasWriteSettings(context)
    val hasUsage = ForegroundAppDetector.hasUsageStatsPermission(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dev Controls") },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(horizontal = 16.dp)
                .verticalScroll(mainScroll)
        ) {
            Spacer(Modifier.height(8.dp))

            if (!hasSecure || !hasSystem || !hasUsage) {
                PermissionWarning(
                    hasSecure = hasSecure,
                    hasSystem = hasSystem,
                    hasUsage = hasUsage,
                    onCopy = { copy(context, it) }
                )
                Spacer(Modifier.height(12.dp))
            }

            EndpointCard(
                endpointText = formatEndpoint(
                    resolved = endpoint?.toString(),
                    wifiIp = WifiUtils.currentWifiIpv4(context),
                    wirelessOn = wireless
                ),
                onCopy = { copy(context, it) }
            )

            Spacer(Modifier.height(16.dp))

            ToggleRow(
                title = "Developer Options",
                checked = devOpts,
                onToggle = { scope.launch { repo.setDeveloperOptions(it).toastOnFailure(context) } }
            )
            ToggleRow(
                title = "USB Debugging",
                checked = usb,
                onToggle = { scope.launch { repo.setUsbDebugging(it).toastOnFailure(context) } }
            )
            ToggleRow(
                title = "Wireless Debugging",
                checked = wireless,
                onToggle = { scope.launch { repo.setWirelessDebugging(it).toastOnFailure(context) } }
            )
            ToggleRow(
                title = "Never Screen Timeout",
                checked = never,
                onToggle = { scope.launch { repo.setNeverTimeout(it).toastOnFailure(context) } }
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            KioskSection(
                active = kioskActive,
                accessibilityGranted = KioskAccessibilityService.isRunning(),
                notificationsGranted = PermissionUtils.hasPostNotifications(context),
                fastAnimations = fastAnimations,
                onToggleFastAnimations = { enabled ->
                    scope.launch {
                        kioskPrefs.setFastAnimations(enabled)
                        if (enabled && kioskActive) AnimationScales.disable(context)
                        if (!enabled && kioskActive) AnimationScales.restore(context)
                    }
                },
                targetLabel = kioskTargetLabel,
                targetPackage = kioskTargetPkg,
                onPickTarget = { onNavigate(Route.KIOSK_PICKER) },
                onStart = {
                    val pkg = kioskTargetPkg ?: return@KioskSection
                    if (!PermissionUtils.hasPostNotifications(context)) {
                        Toast.makeText(
                            context,
                            "Grant the Notifications permission first — Kiosk needs the persistent Stop notification.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@KioskSection
                    }
                    scope.launch {
                        kioskPrefs.setKioskActive(true)
                        if (kioskPrefs.isFastAnimationsOnce()) {
                            AnimationScales.disable(context)
                        }
                        KioskNotification.show(context, kioskTargetLabel ?: pkg)
                        val launch = context.packageManager.getLaunchIntentForPackage(pkg)
                        if (launch != null) {
                            launch.addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                            )
                            context.startActivity(launch)
                        }
                        Toast.makeText(
                            context,
                            "Kiosk active — home/recents will bounce back",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                onStop = {
                    scope.launch {
                        kioskPrefs.setKioskActive(false)
                        KioskNotification.cancel(context)
                        AnimationScales.restore(context)
                    }
                },
                onOpenAccessibilitySettings = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                },
                onCopyAdbGrant = {
                    copy(
                        context,
                        "adb shell appops set pro.jayeshseth.devcontrols ACCESS_RESTRICTED_SETTINGS allow\n" +
                            "adb shell settings put secure enabled_accessibility_services \"\$(adb shell settings get secure enabled_accessibility_services):pro.jayeshseth.devcontrols/pro.jayeshseth.devcontrols.kiosk.KioskAccessibilityService\"\n" +
                            "adb shell settings put secure accessibility_enabled 1"
                    )
                }
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            FocusCallsSection(onManageAllowList = { onNavigate(Route.ALLOWLIST) })

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text(text = "Surfaces", style = MaterialTheme.typography.titleSmall)

            Spacer(Modifier.height(8.dp))

            OutlinedButton(onClick = { requestPinWidget(context) }, modifier = Modifier.fillMaxWidth()) { Text("Add home-screen widget") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { openSettings(context) }, modifier = Modifier.fillMaxWidth()) { Text("Open Settings (Quick Tiles)") }
            Spacer(Modifier.height(12.dp))

            LabCategory("System & ADB") {
                LabBtn("adb_g — local shell") { onNavigate(Route.ADB_CONSOLE) }
                LabBtn("adb_c — wireless ADB") { onNavigate(Route.ADB_C) }
                LabBtn("adb_pm — permission manager") { onNavigate(Route.ADB_PM) }
                LabBtn("Hide Dev (per-app)") { onNavigate(Route.HIDE_DEV) }
                LabBtn("Force Dark (per-app)") { onNavigate(Route.FORCE_DARK) }
                LabBtn("App Info Dump") { onNavigate(Route.INFO_DUMP) }
                LabBtn("ADB Detection") { onNavigate(Route.ADB_DETECTION) }
                LabBtn("Accessibility Detection") { onNavigate(Route.A11Y_DETECTION) }
                LabBtn("Accessibility Cloak") { onNavigate(Route.A11Y_CLOAK) }
                LabBtn("Network & Proxy") { onNavigate(Route.NETWORK_PROXY) }
                LabBtn("Settings Editor") { onNavigate(Route.SETTINGS_EDITOR) }
                LabBtn("Input & Automation") { onNavigate(Route.INPUT_AUTOMATION) }
                LabBtn("Feature Flags") { onNavigate(Route.FEATURE_FLAGS) }
                LabBtn("Package Explorer") { onNavigate(Route.PACKAGE_EXPLORER) }
            }
            LabCategory("Windows & Gestures") {
                LabBtn("Gesture Lab") { onNavigate(Route.GESTURE_LAB) }
                LabBtn("System Bar Sheet") { onNavigate(Route.SYSTEM_BAR_SHEET) }
                LabBtn("Window FX (blur / shader)") { onNavigate(Route.WINDOW_FX) }
                LabBtn("Display Lab (refresh rate)") { onNavigate(Route.DISPLAY_LAB) }
                LabBtn("Window Size") { onNavigate(Route.WINDOW_SIZE) }
                LabBtn("Nested Scroll") { onNavigate(Route.NESTED_SCROLL) }
                LabBtn("Touch Internals") { onNavigate(Route.TOUCH_INTERNALS) }
                LabBtn("Predictive Back") { onNavigate(Route.PREDICTIVE_BACK) }
                LabBtn("Picture-in-Picture") { onNavigate(Route.PIP) }
                LabBtn("Immersive Mode") { onNavigate(Route.IMMERSIVE) }
                LabBtn("Orientation Lab") { onNavigate(Route.ORIENTATION) }
                LabBtn("Cutout & Insets") { onNavigate(Route.CUTOUT_INSETS) }
                LabBtn("IME Insets") { onNavigate(Route.IME_INSETS) }
                LabBtn("Gesture Exclusion") { onNavigate(Route.GESTURE_EXCLUSION) }
                LabBtn("Floating Bubble") { onNavigate(Route.FLOATING_BUBBLE) }
            }
            LabCategory("Shaders & Graphics") {
                LabBtn("Gradient Lab") { onNavigate(Route.GRADIENT_LAB) }
                LabBtn("Shader Editor") { onNavigate(Route.SHADER_EDITOR) }
                LabBtn("RenderEffect Chain") { onNavigate(Route.RENDER_EFFECT) }
                LabBtn("Raymarch SDF") { onNavigate(Route.RAYMARCH) }
                LabBtn("Ripple Shader") { onNavigate(Route.RIPPLE) }
                LabBtn("Voronoi") { onNavigate(Route.VORONOI) }
                LabBtn("Halftone") { onNavigate(Route.HALFTONE) }
                LabBtn("Dissolve") { onNavigate(Route.DISSOLVE) }
                LabBtn("Mandelbrot") { onNavigate(Route.MANDELBROT) }
                LabBtn("Glassmorphism") { onNavigate(Route.GLASSMORPHISM) }
                LabBtn("Dynamic Color") { onNavigate(Route.DYNAMIC_COLOR) }
                LabBtn("fBm Noise") { onNavigate(Route.FBM) }
                LabBtn("Metaballs") { onNavigate(Route.METABALLS) }
                LabBtn("Truchet Tiles") { onNavigate(Route.TRUCHET) }
                LabBtn("Glitch / VHS") { onNavigate(Route.GLITCH) }
                LabBtn("Edge Detect") { onNavigate(Route.EDGE_DETECT) }
                LabBtn("Bayer Dither") { onNavigate(Route.DITHER) }
                LabBtn("Reaction-Diffusion") { onNavigate(Route.REACTION_DIFFUSION) }
                LabBtn("Bloom") { onNavigate(Route.BLOOM) }
            }
            LabCategory("Motion & Transitions") {
                LabBtn("3D Transform") { onNavigate(Route.TRANSFORM_3D) }
                LabBtn("Physics Motion") { onNavigate(Route.PHYSICS_MOTION) }
                LabBtn("Shape Morph") { onNavigate(Route.SHAPE_MORPH) }
                LabBtn("Path Drawing") { onNavigate(Route.PATH_MORPH) }
                LabBtn("Pager Transforms") { onNavigate(Route.PAGER_TRANSFORM) }
                LabBtn("Swipe Cards") { onNavigate(Route.SWIPE_CARD) }
                LabBtn("Circular Reveal") { onNavigate(Route.CIRCULAR_REVEAL) }
                LabBtn("Animated Borders") { onNavigate(Route.ANIMATED_BORDERS) }
                LabBtn("Shimmer Skeletons") { onNavigate(Route.SHIMMER) }
                LabBtn("Flip Clock") { onNavigate(Route.FLIP_CLOCK) }
                LabBtn("Shared Element") { onNavigate(Route.SHARED_ELEMENT) }
                LabBtn("Parallax Header") { onNavigate(Route.PARALLAX_HEADER) }
                LabBtn("AnimatedContent") { onNavigate(Route.ANIMATED_CONTENT) }
                LabBtn("Carousel") { onNavigate(Route.CAROUSEL) }
                LabBtn("Staggered Reveal") { onNavigate(Route.STAGGERED) }
                LabBtn("Pull to Dismiss") { onNavigate(Route.PULL_DISMISS) }
                LabBtn("Page Curl") { onNavigate(Route.PAGE_CURL) }
                LabBtn("Path Player") { onNavigate(Route.PATH_PLAYER) }
            }
            LabCategory("Algorithms") {
                LabBtn("Sorting") { onNavigate(Route.SORTING) }
                LabBtn("Pathfinding (A*)") { onNavigate(Route.PATHFINDING) }
                LabBtn("Maze") { onNavigate(Route.MAZE) }
                LabBtn("Fractal Tree") { onNavigate(Route.FRACTAL_TREE) }
                LabBtn("Spirograph") { onNavigate(Route.SPIROGRAPH) }
                LabBtn("Convex Hull") { onNavigate(Route.CONVEX_HULL) }
                LabBtn("Delaunay") { onNavigate(Route.DELAUNAY) }
                LabBtn("L-System") { onNavigate(Route.LSYSTEM) }
                LabBtn("N-Queens") { onNavigate(Route.NQUEENS) }
                LabBtn("TSP (2-opt)") { onNavigate(Route.TSP) }
                LabBtn("Marching Squares") { onNavigate(Route.MARCHING_SQUARES) }
                LabBtn("Quadtree") { onNavigate(Route.QUADTREE) }
                LabBtn("Perlin Terrain") { onNavigate(Route.PERLIN_TERRAIN) }
            }
            LabCategory("Physics") {
                LabBtn("Double Pendulum") { onNavigate(Route.DOUBLE_PENDULUM) }
                LabBtn("N-Body Gravity") { onNavigate(Route.NBODY) }
                LabBtn("Boids") { onNavigate(Route.BOIDS) }
                LabBtn("Falling Sand") { onNavigate(Route.FALLING_SAND) }
                LabBtn("Soft Body") { onNavigate(Route.SOFT_BODY) }
                LabBtn("Spring Chain") { onNavigate(Route.SPRING_CHAIN) }
                LabBtn("Cloth") { onNavigate(Route.CLOTH) }
                LabBtn("Billiards") { onNavigate(Route.BILLIARDS) }
                LabBtn("Galton Board") { onNavigate(Route.GALTON) }
                LabBtn("Newton's Cradle") { onNavigate(Route.NEWTONS_CRADLE) }
                LabBtn("Orbit Sandbox") { onNavigate(Route.ORBIT_SANDBOX) }
                LabBtn("Charged Particles") { onNavigate(Route.CHARGED) }
                LabBtn("Marble Run") { onNavigate(Route.MARBLE_RUN) }
                LabBtn("Fluid") { onNavigate(Route.FLUID) }
            }
            LabCategory("Software 3D") {
                LabBtn("Wireframe 3D") { onNavigate(Route.WIREFRAME) }
                LabBtn("Point-Cloud Globe") { onNavigate(Route.GLOBE) }
                LabBtn("Starfield") { onNavigate(Route.STARFIELD) }
                LabBtn("Solids") { onNavigate(Route.SOLIDS) }
                LabBtn("Heightmap 3D") { onNavigate(Route.HEIGHTMAP3D) }
                LabBtn("Function Plotter") { onNavigate(Route.FUNCTION_PLOTTER) }
                LabBtn("Point-Cloud Morph") { onNavigate(Route.POINT_CLOUD_MORPH) }
                LabBtn("Raycaster") { onNavigate(Route.RAYCASTER) }
                LabBtn("OBJ Model") { onNavigate(Route.OBJ_MODEL) }
            }
            LabCategory("Canvas & Fun") {
                LabBtn("Particles") { onNavigate(Route.PARTICLES) }
                LabBtn("Confetti") { onNavigate(Route.CONFETTI) }
                LabBtn("Scratch to Reveal") { onNavigate(Route.SCRATCH) }
                LabBtn("Game of Life") { onNavigate(Route.GAME_OF_LIFE) }
                LabBtn("Mandala Pad") { onNavigate(Route.MANDALA) }
                LabBtn("Flow Field") { onNavigate(Route.FLOW_FIELD) }
                LabBtn("Circle Packing") { onNavigate(Route.CIRCLE_PACKING) }
                LabBtn("Lightning") { onNavigate(Route.LIGHTNING) }
                LabBtn("Clock Gallery") { onNavigate(Route.CLOCK_GALLERY) }
                LabBtn("Turtle REPL") { onNavigate(Route.TURTLE) }
                LabBtn("Doodle Physics") { onNavigate(Route.DOODLE_PHYSICS) }
            }
            LabCategory("Camera") {
                LabBtn("Lens Explorer") { onNavigate(Route.LENS_EXPLORER) }
                LabBtn("Pro Mode") { onNavigate(Route.PRO_MODE) }
                LabBtn("Exposure Tools") { onNavigate(Route.EXPOSURE_TOOLS) }
                LabBtn("Video Recorder") { onNavigate(Route.VIDEO_RECORDER) }
                LabBtn("RAW / DNG") { onNavigate(Route.RAW_CAPTURE) }
                LabBtn("Extensions (HDR/Night)") { onNavigate(Route.CAM_EXTENSIONS) }
                LabBtn("High Frame Rate") { onNavigate(Route.HIGH_FPS) }
                LabBtn("Timelapse") { onNavigate(Route.TIMELAPSE) }
                LabBtn("Motion Cam") { onNavigate(Route.MOTION_CAM) }
                LabBtn("Document Scanner") { onNavigate(Route.DOC_SCANNER) }
                LabBtn("Gallery") { onNavigate(Route.CAMERA_GALLERY) }
            }
            LabCategory("Visual FX") {
                LabBtn("Holographic Card") { onNavigate(Route.HOLOGRAPHIC) }
                LabBtn("Liquid Swipe") { onNavigate(Route.LIQUID_SWIPE) }
                LabBtn("Dynamic Island") { onNavigate(Route.DYNAMIC_ISLAND) }
                LabBtn("Touch Ripple Water") { onNavigate(Route.TOUCH_RIPPLE) }
                LabBtn("Gooey Nav Bar") { onNavigate(Route.GOOEY_NAV) }
                LabBtn("Magnifier Lens") { onNavigate(Route.MAGNIFIER) }
                LabBtn("Frosted Glass") { onNavigate(Route.FROSTED_GLASS) }
                LabBtn("Morphing Sheet") { onNavigate(Route.MORPHING_SHEET) }
                LabBtn("Morphing Icons") { onNavigate(Route.MORPHING_ICONS) }
                LabBtn("Particle Text") { onNavigate(Route.PARTICLE_TEXT) }
                LabBtn("Palette Theming") { onNavigate(Route.PALETTE_THEMING) }
                LabBtn("Speedometer") { onNavigate(Route.SPEEDOMETER) }
                LabBtn("Aurora") { onNavigate(Route.AURORA) }
                LabBtn("Caustics") { onNavigate(Route.CAUSTICS) }
                LabBtn("Soap Bubble") { onNavigate(Route.SOAP_BUBBLE) }
                LabBtn("CRT TV") { onNavigate(Route.CRT) }
                LabBtn("Pixelate") { onNavigate(Route.PIXELATE) }
                LabBtn("Heat Haze") { onNavigate(Route.HEAT_HAZE) }
                LabBtn("Black Hole") { onNavigate(Route.BLACK_HOLE) }
                LabBtn("Neon Sign") { onNavigate(Route.NEON_SIGN) }
                LabBtn("Conic Glow Border") { onNavigate(Route.CONIC_GLOW) }
                LabBtn("Liquid Wave Fill") { onNavigate(Route.WAVE_FILL) }
                LabBtn("2.5D Parallax") { onNavigate(Route.DEPTH_PARALLAX) }
                LabBtn("Ink Spread") { onNavigate(Route.INK_SPREAD) }
                LabBtn("Voronoi Shatter") { onNavigate(Route.VORONOI_SHATTER) }
                LabBtn("Smoke") { onNavigate(Route.SMOKE_FLUID) }
            }
            LabCategory("Security & Privacy") {
                LabBtn("Fingerprint Auditor") { onNavigate(Route.FINGERPRINT_AUDIT) }
                LabBtn("Crypto Playground") { onNavigate(Route.CRYPTO_PLAY) }
                LabBtn("Hashing") { onNavigate(Route.HASHING) }
                LabBtn("Password Generator") { onNavigate(Route.PASSWORD_GEN) }
                LabBtn("Biometric Lock") { onNavigate(Route.BIOMETRIC_LOCK) }
                LabBtn("Clipboard Monitor") { onNavigate(Route.CLIPBOARD_MON) }
            }
            LabCategory("Networking & Connectivity") {
                LabBtn("Local Server") { onNavigate(Route.LOCAL_SERVER) }
                LabBtn("Network Info") { onNavigate(Route.NETWORK_INFO) }
                LabBtn("Ping & Port Scan") { onNavigate(Route.PING_SCAN) }
                LabBtn("NSD Discovery") { onNavigate(Route.NSD_DISCOVERY) }
                LabBtn("BLE Peripheral") { onNavigate(Route.BLE_PERIPHERAL) }
            }
            LabCategory("Notifications") {
                LabBtn("Notification Styles") { onNavigate(Route.NOTIF_STYLES) }
                LabBtn("Actions & Reply") { onNavigate(Route.NOTIF_ACTIONS) }
                LabBtn("Progress & Ongoing") { onNavigate(Route.NOTIF_PROGRESS) }
            }
            LabCategory("Developer Utilities") {
                LabBtn("JSON Formatter") { onNavigate(Route.JSON_FMT) }
                LabBtn("Encoder (Base64/Hex/URL)") { onNavigate(Route.ENCODER) }
                LabBtn("Regex Tester") { onNavigate(Route.REGEX_TEST) }
                LabBtn("JWT Decoder") { onNavigate(Route.JWT_DECODE) }
                LabBtn("Generators") { onNavigate(Route.GENERATORS) }
            }
            LabCategory("Time & Date") {
                LabBtn("World Clock") { onNavigate(Route.WORLD_CLOCK) }
                LabBtn("Stopwatch") { onNavigate(Route.STOPWATCH) }
                LabBtn("Pomodoro") { onNavigate(Route.POMODORO) }
            }
            LabCategory("Toys & Novelty") {
                LabBtn("Bubble Wrap") { onNavigate(Route.BUBBLE_WRAP) }
                LabBtn("Decision Wheel") { onNavigate(Route.DECISION_WHEEL) }
                LabBtn("Magic 8-Ball") { onNavigate(Route.MAGIC8) }
                LabBtn("White Noise") { onNavigate(Route.WHITE_NOISE) }
            }
            LabCategory("Audio Lab") {
                LabBtn("Drum Machine") { onNavigate(Route.DRUM_MACHINE) }
                LabBtn("Theremin") { onNavigate(Route.THEREMIN) }
                LabBtn("Tone Generator") { onNavigate(Route.TONE_GEN) }
                LabBtn("Clap & Whistle Detector") { onNavigate(Route.CLAP_WHISTLE) }
            }
            LabCategory("3D Components") {
                LabBtn("Canvas — push/knob/toggle/slider") { onNavigate(Route.COMPONENTS_3D) }
                LabBtn("Shader — glossy orb / emboss") { onNavigate(Route.SHADER_3D) }
            }
            LabCategory("Maps & Location") {
                LabBtn("Coordinate Tools") { onNavigate(Route.COORD_TOOLS) }
                LabBtn("Sun & Moon") { onNavigate(Route.SUN_MOON) }
                LabBtn("Compass Navigator") { onNavigate(Route.COMPASS_NAV) }
                LabBtn("GPS Tracker") { onNavigate(Route.GPS_TRACKER) }
            }
            LabCategory("Machine Learning") {
                LabBtn("Hand Landmarks") { onNavigate(Route.HAND_LANDMARK) }
                LabBtn("Pose Landmarks") { onNavigate(Route.POSE_LANDMARK) }
                LabBtn("Image Classification") { onNavigate(Route.IMAGE_CLASSIFY) }
            }
            LabCategory("Components") {
                LabBtn("✨ Mesh Components") { onNavigate(Route.MESH_GALLERY) }
                LabBtn("Segmented Control") { onNavigate(Route.SEGMENTED) }
                LabBtn("Speed-Dial FAB") { onNavigate(Route.SPEED_DIAL) }
                LabBtn("Bottom Sheet") { onNavigate(Route.BOTTOM_SHEET) }
                LabBtn("Stepper / Wizard") { onNavigate(Route.STEPPER) }
                LabBtn("Timeline") { onNavigate(Route.TIMELINE) }
                LabBtn("Rating") { onNavigate(Route.RATING) }
                LabBtn("Pull to Refresh") { onNavigate(Route.PULL_REFRESH) }
                LabBtn("Accordion") { onNavigate(Route.ACCORDION) }
                LabBtn("PIN Pad") { onNavigate(Route.PIN_PAD) }
                LabBtn("Custom Layout") { onNavigate(Route.CUSTOM_LAYOUT) }
                LabBtn("Reorder List") { onNavigate(Route.REORDER_LIST) }
                LabBtn("Drawing") { onNavigate(Route.DRAWING) }
                LabBtn("Charts") { onNavigate(Route.CHARTS) }
                LabBtn("Color Picker") { onNavigate(Route.COLOR_PICKER) }
                LabBtn("Text FX") { onNavigate(Route.TEXT_FX) }
                LabBtn("Spotlight") { onNavigate(Route.SPOTLIGHT) }
                LabBtn("Drag & Drop") { onNavigate(Route.DRAG_DROP) }
                LabBtn("Calendar") { onNavigate(Route.CALENDAR) }
                LabBtn("Wheel Picker") { onNavigate(Route.WHEEL_PICKER) }
                LabBtn("Tag Input") { onNavigate(Route.TAG_INPUT) }
                LabBtn("Tree Browser") { onNavigate(Route.TREE_BROWSER) }
                LabBtn("Data Table") { onNavigate(Route.DATA_TABLE) }
                LabBtn("Kanban") { onNavigate(Route.KANBAN) }
                LabBtn("Signature Pad") { onNavigate(Route.SIGNATURE) }
                LabBtn("Progress Gallery") { onNavigate(Route.PROGRESS_GALLERY) }
                LabBtn("Coach-mark") { onNavigate(Route.COACH_MARK) }
            }
            LabCategory("Device & Media") {
                LabBtn("Haptics Lab") { onNavigate(Route.HAPTICS) }
                LabBtn("USB Inspector") { onNavigate(Route.USB_INSPECTOR) }
                LabBtn("Audio Reactive") { onNavigate(Route.AUDIO_REACTIVE) }
                LabBtn("Synth Keyboard") { onNavigate(Route.SYNTH) }
                LabBtn("Oscilloscope") { onNavigate(Route.OSCILLOSCOPE) }
                LabBtn("Metronome") { onNavigate(Route.METRONOME) }
                LabBtn("Sensor Parallax") { onNavigate(Route.SENSOR_PARALLAX) }
                LabBtn("Compass") { onNavigate(Route.COMPASS) }
                LabBtn("Light Meter") { onNavigate(Route.LIGHT_METER) }
                LabBtn("Barometer") { onNavigate(Route.BAROMETER) }
                LabBtn("Step Counter") { onNavigate(Route.STEP_COUNTER) }
                LabBtn("BLE Scanner") { onNavigate(Route.BLE) }
                LabBtn("QR Generator") { onNavigate(Route.QR) }
                LabBtn("NFC Status") { onNavigate(Route.NFC) }
                LabBtn("Camera Filter") { onNavigate(Route.CAMERA_FILTER) }
                LabBtn("Social Share") { onNavigate(Route.SOCIAL_SHARE) }
                LabBtn("Torch & Strobe") { onNavigate(Route.TORCH_STROBE) }
                LabBtn("Spectrum") { onNavigate(Route.SPECTRUM) }
                LabBtn("Tuner") { onNavigate(Route.TUNER) }
                LabBtn("Decibel Meter") { onNavigate(Route.DECIBEL) }
                LabBtn("Sensors Dashboard") { onNavigate(Route.SENSORS_DASH) }
                LabBtn("Bubble Level") { onNavigate(Route.BUBBLE_LEVEL) }
                LabBtn("Metal Detector") { onNavigate(Route.METAL_DETECTOR) }
                LabBtn("Barcode Scanner") { onNavigate(Route.BARCODE_SCANNER) }
                LabBtn("GNSS Dashboard") { onNavigate(Route.GNSS) }
                LabBtn("Wi-Fi Scanner") { onNavigate(Route.WIFI_SCANNER) }
                LabBtn("TTS / STT") { onNavigate(Route.TTS_STT) }
                LabBtn("NFC Reader") { onNavigate(Route.NFC_READER) }
                LabBtn("Media Visualizer") { onNavigate(Route.MEDIA_VIZ) }
            }
            LabCategory("Games") {
                LabBtn("2048") { onNavigate(Route.GAME_2048) }
                LabBtn("Snake") { onNavigate(Route.SNAKE) }
                LabBtn("Breakout") { onNavigate(Route.BREAKOUT) }
                LabBtn("Tetris-lite") { onNavigate(Route.TETRIS) }
                LabBtn("Memory Match") { onNavigate(Route.MEMORY_MATCH) }
                LabBtn("Minesweeper") { onNavigate(Route.MINESWEEPER) }
            }
            LabCategory("Creative") {
                LabBtn("Kaleidoscope") { onNavigate(Route.KALEIDOSCOPE) }
                LabBtn("Matrix Rain") { onNavigate(Route.MATRIX_RAIN) }
                LabBtn("Fireworks") { onNavigate(Route.FIREWORKS) }
                LabBtn("Plasma") { onNavigate(Route.PLASMA) }
                LabBtn("Weather") { onNavigate(Route.WEATHER) }
                LabBtn("Pixel Art") { onNavigate(Route.PIXEL_ART) }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

private val LocalExpandedCats =
    androidx.compose.runtime.compositionLocalOf<androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>> {
        error("LocalExpandedCats not provided")
    }

@Composable
private fun LabCategory(title: String, content: @Composable () -> Unit) {
    val map = LocalExpandedCats.current
    val open = map[title] ?: false
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { map[title] = !open }
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text(if (open) "▾" else "▸", style = MaterialTheme.typography.titleMedium)
        }
        if (open) {
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun LabBtn(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) { Text(label) }
}

@Composable
private fun PermissionWarning(
    hasSecure: Boolean,
    hasSystem: Boolean,
    hasUsage: Boolean,
    onCopy: (String) -> Unit,
) {
    val cmd = buildString {
        if (!hasSecure) appendLine("adb shell pm grant pro.jayeshseth.devcontrols android.permission.WRITE_SECURE_SETTINGS")
        if (!hasSystem) appendLine("adb shell appops set pro.jayeshseth.devcontrols WRITE_SETTINGS allow")
        if (!hasUsage) appendLine("adb shell appops set pro.jayeshseth.devcontrols GET_USAGE_STATS allow")
        appendLine("adb shell pm grant pro.jayeshseth.devcontrols android.permission.READ_PHONE_STATE")
        appendLine("adb shell pm grant pro.jayeshseth.devcontrols android.permission.ANSWER_PHONE_CALLS")
    }.trim()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Permissions to grant via ADB",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Run these commands once. The READ_PHONE_STATE / ANSWER_PHONE_CALLS lines are needed for Focus Calls.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = cmd,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { onCopy(cmd) }) { Text("Copy commands") }
        }
    }
}

@Composable
private fun EndpointCard(endpointText: String, onCopy: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ADB endpoint",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = endpointText,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace
                )
                OutlinedButton(onClick = { onCopy(endpointText) }) { Text("Copy") }
            }
        }
    }
}

@Composable
private fun KioskSection(
    active: Boolean,
    accessibilityGranted: Boolean,
    notificationsGranted: Boolean,
    fastAnimations: Boolean,
    onToggleFastAnimations: (Boolean) -> Unit,
    targetLabel: String?,
    targetPackage: String?,
    onPickTarget: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onCopyAdbGrant: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Kiosk Mode", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Moto's launcher doesn't expose the OS Pin option, so this uses an Accessibility " +
                    "service: when active, swipe-home / launcher / other apps bounce right back to " +
                    "your picked target. To exit, use the Stop button (in this app, the QS tile, or the " +
                    "persistent notification).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!accessibilityGranted) {
                Spacer(Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Accessibility permission required",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Grant the Dev Controls Kiosk accessibility service. Either tap the button " +
                                "below to open the Accessibility settings, or run the ADB command.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onOpenAccessibilitySettings) {
                                Text("Open Accessibility settings")
                            }
                            OutlinedButton(onClick = onCopyAdbGrant) {
                                Text("Copy ADB grant")
                            }
                        }
                    }
                }
            }

            if (!notificationsGranted) {
                Spacer(Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Notifications permission required",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Kiosk Mode pins a persistent 'Stop kiosk' notification so you can always exit. " +
                                "Without notifications permission you'd have no way to stop it. Re-launch the app to be prompted again.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Target", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        targetLabel ?: targetPackage ?: "None picked",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = onPickTarget) {
                    Text(if (targetPackage == null) "Pick" else "Change")
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Fast animations", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "While kiosk is active, zero out window/transition/animator scales so the home-swipe flash is barely visible. Restored on stop.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = fastAnimations, onCheckedChange = onToggleFastAnimations)
            }

            Spacer(Modifier.height(8.dp))

            if (active) {
                Button(
                    onClick = onStop,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Stop Kiosk Mode")
                }
            } else {
                Button(
                    onClick = onStart,
                    enabled = targetPackage != null && accessibilityGranted && notificationsGranted,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            targetPackage == null -> "Pick a target first"
                            !accessibilityGranted -> "Grant accessibility first"
                            !notificationsGranted -> "Grant notifications first"
                            else -> "Start Kiosk Mode"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

private fun formatEndpoint(resolved: String?, wifiIp: String?, wirelessOn: Boolean): String =
    when {
        resolved != null -> resolved
        !wirelessOn -> "Enable wireless debugging"
        wifiIp != null -> "$wifiIp:… discovering"
        else -> "No Wi-Fi"
    }

private fun copy(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Dev Controls", text))
    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}

private fun requestPinWidget(context: Context) {
    val mgr = context.getSystemService(AppWidgetManager::class.java)
    if (mgr != null && mgr.isRequestPinAppWidgetSupported) {
        val provider = ComponentName(context, DevControlsWidgetReceiver::class.java)
        mgr.requestPinAppWidget(provider, null, null)
    } else {
        Toast.makeText(
            context,
            "Launcher does not support pinning. Long-press home to add manually.",
            Toast.LENGTH_LONG
        ).show()
    }
}

private fun openSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
    Toast.makeText(
        context,
        "Pull down the shade, edit tiles, and drag in the Dev tiles.",
        Toast.LENGTH_LONG
    ).show()
}

private fun Result<Unit>.toastOnFailure(context: Context) {
    onFailure { e ->
        Toast.makeText(
            context,
            "Failed: ${e.javaClass.simpleName}. Permission granted via ADB?",
            Toast.LENGTH_LONG
        ).show()
    }
}
