# Image Sharing Feature Plan

## Context

The AirGF app currently supports text-only chat with an on-device Gemma 4 E2B model via LiteRT-LM. Users want to:
1. **Send images** to the AI girlfriend (Gemma 4 is multimodal and can understand images)
2. **Receive images** from the AI girlfriend (via an on-device Stable Diffusion model)

The SD model download should be available in both onboarding (optional step) and settings. Image generation is triggered both by the AI deciding contextually and by a user-facing "request image" button. MediaPipe Image Generator is the chosen runtime for SD (already a dependency). If LiteRT-LM doesn't natively support image input, a MediaPipe vision captioning fallback will describe images as text.

---

## Phase 1: Data Layer Foundation

### Message model expansion

**`domain/model/Message.kt`** — add:
- `imagePath: String? = null` (absolute path to image file)
- `imageDescription: String? = null` (alt text or SD prompt)

**`data/local/db/entity/MessageEntity.kt`** — add matching nullable columns

**`data/local/db/AppDatabase.kt`** — bump version to 2, add migration:
```sql
ALTER TABLE messages ADD COLUMN imagePath TEXT DEFAULT NULL
ALTER TABLE messages ADD COLUMN imageDescription TEXT DEFAULT NULL
```

**`data/repository/ChatRepositoryImpl.kt`** — update `toDomain()` / `toEntity()` mappers to include new fields

**`core/di/AppModule.kt`** — register `MIGRATION_1_2` in the Room builder

### Image storage utility

**Create `core/util/ImageStorageUtil.kt`** — copies picked images to `context.filesDir/images/user/` with UUID filenames, saves generated bitmaps to `images/generated/`, provides cleanup and size-calculation methods

### Permissions & file paths

**`AndroidManifest.xml`** — add `CAMERA`, `READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE` (maxSdkVersion=32), and `<uses-feature camera required=false>`

**`res/xml/file_paths.xml`** (create if needed) — add `<files-path>` entries for `images/user/` and `images/generated/`

---

## Phase 2: Image Input (User sends images to AI)

### LLM multimodal support

**`llm/LlmSession.kt`** — add overloaded `sendMessage(text: String, imageBitmap: Bitmap?)`. If LiteRT-LM's `Conversation.sendMessageAsync` accepts `Contents` with image data, use it directly. If not, fall back to:

**Create `imagegen/ImageCaptioner.kt`** (fallback) — uses MediaPipe's image classification/captioning to describe the image as text, then passes `"[The user sent an image: {caption}]\n{userText}"` to the text-only `sendMessage`.

**`llm/LlmEngine.kt`** — when replaying message history in `createSession`, represent historical image messages as text descriptions (e.g., `"(shared an image: {description})"`) to avoid reloading image bytes.

### Chat input changes

**`presentation/chat/ChatScreen.kt`** — `ChatInputBar` modifications:
- Add `Icons.Outlined.Image` button between spicy toggle and text field
- Tapping opens a small bottom sheet / popup: "Gallery" (uses `PickVisualMedia`) and "Camera" (uses `TakePicture`)
- When image is selected, show a thumbnail preview strip above the input bar with an "X" dismiss button
- Send button sends text + attached image together

**`presentation/chat/ChatViewModel.kt`** — add state:
- `pendingImageUri: Uri?`, `pendingImageBitmap: Bitmap?`, `showAttachmentOptions: Boolean`
- Methods: `onImagePicked(uri)`, `onCameraCaptured(uri)`, `clearPendingImage()`, `sendMessageWithImage()`

### Message display

**`presentation/components/ChatBubble.kt`** — both `UserChatBubble` and `GfChatBubble`:
- If `message.imagePath != null`, render the image above the text content using Coil's `AsyncImage`
- Rounded corners matching bubble shape, max height ~280dp, `ContentScale.Crop`

**Add Coil dependency** — `libs.versions.toml`: `coil = "3.1.0"`, add `coil-compose` library

### Use case changes

**`domain/usecase/SendMessageUseCase.kt`** — add optional `imagePath: String?` and `imageBitmap: Bitmap?` parameters. When present, insert user message with `imagePath` set and call the multimodal `sendMessage` variant on `LlmSession`.

---

## Phase 3: Image Generation Infrastructure

### Model download pipeline (follows existing ModelDownloader pattern)

**Create `imagegen/ImageGenConstants.kt`** — SD model URL (HuggingFace), filename, display name, expected size (~1.5-2GB), min free space

**Create `imagegen/ImageGenModelDownloader.kt`** — mirrors `ModelDownloader`: OkHttp download with `Flow<DownloadState>`, progress tracking, `.part` file handling, stores in `context.filesDir/sd_model/`

**Create `domain/repository/ImageGenRepository.kt`** (interface) and **`data/repository/ImageGenRepositoryImpl.kt`** — follows `ModelRepository` pattern: `isModelDownloaded()`, `getModelPath()`, `downloadModel()`, `deleteModel()`

**Update `data/local/datastore/UserPreferences.kt`** — add `IMAGE_MODEL_DOWNLOADED` and `IMAGE_MODEL_PATH` preference keys

**Update `core/di/RepositoryModule.kt`** — bind `ImageGenRepositoryImpl` to `ImageGenRepository`

### Image generation engine

