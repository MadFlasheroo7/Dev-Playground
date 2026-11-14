package pro.jayeshseth.devcontrols.sdui

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import pro.jayeshseth.devcontrols.sdui.dsl.actions
import pro.jayeshseth.devcontrols.sdui.dsl.canvasOperations
import pro.jayeshseth.devcontrols.sdui.dsl.enterTransition
import pro.jayeshseth.devcontrols.sdui.dsl.exitTransition
import pro.jayeshseth.devcontrols.sdui.dsl.keyframes
import pro.jayeshseth.devcontrols.sdui.dsl.modifier
import pro.jayeshseth.devcontrols.sdui.dsl.onClick
import pro.jayeshseth.devcontrols.sdui.dsl.repeatable
import pro.jayeshseth.devcontrols.sdui.dsl.sduiAnimatedVisibility
import pro.jayeshseth.devcontrols.sdui.dsl.sduiBox
import pro.jayeshseth.devcontrols.sdui.dsl.sduiButton
import pro.jayeshseth.devcontrols.sdui.dsl.sduiCanvas
import pro.jayeshseth.devcontrols.sdui.dsl.sduiCard
import pro.jayeshseth.devcontrols.sdui.dsl.sduiColumn
import pro.jayeshseth.devcontrols.sdui.dsl.sduiCustom
import pro.jayeshseth.devcontrols.sdui.dsl.sduiDocument
import pro.jayeshseth.devcontrols.sdui.dsl.sduiImage
import pro.jayeshseth.devcontrols.sdui.dsl.sduiLazyColumn
import pro.jayeshseth.devcontrols.sdui.dsl.sduiLazyGrid
import pro.jayeshseth.devcontrols.sdui.dsl.sduiLazyRow
import pro.jayeshseth.devcontrols.sdui.dsl.sduiModifier
import pro.jayeshseth.devcontrols.sdui.dsl.sduiRow
import pro.jayeshseth.devcontrols.sdui.dsl.sduiSpacer
import pro.jayeshseth.devcontrols.sdui.dsl.sduiText
import pro.jayeshseth.devcontrols.sdui.dsl.sduiTextField
import pro.jayeshseth.devcontrols.sdui.dsl.snap
import pro.jayeshseth.devcontrols.sdui.dsl.spring
import pro.jayeshseth.devcontrols.sdui.dsl.toJson
import pro.jayeshseth.devcontrols.sdui.dsl.toJsonElement
import pro.jayeshseth.devcontrols.sdui.dsl.tween
import pro.jayeshseth.devcontrols.sdui.model.SduiAction
import pro.jayeshseth.devcontrols.sdui.model.SduiAnimationSpec
import pro.jayeshseth.devcontrols.sdui.model.SduiCanvasOp
import pro.jayeshseth.devcontrols.sdui.model.SduiDocument
import pro.jayeshseth.devcontrols.sdui.model.SduiModifier
import pro.jayeshseth.devcontrols.sdui.model.SduiNode
import pro.jayeshseth.devcontrols.sdui.model.defaultSduiJson
import pro.jayeshseth.devcontrols.sdui.state.SduiStateStore

class SduiDslTest {

    // --- 1. SduiDocument and Serialization Tests ---

    @Test
    fun testDocumentBuilderBasic() {
        val doc = sduiDocument("welcome_screen", version = 2) {
            metadata("title" to "Welcome", "author" to "SDUI Team")
            initialState(
                "userName" to "Alice",
                "loginAttempts" to 3,
                "isLoggedIn" to true
            )

            sduiColumn(
                id = "main_col",
                spacingDp = 16f,
                horizontalAlignment = "centerHorizontally",
                modifier = {
                    padding(all = 16f)
                    fillMaxSize()
                }
            ) {
                sduiText(
                    text = "Welcome back, \${userName}!",
                    fontSize = 24f,
                    fontWeight = "bold",
                    color = "#FF000000"
                )
                sduiButton(
                    text = "Continue",
                    onClick = {
                        navigate("/dashboard")
                    }
                )
            }
        }

        assertEquals("welcome_screen", doc.id)
        assertEquals(2, doc.version)
        assertEquals("Welcome", doc.metadata["title"])
        assertEquals("Alice", (doc.initialState["userName"] as? JsonPrimitive)?.content)
        assertEquals(3, (doc.initialState["loginAttempts"] as? JsonPrimitive)?.int)
        assertEquals(true, (doc.initialState["isLoggedIn"] as? JsonPrimitive)?.boolean)

        assertTrue(doc.root is SduiNode.Column)
        val col = doc.root
        assertEquals(2, col.children.size)

        // Serialize to JSON and deserialize back
        val json = doc.toJson()
        assertTrue(json.contains("\"welcome_screen\""))
        assertTrue(json.contains("\"type\": \"column\""))

        val deserialized = defaultSduiJson.decodeFromString<SduiDocument>(json)
        assertEquals(doc.id, deserialized.id)
        assertEquals(doc.version, deserialized.version)
        assertEquals(doc.metadata, deserialized.metadata)
        assertEquals(doc.initialState.keys, deserialized.initialState.keys)

        // toJsonElement
        val element = doc.toJsonElement()
        assertNotNull(element)
    }

