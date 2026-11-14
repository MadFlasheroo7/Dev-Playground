package pro.jayeshseth.devcontrols.sdui

import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pro.jayeshseth.devcontrols.sdui.cache.SduiCacheEntry
import pro.jayeshseth.devcontrols.sdui.cache.SduiSwrCache
import pro.jayeshseth.devcontrols.sdui.eval.SduiExpressionEvaluator
import pro.jayeshseth.devcontrols.sdui.model.SduiAction
import pro.jayeshseth.devcontrols.sdui.model.SduiAnimationSpec
import pro.jayeshseth.devcontrols.sdui.model.SduiDocument
import pro.jayeshseth.devcontrols.sdui.model.SduiModifier
import pro.jayeshseth.devcontrols.sdui.model.SduiNode
import pro.jayeshseth.devcontrols.sdui.model.defaultSduiJson
import pro.jayeshseth.devcontrols.sdui.playground.SduiPresets
import pro.jayeshseth.devcontrols.sdui.state.SduiActionRunner
import pro.jayeshseth.devcontrols.sdui.state.SduiStateStore

class SduiTestSuite {

    // --- 1. Expression Evaluator Tests ---

    @Test
    fun testBasicMathAndPrecedence() {
        val context = emptyMap<String, Any?>()
        assertEquals(14.0, SduiExpressionEvaluator.evaluateDouble("2 + 3 * 4", context), 0.001)
        assertEquals(20.0, SduiExpressionEvaluator.evaluateDouble("(2 + 3) * 4", context), 0.001)
        assertEquals(2.5, SduiExpressionEvaluator.evaluateDouble("5 / 2", context), 0.001)
        assertEquals(1.0, SduiExpressionEvaluator.evaluateDouble("10 % 3", context), 0.001)
    }

    @Test
    fun testComparisonsAndLogic() {
        val context = mapOf("count" to 5, "active" to true, "name" to "Alice")
        assertTrue(SduiExpressionEvaluator.evaluateBoolean("count > 2", context))
        assertFalse(SduiExpressionEvaluator.evaluateBoolean("count <= 0", context))
        assertTrue(SduiExpressionEvaluator.evaluateBoolean("active && count == 5", context))
        assertFalse(SduiExpressionEvaluator.evaluateBoolean("!active || count < 0", context))
        assertTrue(SduiExpressionEvaluator.evaluateBoolean("name == 'Alice'", context))
    }

    @Test
    fun testTernaryExpressions() {
        val context1 = mapOf("score" to 85)
        val res1 = SduiExpressionEvaluator.evaluateString("${'$'}{score >= 80 ? 'Pass' : 'Fail'}", context1)
        assertEquals("Pass", res1)

        val context2 = mapOf("score" to 40)
        val res2 = SduiExpressionEvaluator.evaluateString("${'$'}{score >= 80 ? 'Pass' : 'Fail'}", context2)
        assertEquals("Fail", res2)
    }

    @Test
    fun testStringInterpolation() {
        val context = mapOf("user" to "Bob", "items" to 3, "price" to 15.0)
        val interpolated = SduiExpressionEvaluator.evaluateString("Hello ${'$'}{user}, total is ${'$'}${'$'}{items * price}!", context)
        assertEquals("Hello Bob, total is $45!", interpolated)
    }

    @Test
    fun testNestedPropertyNavigation() {
        val user = mapOf("profile" to mapOf("city" to "Tokyo", "level" to 42))
        val context = mapOf("user" to user)
        assertEquals("Tokyo", SduiExpressionEvaluator.evaluateString("${'$'}{user.profile.city}", context))
        assertEquals(42.0, SduiExpressionEvaluator.evaluateDouble("user.profile.level", context), 0.001)
    }

    @Test
    fun testBuiltInFunctions() {
        val context = mapOf("x" to 12.7, "y" to -4.0)
        assertEquals(12.7, SduiExpressionEvaluator.evaluateDouble("max(x, y)", context), 0.001)
        assertEquals(-4.0, SduiExpressionEvaluator.evaluateDouble("min(x, y)", context), 0.001)
        assertEquals(4.0, SduiExpressionEvaluator.evaluateDouble("abs(y)", context), 0.001)
        assertEquals(13.0, SduiExpressionEvaluator.evaluateDouble("round(x)", context), 0.001)
        assertEquals(10.0, SduiExpressionEvaluator.evaluateDouble("clamp(15, 0, 10)", context), 0.001)
        assertTrue(SduiExpressionEvaluator.evaluateBoolean("contains('hello world', 'world')", context))
    }

    @Test
    fun testPlainStringsAndLiterals() {
        val context = mapOf("count" to 5)
        // Plain multi-word literals must evaluate to their exact text, not empty string or 0
        assertEquals("Submit Order", SduiExpressionEvaluator.evaluateString("Submit Order", context))
        assertEquals("Reset", SduiExpressionEvaluator.evaluateString("Reset", context))
        assertEquals("+ Tap Count", SduiExpressionEvaluator.evaluateString("+ Tap Count", context))
        assertEquals("#1C1B1F", SduiExpressionEvaluator.evaluateString("#1C1B1F", context))

        // Existing context variable evaluated as single identifier
        assertEquals("5", SduiExpressionEvaluator.evaluateString("count", context))
        assertEquals(5.0, SduiExpressionEvaluator.evaluateDouble("count", context), 0.001)

        // Non-existent variable in boolean check defaults correctly
        assertFalse(SduiExpressionEvaluator.evaluateBoolean("nonExistentFlag", context, false))
        assertTrue(SduiExpressionEvaluator.evaluateBoolean("nonExistentFlag", context, true))
    }

