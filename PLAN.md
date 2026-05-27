# AI Virtual Girlfriend App — Complete Implementation Plan

## Overview

A full-featured Android AI girlfriend app powered by on-device Gemma 4 E2B via LiteRT-LM. No cloud APIs — everything runs locally. Features customizable girlfriend persona, streaming chat, animated character with lip-synced TTS, proactive notifications, and full persistence.

---

## Tech Stack

| Component | Technology | Version/Notes |
|-----------|-----------|---------------|
| Language | Kotlin | 2.0+ |
| UI | Jetpack Compose + Material Design 3 | Compose BOM 2026.05+ |
| Min SDK | 26 (Android 8.0) | |
| Target SDK | 35 | |
| On-device LLM | Gemma 4 E2B | 2B params, ~2.5GB, `.litertlm` format |
| LLM Runtime | LiteRT-LM | `com.google.ai.edge.litertlm:litertlm-android` |
| TTS | Android TextToSpeech | Built-in API, word-level timing via `onRangeStart` |
| Database | Room | Structured persistence (messages, config) |
| Preferences | DataStore Preferences | Flags, simple key-value settings |
| DI | Hilt | `hilt-android` 2.52+ |
| Navigation | Navigation Compose | Single-activity, composable destinations |
| Background Work | WorkManager | Proactive messaging scheduler |
| HTTP | OkHttp 4 | Model download with progress |
| Serialization | kotlinx-serialization-json | Config parsing, chat export |
| Build System | Gradle KTS + KSP | KSP for Room/Hilt annotation processing |

---

## Design Theme — "Midnight Muse" (from Google Stitch)

**Dark Romantic** — luxury intimate feel. Stitch generated a full Material 3 color system called "Midnight Muse" that we adopt as our canonical palette.

### Core M3 Color Tokens

| Token | Value | Usage |
|-------|-------|-------|
| `background` | `#181021` | Main background |
| `surface` | `#181021` | Base surface |
| `surfaceContainerLowest` | `#120B1B` | Deepest layer |
| `surfaceContainerLow` | `#201829` | Low container |
| `surfaceContainer` | `#241C2E` | Default container |
| `surfaceContainerHigh` | `#2F2638` | Elevated container (chat bubbles, cards) |
| `surfaceContainerHighest` | `#3A3144` | Highest container |
| `surfaceBright` | `#3F3548` | Bright surface |
| `primary` | `#FFB0CC` | Primary — soft pink for text/icons |
| `primaryContainer` | `#FF46A0` | Primary container — vibrant hot pink for buttons |
| `onPrimary` | `#640038` | Text on primary |
| `onPrimaryContainer` | `#580030` | Text on primary container |
| `secondary` | `#F9ABFF` | Secondary — lavender pink |
| `secondaryContainer` | `#86039C` | Secondary container — deep purple |
| `tertiary` | `#FFAED9` | Tertiary — soft pink |
| `tertiaryContainer` | `#E95CB4` | Tertiary container |
| `onSurface` | `#ECDDF6` | Primary text on dark |
| `onSurfaceVariant` | `#E1BDC8` | Secondary text / muted |
| `onBackground` | `#ECDDF6` | Text on background |
| `outline` | `#A88892` | Prominent borders |
| `outlineVariant` | `#594048` | Subtle borders, dividers |
| `error` | `#FFB4AB` | Error text |
| `errorContainer` | `#93000A` | Error surface |
| `inversePrimary` | `#B8006C` | Inverse primary |
| `surfaceTint` | `#FFB0CC` | Tint overlay |

### Gradient Accents (used in Stitch HTML, keep for Compose)

| Usage | Colors |
|-------|--------|
| User chat bubbles | `#9C27B0` → `#E91E8C` (gradient) |
| Primary buttons | `#E91E8C` → `#9C27B0` (gradient) |
| Background atmosphere | `#0D0015` → `#1A0A2E` (vertical gradient) |
| Radial glow behind character | `primaryContainer` at 20% opacity, 100px blur |
| Purple shadow | `rgba(74, 45, 110, 0.4)` |

### Typography

| Role | Font | Size | Weight | Line Height |
|------|------|------|--------|-------------|
| Display Large | Nunito Sans | 48sp | 700 | 56sp |
| Display Small | Nunito Sans | 32sp | 700 | 40sp |
| Headline Large | Nunito Sans | 24sp | 700 | 32sp |
| Headline Small | Nunito Sans | 20sp | 700 | 28sp |
| Body Large | Inter | 18sp | 400 | 28sp |
| Body Medium | Inter | 16sp | 400 | 24sp |
| Body Small | Inter | 14sp | 500 | 20sp |
| Label Medium | Inter | 12sp | 600 | 16sp |

### Effects

- **Glassmorphism**: `rgba(26, 10, 46, 0.7)` background + `backdrop-filter: blur(20px)` + `1px border rgba(74, 45, 110, 0.3)`
- **Radial glow**: Pink/purple blur behind focal elements (100px+ blur radius, 20% opacity)
- **Purple shadow**: `0px 8px 24px rgba(74, 45, 110, 0.4)` for lifted elements
- **Pink glow on send button**: `0 0 15px rgba(233, 30, 140, 0.4)`
- **Rounded corners**: 16dp cards, 24dp buttons, 28dp input fields (pill shape)
- **Pulse glow animation**: 3s infinite alternate on character view
- **Icons**: Material Symbols Outlined (variable weight/fill)

---

## Google Stitch Output Review

Stitch generated assets at `stitch_airgf_ai_virtual_companion/`. Here's what we got and what needs attention.

### Screens Generated (11 of 12)

