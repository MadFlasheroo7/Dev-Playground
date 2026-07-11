package pro.jayeshseth.devcontrols.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

object SharedFocusState {
    var activeFocusTaskTitle by mutableStateOf<String?>(null)
}