    // --- 2. Serialization & Graceful Fallback Tests ---

    @Test
    fun testDeserializeCounterPreset() {
        val doc = defaultSduiJson.decodeFromString<SduiDocument>(SduiPresets.counterPreset)
        assertEquals("spring_counter", doc.id)
        assertTrue(doc.root is SduiNode.Column)
        val column = doc.root as SduiNode.Column
        assertEquals(4, column.children.size)
    }

    @Test
    fun testDeserializeAllPresets() {
        val presets = listOf(
            SduiPresets.counterPreset,
            SduiPresets.gestureDragPreset,
            SduiPresets.ecommercePreset,
            SduiPresets.canvasGraphicsPreset,
            SduiPresets.formValidationPreset
        )
        presets.forEach { json ->
            val doc = defaultSduiJson.decodeFromString<SduiDocument>(json)
            assertNotNull(doc.id)
            assertNotNull(doc.root)
        }
    }

    @Test
    fun testUnknownNodeGracefulFallback() {
        val jsonWithUnknownNode = """
        {
          "id": "unknown_test",
          "root": {
            "type": "futureQuantum3DView",
            "modifiers": []
          }
        }
        """.trimIndent()

        val doc = defaultSduiJson.decodeFromString<SduiDocument>(jsonWithUnknownNode)
        assertTrue(doc.root is SduiNode.Unknown)
        assertEquals("futureQuantum3DView", (doc.root as SduiNode.Unknown).rawType)
    }

    @Test
    fun testUnknownModifierGracefulFallback() {
        val jsonWithUnknownMod = """
        {
          "id": "unknown_mod_test",
          "root": {
            "type": "box",
            "modifiers": [
              { "type": "alienGravitationalField", "gravity": 9.8 }
            ]
          }
        }
        """.trimIndent()

        val doc = defaultSduiJson.decodeFromString<SduiDocument>(jsonWithUnknownMod)
        val box = doc.root as SduiNode.Box
        assertEquals(1, box.modifiers.size)
        assertTrue(box.modifiers[0] is SduiModifier.Unknown)
    }

    @Test
    fun testUnknownActionGracefulFallback() {
        val jsonWithUnknownAction = """
        {
          "id": "unknown_action_test",
          "root": {
            "type": "button",
            "text": "Teleport",
            "onClick": [
              { "type": "quantumTeleport", "destinationCoords": "0,0,0" }
            ]
          }
        }
        """.trimIndent()

        val doc = defaultSduiJson.decodeFromString<SduiDocument>(jsonWithUnknownAction)
        val btn = doc.root as SduiNode.Button
        assertEquals(1, btn.onClick.size)
        assertTrue(btn.onClick[0] is SduiAction.Unknown)
    }

    // --- 3. Reactive State Store & Actions ---

    @Test
    fun testStateStoreOperations() {
        val store = SduiStateStore(mapOf("counter" to 10, "flag" to true))
        assertEquals(10, store.get("counter"))
        store.set("counter", 20)
        assertEquals(20, store.get("counter"))

        store.toggle("flag")
        assertEquals(false, store.get("flag"))

        assertEquals(21.0, store.evalDouble("counter + 1"), 0.001)
        assertEquals("Counter: 20", store.evalString("Counter: ${'$'}{counter}"))
    }

    // --- 4. SWR Cache Engine Tests ---

    @Test
    fun testSwrCacheExpiry() {
        val cache = SduiSwrCache()
        val dummyDoc = SduiDocument(id = "cache_test", root = SduiNode.Spacer())

        cache.put("screen_1", dummyDoc, ttlMillis = 1000L)
        val entry = cache.get("screen_1")
        assertNotNull(entry)
        assertEquals("cache_test", entry!!.document.id)

        // Expiration check with simulated future timestamp
        val future = entry.cachedAtMillis + 5000L
        assertTrue(entry.isExpired(future))
        val immediate = entry.cachedAtMillis + 100L
        assertFalse(entry.isExpired(immediate))
    }

    // --- 5. Animation Spec Engine Tests ---

    @Test
    fun testAnimationSpecs() {
        val springSpec = SduiActionRunner.toComposeAnimationSpec(SduiAnimationSpec.Spring(dampingRatio = "highBouncy", stiffness = "high"))
        assertNotNull(springSpec)

        val intSizeSpec = SduiActionRunner.toComposeIntSizeAnimationSpec(SduiAnimationSpec.Spring())
        assertNotNull(intSizeSpec)

        val tweenSpec = SduiActionRunner.toComposeIntSizeAnimationSpec(SduiAnimationSpec.Tween(durationMillis = 500))
        assertNotNull(tweenSpec)
    }
}