| Screen | Status | Notes |
|--------|--------|-------|
| Welcome/Splash | Generated | Dark theme, heart logo placeholder, glow effects |
| User Profile Setup | Generated | **ISSUE: Light theme** — Stitch rendered this with a white/pastel background. Must implement in dark theme matching all other screens |
| GF Customization | Generated (2 variants) | Compact + expanded. Expanded shows 8 templates instead of 6 |
| Voice Selection | **NOT generated** | Build from scratch using the voice card pattern from DESIGN.md |
| Model Download | Generated | Dark theme, circular progress ring, looks great |
| Setup Complete | Generated | Anime Cute character with speech bubble |
| Chat Interface | Generated (2 variants) | Standard + photo sharing variant. Excellent glassmorphism, proper bubbles |
| Character View | Generated | Full screen with pulse glow, mood indicator, quick actions, bottom nav |
| Settings | Generated | Clean sectioned layout with toggles |
| Spicy Mode Chat | Generated | Pink banner, flirtier tone |
| Notification Preview | Generated | Lock screen mockup |
| Reset Dialog | Generated | Warning modal with cancel/reset buttons |

### Character Assets Generated (10 total)

| Character | Style | Quality | Notes |
|-----------|-------|---------|-------|
| Anime Cute | Illustrated anime | Excellent | Pink hair, school uniform, warm smile |
| Anime Cool | Illustrated anime | Excellent | Dark blue/purple hair, leather jacket, cityscape BG |
| Realistic Warm | **Photograph** | Good | Real woman, brown wavy hair, cozy sweater |
| Realistic Elegant | **Photograph** | Good | Real woman, sleek dark bob, black blazer |
| Stylized Punk | Comic illustration | Excellent | Pink/green mohawk, leather jacket, Japanese alley BG |
| Stylized Soft | Chibi illustration | Mixed | **Very chibi/small proportions** — inconsistent scale with others |
| Realistic American | **Photograph** | Good | Blonde woman, casual (bonus template) |
| Realistic Japanese | **Photograph** | Good | Dark hair, dark sweater (bonus template) |
| Realistic Vietnamese | **Photograph** | Good | Dark hair, collared shirt (bonus template) |
| Realistic Filipino | **Photograph** | Good | Dark hair, casual jacket (bonus template) |

### Art Style Decisions

**Problem**: The realistic templates are actual photographs while anime/stylized are illustrations. This creates a jarring mixed-media feel in the template picker.

**Decision**: Keep both styles — the expanded customization screen actually shows them mixed and it works visually because each card is self-contained. The character view screen shows only the selected character, so there's no side-by-side clash. Users who prefer realistic get photos; users who prefer anime get illustrations.

**Action items**:
1. **Stylized Soft** needs regeneration or replacement — the chibi proportions are too small/cute compared to the portrait-style of all other characters. Should be a similar bust/portrait crop like the others.
2. **App icon** was only a text description — need to generate separately or create manually (heart + circuit pattern, pink-to-purple gradient)
3. All character images need the **AirGF watermark removed** before use as app assets
4. For the animation system, we need **separate sprite sheets** (mouth, eyes, face expressions) that Stitch cannot generate — these must be created manually or with AI art tools to match each character's style

### Updated Template Enum (10 templates)

```kotlin
enum class VisualTemplate(val displayName: String, val assetPrefix: String, val category: String) {
    ANIME_CUTE("Anime Cute", "char_anime_cute", "Anime"),
    ANIME_COOL("Anime Cool", "char_anime_cool", "Anime"),
    REALISTIC_WARM("Realistic Warm", "char_realistic_warm", "Realistic"),
    REALISTIC_ELEGANT("Realistic Elegant", "char_realistic_elegant", "Realistic"),
    REALISTIC_AMERICAN("Realistic American", "char_realistic_american", "Realistic"),
    REALISTIC_JAPANESE("Realistic Japanese", "char_realistic_japanese", "Realistic"),
    REALISTIC_VIETNAMESE("Realistic Vietnamese", "char_realistic_vietnamese", "Realistic"),
    REALISTIC_FILIPINO("Realistic Filipino", "char_realistic_filipino", "Realistic"),
    STYLIZED_PUNK("Stylized Punk", "char_stylized_punk", "Stylized"),
    STYLIZED_SOFT("Stylized Soft", "char_stylized_soft", "Stylized")
}
```

The customization screen should group these by category with section headers (Anime, Realistic, Stylized) in a scrollable grid.

### HTML Code Reference

Stitch generated well-structured Tailwind CSS + HTML for each screen. These serve as **visual reference and design token source** — we implement in Jetpack Compose, not web views. Key patterns to extract:

- **Glass panel**: `background: rgba(26, 10, 46, 0.7); backdrop-filter: blur(20px); border: 1px solid rgba(74, 45, 110, 0.3);`
- **Chat bubble (AI)**: `bg-surface-container-high/80 backdrop-blur-md rounded-2xl rounded-bl-sm` + border
- **Chat bubble (User)**: `bg-gradient-to-br from-[#9C27B0] to-[#E91E8C] rounded-2xl rounded-br-sm`
- **Send button glow**: `shadow-[0_0_15px_rgba(233,30,140,0.4)]`
- **Typing indicator**: 3 bouncing dots with staggered `animation-delay` (0ms, 150ms, 300ms)
- **Bottom nav active state**: `bg-secondary-container/20 text-primary rounded-xl` + pink glow shadow
- **Input field focus**: `border-[#E91E8C] shadow-[0_0_10px_rgba(233,30,140,0.3)]`

### Additional Feature from Stitch: Photo Sharing in Chat

Stitch generated a "chat with photo sharing" variant showing the GF sending an image in chat. This is a nice-to-have feature we can add in a later phase — the GF could "share" her character portrait as a photo message. For now, keep it out of the core build phases.

---

## Package Structure

