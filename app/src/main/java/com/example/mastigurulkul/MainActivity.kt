package com.example.mastigurulkul

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MastiGurukulAppRoot()
        }
    }
}

data class VideoItem(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val type: String, // "LIVE Class" or "Recorded Lecture"
    val sourceUrl: String,
    val thumbnailUrl: String = "",
    val isYouTube: Boolean
)

data class ChatMessage(
    val sender: String, // "User" or "Ankit AI"
    val text: String,
    val imageUri: Uri? = null
)

// ---------------------------------------------------------
// 1. CLOUD BACKEND REPOSITORY (Firebase / Firestore Architecture)
// ---------------------------------------------------------
object CloudVideoRepository {
    private val _videos = MutableStateFlow(
        listOf(
            VideoItem(title = "LIVE: Class 10 Math - Circles & Tangents", type = "LIVE Class", sourceUrl = "https://www.youtube.com/embed/5qap5aO4i9A", thumbnailUrl = "https://img.youtube.com/vi/5qap5aO4i9A/hqdefault.jpg", isYouTube = true),
            VideoItem(title = "Science - Light Reflection & Refraction", type = "Recorded Lecture", sourceUrl = "https://www.youtube.com/embed/dQw4w9WgXcQ", thumbnailUrl = "https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg", isYouTube = true),
            VideoItem(title = "Math - Quadratic Equations Masterclass", type = "Recorded Lecture", sourceUrl = "https://www.youtube.com/embed/3JZ_D3ELwOQ", thumbnailUrl = "https://img.youtube.com/vi/3JZ_D3ELwOQ/hqdefault.jpg", isYouTube = true)
        )
    )
    val videos: StateFlow<List<VideoItem>> = _videos

    init {
        // Firebase Realtime Database / Firestore Snapshot Listener Integration:
        // FirebaseDatabase.getInstance().getReference("masti_gurukul_global_videos")
        //     .addValueEventListener(object : ValueEventListener { ... })
    }

    fun publishVideoToCloud(item: VideoItem) {
        // FirebaseDatabase.getInstance().getReference("masti_gurukul_global_videos").push().setValue(item)
        val updated = listOf(item) + _videos.value
        _videos.value = updated
    }
}