    // --- 2. Node Builders Tests ---

    @Test
    fun testAllNodeBuilders() {
        val doc = sduiDocument("nodes_test") {
            sduiColumn {
                sduiBox(contentAlignment = "topCenter") {
                    sduiText("Inside Box")
                }
                sduiRow(spacingDp = 8f, horizontalArrangement = "spaceBetween") {
                    sduiImage(url = "https://example.com/pic.png", contentDescription = "Sample")
                    sduiSpacer(width = 16f, height = 16f)
                    sduiTextField(valueStateKey = "query", placeholder = "Search...")
                }
                sduiLazyColumn(spacingDp = 4f, contentPaddingDp = 8f) {
                    sduiText("Item 1")
                    sduiText("Item 2")
                }
                sduiLazyRow(spacingDp = 8f) {
                    sduiCard(elevation = 4f, cornerRadius = 16f) {
                        sduiText("Card Item")
                    }
                }
                sduiLazyGrid(columns = 3, spacingDp = 12f) {
                    sduiText("Grid 1")
                    sduiText("Grid 2")
                    sduiText("Grid 3")
                }
                sduiButton(text = "Submit", variant = "outlined", enabledExpr = "query != ''")
                sduiCanvas {
                    circle(centerX = 20f, centerY = 20f, radius = 15f, color = "#FF00FF00")
                }
                sduiAnimatedVisibility(visibleExpr = "isVisible") {
                    sduiText("Fading Text")
                }
                sduiCustom(componentName = "videoPlayer", "videoId" to "12345")
            }
        }

        val col = doc.root as SduiNode.Column
        assertEquals(9, col.children.size)
        assertTrue(col.children[0] is SduiNode.Box)
        assertTrue(col.children[1] is SduiNode.Row)
        assertTrue(col.children[2] is SduiNode.LazyColumn)
        assertTrue(col.children[3] is SduiNode.LazyRow)
        assertTrue(col.children[4] is SduiNode.LazyVerticalGrid)
        assertTrue(col.children[5] is SduiNode.Button)
        assertTrue(col.children[6] is SduiNode.Canvas)
        assertTrue(col.children[7] is SduiNode.AnimatedVisibility)
        assertTrue(col.children[8] is SduiNode.Custom)

        val json = doc.toJson()
        val deserialized = defaultSduiJson.decodeFromString<SduiDocument>(json)
        val deserializedCol = deserialized.root as SduiNode.Column
        assertEquals(9, deserializedCol.children.size)
    }

    // --- 3. Modifier Builders Tests ---

