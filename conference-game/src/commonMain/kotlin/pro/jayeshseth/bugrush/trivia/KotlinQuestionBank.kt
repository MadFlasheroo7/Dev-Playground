package pro.jayeshseth.bugrush.trivia

object KotlinQuestionBank {

    private val questions = listOf(
        // Tier 1: Easy / Basics
        TriviaQuestion(
            id = "t1_01",
            tier = QuestionTier.TIER_1_EASY,
            powerUp = PowerUpType.SPEED_MAGNET,
            question = "Which keyword declares a read-only (immutable reference) variable in Kotlin?",
            options = listOf("var", "val", "const val", "let"),
            correctIndex = 1,
            explanation = "'val' declares a read-only variable whose reference cannot be reassigned."
        ),
        TriviaQuestion(
            id = "t1_02",
            tier = QuestionTier.TIER_1_EASY,
            powerUp = PowerUpType.SPEED_MAGNET,
            question = "What is the Elvis operator used for handling null values in Kotlin?",
            options = listOf("?:", "?.", "!!", ":?"),
            correctIndex = 0,
            explanation = "'?:' returns the left side if non-null, or the right side otherwise."
        ),
        TriviaQuestion(
            id = "t1_03",
            tier = QuestionTier.TIER_1_EASY,
            powerUp = PowerUpType.SPEED_MAGNET,
            question = "Which method is automatically generated for a 'data class'?",
            options = listOf("copy()", "serialize()", "compareTo()", "dispose()"),
            correctIndex = 0,
            explanation = "Kotlin data classes automatically generate copy(), equals(), hashCode(), toString(), and componentN()."
        ),
        TriviaQuestion(
            id = "t1_04",
            tier = QuestionTier.TIER_1_EASY,
            powerUp = PowerUpType.SPEED_MAGNET,
            question = "What is the return type of a Kotlin function that doesn't return any meaningful value?",
            options = listOf("void", "Nothing", "Unit", "Null"),
            correctIndex = 2,
            explanation = "'Unit' corresponds to void in Java, but is an actual singleton object in Kotlin."
        ),
        TriviaQuestion(
            id = "t1_05",
            tier = QuestionTier.TIER_1_EASY,
            powerUp = PowerUpType.SPEED_MAGNET,
            question = "How do you define a string template in Kotlin?",
            options = listOf("\"Hello \$name\"", "\"Hello {name}\"", "\"Hello @name\"", "\"Hello %name%\""),
            correctIndex = 0,
            explanation = "Kotlin string templates use '\$variable' or '\${expression}'."
        ),

        // Tier 2: Medium / Coroutines & Compose
        TriviaQuestion(
            id = "t2_01",
            tier = QuestionTier.TIER_2_MEDIUM,
            powerUp = PowerUpType.FREEZE_SHIELD,
            question = "Which Dispatcher is designed for disk or network I/O operations in Kotlin Coroutines?",
            options = listOf("Dispatchers.Default", "Dispatchers.IO", "Dispatchers.Main", "Dispatchers.Unconfined"),
            correctIndex = 1,
            explanation = "Dispatchers.IO uses an elastic pool of threads designed for blocking disk/network tasks."
        ),
        TriviaQuestion(
            id = "t2_02",
            tier = QuestionTier.TIER_2_MEDIUM,
            powerUp = PowerUpType.FREEZE_SHIELD,
            question = "In Jetpack Compose, which side-effect API runs a suspend function tied to a key change?",
            options = listOf("SideEffect", "DisposableEffect", "LaunchedEffect", "rememberCoroutineScope"),
            correctIndex = 2,
            explanation = "LaunchedEffect launches a coroutine whenever its specified key changes or enters composition."
        ),
        TriviaQuestion(
            id = "t2_03",
            tier = QuestionTier.TIER_2_MEDIUM,
            powerUp = PowerUpType.FREEZE_SHIELD,
            question = "What coroutine builder returns a 'Deferred<T>' that can be awaited?",
            options = listOf("launch", "async", "runBlocking", "withContext"),
            correctIndex = 1,
            explanation = "'async' starts a coroutine returning a Deferred<T>, whose result is retrieved via .await()."
        ),
        TriviaQuestion(
            id = "t2_04",
            tier = QuestionTier.TIER_2_MEDIUM,
            powerUp = PowerUpType.FREEZE_SHIELD,
            question = "In Compose, which primitive state holder ensures recomposition when its value changes?",
            options = listOf("mutableStateOf()", "rememberFlow()", "observableOf()", "liveData()"),
            correctIndex = 0,
            explanation = "mutableStateOf() creates an observable MutableState that triggers recomposition when mutated."
        ),
        TriviaQuestion(
            id = "t2_05",
            tier = QuestionTier.TIER_2_MEDIUM,
            powerUp = PowerUpType.FREEZE_SHIELD,
            question = "What Flow operator is used to prevent emitting duplicate consecutive values?",
            options = listOf("filterDistinct()", "distinctUntilChanged()", "debounce()", "conflate()"),
            correctIndex = 1,
            explanation = "'distinctUntilChanged()' suppresses emissions that equal the immediately preceding emission."
        ),

        // Tier 3: Hard / Advanced & Multiplatform
        TriviaQuestion(
            id = "t3_01",
            tier = QuestionTier.TIER_3_HARD,
            powerUp = PowerUpType.MEGA_FRENZY_NUKE,
            question = "Which modifier allows type arguments of an inline function to be inspected at runtime?",
            options = listOf("reified", "reflection", "actual", "crossinline"),
            correctIndex = 0,
            explanation = "'inline fun <reified T> foo()' inlines type bytecode so 'T::class' is accessible without reflection."
        ),
        TriviaQuestion(
            id = "t3_02",
            tier = QuestionTier.TIER_3_HARD,
            powerUp = PowerUpType.MEGA_FRENZY_NUKE,
            question = "What feature lets a function inform the compiler about conditions that hold true after execution?",
            options = listOf("Kotlin Contracts", "Smart Bounds", "Sealed Guarantees", "Inline Asserts"),
            correctIndex = 0,
            explanation = "Kotlin Contracts (e.g. returns() implies ...) enrich compiler analysis for smart casts."
        ),
        TriviaQuestion(
            id = "t3_03",
            tier = QuestionTier.TIER_3_HARD,
            powerUp = PowerUpType.MEGA_FRENZY_NUKE,
            question = "In Kotlin Multiplatform, how are platform-specific implementations paired with common declarations?",
            options = listOf("expect / actual", "abstract / concrete", "interface / platform", "export / import"),
            correctIndex = 0,
            explanation = "The common source declares 'expect' declarations, and each platform target provides 'actual' implementations."
        ),
        TriviaQuestion(
            id = "t3_04",
            tier = QuestionTier.TIER_3_HARD,
            powerUp = PowerUpType.MEGA_FRENZY_NUKE,
            question = "What does the 'crossinline' modifier do on a lambda parameter of an inline function?",
            options = listOf(
                "Disallows non-local returns from the lambda",
                "Prevents the lambda from running on background threads",
                "Allows the lambda to be invoked multiple times in parallel",
                "Boxes the lambda for reflection"
            ),
            correctIndex = 0,
            explanation = "'crossinline' forbids non-local 'return' statements from within the passed lambda."
        ),
        TriviaQuestion(
            id = "t3_05",
            tier = QuestionTier.TIER_3_HARD,
            powerUp = PowerUpType.MEGA_FRENZY_NUKE,
            question = "Which annotation in Kotlin defines a zero-allocation single-property wrapper?",
            options = listOf("@JvmInline value class", "@DataWrapper", "@PrimitiveStruct", "@InlineRecord"),
            correctIndex = 0,
            explanation = "@JvmInline value class wraps a single value without heap allocation overhead wherever possible."
        )
    )

    fun getRandomQuestion(tier: QuestionTier): TriviaQuestion {
        return questions.filter { it.tier == tier }.random()
    }
}
