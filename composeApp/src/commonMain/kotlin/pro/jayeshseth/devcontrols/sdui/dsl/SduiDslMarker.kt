package pro.jayeshseth.devcontrols.sdui.dsl

/**
 * DSL marker annotation for type-safe Server-Driven UI builder scopes.
 * Prevents unintended receiver scope leaks across nested builder lambdas.
 */
@DslMarker
annotation class SduiDsl