    @Test
    fun testAllModifierBuilders() {
        val modifiers = sduiModifier {
            padding(all = 12f)
            padding(horizontal = 8f, vertical = 4f)
            padding(start = 2f, top = 4f, end = 6f, bottom = 8f)
            size(width = 100f, height = 200f)
            fillMaxWidth()
            fillMaxHeight()
            fillMaxSize()
            aspectRatio(16f / 9f)
            weight(1.5f)
            background(color = "#FF000000", cornerRadius = 8f)
            backgroundGradient(
                gradientColors = listOf("#FFFF0000", "#FF0000FF"),
                gradientAngle = 45f
            )
            border(width = 2f, color = "#FF333333", cornerRadius = 4f, shape = "rounded")
            clip(shape = "rounded", cornerRadius = 8f)
            clipCircle()
            shadow(elevation = 6f, shape = "circle")
            blur(radius = 15f)
            graphicsLayer(
                scaleX = 1.2f,
                scaleY = 1.2f,
                rotationZ = 45f,
                alpha = 0.9f
            )
            clickable(rippleEnabled = true) {
                toggleState("isOpen")
            }
            gestureDrag(
                orientation = "horizontal",
                minOffset = -200f,
                maxOffset = 200f,
                targetStateKey = "drag"
            )
            animateContentSize(animation = spring())
        }

        assertTrue(modifiers.any { it is SduiModifier.Padding })
        assertTrue(modifiers.any { it is SduiModifier.Size })
        assertTrue(modifiers.any { it is SduiModifier.Background })
        assertTrue(modifiers.any { it is SduiModifier.Border })
        assertTrue(modifiers.any { it is SduiModifier.Clip })
        assertTrue(modifiers.any { it is SduiModifier.Shadow })
        assertTrue(modifiers.any { it is SduiModifier.Blur })
        assertTrue(modifiers.any { it is SduiModifier.GraphicsLayer })
        assertTrue(modifiers.any { it is SduiModifier.Clickable })
        assertTrue(modifiers.any { it is SduiModifier.GestureDrag })
        assertTrue(modifiers.any { it is SduiModifier.AnimateContentSize })

        // Check modifier alias
        val aliasMods = modifier {
            padding(10f)
        }
        assertEquals(1, aliasMods.size)
    }

    // --- 4. Action Builders Tests ---

    @Test
    fun testAllActionBuilders() {
        val actionList = actions {
            setState("count", "count + 1")
            setState("total", 100)
            setState("isValid", true)
            setStateLiteral("status", "ACTIVE")
            toggleState("expanded")
            animateTo("scale", "1.5", animation = spring())
            animateTo("offsetY", 50, animation = tween(durationMillis = 400))
            navigate("/home", isExternal = false)
            apiRequest(
                url = "https://api.example.com/items",
                method = "POST",
                headers = mapOf("Authorization" to "Bearer token"),
                body = "{\"item\": 1}",
                responseStateKey = "response",
                onResponse = {
                    setState("status", "'SUCCESS'")
                },
                onError = {
                    setState("status", "'ERROR'")
                }
            )
            emitNativeEvent("analytics", "event" to "button_click")
            conditional(
                conditionExpr = "count > 10",
                thenActions = {
                    navigate("/celebrate")
                },
                elseActions = {
                    setState("count", "count + 1")
                }
            )
            chain {
                setState("a", 1)
                setState("b", 2)
            }
        }

        assertEquals(12, actionList.size)
        assertTrue(actionList[0] is SduiAction.SetState)
        assertEquals("count + 1", (actionList[0] as SduiAction.SetState).valueExpr)
        assertEquals("100", (actionList[1] as SduiAction.SetState).valueExpr)
        assertEquals("true", (actionList[2] as SduiAction.SetState).valueExpr)
        assertEquals("'ACTIVE'", (actionList[3] as SduiAction.SetState).valueExpr)
        assertTrue(actionList[4] is SduiAction.ToggleState)
        assertTrue(actionList[5] is SduiAction.AnimateTo)
        assertTrue(actionList[6] is SduiAction.AnimateTo)
        assertTrue(actionList[7] is SduiAction.Navigate)
        assertTrue(actionList[8] is SduiAction.ApiRequest)
        assertTrue(actionList[9] is SduiAction.EmitNativeEvent)
        assertTrue(actionList[10] is SduiAction.Conditional)
        assertTrue(actionList[11] is SduiAction.Chain)

        // Check onClick alias
        val clickActions = onClick {
            toggleState("flag")
        }
        assertEquals(1, clickActions.size)
    }

    // --- 5. Canvas Builders Tests ---

