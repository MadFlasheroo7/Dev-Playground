package pro.jayeshseth.bottomsheet

/**
 * Anchor positions for the bottom sheet.
 */
enum class SheetValue {
    /**
     * Completely hidden off-screen (positioned at container height).
     */
    Hidden,

    /**
     * Collapsed resting state displaying only the peek height at the bottom of the container.
     */
    Collapsed,

    /**
     * Optional intermediate partially-expanded state between collapsed and fully expanded.
     */
    PartiallyExpanded,

    /**
     * Fully expanded sheet state.
     */
    Expanded;

    val isVisible: Boolean
        get() = this != Hidden
}
