package pro.jayeshseth.bugrush.trivia

enum class VersionPerk(
    val title: String,
    val subtitle: String,
    val badge: String
) {
    GARBAGE_COLLECTOR_DRONE(
        title = "Garbage Collector Drone",
        subtitle = "Orbiting companion auto-vacuums gems & shoots micro-bytecode (600ms)",
        badge = "🤖 GC DRONE"
    ),
    COROUTINES_TURRET(
        title = "Coroutines Turret",
        subtitle = "Deploys an automated rapid blaster turret on dash (lasts 8s)",
        badge = "🗼 TURRET"
    ),
    JIT_COMPILER(
        title = "JIT Compiler",
        subtitle = "+35% weapon fire rate and movement afterburners",
        badge = "⚡ JIT"
    ),
    REFACTOR_FIREWALL(
        title = "Refactor Firewall",
        subtitle = "Absorbs 1 incoming hit; auto-recharges every 20 seconds",
        badge = "🛡️ FIREWALL"
    )
}