    @Test
    fun testCanvasBuilders() {
        val doc = sduiDocument("canvas_test") {
            sduiCanvas(
                modifier = { size(300f, 300f) }
            ) {
                rect(color = "#FF000000", x = 10f, y = 10f, width = 100f, height = 50f, cornerRadius = 8f)
                circle(color = "#FFFF0000", centerX = 150f, centerY = 150f, radius = 40f)
                line(color = "#FF0000FF", startX = 0f, startY = 0f, endX = 300f, endY = 300f, strokeWidth = 4f)
                arc(color = "#FF00FF00", x = 50f, y = 50f, width = 80f, height = 80f, startAngle = 0f, sweepAngle = 180f)
                rotate(degrees = 45f, pivotX = 100f, pivotY = 100f) {
                    rect(color = "#FFFFFF00", x = 0f, y = 0f, width = 50f, height = 50f)
                }
                translate(x = 20f, y = 20f) {
                    circle(centerX = 0f, centerY = 0f, radius = 10f)
                }
            }
        }

        val canvas = doc.root as SduiNode.Canvas
        assertEquals(6, canvas.operations.size)
        assertTrue(canvas.operations[0] is SduiCanvasOp.Rect)
        assertTrue(canvas.operations[1] is SduiCanvasOp.Circle)
        assertTrue(canvas.operations[2] is SduiCanvasOp.Line)
        assertTrue(canvas.operations[3] is SduiCanvasOp.Arc)
        assertTrue(canvas.operations[4] is SduiCanvasOp.Rotate)
        assertTrue(canvas.operations[5] is SduiCanvasOp.Translate)

        val json = doc.toJson()
        val deserialized = defaultSduiJson.decodeFromString<SduiDocument>(json)
        val deserializedCanvas = deserialized.root as SduiNode.Canvas
        assertEquals(6, deserializedCanvas.operations.size)
    }

    // --- 6. Animation Builders Tests ---

    @Test
    fun testAnimationBuilders() {
        val sp = spring(dampingRatio = "highBouncy", stiffness = "high")
        assertEquals("highBouncy", sp.dampingRatio)
        assertEquals("high", sp.stiffness)

        val tw = tween(durationMillis = 500, delayMillis = 50, easing = "linear")
        assertEquals(500, tw.durationMillis)
        assertEquals(50, tw.delayMillis)
        assertEquals("linear", tw.easing)

        val kf = keyframes(durationMillis = 800) {
            0 at 0f
            400 at 1.5f
            800 at 1.0f
        }
        assertEquals(800, kf.durationMillis)
        assertEquals(3, kf.keyframeValues.size)
        assertEquals(1.5f, kf.keyframeValues[400])

        val rep = repeatable(iterations = 3, repeatMode = "reverse", animation = tw)
        assertEquals(3, rep.iterations)
        assertEquals("reverse", rep.repeatMode)

        val sn = snap(delayMillis = 100)
        assertEquals(100, sn.delayMillis)

        val enter = enterTransition(fadeIn = true, slideInVertically = true, slideVerticalDirection = "up")
        assertTrue(enter.fadeIn)
        assertTrue(enter.slideInVertically)
        assertEquals("up", enter.slideVerticalDirection)

        val exit = exitTransition(fadeOut = true, shrinkVertically = true)
        assertTrue(exit.fadeOut)
        assertTrue(exit.shrinkVertically)
    }

    // --- 7. Full Preset Reconstruction & State Store Integration ---