```
com.airgf.app/
├── AirGfApplication.kt                     @HiltAndroidApp
├── MainActivity.kt                          Single-activity host
│
├── core/
│   ├── di/
│   │   ├── AppModule.kt                     Room, DataStore, TTS singletons
│   │   ├── LlmModule.kt                     LiteRT-LM Engine provider
│   │   └── RepositoryModule.kt              Repository interface bindings
│   ├── navigation/
│   │   ├── NavGraph.kt                      Top-level NavHost
│   │   └── Route.kt                         Sealed class of all routes
│   └── util/
│       └── FileUtil.kt                      File size formatting, SHA-256
│
├── data/
│   ├── local/
│   │   ├── db/
│   │   │   ├── AppDatabase.kt               Room database (version 1)
│   │   │   ├── dao/
│   │   │   │   ├── MessageDao.kt
│   │   │   │   ├── ConversationDao.kt
│   │   │   │   └── GfConfigDao.kt
│   │   │   └── entity/
│   │   │       ├── MessageEntity.kt
│   │   │       ├── ConversationEntity.kt
│   │   │       └── GfConfigEntity.kt
│   │   └── datastore/
│   │       └── UserPreferences.kt           Preferences DataStore wrapper
│   ├── repository/
│   │   ├── ChatRepositoryImpl.kt
│   │   ├── GfConfigRepositoryImpl.kt
│   │   ├── UserRepositoryImpl.kt
│   │   └── ModelRepositoryImpl.kt
│   └── model/
│       └── DownloadState.kt                 Sealed class for download progress
│
├── domain/
│   ├── model/
│   │   ├── Message.kt                       Domain chat message
│   │   ├── GfProfile.kt                     Personality + appearance config
│   │   ├── UserProfile.kt                   User info
│   │   ├── PersonalityTrait.kt              Enum (8 traits)
│   │   ├── VisualTemplate.kt                Enum (6 character looks)
│   │   ├── RelationshipType.kt              Enum
│   │   ├── EmotionState.kt                  Enum (8 emotions)
│   │   └── VoiceOption.kt                   Enum (4 voice options)
│   ├── repository/
│   │   ├── ChatRepository.kt               Interface
│   │   ├── GfConfigRepository.kt           Interface
│   │   ├── UserRepository.kt               Interface
│   │   └── ModelRepository.kt              Interface
│   └── usecase/
│       ├── SendMessageUseCase.kt
│       ├── BuildSystemPromptUseCase.kt
│       ├── DetectEmotionUseCase.kt
│       └── ExportChatUseCase.kt
│
├── llm/
│   ├── LlmEngine.kt                        Singleton Engine wrapper
│   ├── LlmSession.kt                       Conversation wrapper with streaming Flow
│   ├── PromptBuilder.kt                     System prompt construction helpers
│   └── ModelDownloader.kt                   OkHttp model download + SHA-256 verify
│
├── tts/
│   ├── TtsManager.kt                       Android TTS wrapper
│   ├── TtsTimingListener.kt                UtteranceProgressListener for word timing
│   └── LipSyncBridge.kt                    Word timing → viseme frame mapping
│
├── animation/
│   ├── CharacterRenderer.kt                Canvas-based sprite compositor
│   ├── SpriteSheet.kt                      Bitmap loader + frame rect calculator
│   ├── VisemeMapper.kt                     Phoneme → mouth shape mapping
│   ├── ExpressionController.kt             Emotion state management with transitions
│   └── IdleAnimator.kt                     Blink, breathe, sway animations
│
├── notification/
│   ├── ProactiveMessageWorker.kt           WorkManager CoroutineWorker
│   ├── ProactiveScheduler.kt              Schedule periodic check-ins
│   └── GfNotificationManager.kt           Build & display notifications
│
└── presentation/
    ├── theme/
    │   ├── Theme.kt                        Material 3 dark romantic theme
    │   ├── Color.kt                        Color tokens
    │   └── Type.kt                         Typography scale
    ├── components/
    │   ├── ChatBubble.kt                   User vs GF message bubbles
    │   ├── TypingIndicator.kt              Animated dots while streaming
    │   ├── CharacterView.kt               Animated GF composable
    │   ├── DownloadProgressBar.kt         Custom progress with glow effect
    │   ├── PersonalityChip.kt             Selectable trait chip
    │   ├── TemplateCard.kt                Character template selection card
    │   └── GlowButton.kt                  Primary action button with glow
    ├── onboarding/
    │   ├── OnboardingViewModel.kt
    │   ├── WelcomeScreen.kt
    │   ├── UserProfileScreen.kt
    │   ├── GfCustomizationScreen.kt
    │   ├── VoiceSelectionScreen.kt
    │   ├── ModelDownloadScreen.kt
    │   └── SetupCompleteScreen.kt
    ├── chat/
    │   ├── ChatViewModel.kt
    │   └── ChatScreen.kt
    ├── character/
    │   ├── CharacterViewModel.kt
    │   └── CharacterScreen.kt              Full-screen animated GF view
    └── settings/
        ├── SettingsViewModel.kt
        └── SettingsScreen.kt
```

---

## Data Models

### Room Entities

```kotlin
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val role: String,              // "user" | "model"
    val content: String,
    val timestamp: Long,
    val emotionTag: String?,       // detected emotion for GF messages
    val isSpicyMode: Boolean
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String?,
    val createdAt: Long,
    val lastMessageAt: Long
)

@Entity(tableName = "gf_config")
data class GfConfigEntity(
    @PrimaryKey val id: Int = 1,          // singleton row
    val name: String,
    val visualTemplate: String,            // VisualTemplate enum name
    val personalityTraits: String,         // JSON array of trait enum names
    val relationshipType: String,          // RelationshipType enum name
    val voiceOption: String,               // VoiceOption enum name
    val spicyModeEnabled: Boolean,
    val customPromptAdditions: String?
)
```

