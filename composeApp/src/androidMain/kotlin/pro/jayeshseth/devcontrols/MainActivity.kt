package pro.jayeshseth.devcontrols

import pro.jayeshseth.devcontrols.nav.Route
import pro.jayeshseth.devcontrols.ui.CategoryMenu

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
import pro.jayeshseth.devcontrols.ui.FlowLearnerScreen
import pro.jayeshseth.devcontrols.ui.RecompositionVisualizerScreen
import pro.jayeshseth.devcontrols.ui.DispatcherVisualizerScreen
import pro.jayeshseth.devcontrols.ui.ConcurrencyVisualizerScreen
import pro.jayeshseth.devcontrols.ui.ApiVisualizerScreen
import pro.jayeshseth.devcontrols.ui.CryptoVisualizerScreen
import pro.jayeshseth.devcontrols.ui.DbIndexVisualizerScreen
import pro.jayeshseth.devcontrols.ui.TcpVisualizerScreen
import pro.jayeshseth.devcontrols.ui.GitVisualizerScreen
import pro.jayeshseth.devcontrols.ui.ComposePhasesScreen
import pro.jayeshseth.devcontrols.ui.ConstraintFlowScreen
import pro.jayeshseth.devcontrols.ui.LazyLifecycleScreen
import pro.jayeshseth.devcontrols.ui.HtmlParserPlaygroundScreen
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
import pro.jayeshseth.devcontrols.ui.present.PresentScreen
import pro.jayeshseth.devcontrols.ui.present.DeckEditorScreen
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
import pro.jayeshseth.devcontrols.ui.GradientMorphLabScreen
import pro.jayeshseth.devcontrols.ui.MediaStreamScreen

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
            val notifLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= 33 &&
                    !PermissionUtils.hasPostNotifications(this@MainActivity)
                ) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            AppRoot(initialRoute = if (openKioskPicker) Route.KIOSK_PICKER else Route.MAIN)
        }
    }
}

@androidx.compose.runtime.Composable
actual fun BackHandlerCommon(enabled: Boolean, onBack: () -> Unit) =
    androidx.activity.compose.BackHandler(enabled = enabled, onBack = onBack)

@androidx.compose.runtime.Composable
actual fun PlatformHome(
    scroll: androidx.compose.foundation.ScrollState,
    expandedCats: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>,
    onNavigate: (Route) -> Unit,
) {
    DevControlsScreen(mainScroll = scroll, expandedCats = expandedCats, onNavigate = onNavigate)
}