    @Test
    fun testCounterScreenReconstruction() {
        val doc = sduiDocument("spring_counter") {
            metadata("title" to "Spring Physics & Animated Counter")
            initialState(
                "count" to 0,
                "scale" to 1.0,
                "isAwesome" to false
            )

            sduiColumn(
                spacingDp = 16f,
                horizontalAlignment = "centerHorizontally",
                modifier = {
                    padding(all = 20f)
                    fillMaxWidth()
                }
            ) {
                sduiCard(
                    cornerRadius = 20f,
                    elevation = 8f,
                    backgroundColor = "\${count % 2 == 0 ? '#1C1B1F' : '#2D1B36'}",
                    modifier = {
                        padding(all = 8f)
                        fillMaxWidth()
                        graphicsLayer(scaleX = "\${scale}", scaleY = "\${scale}")
                    }
                ) {
                    sduiColumn(
                        spacingDp = 12f,
                        horizontalAlignment = "centerHorizontally",
                        modifier = {
                            padding(all = 24f)
                            fillMaxWidth()
                        }
                    ) {
                        sduiImage(
                            vectorIcon = "star",
                            tintColor = "\${count > 5 ? '#FFD700' : '#888888'}",
                            modifier = { size(64f) }
                        )
                        sduiText(
                            text = "\${count}",
                            fontSize = 48f,
                            fontWeight = "bold",
                            color = "#FFFFFF"
                        )
                    }
                }

                sduiRow(
                    spacingDp = 12f,
                    horizontalArrangement = "center",
                    modifier = { fillMaxWidth() }
                ) {
                    sduiButton(
                        text = "+ Tap Count",
                        variant = "filled",
                        onClick = {
                            setState("count", "count + 1")
                            animateTo("scale", "1.3", animation = spring(dampingRatio = "highBouncy"))
                            conditional(
                                conditionExpr = "count >= 10",
                                thenActions = {
                                    setState("isAwesome", "true")
                                }
                            )
                        }
                    )
                }
            }
        }

        // Verify initial state with SduiStateStore
        val rawState = doc.initialState.mapValues { (_, element) ->
            when {
                element is JsonPrimitive && element.isString -> element.content
                element is JsonPrimitive && element.booleanOrNull != null -> element.boolean
                element is JsonPrimitive -> element.content.toDoubleOrNull() ?: element.content
                else -> element.toString()
            }
        }
        val store = SduiStateStore(rawState)
        assertEquals(0.0, (store.get("count") as Number).toDouble(), 0.001)

        store.set("count", 1)
        assertEquals(1, store.get("count"))
        assertEquals("1", store.evalString("\${count}"))

        // Roundtrip JSON serialization check
        val json = doc.toJson()
        val deserialized = defaultSduiJson.decodeFromString<SduiDocument>(json)
        assertEquals(doc.id, deserialized.id)
        assertTrue(deserialized.root is SduiNode.Column)
    }

    // --- 8. Edge Cases ---

    @Test
    fun testMultipleRootNodesAutoWrapInColumn() {
        val doc = sduiDocument("multi_root") {
            sduiText("First")
            sduiText("Second")
        }
        assertTrue(doc.root is SduiNode.Column)
        val col = doc.root
        assertEquals(2, col.children.size)
    }

    @Test
    fun testExplicitRootSetter() {
        val doc = sduiDocument("explicit_root") {
            root {
                sduiCard {
                    sduiText("Card Root")
                }
            }
        }
        assertTrue(doc.root is SduiNode.Card)
    }

    @Test
    fun testAnimatedVisibilitySingleAndMultipleChildren() {
        val singleChildDoc = sduiAnimatedVisibility(
            visibleExpr = "active"
        ) {
            sduiText("Only One")
        }
        assertTrue(singleChildDoc.child is SduiNode.Text)

        val multiChildDoc = sduiAnimatedVisibility(
            visibleExpr = "active"
        ) {
            sduiText("One")
            sduiText("Two")
        }
        assertTrue(multiChildDoc.child is SduiNode.Column)
    }

    @Test
    fun testInitialStateDiverseTypes() {
        val doc = sduiDocument("state_types") {
            initialState(
                "str" to "hello",
                "int" to 42,
                "double" to 3.14,
                "bool" to false,
                "list" to listOf(1, 2, 3),
                "map" to mapOf("nestedKey" to "nestedValue"),
                "nullVal" to null
            )
            sduiSpacer()
        }

        assertEquals("hello", (doc.initialState["str"] as JsonPrimitive).content)
        assertEquals(42, (doc.initialState["int"] as JsonPrimitive).int)
        assertTrue(doc.initialState.containsKey("nullVal"))
        assertTrue(doc.initialState.containsKey("list"))
        assertTrue(doc.initialState.containsKey("map"))
    }

