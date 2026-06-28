package pro.jayeshseth.devcontrols.nav

import androidx.compose.runtime.Composable

/**
 * Renders a portable (commonMain) screen for [route] and returns true; returns false if [route]
 * is not a common screen (then the platform host handles it). Generated as screens migrate.
 */
@Composable
fun CommonScreen(route: Route, onBack: () -> Unit): Boolean {
    when (route) {
        Route.ACCORDION -> pro.jayeshseth.devcontrols.ui.AccordionLabScreen(onBack = onBack)
        Route.ANIMATED_BORDERS -> pro.jayeshseth.devcontrols.ui.AnimatedBordersLabScreen(onBack = onBack)
        Route.ANIMATED_CONTENT -> pro.jayeshseth.devcontrols.ui.AnimatedContentGalleryScreen(onBack = onBack)
        Route.API_VISUALIZER -> pro.jayeshseth.devcontrols.ui.ApiVisualizerScreen(onBack = onBack)
        Route.BILLIARDS -> pro.jayeshseth.devcontrols.ui.BilliardsLabScreen(onBack = onBack)
        Route.BOIDS -> pro.jayeshseth.devcontrols.ui.BoidsLabScreen(onBack = onBack)
        Route.BOTTOM_SHEET -> pro.jayeshseth.devcontrols.ui.BottomSheetLabScreen(onBack = onBack)
        Route.BREAKOUT -> pro.jayeshseth.devcontrols.ui.BreakoutGameScreen(onBack = onBack)
        Route.CALENDAR -> pro.jayeshseth.devcontrols.ui.CalendarLabScreen(onBack = onBack)
        Route.CAROUSEL -> pro.jayeshseth.devcontrols.ui.CarouselLabScreen(onBack = onBack)
        Route.CHARGED -> pro.jayeshseth.devcontrols.ui.ChargedParticlesLabScreen(onBack = onBack)
        Route.CHARTS -> pro.jayeshseth.devcontrols.ui.ChartsLabScreen(onBack = onBack)
        Route.CIRCLE_PACKING -> pro.jayeshseth.devcontrols.ui.CirclePackingLabScreen(onBack = onBack)
        Route.CIRCULAR_REVEAL -> pro.jayeshseth.devcontrols.ui.CircularRevealThemeScreen(onBack = onBack)
        Route.CLOCK_GALLERY -> pro.jayeshseth.devcontrols.ui.ClockGalleryLabScreen(onBack = onBack)
        Route.CLOTH -> pro.jayeshseth.devcontrols.ui.ClothLabScreen(onBack = onBack)
        Route.COACH_MARK -> pro.jayeshseth.devcontrols.ui.CoachMarkLabScreen(onBack = onBack)
        Route.COLOR_PICKER -> pro.jayeshseth.devcontrols.ui.ColorPickerLabScreen(onBack = onBack)
        Route.COMPOSE_PHASES -> pro.jayeshseth.devcontrols.ui.ComposePhasesScreen(onBack = onBack)
        Route.CONFETTI -> pro.jayeshseth.devcontrols.ui.ConfettiLabScreen(onBack = onBack)
        Route.CONCURRENCY_VISUALIZER -> pro.jayeshseth.devcontrols.ui.ConcurrencyVisualizerScreen(onBack = onBack)
        Route.CONSTRAINT_FLOW -> pro.jayeshseth.devcontrols.ui.ConstraintFlowScreen(onBack = onBack)
        Route.CONIC_GLOW -> pro.jayeshseth.devcontrols.ui.visualfx.ConicGlowBorderScreen(onBack = onBack)
        Route.CONVEX_HULL -> pro.jayeshseth.devcontrols.ui.ConvexHullLabScreen(onBack = onBack)
        Route.COORD_TOOLS -> pro.jayeshseth.devcontrols.ui.maps.CoordinateToolsScreen(onBack = onBack)
        Route.CRYPTO_VISUALIZER -> pro.jayeshseth.devcontrols.ui.CryptoVisualizerScreen(onBack = onBack)
        Route.CUSTOM_LAYOUT -> pro.jayeshseth.devcontrols.ui.CustomLayoutLabScreen(onBack = onBack)
        Route.DATA_TABLE -> pro.jayeshseth.devcontrols.ui.DataTableLabScreen(onBack = onBack)
        Route.DB_INDEX_VISUALIZER -> pro.jayeshseth.devcontrols.ui.DbIndexVisualizerScreen(onBack = onBack)
        Route.DECK_EDITOR -> pro.jayeshseth.devcontrols.ui.present.DeckEditorScreen(onBack = onBack)
        Route.DELAUNAY -> pro.jayeshseth.devcontrols.ui.DelaunayLabScreen(onBack = onBack)
        Route.DISPATCHER_VISUALIZER -> pro.jayeshseth.devcontrols.ui.DispatcherVisualizerScreen(onBack = onBack)
        Route.DOODLE_PHYSICS -> pro.jayeshseth.devcontrols.ui.DoodlePhysicsLabScreen(onBack = onBack)
        Route.DOUBLE_PENDULUM -> pro.jayeshseth.devcontrols.ui.DoublePendulumLabScreen(onBack = onBack)
        Route.DYNAMIC_ISLAND -> pro.jayeshseth.devcontrols.ui.visualfx.DynamicIslandScreen(onBack = onBack)
        Route.FALLING_SAND -> pro.jayeshseth.devcontrols.ui.FallingSandLabScreen(onBack = onBack)
        Route.FIREWORKS -> pro.jayeshseth.devcontrols.ui.FireworksLabScreen(onBack = onBack)
        Route.FLIP_CLOCK -> pro.jayeshseth.devcontrols.ui.FlipClockLabScreen(onBack = onBack)
        Route.FLOW_FIELD -> pro.jayeshseth.devcontrols.ui.FlowFieldLabScreen(onBack = onBack)
        Route.FLOW_LEARNER -> pro.jayeshseth.devcontrols.ui.FlowLearnerScreen(onBack = onBack)
        Route.FRACTAL_TREE -> pro.jayeshseth.devcontrols.ui.FractalTreeLabScreen(onBack = onBack)
        Route.FUNCTION_PLOTTER -> pro.jayeshseth.devcontrols.ui.FunctionPlotterLabScreen(onBack = onBack)
        Route.GALTON -> pro.jayeshseth.devcontrols.ui.GaltonBoardLabScreen(onBack = onBack)
        Route.GAME_2048 -> pro.jayeshseth.devcontrols.ui.Game2048Screen(onBack = onBack)
        Route.GAME_OF_LIFE -> pro.jayeshseth.devcontrols.ui.GameOfLifeLabScreen(onBack = onBack)
        Route.GIT_VISUALIZER -> pro.jayeshseth.devcontrols.ui.GitVisualizerScreen(onBack = onBack)
        Route.GLOBE -> pro.jayeshseth.devcontrols.ui.PointCloudGlobeLabScreen(onBack = onBack)
        Route.GOOEY_NAV -> pro.jayeshseth.devcontrols.ui.visualfx.GooeyNavBarScreen(onBack = onBack)
        Route.GRADIENT_MORPH -> pro.jayeshseth.devcontrols.ui.GradientMorphLabScreen(onBack = onBack)
        Route.HASHING -> pro.jayeshseth.devcontrols.ui.privacy.HashingScreen(onBack = onBack)
        Route.HEIGHTMAP3D -> pro.jayeshseth.devcontrols.ui.Heightmap3DLabScreen(onBack = onBack)
        Route.HTML_PARSER_PLAYGROUND -> pro.jayeshseth.devcontrols.ui.HtmlParserPlaygroundScreen(onBack = onBack)
        Route.IME_INSETS -> pro.jayeshseth.devcontrols.ui.ImeInsetsLabScreen(onBack = onBack)
        Route.JSON_FMT -> pro.jayeshseth.devcontrols.ui.devtools.JsonFormatterScreen(onBack = onBack)
        Route.JWT_DECODE -> pro.jayeshseth.devcontrols.ui.devtools.JwtDecoderScreen(onBack = onBack)
        Route.KALEIDOSCOPE -> pro.jayeshseth.devcontrols.ui.KaleidoscopeLabScreen(onBack = onBack)
        Route.KANBAN -> pro.jayeshseth.devcontrols.ui.KanbanLabScreen(onBack = onBack)
        Route.KTOR_PLAYGROUND -> pro.jayeshseth.devcontrols.ui.KtorPlaygroundScreen(onBack = onBack)
        Route.LAZY_LIFECYCLE -> pro.jayeshseth.devcontrols.ui.LazyLifecycleScreen(onBack = onBack)
        Route.LIGHTNING -> pro.jayeshseth.devcontrols.ui.LightningLabScreen(onBack = onBack)
        Route.LIQUID_SWIPE -> pro.jayeshseth.devcontrols.ui.visualfx.LiquidSwipeScreen(onBack = onBack)
        Route.LSYSTEM -> pro.jayeshseth.devcontrols.ui.LSystemLabScreen(onBack = onBack)
        Route.MANDALA -> pro.jayeshseth.devcontrols.ui.MandalaPadLabScreen(onBack = onBack)
        Route.MARBLE_RUN -> pro.jayeshseth.devcontrols.ui.MarbleRunLabScreen(onBack = onBack)
        Route.MARCHING_SQUARES -> pro.jayeshseth.devcontrols.ui.MarchingSquaresLabScreen(onBack = onBack)
        Route.MARKDOWN_PARSER_PLAYGROUND -> pro.jayeshseth.devcontrols.ui.MarkdownParserPlaygroundScreen(onBack = onBack)
        Route.MAZE -> pro.jayeshseth.devcontrols.ui.MazeLabScreen(onBack = onBack)
        Route.MEDIA_STREAM -> pro.jayeshseth.devcontrols.ui.MediaStreamScreen(onBack = onBack)
        Route.MEMORY_MATCH -> pro.jayeshseth.devcontrols.ui.MemoryMatchScreen(onBack = onBack)
        Route.MESH_GALLERY -> pro.jayeshseth.devcontrols.ui.MeshGalleryScreen(onBack = onBack)
        Route.MORPHING_ICONS -> pro.jayeshseth.devcontrols.ui.visualfx.MorphingIconsScreen(onBack = onBack)
        Route.MORPHING_SHEET -> pro.jayeshseth.devcontrols.ui.visualfx.MorphingSheetScreen(onBack = onBack)
        Route.NBODY -> pro.jayeshseth.devcontrols.ui.NBodyLabScreen(onBack = onBack)
        Route.NESTED_SCROLL -> pro.jayeshseth.devcontrols.ui.NestedScrollLabScreen(onBack = onBack)
        Route.NEWTONS_CRADLE -> pro.jayeshseth.devcontrols.ui.NewtonsCradleLabScreen(onBack = onBack)
        Route.NQUEENS -> pro.jayeshseth.devcontrols.ui.NQueensLabScreen(onBack = onBack)
        Route.OBJ_MODEL -> pro.jayeshseth.devcontrols.ui.ObjModelLabScreen(onBack = onBack)
        Route.ORBIT_SANDBOX -> pro.jayeshseth.devcontrols.ui.OrbitSandboxLabScreen(onBack = onBack)
        Route.PAGER_TRANSFORM -> pro.jayeshseth.devcontrols.ui.PagerTransformLabScreen(onBack = onBack)
        Route.PAGE_CURL -> pro.jayeshseth.devcontrols.ui.PageCurlLabScreen(onBack = onBack)
        Route.PARALLAX_HEADER -> pro.jayeshseth.devcontrols.ui.ParallaxHeaderLabScreen(onBack = onBack)
        Route.PARTICLES -> pro.jayeshseth.devcontrols.ui.ParticleLabScreen(onBack = onBack)
        Route.PARTICLE_TEXT -> pro.jayeshseth.devcontrols.ui.visualfx.ParticleTextScreen(onBack = onBack)
        Route.PARTICLE_VECTOR -> pro.jayeshseth.devcontrols.ui.visualfx.ParticleVectorScreen(onBack = onBack)
        Route.PNG_VECTORIZER -> pro.jayeshseth.devcontrols.ui.PngVectorizerLabScreen(onBack = onBack)
        Route.PATHFINDING -> pro.jayeshseth.devcontrols.ui.PathfindingLabScreen(onBack = onBack)
        Route.PATH_MORPH -> pro.jayeshseth.devcontrols.ui.PathMorphLabScreen(onBack = onBack)
        Route.PATH_PLAYER -> pro.jayeshseth.devcontrols.ui.PathKeyframePlayerScreen(onBack = onBack)
        Route.PHYSICS_MOTION -> pro.jayeshseth.devcontrols.ui.PhysicsMotionLabScreen(onBack = onBack)
        Route.PIN_PAD -> pro.jayeshseth.devcontrols.ui.PinPadLabScreen(onBack = onBack)
        Route.PIXEL_ART -> pro.jayeshseth.devcontrols.ui.PixelArtLabScreen(onBack = onBack)
        Route.POINT_CLOUD_MORPH -> pro.jayeshseth.devcontrols.ui.PointCloudMorphLabScreen(onBack = onBack)
        Route.POMODORO -> pro.jayeshseth.devcontrols.ui.timedate.PomodoroScreen(onBack = onBack)
        Route.PRESENT -> pro.jayeshseth.devcontrols.ui.present.PresentScreen(onBack = onBack)
        Route.PROGRESS_GALLERY -> pro.jayeshseth.devcontrols.ui.ProgressGalleryLabScreen(onBack = onBack)
        Route.PULL_DISMISS -> pro.jayeshseth.devcontrols.ui.PullToDismissLabScreen(onBack = onBack)
        Route.PULL_REFRESH -> pro.jayeshseth.devcontrols.ui.PullRefreshLabScreen(onBack = onBack)
        Route.QUADTREE -> pro.jayeshseth.devcontrols.ui.QuadtreeLabScreen(onBack = onBack)
        Route.RATING -> pro.jayeshseth.devcontrols.ui.RatingBarLabScreen(onBack = onBack)
        Route.RAYCASTER -> pro.jayeshseth.devcontrols.ui.RaycasterLabScreen(onBack = onBack)
        Route.REGEX_TEST -> pro.jayeshseth.devcontrols.ui.devtools.RegexTesterScreen(onBack = onBack)
        Route.REORDER_LIST -> pro.jayeshseth.devcontrols.ui.ReorderListLabScreen(onBack = onBack)
        Route.RECOMPOSITION_VISUALIZER -> pro.jayeshseth.devcontrols.ui.RecompositionVisualizerScreen(onBack = onBack)
        Route.SCRATCH -> pro.jayeshseth.devcontrols.ui.ScratchRevealLabScreen(onBack = onBack)
        Route.SEGMENTED -> pro.jayeshseth.devcontrols.ui.SegmentedControlLabScreen(onBack = onBack)
        Route.SHARED_ELEMENT -> pro.jayeshseth.devcontrols.ui.SharedElementLabScreen(onBack = onBack)
        Route.SHIMMER -> pro.jayeshseth.devcontrols.ui.ShimmerLabScreen(onBack = onBack)
        Route.SIGNATURE -> pro.jayeshseth.devcontrols.ui.SignaturePadLabScreen(onBack = onBack)
        Route.SNAKE -> pro.jayeshseth.devcontrols.ui.SnakeGameScreen(onBack = onBack)
        Route.SOFT_BODY -> pro.jayeshseth.devcontrols.ui.SoftBodyLabScreen(onBack = onBack)
        Route.SOLIDS -> pro.jayeshseth.devcontrols.ui.SolidsLabScreen(onBack = onBack)
        Route.SORTING -> pro.jayeshseth.devcontrols.ui.SortingVisualizerScreen(onBack = onBack)
        Route.SPEED_DIAL -> pro.jayeshseth.devcontrols.ui.SpeedDialFabLabScreen(onBack = onBack)
        Route.SPIROGRAPH -> pro.jayeshseth.devcontrols.ui.SpirographLabScreen(onBack = onBack)
        Route.SPOTLIGHT -> pro.jayeshseth.devcontrols.ui.SpotlightLabScreen(onBack = onBack)
        Route.SPRING_CHAIN -> pro.jayeshseth.devcontrols.ui.SpringChainLabScreen(onBack = onBack)
        Route.STAGGERED -> pro.jayeshseth.devcontrols.ui.StaggeredRevealLabScreen(onBack = onBack)
        Route.STARFIELD -> pro.jayeshseth.devcontrols.ui.StarfieldLabScreen(onBack = onBack)
        Route.STEPPER -> pro.jayeshseth.devcontrols.ui.StepperWizardLabScreen(onBack = onBack)
        Route.STOPWATCH -> pro.jayeshseth.devcontrols.ui.timedate.StopwatchScreen(onBack = onBack)
        Route.SUN_MOON -> pro.jayeshseth.devcontrols.ui.maps.SunMoonScreen(onBack = onBack)
        Route.SVG_COMPILER_PLAYGROUND -> pro.jayeshseth.devcontrols.ui.SvgCompilerPlaygroundScreen(onBack = onBack)
        Route.SWIPE_CARD -> pro.jayeshseth.devcontrols.ui.SwipeCardLabScreen(onBack = onBack)
        Route.TAG_INPUT -> pro.jayeshseth.devcontrols.ui.TagInputLabScreen(onBack = onBack)
        Route.TCP_VISUALIZER -> pro.jayeshseth.devcontrols.ui.TcpVisualizerScreen(onBack = onBack)
        Route.TETRIS -> pro.jayeshseth.devcontrols.ui.TetrisLiteScreen(onBack = onBack)
        Route.TIMELINE -> pro.jayeshseth.devcontrols.ui.TimelineLabScreen(onBack = onBack)
        Route.TOUCH_INTERNALS -> pro.jayeshseth.devcontrols.ui.TouchInternalsLabScreen(onBack = onBack)
        Route.TRANSFORM_3D -> pro.jayeshseth.devcontrols.ui.Transform3DLabScreen(onBack = onBack)
        Route.TREE_BROWSER -> pro.jayeshseth.devcontrols.ui.TreeBrowserLabScreen(onBack = onBack)
        Route.TSP -> pro.jayeshseth.devcontrols.ui.TspLabScreen(onBack = onBack)
        Route.TURTLE -> pro.jayeshseth.devcontrols.ui.TurtleReplLabScreen(onBack = onBack)
        Route.VORONOI_SHATTER -> pro.jayeshseth.devcontrols.ui.visualfx.VoronoiShatterScreen(onBack = onBack)
        Route.WAVE_FILL -> pro.jayeshseth.devcontrols.ui.visualfx.LiquidWaveFillScreen(onBack = onBack)
        Route.WEATHER -> pro.jayeshseth.devcontrols.ui.WeatherLabScreen(onBack = onBack)
        Route.WHEEL_PICKER -> pro.jayeshseth.devcontrols.ui.WheelPickerLabScreen(onBack = onBack)
        Route.WINDOW_SIZE -> pro.jayeshseth.devcontrols.ui.WindowSizeLabScreen(onBack = onBack)
        Route.WIREFRAME -> pro.jayeshseth.devcontrols.ui.WireframeLabScreen(onBack = onBack)
        Route.WORLD_CLOCK -> pro.jayeshseth.devcontrols.ui.timedate.WorldClockScreen(onBack = onBack)
        Route.TONE_GEN -> pro.jayeshseth.devcontrols.ui.audiolab.ToneGeneratorScreen(onBack = onBack)
        Route.THEREMIN -> pro.jayeshseth.devcontrols.ui.audiolab.ThereminScreen(onBack = onBack)
        Route.SYNTH -> pro.jayeshseth.devcontrols.ui.SynthKeyboardLabScreen(onBack = onBack)
        Route.METRONOME -> pro.jayeshseth.devcontrols.ui.MetronomeLabScreen(onBack = onBack)
        Route.DRUM_MACHINE -> pro.jayeshseth.devcontrols.ui.audiolab.DrumMachineScreen(onBack = onBack)
        Route.CLAP_WHISTLE -> pro.jayeshseth.devcontrols.ui.audiolab.ClapWhistleScreen(onBack = onBack)
        Route.BINAURAL_BEATS -> pro.jayeshseth.devcontrols.ui.audiolab.BinauralBeatsScreen(onBack = onBack)
        Route.AUDIO_SYNC -> pro.jayeshseth.devcontrols.ui.audiolab.AudioSyncScreen(onBack = onBack)
        Route.STANDBY_TRACKER -> pro.jayeshseth.devcontrols.ui.devtools.StandbyTrackerScreen(onBack = onBack)
        Route.ANIMATION_BUILDER -> pro.jayeshseth.devcontrols.ui.devtools.AnimationBuilderScreen(onBack = onBack)
        Route.NEON_DRAW -> pro.jayeshseth.devcontrols.ui.visualfx.NeonGlowBoardScreen(onBack = onBack)
        Route.PROMPT_TO_UI -> pro.jayeshseth.devcontrols.ui.PromptToUiScreen(onBack = onBack)
        else -> return false
    }
    return true
}