@androidx.compose.runtime.Composable
actual fun PlatformScreenHost(route: Route, onBack: () -> Unit) {
    when (route) {
        Route.MAIN -> {}
        Route.ALLOWLIST -> AllowListScreen(onBack = onBack)
        Route.KIOSK_PICKER -> KioskTargetPickerScreen(onBack = onBack)
        Route.ADB_CONSOLE -> AdbConsoleScreen(onBack = onBack)
        Route.ADB_C -> AdbcScreen(onBack = onBack)
        Route.ADB_PM -> PermissionManagerScreen(onBack = onBack)
        Route.HIDE_DEV -> HideDevScreen(onBack = onBack)
        Route.HAPTICS -> HapticsLabScreen(onBack = onBack)
        Route.USB_INSPECTOR -> UsbInspectorScreen(onBack = onBack)
        Route.PREDICTIVE_BACK -> PredictiveBackPlaygroundScreen(onBack = onBack)
        Route.FORCE_DARK -> ForceDarkScreen(onBack = onBack)
        Route.INFO_DUMP -> InfoDumpScreen(onBack = onBack)
        Route.ADB_DETECTION -> AdbDetectionScreen(onBack = onBack)
        Route.A11Y_DETECTION -> A11yDetectionScreen(onBack = onBack)
        Route.A11Y_CLOAK -> A11yCloakScreen(onBack = onBack)
        Route.GESTURE_LAB -> GestureLabScreen(onBack = onBack)
        Route.SYSTEM_BAR_SHEET -> SystemBarSheetScreen(onBack = onBack)
        Route.WINDOW_FX -> WindowFxLabScreen(onBack = onBack)
        Route.DISPLAY_LAB -> DisplayLabScreen(onBack = onBack)
        Route.GRADIENT_LAB -> GradientLabScreen(onBack = onBack)
        Route.SOCIAL_SHARE -> SocialShareScreen(onBack = onBack)
        Route.SHADER_EDITOR -> ShaderEditorLabScreen(onBack = onBack)
        Route.RENDER_EFFECT -> RenderEffectLabScreen(onBack = onBack)
        Route.TRANSFORM_3D -> Transform3DLabScreen(onBack = onBack)
        Route.CUSTOM_LAYOUT -> CustomLayoutLabScreen(onBack = onBack)
        Route.PHYSICS_MOTION -> PhysicsMotionLabScreen(onBack = onBack)
        Route.NESTED_SCROLL -> NestedScrollLabScreen(onBack = onBack)
        Route.TOUCH_INTERNALS -> TouchInternalsLabScreen(onBack = onBack)
        Route.WINDOW_SIZE -> WindowSizeLabScreen(onBack = onBack)
        Route.DYNAMIC_COLOR -> DynamicColorLabScreen(onBack = onBack)
        Route.AUDIO_REACTIVE -> AudioReactiveLabScreen(onBack = onBack)
        Route.SENSOR_PARALLAX -> SensorParallaxLabScreen(onBack = onBack)
        Route.SHAPE_MORPH -> ShapeMorphLabScreen(onBack = onBack)
        Route.PATH_MORPH -> PathMorphLabScreen(onBack = onBack)
        Route.PAGER_TRANSFORM -> PagerTransformLabScreen(onBack = onBack)
        Route.REORDER_LIST -> ReorderListLabScreen(onBack = onBack)
        Route.SWIPE_CARD -> SwipeCardLabScreen(onBack = onBack)
        Route.DRAWING -> DrawingLabScreen(onBack = onBack)
        Route.PARTICLES -> ParticleLabScreen(onBack = onBack)
        Route.CONFETTI -> ConfettiLabScreen(onBack = onBack)
        Route.SCRATCH -> ScratchRevealLabScreen(onBack = onBack)
        Route.TEXT_FX -> TextFxLabScreen(onBack = onBack)
        Route.CIRCULAR_REVEAL -> CircularRevealThemeScreen(onBack = onBack)
        Route.CHARTS -> ChartsLabScreen(onBack = onBack)
        Route.COLOR_PICKER -> ColorPickerLabScreen(onBack = onBack)
        Route.GAME_OF_LIFE -> GameOfLifeLabScreen(onBack = onBack)
        Route.GLASSMORPHISM -> GlassmorphismLabScreen(onBack = onBack)
        Route.SHIMMER -> ShimmerLabScreen(onBack = onBack)
        Route.ANIMATED_BORDERS -> AnimatedBordersLabScreen(onBack = onBack)
        Route.SPOTLIGHT -> SpotlightLabScreen(onBack = onBack)
        Route.SPRING_CHAIN -> SpringChainLabScreen(onBack = onBack)
        Route.FLIP_CLOCK -> FlipClockLabScreen(onBack = onBack)
        Route.MANDELBROT -> MandelbrotLabScreen(onBack = onBack)
        Route.CAMERA_FILTER -> CameraFilterLabScreen(onBack = onBack)
        Route.DRAG_DROP -> DragDropLabScreen(onBack = onBack)
        Route.MESH_GALLERY -> MeshGalleryScreen(onBack = onBack)
        Route.RAYMARCH -> RaymarchSdfLabScreen(onBack = onBack)
        Route.RIPPLE -> RippleShaderLabScreen(onBack = onBack)
        Route.VORONOI -> VoronoiLabScreen(onBack = onBack)
        Route.HALFTONE -> HalftoneLabScreen(onBack = onBack)
        Route.DISSOLVE -> DissolveLabScreen(onBack = onBack)
        Route.SORTING -> SortingVisualizerScreen(onBack = onBack)
        Route.PATHFINDING -> PathfindingLabScreen(onBack = onBack)
        Route.MAZE -> MazeLabScreen(onBack = onBack)
        Route.FRACTAL_TREE -> FractalTreeLabScreen(onBack = onBack)
        Route.SPIROGRAPH -> SpirographLabScreen(onBack = onBack)
        Route.DOUBLE_PENDULUM -> DoublePendulumLabScreen(onBack = onBack)
        Route.NBODY -> NBodyLabScreen(onBack = onBack)
        Route.BOIDS -> BoidsLabScreen(onBack = onBack)
        Route.FALLING_SAND -> FallingSandLabScreen(onBack = onBack)
        Route.SOFT_BODY -> SoftBodyLabScreen(onBack = onBack)
        Route.WIREFRAME -> WireframeLabScreen(onBack = onBack)
        Route.GLOBE -> PointCloudGlobeLabScreen(onBack = onBack)
        Route.STARFIELD -> StarfieldLabScreen(onBack = onBack)
        Route.SYNTH -> SynthKeyboardLabScreen(onBack = onBack)
        Route.OSCILLOSCOPE -> OscilloscopeLabScreen(onBack = onBack)
        Route.METRONOME -> MetronomeLabScreen(onBack = onBack)
        Route.SEGMENTED -> SegmentedControlLabScreen(onBack = onBack)
        Route.SPEED_DIAL -> SpeedDialFabLabScreen(onBack = onBack)
        Route.BOTTOM_SHEET -> BottomSheetLabScreen(onBack = onBack)
        Route.STEPPER -> StepperWizardLabScreen(onBack = onBack)
        Route.TIMELINE -> TimelineLabScreen(onBack = onBack)
        Route.RATING -> RatingBarLabScreen(onBack = onBack)
        Route.PULL_REFRESH -> PullRefreshLabScreen(onBack = onBack)
        Route.ACCORDION -> AccordionLabScreen(onBack = onBack)
        Route.PIN_PAD -> PinPadLabScreen(onBack = onBack)
        Route.COMPASS -> CompassLabScreen(onBack = onBack)
        Route.LIGHT_METER -> LightMeterLabScreen(onBack = onBack)
        Route.BAROMETER -> BarometerLabScreen(onBack = onBack)
        Route.STEP_COUNTER -> StepCounterLabScreen(onBack = onBack)
        Route.BLE -> BleScannerLabScreen(onBack = onBack)
        Route.QR -> QrToolsLabScreen(onBack = onBack)
        Route.NFC -> NfcStatusLabScreen(onBack = onBack)
        Route.GAME_2048 -> Game2048Screen(onBack = onBack)
        Route.SNAKE -> SnakeGameScreen(onBack = onBack)
        Route.BREAKOUT -> BreakoutGameScreen(onBack = onBack)
        Route.TETRIS -> TetrisLiteScreen(onBack = onBack)
        Route.MEMORY_MATCH -> MemoryMatchScreen(onBack = onBack)
        Route.MINESWEEPER -> MinesweeperScreen(onBack = onBack)
        Route.KALEIDOSCOPE -> KaleidoscopeLabScreen(onBack = onBack)
        Route.MATRIX_RAIN -> MatrixRainLabScreen(onBack = onBack)
        Route.FIREWORKS -> FireworksLabScreen(onBack = onBack)
        Route.PLASMA -> PlasmaLabScreen(onBack = onBack)
        Route.WEATHER -> WeatherLabScreen(onBack = onBack)
        Route.PIXEL_ART -> PixelArtLabScreen(onBack = onBack)
        Route.NETWORK_PROXY -> NetworkProxyLabScreen(onBack = onBack)
        Route.SETTINGS_EDITOR -> SettingsEditorLabScreen(onBack = onBack)
        Route.INPUT_AUTOMATION -> InputAutomationLabScreen(onBack = onBack)
        Route.FEATURE_FLAGS -> FeatureFlagsLabScreen(onBack = onBack)
        Route.PACKAGE_EXPLORER -> PackageExplorerLabScreen(onBack = onBack)
        Route.TORCH_STROBE -> TorchStrobeLabScreen(onBack = onBack)
        Route.SPECTRUM -> SpectrumLabScreen(onBack = onBack)
        Route.TUNER -> TunerLabScreen(onBack = onBack)
        Route.DECIBEL -> DecibelMeterLabScreen(onBack = onBack)
        Route.SENSORS_DASH -> SensorsDashboardLabScreen(onBack = onBack)
        Route.BUBBLE_LEVEL -> BubbleLevelLabScreen(onBack = onBack)
        Route.METAL_DETECTOR -> MetalDetectorLabScreen(onBack = onBack)
        Route.BARCODE_SCANNER -> BarcodeScannerLabScreen(onBack = onBack)
        Route.GNSS -> GnssDashboardLabScreen(onBack = onBack)
        Route.WIFI_SCANNER -> WifiScannerLabScreen(onBack = onBack)
        Route.TTS_STT -> TtsSttLabScreen(onBack = onBack)
        Route.NFC_READER -> NfcReaderLabScreen(onBack = onBack)
        Route.MEDIA_VIZ -> MediaVisualizerLabScreen(onBack = onBack)
        Route.FBM -> FbmNoiseLabScreen(onBack = onBack)
        Route.METABALLS -> MetaballsLabScreen(onBack = onBack)
        Route.TRUCHET -> TruchetLabScreen(onBack = onBack)
        Route.GLITCH -> GlitchLabScreen(onBack = onBack)
        Route.EDGE_DETECT -> EdgeDetectLabScreen(onBack = onBack)
        Route.DITHER -> DitherLabScreen(onBack = onBack)
        Route.REACTION_DIFFUSION -> ReactionDiffusionLabScreen(onBack = onBack)
        Route.BLOOM -> BloomLabScreen(onBack = onBack)
        Route.PIP -> PipLabScreen(onBack = onBack)
        Route.IMMERSIVE -> ImmersiveLabScreen(onBack = onBack)
        Route.ORIENTATION -> OrientationLabScreen(onBack = onBack)
        Route.CUTOUT_INSETS -> CutoutInsetsLabScreen(onBack = onBack)
        Route.IME_INSETS -> ImeInsetsLabScreen(onBack = onBack)
        Route.GESTURE_EXCLUSION -> GestureExclusionLabScreen(onBack = onBack)
        Route.FLOATING_BUBBLE -> FloatingBubbleLabScreen(onBack = onBack)
        Route.CALENDAR -> CalendarLabScreen(onBack = onBack)
        Route.WHEEL_PICKER -> WheelPickerLabScreen(onBack = onBack)
        Route.TAG_INPUT -> TagInputLabScreen(onBack = onBack)
        Route.TREE_BROWSER -> TreeBrowserLabScreen(onBack = onBack)
        Route.DATA_TABLE -> DataTableLabScreen(onBack = onBack)
        Route.KANBAN -> KanbanLabScreen(onBack = onBack)
        Route.SIGNATURE -> SignaturePadLabScreen(onBack = onBack)
        Route.PROGRESS_GALLERY -> ProgressGalleryLabScreen(onBack = onBack)
        Route.COACH_MARK -> CoachMarkLabScreen(onBack = onBack)
        Route.SHARED_ELEMENT -> SharedElementLabScreen(onBack = onBack)
        Route.PARALLAX_HEADER -> ParallaxHeaderLabScreen(onBack = onBack)
        Route.ANIMATED_CONTENT -> AnimatedContentGalleryScreen(onBack = onBack)
        Route.CAROUSEL -> CarouselLabScreen(onBack = onBack)
        Route.STAGGERED -> StaggeredRevealLabScreen(onBack = onBack)
        Route.PULL_DISMISS -> PullToDismissLabScreen(onBack = onBack)
        Route.PAGE_CURL -> PageCurlLabScreen(onBack = onBack)
        Route.PATH_PLAYER -> PathKeyframePlayerScreen(onBack = onBack)
        Route.CONVEX_HULL -> ConvexHullLabScreen(onBack = onBack)
        Route.DELAUNAY -> DelaunayLabScreen(onBack = onBack)
        Route.LSYSTEM -> LSystemLabScreen(onBack = onBack)
        Route.NQUEENS -> NQueensLabScreen(onBack = onBack)
        Route.TSP -> TspLabScreen(onBack = onBack)
        Route.MARCHING_SQUARES -> MarchingSquaresLabScreen(onBack = onBack)
        Route.QUADTREE -> QuadtreeLabScreen(onBack = onBack)
        Route.PERLIN_TERRAIN -> PerlinTerrainLabScreen(onBack = onBack)
        Route.CLOTH -> ClothLabScreen(onBack = onBack)
        Route.BILLIARDS -> BilliardsLabScreen(onBack = onBack)
        Route.GALTON -> GaltonBoardLabScreen(onBack = onBack)
        Route.NEWTONS_CRADLE -> NewtonsCradleLabScreen(onBack = onBack)
        Route.ORBIT_SANDBOX -> OrbitSandboxLabScreen(onBack = onBack)
        Route.CHARGED -> ChargedParticlesLabScreen(onBack = onBack)
        Route.MARBLE_RUN -> MarbleRunLabScreen(onBack = onBack)
        Route.FLUID -> FluidLabScreen(onBack = onBack)
        Route.SOLIDS -> SolidsLabScreen(onBack = onBack)
        Route.HEIGHTMAP3D -> Heightmap3DLabScreen(onBack = onBack)
        Route.FUNCTION_PLOTTER -> FunctionPlotterLabScreen(onBack = onBack)
        Route.POINT_CLOUD_MORPH -> PointCloudMorphLabScreen(onBack = onBack)
        Route.RAYCASTER -> RaycasterLabScreen(onBack = onBack)
        Route.OBJ_MODEL -> ObjModelLabScreen(onBack = onBack)
        Route.MANDALA -> MandalaPadLabScreen(onBack = onBack)
        Route.FLOW_FIELD -> FlowFieldLabScreen(onBack = onBack)
        Route.CIRCLE_PACKING -> CirclePackingLabScreen(onBack = onBack)
        Route.LIGHTNING -> LightningLabScreen(onBack = onBack)
        Route.CLOCK_GALLERY -> ClockGalleryLabScreen(onBack = onBack)
        Route.TURTLE -> TurtleReplLabScreen(onBack = onBack)
        Route.DOODLE_PHYSICS -> DoodlePhysicsLabScreen(onBack = onBack)
        Route.HAND_LANDMARK -> HandLandmarkScreen(onBack = onBack)
        Route.POSE_LANDMARK -> PoseLandmarkScreen(onBack = onBack)
        Route.IMAGE_CLASSIFY -> ImageClassificationScreen(onBack = onBack)
        Route.LENS_EXPLORER -> LensExplorerScreen(onBack = onBack)
        Route.PRO_MODE -> ProModeScreen(onBack = onBack)
        Route.EXPOSURE_TOOLS -> ExposureToolsScreen(onBack = onBack)
        Route.VIDEO_RECORDER -> VideoRecorderScreen(onBack = onBack)
        Route.RAW_CAPTURE -> RawCaptureScreen(onBack = onBack)
        Route.CAM_EXTENSIONS -> ExtensionsScreen(onBack = onBack)
        Route.HIGH_FPS -> HighFrameRateScreen(onBack = onBack)
        Route.TIMELAPSE -> TimelapseScreen(onBack = onBack)
        Route.MOTION_CAM -> MotionCamScreen(onBack = onBack)
        Route.DOC_SCANNER -> DocScannerScreen(onBack = onBack)
        Route.CAMERA_GALLERY -> CameraGalleryScreen(onBack = onBack)
        Route.DYNAMIC_ISLAND -> DynamicIslandScreen(onBack = onBack)
        Route.LIQUID_SWIPE -> LiquidSwipeScreen(onBack = onBack)
        Route.PARTICLE_TEXT -> ParticleTextScreen(onBack = onBack)
        Route.MORPHING_ICONS -> MorphingIconsScreen(onBack = onBack)
        Route.GOOEY_NAV -> GooeyNavBarScreen(onBack = onBack)
        Route.MORPHING_SHEET -> MorphingSheetScreen(onBack = onBack)
        Route.FROSTED_GLASS -> FrostedGlassScreen(onBack = onBack)
        Route.PALETTE_THEMING -> PaletteThemingScreen(onBack = onBack)
        Route.HOLOGRAPHIC -> HolographicCardScreen(onBack = onBack)
        Route.TOUCH_RIPPLE -> TouchRippleWaterScreen(onBack = onBack)
        Route.MAGNIFIER -> MagnifierLensScreen(onBack = onBack)
        Route.AURORA -> AuroraScreen(onBack = onBack)
        Route.CAUSTICS -> CausticsScreen(onBack = onBack)
        Route.SOAP_BUBBLE -> SoapBubbleScreen(onBack = onBack)
        Route.CRT -> CrtScreen(onBack = onBack)
        Route.PIXELATE -> PixelateScreen(onBack = onBack)
        Route.HEAT_HAZE -> HeatHazeScreen(onBack = onBack)
        Route.BLACK_HOLE -> BlackHoleScreen(onBack = onBack)
        Route.NEON_SIGN -> NeonSignScreen(onBack = onBack)
        Route.CONIC_GLOW -> ConicGlowBorderScreen(onBack = onBack)
        Route.WAVE_FILL -> LiquidWaveFillScreen(onBack = onBack)
        Route.DEPTH_PARALLAX -> DepthParallaxScreen(onBack = onBack)
        Route.INK_SPREAD -> InkSpreadScreen(onBack = onBack)
        Route.VORONOI_SHATTER -> VoronoiShatterScreen(onBack = onBack)
        Route.SMOKE_FLUID -> SmokeFluidScreen(onBack = onBack)
        Route.SPEEDOMETER -> SpeedometerScreen(onBack = onBack)
        Route.FINGERPRINT_AUDIT -> FingerprintAuditorScreen(onBack = onBack)
        Route.HASHING -> HashingScreen(onBack = onBack)
        Route.CRYPTO_PLAY -> CryptoPlaygroundScreen(onBack = onBack)
        Route.PASSWORD_GEN -> PasswordGeneratorScreen(onBack = onBack)
        Route.BIOMETRIC_LOCK -> BiometricLockScreen(onBack = onBack)
        Route.CLIPBOARD_MON -> ClipboardMonitorScreen(onBack = onBack)
        Route.LOCAL_SERVER -> LocalServerScreen(onBack = onBack)
        Route.NETWORK_INFO -> NetworkInfoScreen(onBack = onBack)
        Route.PING_SCAN -> PingScanScreen(onBack = onBack)
        Route.NSD_DISCOVERY -> NsdDiscoveryScreen(onBack = onBack)
        Route.BLE_PERIPHERAL -> BlePeripheralScreen(onBack = onBack)
        Route.NOTIF_STYLES -> NotificationStylesScreen(onBack = onBack)
        Route.NOTIF_ACTIONS -> NotificationActionsScreen(onBack = onBack)
        Route.NOTIF_PROGRESS -> NotificationProgressScreen(onBack = onBack)
        Route.JSON_FMT -> JsonFormatterScreen(onBack = onBack)
        Route.ENCODER -> EncoderScreen(onBack = onBack)
        Route.REGEX_TEST -> RegexTesterScreen(onBack = onBack)
        Route.JWT_DECODE -> JwtDecoderScreen(onBack = onBack)
        Route.GENERATORS -> GeneratorsScreen(onBack = onBack)
        Route.WORLD_CLOCK -> WorldClockScreen(onBack = onBack)
        Route.STOPWATCH -> StopwatchScreen(onBack = onBack)
        Route.POMODORO -> PomodoroScreen(onBack = onBack)
        Route.BUBBLE_WRAP -> BubbleWrapScreen(onBack = onBack)
        Route.DECISION_WHEEL -> DecisionWheelScreen(onBack = onBack)
        Route.MAGIC8 -> Magic8BallScreen(onBack = onBack)
        Route.WHITE_NOISE -> WhiteNoiseScreen(onBack = onBack)
        Route.DRUM_MACHINE -> DrumMachineScreen(onBack = onBack)
        Route.THEREMIN -> ThereminScreen(onBack = onBack)
        Route.TONE_GEN -> ToneGeneratorScreen(onBack = onBack)
        Route.CLAP_WHISTLE -> ClapWhistleScreen(onBack = onBack)
        Route.COMPONENTS_3D -> Components3DScreen(onBack = onBack)
        Route.SHADER_3D -> ShaderComponents3DScreen(onBack = onBack)
        Route.COORD_TOOLS -> CoordinateToolsScreen(onBack = onBack)
        Route.SUN_MOON -> SunMoonScreen(onBack = onBack)
        Route.COMPASS_NAV -> CompassNavigatorScreen(onBack = onBack)
        Route.GPS_TRACKER -> GpsTrackerScreen(onBack = onBack)
        Route.PRESENT -> PresentScreen(onBack = onBack)
        Route.DECK_EDITOR -> DeckEditorScreen(onBack = onBack)
        Route.GRADIENT_MORPH -> GradientMorphLabScreen(onBack = onBack)
        Route.MEDIA_STREAM -> MediaStreamScreen(onBack = onBack)
        Route.FLOW_LEARNER -> FlowLearnerScreen(onBack = onBack)
        Route.RECOMPOSITION_VISUALIZER -> RecompositionVisualizerScreen(onBack = onBack)
        Route.DISPATCHER_VISUALIZER -> DispatcherVisualizerScreen(onBack = onBack)
        Route.CONCURRENCY_VISUALIZER -> ConcurrencyVisualizerScreen(onBack = onBack)
        Route.API_VISUALIZER -> ApiVisualizerScreen(onBack = onBack)
        Route.CRYPTO_VISUALIZER -> CryptoVisualizerScreen(onBack = onBack)
        Route.DB_INDEX_VISUALIZER -> DbIndexVisualizerScreen(onBack = onBack)
        Route.TCP_VISUALIZER -> TcpVisualizerScreen(onBack = onBack)
        Route.GIT_VISUALIZER -> GitVisualizerScreen(onBack = onBack)
        Route.COMPOSE_PHASES -> ComposePhasesScreen(onBack = onBack)
        Route.CONSTRAINT_FLOW -> ConstraintFlowScreen(onBack = onBack)
        Route.LAZY_LIFECYCLE -> LazyLifecycleScreen(onBack = onBack)
        Route.HTML_PARSER_PLAYGROUND -> HtmlParserPlaygroundScreen(onBack = onBack)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevControlsScreen(
    mainScroll: androidx.compose.foundation.ScrollState,
    expandedCats: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>,
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

            CategoryMenu(expandedCats = expandedCats, onNavigate = onNavigate)

            Spacer(Modifier.height(24.dp))
        }
    }
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