**Create `imagegen/ImageGenerator.kt`** — singleton wrapping MediaPipe's `ImageGenerator`:
- `GenState` sealed class: Uninitialized / Loading / Ready / Error
- `initialize(modelDir)` — loads the SD model via MediaPipe
- `generate(prompt, steps, seed) -> Bitmap` — runs inference
- `release()` — frees resources
- Lazy initialization: only loaded when needed, released after generation to avoid memory pressure with Gemma 4

---

## Phase 4: Image Generation Pipeline

### Detection and generation

**Create `domain/usecase/DetectImageRequestUseCase.kt`** — regex `\[IMAGE:\s*(.+?)]` (parallel to `DetectEmotionUseCase`). Strips the tag from displayed text, returns the image prompt.

**Create `domain/usecase/GenerateImageUseCase.kt`** — takes a prompt, calls `ImageGenerator.generate()`, saves bitmap via `ImageStorageUtil`, returns the file path. Emits `Generating` / `Complete(path)` / `Error` events.

### Integration into message flow

**`domain/usecase/SendMessageUseCase.kt`** — after emotion detection, also run `DetectImageRequestUseCase`. If an image prompt is found:
1. Strip the `[IMAGE:...]` tag from display text
2. Emit new `SendMessageEvent.GeneratingImage(prompt, textSoFar)`
3. Run `GenerateImageUseCase`
4. Save final message with both `content` and `imagePath`
5. Emit `SendMessageEvent.Complete` with the full message

Add `SendMessageEvent.GeneratingImage` variant to the sealed class.

### System prompt update

**`domain/usecase/BuildSystemPromptUseCase.kt`** — when SD model is available (`isImageGenAvailable: Boolean` param), append instructions telling the model:
- Use `[IMAGE: description]` on its own line to generate images
- Good times: when asked for selfies, describing visual scenes, reacting visually
- Keep descriptions detailed and specific
- Maximum one image per response

---

## Phase 5: User "Request Image" Button

**`presentation/chat/ChatScreen.kt`** — add a small camera/image icon in the chat header area or as a quick action. When tapped, sends a system-injected prompt like `"Send me a picture of yourself"` or appends a hint to the next user message that triggers the `[IMAGE:]` response from the LLM.

**`presentation/chat/ChatViewModel.kt`** — add `requestImage()` method that sends a message prompting the AI to generate an image.

---

## Phase 6: Onboarding & Settings

### Onboarding

**`core/navigation/Route.kt`** — add `ImageModelDownload` route

**`presentation/onboarding/OnboardingStep.kt`** — add `IMAGE_MODEL_DOWNLOAD` step (renumber subsequent steps)

**Create `presentation/onboarding/ImageModelDownloadScreen.kt`** — mirrors `ModelDownloadScreen` but:
- Uses `ImageGenConstants` for display
- Has a prominent "Skip" button (download is optional)
- Different tips and completion text

**Create `presentation/onboarding/ImageModelDownloadViewModel.kt`** — wired to `ImageGenRepository`

**`core/navigation/NavGraph.kt`** — insert new screen between `ModelDownload` and `GfCustomization` (or between `VoiceSelection` and `SetupComplete`)

### Settings

**`presentation/settings/SettingsScreen.kt`** — add "Image Generation" section with:
- Download/delete/re-download controls (same pattern as Gemma model section)
- Storage size display
- Status indicator

**`presentation/settings/SettingsViewModel.kt`** — inject `ImageGenRepository`, add methods for SD model management

---

## Phase 7: Polish

- **Chat UI**: Show "Creating an image..." shimmer indicator in streaming bubble during SD inference
- **`domain/usecase/ResetEverythingUseCase.kt`**: Delete all images in `images/user/` and `images/generated/` on reset
- **`domain/usecase/ExportChatUseCase.kt`**: Include image references in exports
- **Memory management**: Release SD model after generation completes; don't keep both Gemma 4 and SD resident simultaneously

---

## Key Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| LiteRT-LM may not support image input natively | Caption fallback via MediaPipe vision — describe image as text |
| MediaPipe SD model format/availability | Use officially supported Google model; can convert community models with Model Maker |
| Memory pressure (Gemma 4 + SD both ~2GB) | Lazy-load SD only during generation, release immediately after |
| SD generation speed (30-60s on mid-range) | Use 15-20 steps, show progress indicator, consider LCM variants for fewer steps |

---

## Build Sequence

1. **Data layer** — Message model, entity, migration, storage util, permissions
2. **Image input** — Coil dependency, LLM multimodal/fallback, chat input UI, chat bubble rendering
3. **SD infrastructure** — Constants, downloader, repository, MediaPipe engine
4. **Generation pipeline** — Detection use case, generation use case, SendMessage integration, system prompt
5. **Request button** — User-triggered image generation UI
6. **Onboarding + Settings** — Download screens, settings section
7. **Polish** — Loading indicators, reset cleanup, memory management

---

## Verification

1. Send a photo from gallery -> AI responds describing the image content
2. Take a photo with camera -> same as above
3. AI spontaneously generates an image during conversation -> image renders in GF bubble
4. Tap "request image" button -> AI generates and sends an image
5. Download SD model during onboarding -> verify model persists, skip button works
6. Settings: delete and re-download SD model -> verify lifecycle
7. Reset everything -> verify images are cleaned up
8. Send message without image -> existing text-only flow unchanged
