package pro.jayeshseth.devcontrols.ui

import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pro.jayeshseth.devcontrols.util.SocialShare

/**
 * Compose a post once (text + optional link + optional image), then fan it out to each
 * social app. See [SocialShare] for the per-platform hand-off and PLAN.md for the
 * platform caveats surfaced as hints below.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialShareScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var text by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var image by remember { mutableStateOf<Uri?>(null) }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        image = uri
    }

    LaunchedEffect(image) {
        preview = image?.let { uri ->
            withContext(Dispatchers.IO) {
                runCatching {
                    val src = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src) { decoder, _, _ -> decoder.setTargetSampleSize(4) }
                        .asImageBitmap()
                }.getOrNull()
            }
        }
    }

    val hasUrl = url.isNotBlank()
    val hasImage = image != null
    val hasAnything = text.isNotBlank() || hasUrl || hasImage

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Social Share") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Post text") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Link URL (LinkedIn / Facebook share this)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text(if (hasImage) "Change image" else "Add image") }
                if (hasImage) {
                    TextButton(onClick = { image = null; preview = null }) { Text("Remove") }
                }
                preview?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "Selected image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Share to", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))

            ShareTarget(
                label = "X / Twitter",
                hint = "Text prefilled" + if (hasImage) " + image (if X installed)" else "",
                enabled = hasAnything
            ) { SocialShare.shareX(context, text, url, image) }

            ShareTarget(
                label = "WhatsApp",
                hint = "Text prefilled" + if (hasImage) " + image" else "",
                enabled = hasAnything
            ) { SocialShare.shareWhatsApp(context, text, url, image) }

            ShareTarget(
                label = "LinkedIn",
                hint = "URL only (no text prefill — LinkedIn policy)",
                enabled = hasUrl
            ) { SocialShare.shareLinkedIn(context, url) }

            ShareTarget(
                label = "Instagram",
                hint = if (hasImage) "Posts the image (no caption prefill)" else "Needs an image",
                enabled = hasImage
            ) { SocialShare.shareInstagram(context, image) }

            ShareTarget(
                label = "Facebook",
                hint = if (hasImage) "Image to FB app, or URL via web" else "URL only (Facebook policy)",
                enabled = hasUrl || hasImage
            ) { SocialShare.shareFacebook(context, url, image) }

            ShareTarget(
                label = "More… (system share sheet)",
                hint = "Telegram, Mastodon, email, and anything else",
                enabled = hasAnything
            ) { SocialShare.systemShare(context, text, url, image) }

            Spacer(Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "There's no Android API to post to every network at once — you compose here once, " +
                        "then tap each network (it foregrounds that app with whatever it allows prefilled). " +
                        "True one-tap auto-posting needs each platform's API/OAuth (see PLAN.md).",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ShareTarget(label: String, hint: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(label)
            Text(
                hint,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