    @Test
    fun testModifierAndOnClickSyntaxVariations() {
        // Test modifier = modifier { ... } and onClick = onClick { ... } syntax
        val standaloneCol = sduiColumn(
            modifier = modifier {
                fillMaxWidth()
                padding(16f)
            }
        ) {
            sduiButton(
                text = "Click me",
                modifier = modifier { size(100f, 50f) },
                onClick = onClick {
                    setState("counter", "counter + 1")
                }
            )
            sduiCard(
                modifier = modifier { padding(8f) },
                onClick = onClick {
                    toggleState("cardExpanded")
                }
            ) {
                sduiText("Card body", modifier = modifier { padding(4f) })
            }
            sduiBox(modifier = modifier { fillMaxSize() }) {
                sduiImage(url = "https://example.com/logo.png", modifier = modifier { size(32f) })
                sduiSpacer(modifier = modifier { size(8f) })
                sduiTextField(valueStateKey = "input", modifier = modifier { fillMaxWidth() })
                sduiCanvas(modifier = modifier { size(100f) }) {
                    circle(centerX = 50f, centerY = 50f, radius = 25f)
                }
            }
        }

        assertEquals(3, standaloneCol.children.size)
        assertEquals(2, standaloneCol.modifiers.size)
        val button = standaloneCol.children[0] as SduiNode.Button
        assertEquals(1, button.onClick.size)
        assertEquals(1, button.modifiers.size)
        val card = standaloneCol.children[1] as SduiNode.Card
        assertEquals(1, card.onClick.size)
        assertEquals(1, card.modifiers.size)

        // Verify JSON roundtrip
        val json = standaloneCol.toJson()
        val deserialized = defaultSduiJson.decodeFromString<SduiNode>(json) as SduiNode.Column
        assertEquals(3, deserialized.children.size)
    }

    @Test
    fun testPrimitiveArraysInInitialState() {
        val doc = sduiDocument("arrays_doc") {
            initialState(
                "ints" to intArrayOf(1, 2, 3),
                "floats" to floatArrayOf(1.5f, 2.5f),
                "doubles" to doubleArrayOf(3.14, 2.718),
                "booleans" to booleanArrayOf(true, false, true),
                "longs" to longArrayOf(1000L, 2000L),
                "bytes" to byteArrayOf(10, 20),
                "shorts" to shortArrayOf(100, 200),
                "chars" to charArrayOf('a', 'b', 'c')
            )
            sduiSpacer()
        }

        val ints = doc.initialState["ints"] as JsonArray
        assertEquals(3, ints.size)
        assertEquals(1, ints[0].jsonPrimitive.int)
        assertEquals(2, ints[1].jsonPrimitive.int)
        assertEquals(3, ints[2].jsonPrimitive.int)

        val booleans = doc.initialState["booleans"] as JsonArray
        assertEquals(3, booleans.size)
        assertEquals(true, booleans[0].jsonPrimitive.boolean)
        assertEquals(false, booleans[1].jsonPrimitive.boolean)

        val chars = doc.initialState["chars"] as JsonArray
        assertEquals(3, chars.size)
        assertEquals("a", (chars[0] as JsonPrimitive).content)

        // Ensure serialization contains valid arrays without object reference markers like [I@
        val json = doc.toJson()
        assertFalse(json.contains("[I@"))
        assertFalse(json.contains("[F@"))
        assertFalse(json.contains("[Z@"))
        assertFalse(json.contains("[C@"))

        val deserialized = defaultSduiJson.decodeFromString<SduiDocument>(json)
        val deserializedInts = deserialized.initialState["ints"] as JsonArray
        assertEquals(3, deserializedInts.size)
    }

    @Test
    fun testModifierMethodChainingAndIntPadding() {
        val mods = modifier {
            clickable {
                toggleState("isOpen")
            }.padding(horizontal = 16, vertical = 8)
                .size(100, 200)
                .clipRounded(12f)
                .padding(start = 2, top = 4, end = 6, bottom = 8)
        }

        assertEquals(5, mods.size)
        assertTrue(mods[0] is SduiModifier.Clickable)
        assertTrue(mods[1] is SduiModifier.Padding)
        assertEquals("16.0", (mods[1] as SduiModifier.Padding).horizontal)
        assertEquals("8.0", (mods[1] as SduiModifier.Padding).vertical)
        assertTrue(mods[2] is SduiModifier.Size)
        assertEquals("100.0", (mods[2] as SduiModifier.Size).width)
        assertEquals("200.0", (mods[2] as SduiModifier.Size).height)
        assertTrue(mods[3] is SduiModifier.Clip)
        assertTrue(mods[4] is SduiModifier.Padding)
        assertEquals("2.0", (mods[4] as SduiModifier.Padding).start)
    }