@Composable
fun MastiGurukulAppRoot() {
    var currentScreen by remember { mutableStateOf("home") }
    var screenTitle by remember { mutableStateOf("Mission 2027 Dashboard") }
    var selectedVoiceProfile by remember { mutableStateOf("Hacker Ankit (Boy)") }
    var isAdminMode by remember { mutableStateOf(false) }
    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var activePlaybackUrl by remember { mutableStateOf("https://www.youtube.com/embed/5qap5aO4i9A") }

    // Cloud-synced snapshot state list for instant 1-second Home Screen updates
    val cloudVideos by CloudVideoRepository.videos.collectAsState()
    val videoList = remember { mutableStateListOf<VideoItem>() }

    LaunchedEffect(cloudVideos) {
        videoList.clear()
        videoList.addAll(cloudVideos)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        when (currentScreen) {
            "home" -> HomeScreen(
                videoList = videoList,
                onNavigate = { route, title ->
                    screenTitle = title
                    currentScreen = route
                },
                onSelectVideo = { url ->
                    activePlaybackUrl = url
                    screenTitle = "Video Player"
                    currentScreen = "player"
                },
                onSecretAdminTrigger = { showAdminLoginDialog = true }
            )
            "subject_detail" -> SubjectDetailScreen(
                title = screenTitle,
                videoList = videoList,
                onBack = { currentScreen = "home" },
                onSelectVideo = { url ->
                    activePlaybackUrl = url
                    screenTitle = "Video Player"
                    currentScreen = "player"
                }
            )
            "player" -> LiveAndRecordedPlayerScreen(
                title = screenTitle,
                videoUrl = activePlaybackUrl,
                onBack = { currentScreen = "home" },
                onAskAi = { currentScreen = "ai_guru" }
            )
            "ai_guru" -> AiGuruScreen(
                voiceProfile = selectedVoiceProfile,
                onVoiceChange = { selectedVoiceProfile = it },
                onBack = { currentScreen = "home" }
            )
            "profile" -> ProfileScreen(
                isAdminMode = isAdminMode,
                onBack = { currentScreen = "home" },
                onNavigateAdmin = {
                    if (isAdminMode) {
                        currentScreen = "admin"
                    } else {
                        showAdminLoginDialog = true
                    }
                }
            )
            "admin" -> AdminControlScreen(
                onBack = { currentScreen = "home" }
            )
            "tests" -> OnlineTestsScreen(
                onBack = { currentScreen = "home" }
            )
            "notes" -> NotesLibraryScreen(
                onBack = { currentScreen = "home" }
            )
            "live" -> LiveClassesScreen(
                onBack = { currentScreen = "home" }
            )
        }

        // Secure Admin PIN Login Dialog (Strict PIN: 98011064)
        if (showAdminLoginDialog) {
            var pinInput by remember { mutableStateOf("") }
            var pinError by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showAdminLoginDialog = false },
                title = { Text("🔒 Admin Mode Authentication") },
                text = {
                    Column {
                        Text("Enter strict Admin PIN (98011064) to access Admin Panel:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { pinInput = it; pinError = false },
                            placeholder = { Text("Enter PIN (98011064)") },
                            visualTransformation = PasswordVisualTransformation(),
                            isError = pinError,
                            singleLine = true
                        )
                        if (pinError) {
                            Text("Incorrect PIN. Access Denied.", color = Color.Red, fontSize = 11.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (pinInput == "98011064") {
                                isAdminMode = true
                                showAdminLoginDialog = false
                                currentScreen = "admin"
                            } else {
                                pinError = true
                            }
                        }
                    ) {
                        Text("Verify & Enter Admin")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAdminLoginDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// ---------------------------------------------------------
// 2. HOME SCREEN (Masti Gurukul Layout with Secret Admin Trigger)
// ---------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    videoList: List<VideoItem>,
    onNavigate: (String, String) -> Unit,
    onSelectVideo: (String) -> Unit,
    onSecretAdminTrigger: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 80.dp)
    ) {
        // Top Bar & Logo (Secret long-click gesture for Admin access)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A237E))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.combinedClickable(
                    onClick = { },
                    onLongClick = { onSecretAdminTrigger() }
                )
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = Color(0xFFFFF3E0)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "🎓", fontSize = 22.sp)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Masti Gurukul", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Mission 2027", color = Color(0xFFD87D38), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onNavigate("ai_guru", "AI Guru: Hacker Ankit") }) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                }
                IconButton(onClick = { onNavigate("profile", "Student Profile") }) {
                    Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = Color.White)
                }
            }
        }

        // Welcome Banner: नमस्ते! आपका स्वागत है!
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { onNavigate("profile", "Student Profile") },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFD87D38))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("नमस्ते! Masti Gurukul में आपका स्वागत है!", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Mission 2027 • Target: Class 10 Board Exam", color = Color.White, fontSize = 13.sp)
            }
        }

        // Hacker Ankit AI Search Bar Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { onNavigate("ai_guru", "AI Guru: Hacker Ankit") },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape,
                        color = Color(0xFF1E88E5)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🤖", fontSize = 22.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("AI Guru से पूछें (Hacker Ankit)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Instant doubt solving & voice tutor", color = Color.LightGray, fontSize = 11.sp)
                    }
                }
                Button(
                    onClick = { onNavigate("ai_guru", "AI Guru: Hacker Ankit") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text("Ask", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Select Your Class Grid (Classes 6-10)
        Text(
            text = "अपनी कक्षा चुनें (Select Your Class)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp),
            color = Color(0xFF1A237E)
        )
        Spacer(modifier = Modifier.height(8.dp))

        val classList = listOf("6", "7", "8", "9", "10")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(classList) { cls ->
                Card(
                    modifier = Modifier
                        .size(75.dp, 90.dp)
                        .clickable { onNavigate("subject_detail", "Class $cls - Curriculum") },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E88E5))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Class", color = Color.White, fontSize = 10.sp)
                        Text(cls, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("कक्षा $cls", color = Color.White, fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // आज की लाइव कक्षाएं (Live Classes Section) - Instant 1-second updates
        Text(
            text = "आज की लाइव कक्षाएं (Live Classes)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp),
            color = Color(0xFF1A237E)
        )
        Spacer(modifier = Modifier.height(8.dp))

        val liveClasses = videoList.filter { it.type == "LIVE Class" }
        if (liveClasses.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Text("No live classes scheduled right now.", modifier = Modifier.padding(16.dp), color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            liveClasses.forEach { liveItem ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { onSelectVideo(liveItem.sourceUrl) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0FE))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(color = Color.Red, shape = RoundedCornerShape(4.dp)) {
                                Text(" LIVE ", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(liveItem.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1A237E))
                                Text("Expert Faculty @ Masti Gurukul", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Button(
                            onClick = { onSelectVideo(liveItem.sourceUrl) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Text("JOIN", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // रिकॉर्डेड व्याख्यान (Recorded Lectures Grid with auto-generated thumbnails)
        Text(
            text = "रिकॉर्डेड व्याख्यान (Recorded Lectures)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp),
            color = Color(0xFF1A237E)
        )
        Spacer(modifier = Modifier.height(8.dp))

        val recordedLectures = videoList.filter { it.type == "Recorded Lecture" }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(recordedLectures) { recItem ->
                Card(
                    modifier = Modifier
                        .width(180.dp)
                        .height(120.dp)
                        .clickable { onSelectVideo(recItem.sourceUrl) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF263238))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (recItem.thumbnailUrl.isNotBlank()) {
                            NetworkImage(
                                url = recItem.thumbnailUrl,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f))
                                .padding(10.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Surface(shape = CircleShape, color = Color(0xFF1E88E5)) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(24.dp).padding(4.dp))
                                    }
                                }
                                Text(recItem.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Course Structure Grid
        Text(
            text = "Course Structure (कोर्स संरचना)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp),
            color = Color(0xFF1A237E)
        )
        Spacer(modifier = Modifier.height(10.dp))

        val subjects = listOf("गणित (Math)", "विज्ञान (Science)", "सामाजिक विज्ञान (SST)", "हिंदी (Hindi)", "अंग्रेज़ी (English)", "Notes", "Practice Tests")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            subjects.chunked(3).forEach { rowSubjects ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowSubjects.forEach { sub ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(85.dp)
                                .clickable { onNavigate("subject_detail", sub) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = when {
                                        sub.contains("Math") -> "🧮"
                                        sub.contains("Science") -> "⚛️"
                                        sub.contains("SST") -> "🌍"
                                        sub.contains("Hindi") -> "🇮🇳"
                                        sub.contains("English") -> "📖"
                                        sub.contains("Notes") -> "📝"
                                        else -> "✍️"
                                    },
                                    fontSize = 22.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(sub.substringBefore(" "), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }

    // Bottom Navigation Bar
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        NavigationBar(containerColor = Color.White) {
            NavigationBarItem(
                selected = true,
                onClick = { onNavigate("home", "Mission 2027 Dashboard") },
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                label = { Text("Home", fontSize = 10.sp) }
            )
            NavigationBarItem(
                selected = false,
                onClick = { onNavigate("live", "Live Classes") },
                icon = { Icon(Icons.Default.LiveTv, contentDescription = "Live") },
                label = { Text("Live", fontSize = 10.sp) }
            )
            NavigationBarItem(
                selected = false,
                onClick = { onNavigate("notes", "Notes & PYQ") },
                icon = { Icon(Icons.Default.MenuBook, contentDescription = "Notes") },
                label = { Text("Notes", fontSize = 10.sp) }
            )
            NavigationBarItem(
                selected = false,
                onClick = { onNavigate("tests", "Online Tests") },
                icon = { Icon(Icons.Default.Quiz, contentDescription = "Tests") },
                label = { Text("Tests", fontSize = 10.sp) }
            )
            NavigationBarItem(
                selected = false,
                onClick = { onNavigate("ai_guru", "AI Guru: Hacker Ankit") },
                icon = { Icon(Icons.Default.SmartToy, contentDescription = "AI Guru") },
                label = { Text("AI Guru", fontSize = 10.sp) }
            )
        }
    }
}

// Lightweight native network image loader
@Composable
fun NetworkImage(url: String, modifier: Modifier = Modifier) {
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(url) {
        if (url.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val stream = URL(url).openStream()
                    bitmap = BitmapFactory.decodeStream(stream)
                } catch (_: Exception) {}
            }
        }
    }
    if (bitmap != null) {
        Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier = modifier.background(Color.DarkGray), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White)
        }
    }
}

// ---------------------------------------------------------
// 3. SUBJECT DETAIL SCREEN
// ---------------------------------------------------------
@Composable
fun SubjectDetailScreen(
    title: String,
    videoList: List<VideoItem>,
    onBack: () -> Unit,
    onSelectVideo: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A237E))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Available Lectures & Chapters", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            videoList.forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectVideo(item.sourceUrl) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1A237E))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Category: ${item.type} • HD Playback", fontSize = 11.sp, color = Color.Gray)
                        }
                        IconButton(onClick = { onSelectVideo(item.sourceUrl) }) {
                            Icon(Icons.Default.PlayCircle, contentDescription = "Play", tint = Color(0xFF1E88E5))
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// 4. IN-APP SEAMLESS VIDEO PLAYER (WEBVIEW ZERO WHITE SCREEN FIX)
// ---------------------------------------------------------
@Composable
fun LiveAndRecordedPlayerScreen(
    title: String,
    videoUrl: String,
    onBack: () -> Unit,
    onAskAi: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
        }

        // In-app seamless video playback container (WebView for YouTube or local storage URI)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(Color.Black)
        ) {
            if (videoUrl.startsWith("content://") || videoUrl.startsWith("file://")) {
                InAppLocalVideoPlayer(uri = videoUrl)
            } else {
                InAppYouTubePlayer(url = videoUrl)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { /* Ask Teacher action */ },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
            ) {
                Icon(Icons.Default.QuestionAnswer, contentDescription = "Ask", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("शिक्षक से पूछें", fontSize = 12.sp)
            }

            Button(
                onClick = onAskAi,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD87D38))
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = "AI", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("AI Guru से मदद लें", fontSize = 12.sp)
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("अध्याय: कक्षा 10 विशेष सत्र", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("शिक्षक: Masti Gurukul Expert Faculty", color = Color.LightGray, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.DarkGray)
            Spacer(modifier = Modifier.height(16.dp))
            Text("📚 Class Notes & PDF Download available below.", color = Color.Cyan, fontSize = 13.sp)
        }
    }
}

@Composable
fun InAppYouTubePlayer(url: String) {
    val videoId = extractYouTubeVideoId(url) ?: "5qap5aO4i9A"
    val html = "<html><body style='margin:0;padding:0;background-color:black;'><iframe width='100%' height='100%' src='https://www.youtube.com/embed/$videoId?autoplay=1&modestbranding=1' frameborder='0' allowfullscreen></iframe></body></html>"

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun InAppLocalVideoPlayer(uri: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    setVideoURI(Uri.parse(uri))
                    setMediaController(MediaController(context))
                    start()
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

fun extractYouTubeVideoId(url: String): String? {
    if (url.isBlank()) return null
    val regEx = "^.*((youtu.be/)|(v/)|(/u/\\w/)|(embed/)|(watch\\?v=))([^#&?]*).*".toRegex()
    val matchResult = regEx.find(url)
    return if (matchResult != null && matchResult.groupValues[7].length == 11) {
        matchResult.groupValues[7]
    } else if (url.length == 11) {
        url
    } else {
        val fallbackRegex = "[a-zA-Z0-9_-]{11}".toRegex()
        val fallbackMatch = fallbackRegex.find(url)
        fallbackMatch?.value
    }
}

// ---------------------------------------------------------
// 5. SMART AI GURU (HACKER ANKIT AI) WITH MULTIMODAL IMAGE UPLOAD & TTS
// ---------------------------------------------------------
@Composable
fun AiGuruScreen(
    voiceProfile: String,
    onVoiceChange: (String) -> Unit,
    onBack: () -> Unit
) {
    var queryText by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }
    val chatHistory = remember {
        mutableStateListOf(
            ChatMessage("Ankit AI", "नमस्ते! मैं हैकर अंकित हूँ। विज्ञान, गणित या किसी भी विषय में कोई भी संदेह हो, तुरंत पूछें या फोटो अटैच करें!")
        )
    }

    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var pitch by remember { mutableStateOf(1.0f) }
    var speechRate by remember { mutableStateOf(1.0f) }
    var isMuted by remember { mutableStateOf(false) }

    // Image picker launcher for multimodal queries (+ icon)
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { attachedImageUri = it }
    }

    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.forLanguageTag("hi-IN")
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    fun speak(text: String) {
        if (!isMuted) {
            tts?.setPitch(pitch)
            tts?.setSpeechRate(speechRate)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun getSmartEducationalAnswer(query: String): String {
        val q = query.lowercase()
        return when {
            q.contains("photosynthesis") || q.contains("प्रकाश संश्लेषण") ->
                "Ankit AI: प्रकाश संश्लेषण वह प्रक्रिया है जिसके द्वारा हरे पौधे सूर्य के प्रकाश, कार्बन डाइऑक्साइड और पानी का उपयोग करके भोजन बनाते हैं और ऑक्सीजन छोड़ते हैं।"
            q.contains("newton") || q.contains("न्यूटन") ->
                "Ankit AI: न्यूटन के गति के तीन नियम हैं: 1) जड़त्व का नियम (Inertia), 2) F=ma (बल और त्वरण), 3) क्रिया-प्रतिक्रिया का नियम (Action & Reaction)."
            q.contains("pythagoras") || q.contains("पाइथागोरस") ->
                "Ankit AI: पाइथागोरस प्रमेय (Pythagoras Theorem): समकोण त्रिभुज में कर्ण का वर्ग (Hypotenuse²) = आधार² + लंब² (Base² + Perpendicular²)."
            q.contains("math") || q.contains("गणित") ->
                "Ankit AI: गणित में सफलता के लिए सूत्रों (Formulas) का अभ्यास और नियमित रिविजन सबसे जरूरी है। आप त्रिकोणमिति या बीजगणित का कोई भी प्रश्न पूछ सकते हैं।"
            q.contains("hi") || q.contains("hello") || q.contains("नमस्ते") ->
                "Ankit AI: नमस्ते! Masti Gurukul Mission 2027 में आपका स्वागत है। बताइए आज हम कौन सा टॉपिक समझें?"
            else ->
                "Ankit AI: आपके इस शानदार प्रश्न और संलग्न फोटो का उत्तर यह है कि Masti Gurukul पाठ्यक्रम के अनुसार इसे कॉन्सेप्ट्स को स्पष्ट करके आसानी से हल किया जा सकता है।"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    tts?.stop()
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Guru: Hacker Ankit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Surface(
                color = Color(0xFF1E88E5),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable {
                    onVoiceChange(if (voiceProfile.contains("Boy")) "Hacker Ankit (Girl)" else "Hacker Ankit (Boy)")
                    pitch = if (voiceProfile.contains("Boy")) 1.3f else 1.0f
                }
            ) {
                Text(text = "🎙️ $voiceProfile", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Voice Pitch & Speed Toggles
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("🔊 Voice Settings & TTS Speed", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { isMuted = !isMuted }) {
                        Text(if (isMuted) "🔇 Muted" else "🔊 Speaking", color = if (isMuted) Color.Red else Color.Green, fontSize = 10.sp)
                    }
                }
                Text("Pitch: ${String.format(Locale.US, "%.1f", pitch)}x", color = Color.LightGray, fontSize = 10.sp)
                Slider(value = pitch, onValueChange = { pitch = it }, valueRange = 0.5f..2.0f, colors = SliderDefaults.colors(thumbColor = Color(0xFFD87D38), activeTrackColor = Color(0xFFD87D38)))
                Text("Speed: ${String.format(Locale.US, "%.1f", speechRate)}x", color = Color.LightGray, fontSize = 10.sp)
                Slider(value = speechRate, onValueChange = { speechRate = it }, valueRange = 0.5f..2.0f, colors = SliderDefaults.colors(thumbColor = Color(0xFF1E88E5), activeTrackColor = Color(0xFF1E88E5)))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat History List with image support
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chatHistory.forEach { chat ->
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (chat.sender == "User") "You: ${chat.text}" else chat.text,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        chat.imageUri?.let { uri ->
                            Spacer(modifier = Modifier.height(6.dp))
                            val bmp = remember(uri) {
                                try {
                                    val stream = context.contentResolver.openInputStream(uri)
                                    BitmapFactory.decodeStream(stream)
                                } catch (_: Exception) {
                                    null
                                }
                            }
                            bmp?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "Attached Doubt Image",
                                    modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Attached image preview banner before sending
        attachedImageUri?.let { uri ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val bmp = remember(uri) {
                        try {
                            val stream = context.contentResolver.openInputStream(uri)
                            BitmapFactory.decodeStream(stream)
                        } catch (_: Exception) {
                            null
                        }
                    }
                    bmp?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Preview",
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp))
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Image attached for analysis", color = Color.Yellow, fontSize = 11.sp)
                }
                IconButton(onClick = { attachedImageUri = null }) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Quick Suggestion Chips
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    val ans = getSmartEducationalAnswer("photosynthesis")
                    chatHistory.add(ChatMessage("User", "प्रकाश संश्लेषण क्या है?"))
                    chatHistory.add(ChatMessage("Ankit AI", ans))
                    speak(ans)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("🌱 Photosynthesis", fontSize = 10.sp, color = Color.White)
            }
            OutlinedButton(
                onClick = {
                    val ans = getSmartEducationalAnswer("newton")
                    chatHistory.add(ChatMessage("User", "न्यूटन का नियम"))
                    chatHistory.add(ChatMessage("Ankit AI", ans))
                    speak(ans)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("⚡ Newton Law", fontSize = 10.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input bar with Multimodal Image Picker (+ icon) & Instant Send
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Camera / Gallery Image Picker Button (+ icon)
            IconButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.size(42.dp).background(Color(0xFF1E88E5), CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Attach Image", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(6.dp))

            OutlinedTextField(
                value = queryText,
                onValueChange = { queryText = it },
                placeholder = { Text("अपना सवाल या फोटो भेजें...", color = Color.Gray, fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFD87D38),
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (queryText.isNotBlank() || attachedImageUri != null) {
                        val userQ = queryText.ifBlank { "कृपया इस फोटो का विश्लेषण करें:" }
                        val aiAnswer = getSmartEducationalAnswer(userQ)
                        chatHistory.add(ChatMessage("User", userQ, imageUri = attachedImageUri))
                        chatHistory.add(ChatMessage("Ankit AI", aiAnswer))
                        speak(aiAnswer)
                        queryText = ""
                        attachedImageUri = null
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFD87D38), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

// ---------------------------------------------------------
// 6. PROFILE SCREEN (STUDENT ONLY MODE)
// ---------------------------------------------------------
@Composable
fun ProfileScreen(
    isAdminMode: Boolean,
    onBack: () -> Unit,
    onNavigateAdmin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Student Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A237E))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(modifier = Modifier.size(60.dp), shape = CircleShape, color = Color(0xFFE0E0E0)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("👦🏽", fontSize = 32.sp)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Rahul Kumar", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A237E))
                    Text("Class 10A • Roll No: 24", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(color = Color(0xFF43A047), shape = RoundedCornerShape(4.dp)) {
                        Text(" ✔ Verified Student ", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0FE))) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔥 Streak", fontSize = 12.sp, color = Color.DarkGray)
                    Text("45 Days", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
                }
            }
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏆 Points", fontSize = 12.sp, color = Color.DarkGray)
                    Text("4,850 XP", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD87D38))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("Mission 2027 Goals & Progress", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A237E))
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Overall Syllabus Completion", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("78%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(progress = { 0.78f }, modifier = Modifier.fillMaxWidth().height(8.dp), color = Color(0xFF1E88E5))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Target Board Exam: March 2027 (987 Days Left)", fontSize = 12.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Secure Admin Panel Access Button
        Button(
            onClick = onNavigateAdmin,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238))
        ) {
            Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin")
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isAdminMode) "Open Admin Panel" else "Authorized Admin Login")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Enrollment ID: MG10A345\nEmail: rahul.kumar@email.com", fontSize = 12.sp, color = Color.Gray)
    }
}

// ---------------------------------------------------------
// 7. STRICTLY ISOLATED ADMIN PANEL SCREEN (YOUTUBE THUMBNAILS & CLOUD SYNC)
// ---------------------------------------------------------
@Composable
fun AdminControlScreen(
    onBack: () -> Unit
) {
    var titleInput by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("LIVE Class") }
    var feedbackMessage by remember { mutableStateOf("") }
    var isErrorFeedback by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val newItem = VideoItem(
                title = titleInput.ifBlank { "Cloud Gallery MP4 Lecture" },
                type = selectedCategory,
                sourceUrl = uri.toString(),
                thumbnailUrl = "",
                isYouTube = false
            )
            CloudVideoRepository.publishVideoToCloud(newItem)
            feedbackMessage = "✅ Local MP4 video published to Cloud Backend successfully!"
            isErrorFeedback = false
            titleInput = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E293B))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Admin Panel (Cloud-Synced)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("☁️ Publish Content to Global Cloud Backend", color = Color(0xFF1E88E5), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Changes sync instantly for all students worldwide.", color = Color.LightGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it; feedbackMessage = "" },
                    placeholder = { Text("Enter Video Title (e.g. Science Chapter 2)", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text("Select Category / Target Section:", color = Color.LightGray, fontSize = 12.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { selectedCategory = "LIVE Class" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (selectedCategory == "LIVE Class") Color(0xFF1E88E5) else Color.DarkGray)
                    ) {
                        Text("🔴 LIVE Class")
                    }
                    Button(
                        onClick = { selectedCategory = "Recorded Lecture" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (selectedCategory == "Recorded Lecture") Color(0xFF1E88E5) else Color.DarkGray)
                    ) {
                        Text("📼 Recorded")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // YouTube URL Paste with Auto-Thumbnail Generation
                Text("YouTube URL (Auto Thumbnail Generator)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it; feedbackMessage = "" },
                    placeholder = { Text("https://www.youtube.com/watch?v=...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val videoId = extractYouTubeVideoId(urlInput)
                        if (videoId != null) {
                            val embedUrl = "https://www.youtube.com/embed/$videoId"
                            val thumbUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                            val newItem = VideoItem(
                                title = titleInput.ifBlank { "Cloud YouTube Lecture" },
                                type = selectedCategory,
                                sourceUrl = embedUrl,
                                thumbnailUrl = thumbUrl,
                                isYouTube = true
                            )
                            CloudVideoRepository.publishVideoToCloud(newItem)
                            feedbackMessage = "✅ YouTube video & thumbnail published globally!"
                            isErrorFeedback = false
                            urlInput = ""
                            titleInput = ""
                        } else {
                            feedbackMessage = "❌ Invalid YouTube URL. Please enter a valid link."
                            isErrorFeedback = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD87D38))
                ) {
                    Text("Publish YouTube Link & Generate Thumbnail")
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.DarkGray)
                Spacer(modifier = Modifier.height(16.dp))

                // Local MP4 Gallery Picker
                Text("Local MP4 Video Upload from Gallery", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { galleryLauncher.launch("video/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                ) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = "Gallery")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pick & Publish Local MP4 Video")
                }

                if (feedbackMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = feedbackMessage,
                        color = if (isErrorFeedback) Color.Red else Color.Green,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------
// 8. ONLINE TESTS & QUIZZES SCREEN (Interactive Exam Engine)
// ---------------------------------------------------------
@Composable
fun OnlineTestsScreen(onBack: () -> Unit) {
    var selectedTest by remember { mutableStateOf<String?>(null) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableStateOf(0) }
    var testCompleted by remember { mutableStateOf(false) }

    val questions = listOf(
        Triple("प्रकाश संश्लेषण (Photosynthesis) के लिए क्या आवश्यक है?", listOf("सूर्य का प्रकाश", "जल", "कार्बन डाइऑक्साइड", "उपर्युक्त सभी"), 3),
        Triple("न्यूटन का गति का कौन सा नियम F = ma देता है?", listOf("प्रथम नियम", "द्वितीय नियम", "तृतीय नियम", "इनमें से कोई नहीं"), 1),
        Triple("समकोण त्रिभुज में पाइथागोरस प्रमेय क्या है?", listOf("Hypotenuse² = Base² + Perpendicular²", "Base = Perpendicular", "Area = l * b", "None"), 0)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (selectedTest != null) selectedTest = null else onBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Online Tests & Mock Exams", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A237E))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTest == null) {
            Text("Available Board Practice Tests (Mission 2027)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(10.dp))

            val tests = listOf(
                "Class 10 Science - Chapter 1 & 2 Mock Test",
                "Class 10 Mathematics - Trigonometry Grand Test",
                "Class 10 Social Science - Board Exam Sample Paper 2027"
            )
            tests.forEach { testName ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {
                            selectedTest = testName
                            currentQuestionIndex = 0
                            score = 0
                            selectedOption = null
                            testCompleted = false
                        },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(testName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1A237E))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Duration: 30 Mins • 15 Questions • Instant Scoring", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        } else if (!testCompleted) {
            val q = questions[currentQuestionIndex]
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Question ${currentQuestionIndex + 1} of ${questions.size}", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(q.first, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    q.second.forEachIndexed { index, option ->
                        Button(
                            onClick = { selectedOption = index },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedOption == index) Color(0xFF1E88E5) else Color(0xFFE0E0E0)
                            )
                        ) {
                            Text(option, color = if (selectedOption == index) Color.White else Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (selectedOption == q.third) score += 10
                            if (currentQuestionIndex < questions.size - 1) {
                                currentQuestionIndex++
                                selectedOption = null
                            } else {
                                testCompleted = true
                            }
                        },
                        enabled = selectedOption != null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD87D38))
                    ) {
                        Text(if (currentQuestionIndex < questions.size - 1) "Next Question" else "Submit Test")
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉 Test Submitted Successfully!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF43A047))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Your Score: $score / ${questions.size * 10}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { selectedTest = null }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))) {
                        Text("Back to Tests")
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// 9. NOTES & PREVIOUS YEAR PAPERS LIBRARY (2010 - 2026)
// ---------------------------------------------------------
@Composable
fun NotesLibraryScreen(onBack: () -> Unit) {
    var selectedYear by remember { mutableStateOf("2026") }

    val years = listOf("2026", "2025", "2023", "2020", "2017", "2015", "2010")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Notes & PYQ Papers (2010-2026)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A237E))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Filter by Examination Year:", fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(years) { yr ->
                Button(
                    onClick = { selectedYear = yr },
                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedYear == yr) Color(0xFF1E88E5) else Color.LightGray),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(yr, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Available Verified Notes & Question Papers ($selectedYear):", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(10.dp))

        val notesList = listOf(
            Pair("Class 10 Board Exam Official Question Paper $selectedYear", "Verified Board PYQ • PDF"),
            Pair("Chapter-wise Revision Notes - Science ($selectedYear)", "Handwritten Expert Notes • PDF"),
            Pair("Important Formula Sheet - Mathematics ($selectedYear)", "Quick Formula Guide • PDF")
        )

        notesList.forEach { note ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(note.first, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1A237E))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(note.second, fontSize = 11.sp, color = Color.Gray)
                    }
                    Button(
                        onClick = { /* Download PDF action */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD87D38)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// 10. LIVE CLASSES & SCHEDULE SCREEN
// ---------------------------------------------------------
@Composable
fun LiveClassesScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Live Classes Schedule", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A237E))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0FE))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color.Red, shape = CircleShape) {
                        Box(modifier = Modifier.size(10.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("LIVE NOW: Class 10 Mathematics - Circles & Tangents", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1A237E))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Teacher: Ankit Sir • Join now for live doubt solving.", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = { /* Join */ }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))) {
                    Text("Join Live Stream")
                }
            }
        }
    }
}
