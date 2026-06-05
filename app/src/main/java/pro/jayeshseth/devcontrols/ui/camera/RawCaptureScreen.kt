package pro.jayeshseth.devcontrols.ui.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.DngCreator
import android.hardware.camera2.TotalCaptureResult
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.ui.unit.dp

/** RAW / DNG capture — Camera2 RAW_SENSOR still + DngCreator → .dng in MediaStore. Device-gated. */
@OptIn(ExperimentalGetImage::class)
@Composable
fun RawCaptureScreen(onBack: () -> Unit) {
    CameraScreenScaffold("RAW / DNG", onBack) { RawContent() }
}

@Composable
private fun RawContent() {
    val context = LocalContext.current
    val controller = remember { RawController(context) }
    DisposableEffect(Unit) { onDispose { controller.close() } }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            AndroidView(factory = { ctx ->
                TextureView(ctx).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) = controller.start(st)
                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture) = true.also { controller.close() }
                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                    }
                }
            }, modifier = Modifier.fillMaxSize())
        }
        Column(Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color(0xFF101018)).padding(12.dp)) {
            Text(if (controller.rawSupported) "RAW supported — captures a .dng + saves to Pictures/DevControls"
                 else "This camera does not advertise RAW capability.",
                style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.White)
            Button(onClick = { controller.capture() }, enabled = controller.rawSupported, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Capture RAW")
            }
        }
    }
}

private class RawController(private val context: Context) {
    private val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val thread = HandlerThread("raw").apply { start() }
    private val handler = Handler(thread.looper)
    private var device: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var reader: ImageReader? = null
    private var camId: String = ""
    private var chars: CameraCharacteristics? = null
    private var lastResult: TotalCaptureResult? = null
    var rawSupported = false; private set

    init {
        runCatching {
            camId = cm.cameraIdList.firstOrNull {
                val c = cm.getCameraCharacteristics(it)
                c.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK &&
                    c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW) == true
            } ?: ""
            if (camId.isNotEmpty()) { chars = cm.getCameraCharacteristics(camId); rawSupported = true }
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun start(st: SurfaceTexture) {
        if (!rawSupported) return
        val ch = chars ?: return
        val map = ch.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return
        val rawSize = map.getOutputSizes(ImageFormat.RAW_SENSOR)?.maxByOrNull { it.width.toLong() * it.height } ?: return
        val previewSize = map.getOutputSizes(SurfaceTexture::class.java)?.firstOrNull { it.width <= 1920 } ?: Size(1280, 720)
        st.setDefaultBufferSize(previewSize.width, previewSize.height)
        reader = ImageReader.newInstance(rawSize.width, rawSize.height, ImageFormat.RAW_SENSOR, 2).apply {
            setOnImageAvailableListener({ r ->
                val image = r.acquireNextImage(); val result = lastResult
                if (image != null && result != null) {
                    runCatching {
                        val cv = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, "RAW_${System.currentTimeMillis()}.dng")
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/x-adobe-dng")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/DevControls")
                        }
                        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
                        if (uri != null) context.contentResolver.openOutputStream(uri)?.use { os ->
                            DngCreator(chars!!, result).use { it.writeImage(os, image) }
                        }
                        ContextCompat.getMainExecutor(context).execute { Toast.makeText(context, "Saved DNG", Toast.LENGTH_SHORT).show() }
                    }
                }
                image?.close()
            }, handler)
        }
        val previewSurface = Surface(st)
        cm.openCamera(camId, object : CameraDevice.StateCallback() {
            override fun onOpened(d: CameraDevice) {
                device = d
                val targets = listOf(previewSurface, reader!!.surface)
                @Suppress("DEPRECATION")
                d.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(s: CameraCaptureSession) {
                        session = s
                        val req = d.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply { addTarget(previewSurface) }
                        runCatching { s.setRepeatingRequest(req.build(), null, handler) }
                    }
                    override fun onConfigureFailed(s: CameraCaptureSession) {}
                }, handler)
            }
            override fun onDisconnected(d: CameraDevice) { d.close(); device = null }
            override fun onError(d: CameraDevice, e: Int) { d.close(); device = null }
        }, handler)
    }

    fun capture() {
        val d = device ?: return; val s = session ?: return; val r = reader ?: return
        val req = d.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
            addTarget(r.surface)
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        }
        runCatching {
            s.capture(req.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(sess: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    lastResult = result
                }
            }, handler)
        }
    }

    fun close() {
        runCatching { session?.close() }; session = null
        runCatching { device?.close() }; device = null
        runCatching { reader?.close() }; reader = null
    }
}