    @Test
    fun testDeeplyNestedCanvasMatrices() {
        val doc = sduiDocument("nested_canvas") {
            sduiCanvas(modifier = { size(400f, 400f) }) {
                rotate(degrees = 45f, pivotX = 200f, pivotY = 200f) {
                    translate(x = 50f, y = 50f) {
                        rotate(degrees = 30f) {
                            circle(color = "#FF0000FF", centerX = 0f, centerY = 0f, radius = 20f)
                            rect(color = "#FFFF0000", x = -10f, y = -10f, width = 20f, height = 20f)
                        }
                    }
                }
            }
        }

        val canvas = doc.root as SduiNode.Canvas
        assertEquals(1, canvas.operations.size)
        val outerRotate = canvas.operations[0] as SduiCanvasOp.Rotate
        assertEquals("45.0", outerRotate.degrees)
        assertEquals(1, outerRotate.ops.size)

        val translate = outerRotate.ops[0] as SduiCanvasOp.Translate
        assertEquals("50.0", translate.x)
        assertEquals(1, translate.ops.size)

        val innerRotate = translate.ops[0] as SduiCanvasOp.Rotate
        assertEquals("30.0", innerRotate.degrees)
        assertEquals(2, innerRotate.ops.size)

        // Roundtrip serialization
        val json = doc.toJson()
        val deserialized = defaultSduiJson.decodeFromString<SduiDocument>(json)
        val deserializedCanvas = deserialized.root as SduiNode.Canvas
        val dOuterRotate = deserializedCanvas.operations[0] as SduiCanvasOp.Rotate
        val dTranslate = dOuterRotate.ops[0] as SduiCanvasOp.Translate
        val dInnerRotate = dTranslate.ops[0] as SduiCanvasOp.Rotate
        assertEquals(2, dInnerRotate.ops.size)
    }

    @Test
    fun testDeeplyNestedApiRequestWithConditionalsAndChains() {
        val actionList = actions {
            apiRequest(
                url = "https://api.example.com/login",
                method = "POST",
                onResponse = {
                    conditional(
                        conditionExpr = "response.status == 200",
                        thenActions = {
                            chain {
                                setState("token", "response.body.token")
                                animateTo("bannerAlpha", 1.0f)
                                navigate("/dashboard")
                            }
                        },
                        elseActions = {
                            setState("error", "response.body.message")
                        }
                    )
                },
                onError = {
                    setState("error", "'Network connection error'")
                }
            )
        }

        assertEquals(1, actionList.size)
        val apiReq = actionList[0] as SduiAction.ApiRequest
        assertEquals(1, apiReq.onResponse.size)
        assertEquals(1, apiReq.onError.size)

        val cond = apiReq.onResponse[0] as SduiAction.Conditional
        assertEquals(1, cond.thenActions.size)
        assertEquals(1, cond.elseActions.size)

        val chain = cond.thenActions[0] as SduiAction.Chain
        assertEquals(3, chain.actions.size)

        // Roundtrip serialization
        val json = actionList.toJson()
        val deserialized = defaultSduiJson.decodeFromString<List<SduiAction>>(json)
        assertEquals(1, deserialized.size)
        val dApiReq = deserialized[0] as SduiAction.ApiRequest
        val dCond = dApiReq.onResponse[0] as SduiAction.Conditional
        val dChain = dCond.thenActions[0] as SduiAction.Chain
        assertEquals(3, dChain.actions.size)
    }