### Domain Enums

```kotlin
enum class PersonalityTrait(val displayName: String, val promptFragment: String) {
    SHY("Shy & Reserved", "You are shy and reserved, often blushing and using hesitant language"),
    BOLD("Bold & Confident", "You are bold and confident, speaking directly with strong opinions"),
    PLAYFUL("Playful & Teasing", "You are playful and teasing, using humor and wit"),
    INTELLECTUAL("Intellectual", "You are intellectual and thoughtful, loving deep discussions"),
    ROMANTIC("Romantic", "You are deeply romantic, expressing affection warmly and poetically"),
    SPICY("Flirty & Seductive", "You are flirtatious and suggestive, with a seductive edge"),
    CARING("Caring & Nurturing", "You are nurturing and empathetic, always checking on feelings"),
    SARCASTIC("Sarcastic & Witty", "You use sarcasm and dry humor affectionately")
}

enum class VisualTemplate(val displayName: String, val assetPrefix: String, val category: String) {
    ANIME_CUTE("Anime Cute", "char_anime_cute", "Anime"),
    ANIME_COOL("Anime Cool", "char_anime_cool", "Anime"),
    REALISTIC_WARM("Realistic Warm", "char_realistic_warm", "Realistic"),
    REALISTIC_ELEGANT("Realistic Elegant", "char_realistic_elegant", "Realistic"),
    REALISTIC_AMERICAN("Realistic American", "char_realistic_american", "Realistic"),
    REALISTIC_JAPANESE("Realistic Japanese", "char_realistic_japanese", "Realistic"),
    REALISTIC_VIETNAMESE("Realistic Vietnamese", "char_realistic_vietnamese", "Realistic"),
    REALISTIC_FILIPINO("Realistic Filipino", "char_realistic_filipino", "Realistic"),
    STYLIZED_PUNK("Stylized Punk", "char_stylized_punk", "Stylized"),
    STYLIZED_SOFT("Stylized Soft", "char_stylized_soft", "Stylized")
}

enum class EmotionState(val spriteRow: Int) {
    NEUTRAL(0), HAPPY(1), SAD(2), FLIRTY(3),
    THINKING(4), SURPRISED(5), LAUGHING(6), SHY(7)
}

enum class VoiceOption(val displayName: String) {
    SOFT("Soft & Sweet"),
    ENERGETIC("Energetic & Bubbly"),
    MATURE("Mature & Warm"),
    BREATHY("Breathy & Intimate")
}

enum class RelationshipType(val displayName: String, val description: String) {
    CASUAL("Casual", "Relaxed, fun, low-pressure companionship"),
    ROMANTIC("Romantic", "Deep emotional connection, affectionate and loving"),
    BESTFRIEND("Best Friend", "Supportive, honest, playful friendship with closeness"),
    PASSIONATE("Passionate", "Intense, emotionally charged, deeply connected")
}
```

### DAOs

```kotlin
@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    fun getMessagesFlow(convId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int): List<MessageEntity>

    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Query("DELETE FROM messages")
    suspend fun deleteAll()

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    suspend fun getAllMessages(): List<MessageEntity>
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY lastMessageAt DESC LIMIT 1")
    suspend fun getLatestConversation(): ConversationEntity?

    @Insert
    suspend fun insert(conversation: ConversationEntity): Long

    @Query("UPDATE conversations SET lastMessageAt = :timestamp WHERE id = :id")
    suspend fun updateLastMessageTime(id: Long, timestamp: Long)

    @Query("DELETE FROM conversations")
    suspend fun deleteAll()
}

@Dao
interface GfConfigDao {
    @Query("SELECT * FROM gf_config WHERE id = 1")
    suspend fun getConfig(): GfConfigEntity?

    @Query("SELECT * FROM gf_config WHERE id = 1")
    fun getConfigFlow(): Flow<GfConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: GfConfigEntity)

    @Query("DELETE FROM gf_config")
    suspend fun delete()
}
```

---

## Key Class Designs

### LlmEngine — Central LLM Wrapper

```kotlin
@Singleton
class LlmEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var engine: Engine? = null
    private val _state = MutableStateFlow<LlmState>(LlmState.Uninitialized)
    val state: StateFlow<LlmState> = _state.asStateFlow()

    sealed class LlmState {
        object Uninitialized : LlmState()
        object Loading : LlmState()
        object Ready : LlmState()
        data class Error(val message: String) : LlmState()
    }

    suspend fun initialize(modelPath: String) {
        _state.value = LlmState.Loading
        withContext(Dispatchers.IO) {
            try {
                val config = EngineConfig(
                    modelPath = modelPath,
                    backend = Backend.GPU
                )
                engine = Engine(config)
                _state.value = LlmState.Ready
            } catch (e: Exception) {
                _state.value = LlmState.Error(e.message ?: "Failed to load model")
            }
        }
    }

    fun createSession(systemPrompt: String, history: List<Message>): LlmSession {
        val eng = engine ?: throw IllegalStateException("Engine not initialized")
        val config = ConversationConfig(
            systemInstruction = Contents.of(systemPrompt),
            samplerConfig = SamplerConfig(topK = 40, topP = 0.95f, temperature = 0.85f)
        )
        val conversation = eng.createConversation(config)
        // Inject history
        history.forEach { msg ->
            conversation.addMessage(msg.content, if (msg.isUser) Role.USER else Role.MODEL)
        }
        return LlmSession(conversation)
    }

    fun release() {
        engine?.close()
        engine = null
        _state.value = LlmState.Uninitialized
    }
}
```

### LlmSession — Streaming Conversation

