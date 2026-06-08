package pro.jayeshseth.devcontrols.adb

data class AdbExecResult(
    val command: String,
    val output: String,
    val durationMs: Long,
    val ok: Boolean,
    val error: String? = null,
)