    @Test
    fun testPartialAstSerialization() {
        val node: SduiNode = sduiText("Partial Node", color = "#FFFFFFFF")
        val nodeJson = node.toJson()
        assertTrue(nodeJson.contains("\"type\": \"text\""))
        val nodeElement = node.toJsonElement()
        assertTrue(nodeElement is JsonObject)

        val nodesList = listOf(node, sduiSpacer(width = 10f))
        val nodesJson = nodesList.toJson()
        val nodesElement = nodesList.toJsonElement()
        assertTrue(nodesJson.contains("\"type\": \"spacer\""))
        assertTrue(nodesElement is JsonArray)

        val mod: SduiModifier = SduiModifier.Padding(all = "16.0")
        val modJson = mod.toJson()
        val modElement = mod.toJsonElement()
        assertTrue(modJson.contains("\"type\": \"padding\""))
        assertTrue(modElement is JsonObject)

        val modList = modifier { padding(8f); fillMaxWidth() }
        val modListJson = modList.toJson()
        val modListElement = modList.toJsonElement()
        assertTrue(modListJson.contains("\"fillMaxWidth\""))
        assertTrue(modListElement is JsonArray)

        val action: SduiAction = SduiAction.ToggleState(key = "flag")
        val actionJson = action.toJson()
        val actionElement = action.toJsonElement()
        assertTrue(actionJson.contains("\"type\": \"toggleState\""))
        assertTrue(actionElement is JsonObject)

        val actionList = actions { toggleState("a"); toggleState("b") }
        val actionListJson = actionList.toJson()
        val actionListElement = actionList.toJsonElement()
        assertTrue(actionListJson.contains("\"toggleState\""))
        assertTrue(actionListElement is JsonArray)

        val canvasOp: SduiCanvasOp = SduiCanvasOp.Circle(radius = "50")
        val canvasOpJson = canvasOp.toJson()
        val canvasOpElement = canvasOp.toJsonElement()
        assertTrue(canvasOpJson.contains("\"type\": \"circle\""))
        assertTrue(canvasOpElement is JsonObject)

        val canvasOps = canvasOperations { circle(radius = 30f); rect(width = 100f) }
        val canvasOpsJson = canvasOps.toJson()
        val canvasOpsElement = canvasOps.toJsonElement()
        assertTrue(canvasOpsJson.contains("\"rect\""))
        assertTrue(canvasOpsElement is JsonArray)

        val animSpec: SduiAnimationSpec = spring(dampingRatio = 0.8f, stiffness = 1200f)
        val animJson = animSpec.toJson()
        val animElement = animSpec.toJsonElement()
        assertTrue(animJson.contains("\"type\": \"spring\""))
        assertTrue(animElement is JsonObject)

        val enter = enterTransition(fadeIn = true, slideInVertically = true)
        val exit = exitTransition(fadeOut = true, shrinkVertically = true)
        assertTrue(enter.toJson().contains("\"fadeIn\": true"))
        assertTrue(exit.toJson().contains("\"fadeOut\": true"))
    }

    @Test
    fun testSnakeCaseDeserializationFidelity() {
        val jsonPayload = """
            {
                "id": "snake_case_doc",
                "version": 1,
                "metadata": {},
                "initialState": {},
                "root": {
                    "type": "lazy_column",
                    "modifiers": [
                        { "type": "graphics_layer", "scaleX": "1.5" },
                        { "type": "gesture_drag", "orientation": "vertical" },
                        { "type": "animate_content_size" }
                    ],
                    "children": [
                        {
                            "type": "text_field",
                            "valueStateKey": "query",
                            "label": "Search"
                        },
                        {
                            "type": "animated_visibility",
                            "visibleExpr": "query != ''",
                            "child": {
                                "type": "button",
                                "text": "Run",
                                "onClick": [
                                    { "type": "set_state", "key": "query", "valueExpr": "''" },
                                    { "type": "toggle_state", "key": "isRunning" },
                                    { "type": "animate_to", "key": "progress", "targetValueExpr": "100" },
                                    { "type": "api_request", "url": "https://api.example.com/run" },
                                    { "type": "emit_native_event", "eventName": "started" }
                                ]
                            }
                        }
                    ]
                }
            }
        """.trimIndent()

        val doc = defaultSduiJson.decodeFromString<SduiDocument>(jsonPayload)
        assertEquals("snake_case_doc", doc.id)
        val col = doc.root
        assertTrue(col is SduiNode.LazyColumn)
        assertEquals(3, col.modifiers.size)
        assertTrue(col.modifiers[0] is SduiModifier.GraphicsLayer)
        assertTrue(col.modifiers[1] is SduiModifier.GestureDrag)
        assertTrue(col.modifiers[2] is SduiModifier.AnimateContentSize)

        assertEquals(2, col.children.size)
        assertTrue(col.children[0] is SduiNode.TextField)
        assertTrue(col.children[1] is SduiNode.AnimatedVisibility)

        val animVis = col.children[1] as SduiNode.AnimatedVisibility
        val btn = animVis.child
        assertTrue(btn is SduiNode.Button)
        assertEquals(5, btn.onClick.size)
        assertTrue(btn.onClick[0] is SduiAction.SetState)
        assertTrue(btn.onClick[1] is SduiAction.ToggleState)
        assertTrue(btn.onClick[2] is SduiAction.AnimateTo)
        assertTrue(btn.onClick[3] is SduiAction.ApiRequest)
        assertTrue(btn.onClick[4] is SduiAction.EmitNativeEvent)
    }
}