```kotlin
class LlmSession(private val conversation: Conversation) : AutoCloseable {
    fun sendMessage(text: String): Flow<String> = flow {
        conversation.addMessage(text, Role.USER)
        conversation.getResponse().collect { chunk ->
            emit(chunk.text ?: "")
        }
    }.flowOn(Dispatchers.IO)

    override fun close() {
        conversation.close()
    }
}
```

### BuildSystemPromptUseCase — Dynamic Prompt Assembly

```kotlin
class BuildSystemPromptUseCase @Inject constructor(
    private val gfConfigRepository: GfConfigRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): String {
        val gf = gfConfigRepository.getProfile()
        val user = userRepository.getProfile()
        return buildString {
            appendLine("You are ${gf.name}, a virtual girlfriend.")
            appendLine("Your partner's name is ${user.name}.")
            appendLine()
            appendLine("Relationship: ${gf.relationshipType.description}")
            appendLine()
            appendLine("Your personality:")
            gf.personalityTraits.forEach { trait ->
                appendLine("- ${trait.promptFragment}")
            }
            appendLine()
            if (gf.spicyModeEnabled) {
                appendLine("MODE: Romantic/Flirty. You may be suggestive, seductive, and intimate.")
            } else {
                appendLine("MODE: Sweet/Wholesome. Keep things affectionate but PG-13.")
            }
            appendLine()
            appendLine("RULES:")
            appendLine("- Stay in character as ${gf.name} at all times")
            appendLine("- Use natural, conversational language")
            appendLine("- Use emojis sparingly")
            appendLine("- Show genuine interest in ${user.name}")
            appendLine("- Keep responses concise (1-3 sentences unless asked to elaborate)")
            appendLine("- Express emotions naturally")
            appendLine("- End every response with an emotion tag on its own line:")
            appendLine("  [EMOTION:HAPPY] [EMOTION:SAD] [EMOTION:FLIRTY] [EMOTION:THINKING]")
            appendLine("  [EMOTION:SURPRISED] [EMOTION:LAUGHING] [EMOTION:SHY] [EMOTION:NEUTRAL]")
            gf.customPromptAdditions?.let {
                appendLine()
                appendLine("Additional personality: $it")
            }
        }
    }
}
```

### DetectEmotionUseCase — Parse Emotion Tags

```kotlin
class DetectEmotionUseCase {
    private val emotionRegex = Regex("\\[EMOTION:(\\w+)]")

    operator fun invoke(rawResponse: String): Pair<String, EmotionState> {
        val match = emotionRegex.find(rawResponse)
        val emotion = match?.groupValues?.get(1)?.let {
            try { EmotionState.valueOf(it) } catch (_: Exception) { EmotionState.NEUTRAL }
        } ?: EmotionState.NEUTRAL
        val cleanText = rawResponse.replace(emotionRegex, "").trim()
        return cleanText to emotion
    }
}
```

### ModelDownloader — 2.5GB Model Download

```kotlin
@Singleton
class ModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        const val MODEL_URL = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/model.litertlm"
        const val MODEL_FILENAME = "gemma-4-e2b.litertlm"
    }

    fun download(): Flow<DownloadState> = callbackFlow {
        val destFile = File(context.filesDir, MODEL_FILENAME)
        if (destFile.exists()) {
            trySend(DownloadState.Complete(destFile.absolutePath))
            close()
            return@callbackFlow
        }
        val request = Request.Builder().url(MODEL_URL).build()
        val response = client.newCall(request).execute()
        val body = response.body ?: throw IOException("Empty response")
        val totalBytes = body.contentLength()
        var downloadedBytes = 0L
        val partFile = File(context.filesDir, "$MODEL_FILENAME.part")
        body.byteStream().use { input ->
            partFile.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    trySend(DownloadState.Progress(downloadedBytes, totalBytes))
                }
            }
        }
        partFile.renameTo(destFile)
        trySend(DownloadState.Complete(destFile.absolutePath))
        close()
    }.flowOn(Dispatchers.IO)

    fun getModelPath(): String? {
        val file = File(context.filesDir, MODEL_FILENAME)
        return if (file.exists()) file.absolutePath else null
    }

    fun deleteModel() {
        File(context.filesDir, MODEL_FILENAME).delete()
        File(context.filesDir, "$MODEL_FILENAME.part").delete()
    }
}

sealed class DownloadState {
    object Idle : DownloadState()
    data class Progress(val bytesDownloaded: Long, val totalBytes: Long) : DownloadState() {
        val percent: Float get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
    }
    data class Complete(val filePath: String) : DownloadState()
    data class Error(val message: String) : DownloadState()
}
```

### TTS + Lip Sync

