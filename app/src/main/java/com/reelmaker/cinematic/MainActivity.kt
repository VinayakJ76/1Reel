package com.reelmaker.cinematic

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.reelmaker.cinematic.ui.theme.CinematicReelTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CinematicReelTheme {
                ReelMakerScreen(
                    appContext = this,
                    createVideoUri = { createVideoUri() }
                )
            }
        }
    }

    private fun createVideoUri(): Uri? {
        val name = "reel_capture_${System.currentTimeMillis()}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CinematicReelMaker")
            }
        }

        return contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
    }
}

enum class AssetType { VIDEO, IMAGE, AUDIO }

data class ReelAsset(
    val uri: Uri,
    val type: AssetType
)

data class CreativePreset(
    val title: String,
    val detail: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReelMakerScreen(
    appContext: Context,
    createVideoUri: () -> Uri?
) {
    val scope = rememberCoroutineScope()
    val snackState = remember { SnackbarHostState() }

    val reelAssets = remember { mutableStateListOf<ReelAsset>() }
    val presets = remember {
        listOf(
            CreativePreset("Beat Sync Cuts", "Auto-align cuts to your soundtrack tempo."),
            CreativePreset("Cinematic LUT Mood", "Apply orange-teal and night bloom color styles."),
            CreativePreset("AI Story Flow", "Suggest shot order: hook, build-up, payoff, CTA."),
            CreativePreset("Velocity Ramps", "Add smooth speed ramps for action moments."),
            CreativePreset("Caption Pulse", "Animated captions that react to your voice peaks.")
        )
    }

    var selectedPreset by rememberSaveable { mutableStateOf(presets.first().title) }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                scope.launch {
                    snackState.showSnackbar("Camera permission is needed to shoot reel clips.")
                }
            }
        }
    )

    val captureVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo(),
        onResult = { success ->
            if (success && pendingCaptureUri != null) {
                reelAssets.add(ReelAsset(pendingCaptureUri!!, AssetType.VIDEO))
            }
            scope.launch {
                snackState.showSnackbar(
                    if (success) "Clip captured and added to your reel timeline."
                    else "Video capture cancelled."
                )
            }
            pendingCaptureUri = null
        }
    )

    val pickGalleryMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10),
        onResult = { uris ->
            uris.forEach { uri ->
                val type = if ((uri.toString()).contains("image")) AssetType.IMAGE else AssetType.VIDEO
                reelAssets.add(ReelAsset(uri, type))
            }
            scope.launch {
                snackState.showSnackbar("Added ${uris.size} media item(s) from gallery.")
            }
        }
    )

    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            uris.forEach { uri -> reelAssets.add(ReelAsset(uri, AssetType.AUDIO)) }
            scope.launch {
                snackState.showSnackbar("Added ${uris.size} audio track(s) from storage.")
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Cinematic Reel Maker") })
        },
        snackbarHost = { SnackbarHost(snackState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeroCard()
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val permission = Manifest.permission.CAMERA
                            if (ContextCompat.checkSelfPermission(
                                    appContext,
                                    permission
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                pendingCaptureUri = createVideoUri()
                                pendingCaptureUri?.let { captureVideoLauncher.launch(it) }
                            } else {
                                cameraPermissionLauncher.launch(permission)
                            }
                        }
                    ) {
                        Icon(Icons.Default.MovieCreation, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Shoot Clip")
                    }

                    Button(
                        onClick = {
                            pickGalleryMediaLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Media")
                    }
                }
            }

            item {
                Button(onClick = { pickAudioLauncher.launch(arrayOf("audio/*")) }) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Audio")
                }
            }

            item {
                Text(
                    text = "Creative Experiences",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        AssistChip(
                            onClick = { selectedPreset = preset.title },
                            label = { Text(preset.title) },
                            leadingIcon = {
                                if (selectedPreset == preset.title) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = presets.first { it.title == selectedPreset }.detail,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            item {
                Text(
                    text = "Timeline Assets",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (reelAssets.isEmpty()) {
                item {
                    EmptyTimeline()
                }
            } else {
                items(reelAssets) { asset ->
                    AssetTile(asset = asset)
                }
            }
        }
    }
}

@Composable
private fun HeroCard() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF5B2EFF),
                            Color(0xFFC13EFF),
                            Color(0xFF18D9E1)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Shoot. Remix. Go Viral.",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Build cinematic reels from live camera clips, gallery videos, and phone audio in minutes.",
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timelapse, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Smart rhythm edits + dramatic transitions", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun AssetTile(asset: ReelAsset) {
    val icon = when (asset.type) {
        AssetType.VIDEO -> "🎬"
        AssetType.IMAGE -> "🖼️"
        AssetType.AUDIO -> "🎵"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = "$icon ${asset.type.name.lowercase().replaceFirstChar { it.uppercase() }}")
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = asset.uri.toString(), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EmptyTimeline() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("No assets yet", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Capture a clip or import media/audio from your phone to start composing your reel.")
        }
    }
}