/** Routes available in commonMain - used by the desktop/web default home. */
val commonRoutes: Set<Route> = setOf(
    Route.ACCORDION,
    Route.ANIMATED_BORDERS,
    Route.ANIMATED_CONTENT,
    Route.API_VISUALIZER,
    Route.BILLIARDS,
    Route.BOIDS,
    Route.BOTTOM_SHEET,
    Route.BREAKOUT,
    Route.CALENDAR,
    Route.CAROUSEL,
    Route.CHARGED,
    Route.CHARTS,
    Route.CIRCLE_PACKING,
    Route.CIRCULAR_REVEAL,
    Route.CLOCK_GALLERY,
    Route.CLOTH,
    Route.COACH_MARK,
    Route.COLOR_PICKER,
    Route.COMPOSE_PHASES,
    Route.CONFETTI,
    Route.CONCURRENCY_VISUALIZER,
    Route.CONSTRAINT_FLOW,
    Route.CONIC_GLOW,
    Route.CONVEX_HULL,
    Route.COORD_TOOLS,
    Route.CRYPTO_VISUALIZER,
    Route.CUSTOM_LAYOUT,
    Route.DATA_TABLE,
    Route.DB_INDEX_VISUALIZER,
    Route.DECK_EDITOR,
    Route.DELAUNAY,
    Route.DISPATCHER_VISUALIZER,
    Route.DOODLE_PHYSICS,
    Route.DOUBLE_PENDULUM,
    Route.DYNAMIC_ISLAND,
    Route.FALLING_SAND,
    Route.FIREWORKS,
    Route.FLIP_CLOCK,
    Route.FLOW_FIELD,
    Route.FLOW_LEARNER,
    Route.FRACTAL_TREE,
    Route.FUNCTION_PLOTTER,
    Route.GALTON,
    Route.GAME_2048,
    Route.GAME_OF_LIFE,
    Route.GIT_VISUALIZER,
    Route.GLOBE,
    Route.GOOEY_NAV,
    Route.GRADIENT_MORPH,
    Route.HASHING,
    Route.HEIGHTMAP3D,
    Route.HTML_PARSER_PLAYGROUND,
    Route.IME_INSETS,
    Route.JSON_FMT,
    Route.JWT_DECODE,
    Route.KALEIDOSCOPE,
    Route.KANBAN,
    Route.KTOR_PLAYGROUND,
    Route.LAZY_LIFECYCLE,
    Route.LIGHTNING,
    Route.LIQUID_SWIPE,
    Route.LSYSTEM,
    Route.MANDALA,
    Route.MARBLE_RUN,
    Route.MARCHING_SQUARES,
    Route.MARKDOWN_PARSER_PLAYGROUND,
    Route.MAZE,
    Route.MEDIA_STREAM,
    Route.MEMORY_MATCH,
    Route.MESH_GALLERY,
    Route.MORPHING_ICONS,
    Route.MORPHING_SHEET,
    Route.NBODY,
    Route.NESTED_SCROLL,
    Route.NEWTONS_CRADLE,
    Route.NQUEENS,
    Route.OBJ_MODEL,
    Route.ORBIT_SANDBOX,
    Route.PAGER_TRANSFORM,
    Route.PAGE_CURL,
    Route.PARALLAX_HEADER,
    Route.PARTICLES,
    Route.PARTICLE_TEXT,
    Route.PARTICLE_VECTOR,
    Route.PNG_VECTORIZER,
    Route.PATHFINDING,
    Route.PATH_MORPH,
    Route.PATH_PLAYER,
    Route.PHYSICS_MOTION,
    Route.PIN_PAD,
    Route.PIXEL_ART,
    Route.POINT_CLOUD_MORPH,
    Route.POMODORO,
    Route.PRESENT,
    Route.PROGRESS_GALLERY,
    Route.PULL_DISMISS,
    Route.PULL_REFRESH,
    Route.QUADTREE,
    Route.RATING,
    Route.RAYCASTER,
    Route.REGEX_TEST,
    Route.REORDER_LIST,
    Route.RECOMPOSITION_VISUALIZER,
    Route.SCRATCH,
    Route.SEGMENTED,
    Route.SHARED_ELEMENT,
    Route.SHIMMER,
    Route.SIGNATURE,
    Route.SNAKE,
    Route.SOFT_BODY,
    Route.SOLIDS,
    Route.SORTING,
    Route.SPEED_DIAL,
    Route.SPIROGRAPH,
    Route.SPOTLIGHT,
    Route.SPRING_CHAIN,
    Route.STAGGERED,
    Route.STARFIELD,
    Route.STEPPER,
    Route.STOPWATCH,
    Route.SUN_MOON,
    Route.SVG_COMPILER_PLAYGROUND,
    Route.SWIPE_CARD,
    Route.TAG_INPUT,
    Route.TCP_VISUALIZER,
    Route.TETRIS,
    Route.TIMELINE,
    Route.TOUCH_INTERNALS,
    Route.TRANSFORM_3D,
    Route.TREE_BROWSER,
    Route.TSP,
    Route.TURTLE,
    Route.VORONOI_SHATTER,
    Route.WAVE_FILL,
    Route.WEATHER,
    Route.WHEEL_PICKER,
    Route.WINDOW_SIZE,
    Route.WIREFRAME,
    Route.WORLD_CLOCK,
    Route.TONE_GEN,
    Route.THEREMIN,
    Route.SYNTH,
    Route.METRONOME,
    Route.DRUM_MACHINE,
    Route.CLAP_WHISTLE,
    Route.BINAURAL_BEATS,
    Route.AUDIO_SYNC,
    Route.STANDBY_TRACKER,
    Route.ANIMATION_BUILDER,
    Route.NEON_DRAW,
    Route.PROMPT_TO_UI,
)