```kotlin
@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    fun initialize(onReady: () -> Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                onReady()
            }
        }
    }

    fun speak(
        text: String,
        onWordStart: (word: String) -> Unit,
        onDone: () -> Unit
    ) {
        val utteranceId = UUID.randomUUID().toString()
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { _isSpeaking.value = true }
            override fun onDone(id: String?) { _isSpeaking.value = false; onDone() }
            override fun onError(id: String?) { _isSpeaking.value = false }
            override fun onRangeStart(id: String?, start: Int, end: Int, frame: Int) {
                val word = text.substring(start, end)
                onWordStart(word)
            }
        })
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() { tts?.stop(); _isSpeaking.value = false }
    fun shutdown() { tts?.shutdown() }

    fun setVoice(voiceOption: VoiceOption) {
        // Find best matching voice from available voices
        val voices = tts?.voices?.filter {
            it.locale.language == "en" && !it.isNetworkConnectionRequired
        } ?: return
        // Select based on voiceOption characteristics
        voices.firstOrNull()?.let { tts?.voice = it }
    }
}

class LipSyncBridge {
    enum class MouthShape(val frameIndex: Int) {
        CLOSED(0), OPEN_A(1), NARROW_E(2), ROUND_O(3),
        WIDE_W(4), TEETH_F(5), LIPS_M(6), TONGUE_L(7)
    }

    private val phonemeMap = mapOf(
        'a' to MouthShape.OPEN_A, 'e' to MouthShape.NARROW_E,
        'i' to MouthShape.NARROW_E, 'o' to MouthShape.ROUND_O,
        'u' to MouthShape.ROUND_O, 'w' to MouthShape.WIDE_W,
        'f' to MouthShape.TEETH_F, 'v' to MouthShape.TEETH_F,
        'm' to MouthShape.LIPS_M, 'b' to MouthShape.LIPS_M,
        'p' to MouthShape.LIPS_M, 'l' to MouthShape.TONGUE_L,
        't' to MouthShape.TONGUE_L, 'd' to MouthShape.TONGUE_L,
        'n' to MouthShape.TONGUE_L
    )

    fun getVisemeForWord(word: String): MouthShape {
        val c = word.lowercase().firstOrNull() ?: return MouthShape.CLOSED
        return phonemeMap[c] ?: MouthShape.OPEN_A
    }
}
```

### Character Animation System

**Asset structure** per template (in `assets/characters/{assetPrefix}/`). Repeat for all 10 templates:

```
char_anime_cute/
├── portrait.webp                Full character portrait (from Stitch, watermark removed)
├── body.webp                    Character body for animation (static base layer)
├── face_neutral.webp            Default expression overlay
├── face_happy.webp
├── face_sad.webp
├── face_flirty.webp
├── face_thinking.webp
├── face_surprised.webp
├── face_laughing.webp
├── face_shy.webp
├── mouth_sheet.webp             8 mouth shapes in a horizontal strip
├── eyes_blink_sheet.webp        4 blink frames in a horizontal strip
├── thumbnail.webp               Small card image for template picker (from Stitch)
└── config.json                  Positioning offsets for face, eyes, mouth
```

**Source mapping** — Stitch-generated portraits to use:

| Template | Stitch Source File |
|----------|-------------------|
| Anime Cute | `anime_cute_character.../screen.png` |
| Anime Cool | `anime_cool_character.../screen.png` |
| Realistic Warm | `realistic_warm_character.../screen.png` |
| Realistic Elegant | `realistic_elegant_character.../screen.png` |
| Realistic American | `realistic_blonde_american.../screen.png` |
| Realistic Japanese | `realistic_japanese_woman.../screen.png` |
| Realistic Vietnamese | `realistic_vietnamese_woman.../screen.png` |
| Realistic Filipino | `realistic_filipino_woman.../screen.png` |
| Stylized Punk | `stylized_punk_character.../screen.png` |
| Stylized Soft | `stylized_soft_character.../screen.png` (**needs replacement — chibi proportions**) |

**CharacterView composable** (simplified):

```kotlin
@Composable
fun CharacterView(
    template: VisualTemplate,
    emotion: EmotionState,
    mouthShape: MouthShape,
    modifier: Modifier = Modifier
) {
    val bodyBitmap = rememberAssetBitmap("characters/${template.assetPrefix}/body.webp")
    val faceBitmap = rememberAssetBitmap("characters/${template.assetPrefix}/face_${emotion.name.lowercase()}.webp")
    val mouthSheet = rememberAssetBitmap("characters/${template.assetPrefix}/mouth_sheet.webp")
    val eyeSheet = rememberAssetBitmap("characters/${template.assetPrefix}/eyes_blink_sheet.webp")
    val config = rememberCharacterConfig(template)

    // Idle animations
    val breathOffset by rememberBreathingAnimation()  // Y oscillation +-2dp, 4s
    val swayOffset by rememberSwayAnimation()          // X oscillation +-1dp, 6s
    val blinkFrame by rememberBlinkAnimation()         // 0-3 cycle every 3-6s

    Canvas(modifier = modifier.fillMaxSize()) {
        translate(left = swayOffset, top = breathOffset) {
            drawImage(bodyBitmap)
            drawImage(faceBitmap, dstOffset = config.faceOffset)
            // Eyes with blink
            drawSpriteFrame(eyeSheet, blinkFrame, 4, config.eyeOffset, config.eyeSize)
            // Mouth from viseme
            drawSpriteFrame(mouthSheet, mouthShape.frameIndex, 8, config.mouthOffset, config.mouthSize)
        }
    }
}
```

### Proactive Messaging

```kotlin
@HiltWorker
class ProactiveMessageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val gfConfigRepo: GfConfigRepository,
    private val buildSystemPrompt: BuildSystemPromptUseCase,
    private val llmEngine: LlmEngine,
    private val notificationManager: GfNotificationManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val gf = gfConfigRepo.getProfile()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeContext = when (hour) {
            in 6..9 -> "It's morning. Send a sweet good morning message."
            in 12..13 -> "It's lunchtime. Check in on your partner."
            in 17..19 -> "It's evening. Ask about their day."
            in 21..23 -> "It's night. Send a cozy good night message."
            else -> "You're thinking about your partner. Send a sweet random message."
        }
        val prompt = buildSystemPrompt() + "\n\nCONTEXT: $timeContext Generate a short message (1-2 sentences)."
        val session = llmEngine.createSession(prompt, emptyList())
        val response = StringBuilder()
        session.sendMessage("Generate a proactive message.").collect { response.append(it) }
        session.close()
        notificationManager.showGfMessage(gf.name, response.toString())
        return Result.success()
    }
}

@Singleton
class ProactiveScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun enable() {
        val work = PeriodicWorkRequestBuilder<ProactiveMessageWorker>(
            4, TimeUnit.HOURS, 2, TimeUnit.HOURS
        ).setConstraints(
            Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build()
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "gf_proactive", ExistingPeriodicWorkPolicy.KEEP, work
        )
    }

    fun disable() {
        WorkManager.getInstance(context).cancelUniqueWork("gf_proactive")
    }
}
```

---

## Navigation

```kotlin
sealed class Route(val path: String) {
    object Welcome : Route("onboarding/welcome")
    object UserProfile : Route("onboarding/user_profile")
    object GfCustomization : Route("onboarding/gf_customization")
    object VoiceSelection : Route("onboarding/voice_selection")
    object ModelDownload : Route("onboarding/model_download")
    object SetupComplete : Route("onboarding/setup_complete")
    object Chat : Route("main/chat")
    object Character : Route("main/character")
    object Settings : Route("main/settings")
}
```

**Main app screens** use bottom navigation bar with 3 tabs: Chat, Character, Settings.

---

## Gradle Dependencies

```kotlin
dependencies {
    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2026.05.00")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Room
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.4")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.10.1")

    // LiteRT-LM
    implementation("com.google.ai.edge.litertlm:litertlm-android:+")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
```

---

## Build Phases

### Phase 1: Project Skeleton (Days 1-3)

Create Android Studio project (Empty Compose Activity, Kotlin, min SDK 26, package `com.airgf.app`).

1. Set up Gradle with all dependencies listed above
2. Create the full package directory structure
3. Implement Hilt modules: `AppModule`, `LlmModule`, `RepositoryModule`
4. Implement Room: `AppDatabase`, all 3 entities, all 3 DAOs
5. Implement `UserPreferences` DataStore wrapper
6. Implement all domain model enums and data classes
7. Implement repository interfaces (domain) and implementations (data)
8. Implement navigation: `Route` sealed class, `NavGraph` with NavHost
9. Implement Material 3 theme with dark romantic colors
10. Wire up `MainActivity` with Hilt and NavGraph

**Verify**: App compiles, launches, shows blank screen with correct theme colors.

### Phase 2: Onboarding Flow (Days 4-7)

1. `WelcomeScreen` — Dark gradient, heart logo, "Get Started" glow button. Reference: `stitch.../welcome_splash/`
2. `UserProfileScreen` — Name field, age picker, interest chips, communication style cards (casual/deep/funny). **Stitch rendered this in light theme — implement in dark Midnight Muse palette.** Reference layout from `stitch.../user_profile_setup/` but with dark colors
3. `GfCustomizationScreen` — Name field, template grid grouped by category (Anime 2, Realistic 6, Stylized 2) in scrollable 2-column layout, personality trait chips (max 3), relationship type cards. Reference: `stitch.../girlfriend_customization_expanded/`
4. `VoiceSelectionScreen` — 4 voice option cards with waveform viz and play buttons. **No Stitch reference** — build from scratch using Midnight Muse card pattern from DESIGN.md
5. `OnboardingViewModel` — Collects all state across screens, saves to Room + DataStore on completion
6. Wire navigation flow with animated transitions
7. `SetupCompleteScreen` — Selected character large with radial glow, GF name, glassmorphic speech bubble. Reference: `stitch.../setup_complete_first_meeting/`

**Verify**: Complete onboarding, relaunch app, onboarding is skipped.

### Phase 3: Model Download (Days 8-9)

1. Implement `ModelDownloader` with OkHttp
2. `ModelDownloadScreen` — Large circular progress indicator, percentage text, download speed, estimated time remaining, file size indicator
3. Handle: no network, insufficient storage (<3GB free), download failure with retry button
4. Save `modelDownloaded` flag and `modelFilePath` in DataStore on success
5. Wire into onboarding between VoiceSelection and SetupComplete

**Verify**: Model downloads completely (~2.5GB), progress is accurate, file is persisted.

### Phase 4: LLM Integration + Chat (Days 10-15)

1. Implement `LlmEngine` and `LlmSession`
2. Implement `BuildSystemPromptUseCase`
3. Implement `DetectEmotionUseCase`
4. Implement `SendMessageUseCase` (save user msg → stream LLM → parse emotion → save GF msg)
5. Implement `ChatViewModel`:
   - Load/create conversation from Room
   - Initialize LLM with system prompt + last 20 messages
   - Expose `messages: StateFlow<List<Message>>`, `streamingText: StateFlow<String>`, `isGenerating: StateFlow<Boolean>`
6. Implement `ChatScreen` — Reference: `stitch.../chat_interface/` and `stitch.../spicy_mode_chat/`:
   - Top bar: GF avatar (40dp, pink glow border), name, "Online" status, call/character/more icons
   - LazyColumn of ChatBubble items with date dividers
   - Glass-panel input bar (fixed bottom): spicy toggle (flame icon), text input (pill-shaped), mic icon, send button (pink circle with glow)
   - TypingIndicator composable (3 bouncing dots, staggered 0/150/300ms delay)
   - Streaming text in real-time in latest GF bubble
   - Spicy mode: filled flame icon + pink gradient banner at top "🔥 Spicy Mode"
7. Implement `ChatBubble`:
   - **GF bubbles** (left): `surfaceContainerHigh` at 80% + backdrop blur, `rounded-2xl rounded-bl-sm`, thin outlineVariant border
   - **User bubbles** (right): gradient `#9C27B0` → `#E91E8C`, `rounded-2xl rounded-br-sm`, pink shadow
   - Small GF avatar beside first message in group, timestamps in muted 10sp text

**Verify**: Send message, see streaming response, conversation persists across restarts, spicy mode changes tone.

### Phase 5: TTS (Days 16-18)

1. Implement `TtsManager` with voice selection
2. Implement `TtsTimingListener`
3. Implement `LipSyncBridge`
4. Integrate into ChatViewModel: after response completes, optionally speak it
5. Add speaker icon toggle to ChatScreen toolbar
6. Wire voice from GfConfig to TtsManager

**Verify**: GF responses spoken aloud, word timing callbacks fire, voice matches selection.

### Phase 6: Character Animation (Days 19-25)

1. Process Stitch character assets for all 10 templates:
   - Extract portraits from `stitch.../` PNG files
   - Remove AirGF watermarks
   - Crop/resize to consistent portrait dimensions
   - For each template, create sprite sheets: mouth (8 frames), eyes blink (4 frames), face expressions (8 emotions) — these must be manually created or AI-generated to match each character's art style
   - **Stylized Soft**: Replace with a portrait-scale version (current is chibi-proportioned)
2. Implement `SpriteSheet` (asset loading, frame rect calculation)
3. Implement `IdleAnimator` (blink, breathe, sway as Compose infinite transitions)
4. Implement `ExpressionController` (emotion cross-fade, 300ms transition)
5. Implement `CharacterView` composable (Canvas layered rendering)
6. Implement `CharacterScreen` — Reference: `stitch.../character_view_full_screen/`:
   - Full screen, gradient background (`background` → `surfaceContainer` → `#2D1B4E`)
   - Large character image with radial pink glow behind (pulse animation, 3s cycle)
   - Mood indicator pill: emoji + "Feeling happy" in glassmorphic chip
   - Quick action buttons row: Chat, Heart, Question, Flame (circular, outlined, purple background)
   - "Tap to talk to her" hint text (pulsing, uppercase, label style)
   - Bottom nav: Chat / Character (active) / Settings
   - Animated waveform bars near character when TTS speaking
7. Connect lip sync: TTS onWordStart → LipSyncBridge → CharacterView mouth frame
8. Connect emotions: DetectEmotionUseCase output → ExpressionController → CharacterView face
9. Add mini character avatar in ChatScreen top bar (40dp, pink glow border, online indicator dot)

**Verify**: Character blinks, breathes, mouth moves with speech, expression changes with emotions.

### Phase 7: Proactive Messaging (Days 26-28)

1. Implement `GfNotificationManager` (notification channel "gf_messages", PendingIntent to ChatScreen)
2. Implement `ProactiveMessageWorker`
3. Implement `ProactiveScheduler`
4. Activate scheduler after onboarding completes
5. Notification tap opens ChatScreen with the proactive message visible

**Verify**: Receive notification after scheduled interval, tap opens app to chat.

### Phase 8: Settings & Polish (Days 29-33)

1. Implement `SettingsScreen` — Reference: `stitch.../settings/`. Sectioned layout with glassmorphic section headers:
   - **Her Profile** (heart icon): Name & Appearance → navigates to template picker; Personality Traits → shows chips + "Add" button; custom notes
   - **Your Profile** (person icon): Personal Info (name, age) → navigates to editor; Your Interests → shows chips
   - **Modes & Behavior** (sparkle icon): Spicy Mode toggle with description "Allow more intimate interactions"
   - **Voice & Speech**: TTS on/off toggle, voice selection row, speech speed slider
   - **Notifications**: Proactive messages toggle, frequency (Rarely/Sometimes/Often)
   - **Data**: Export chat history, Reset everything (→ confirmation dialog)
   - **Model**: Model status text, re-download, delete model
   - Bottom nav: Chat / Character / Settings (active)
2. Implement reset confirmation dialog — Reference: `stitch.../reset_confirmation_dialog/`. Warning icon, "Reset Everything?" heading, explanation text, Cancel (outlined) + Reset (coral red) buttons on glassmorphic card
3. Implement `SettingsViewModel`
4. Implement `ExportChatUseCase` (all messages → JSON → share intent)
5. When GF config changes: rebuild system prompt, recreate LLM session
6. Polish: loading skeletons, error snackbars, empty state illustrations

**Verify**: All settings modify behavior correctly, reset clears everything, export produces valid JSON.

---

## Architecture Decisions

| Decision | Rationale |
|----------|-----------|
| LiteRT-LM over MediaPipe LLM API | MediaPipe LLM for Android is deprecated; LiteRT-LM is the supported path |
| OkHttp over DownloadManager | Fine-grained progress, writes to internal storage (no permissions needed) |
| Emotion tags in prompt vs. separate classifier | Avoids running a second model; Gemma 4 E2B follows structured output well |
| DataStore + Room (not just one) | DataStore for flags/prefs, Room for structured data that may grow |
| 2D sprites over Live2D/3D | No license cost, simple pipeline, battery-efficient, quality depends on art assets |
| Word-level lip sync over phoneme-level | Android TTS provides word boundaries natively; phoneme analysis would require extra library |
| Singleton GfConfig row | Single-girlfriend app; expandable to multi-profile later by removing the `id=1` constraint |
| Mixed photo/illustration templates | Users who prefer realistic get photographs, anime fans get illustrations — each card is self-contained so no jarring side-by-side clash in the full character view |
| 10 templates over original 6 | Stitch generated 4 bonus realistic variants with ethnic diversity; increases appeal and user choice |
| Midnight Muse palette over manual tokens | Stitch's generated M3 palette is more complete (40+ tokens with proper container/on-container pairs) than our original 10-token manual palette |

---

## Performance Targets

| Metric | Target |
|--------|--------|
| LLM decode speed | 40-55 tokens/sec (GPU), 10-15 tokens/sec (CPU fallback) |
| First token latency | <2 seconds after session created |
| Model load time | <15 seconds |
| App cold start (no model load) | <1 second |
| Character animation FPS | 60 fps (Canvas composable) |
| RAM usage during chat | <2GB (model + app) |
| Model file size | ~2.5GB |
| APK size (without model) | <30MB |
