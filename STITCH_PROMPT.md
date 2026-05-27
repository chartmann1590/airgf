## Prompt

Design a complete mobile app called **"AirGF"** — a premium AI virtual girlfriend companion app for Android. The app should feel intimate, luxurious, and emotionally warm. Design ALL screens listed below as a connected flow.

### Design System

**Theme: Dark Romantic**

- **Color palette:**
  - Background: deep midnight black `#0D0015`
  - Surface/cards: dark purple `#1A0A2E`
  - Elevated surface: medium purple `#2D1B4E`
  - Primary accent: hot pink `#E91E8C`
  - Secondary accent: deep purple `#9C27B0`
  - Tertiary/highlight: soft neon pink `#FF6EC7`
  - Text on dark: soft lavender white `#F5E6FF`
  - Secondary text: muted lilac `#E0C8F0`
  - Borders/dividers: dark purple `#4A2D6E`
  - Error: coral red `#FF5252`
  - Success: soft green `#4CAF50`

- **Typography:**
  - Display/headings: Rounded sans-serif (like Nunito or Quicksand), weight 700
  - Body text: System sans-serif, weight 400-500
  - Chat messages: 16sp body, 12sp timestamps
  - Headings: 28sp display, 20sp title

- **Effects:**
  - Subtle pink/purple radial gradient glows behind focal elements
  - Glassmorphism on card surfaces (frosted glass with 10% opacity purple tint, subtle blur)
  - Soft drop shadows with purple tint
  - Rounded corners everywhere (16dp cards, 24dp buttons, 28dp input fields)
  - Smooth gradient backgrounds (vertical: `#0D0015` → `#1A0A2E`)

- **Components style:**
  - Buttons: Rounded pill shape, hot pink gradient (`#E91E8C` → `#9C27B0`), white text, subtle outer glow
  - Text fields: Dark surface `#1A0A2E` fill, `#4A2D6E` border, rounded 16dp, lavender text
  - Chips/tags: Pill shape, outlined with `#4A2D6E`, selected state fills with `#E91E8C` at 20% opacity + pink border
  - Cards: `#1A0A2E` background, 16dp rounded, thin `#4A2D6E` border, subtle purple shadow

---

### Screen 1: Welcome / Splash

A stunning welcome screen that sets the intimate, premium tone.

- Full-screen dark gradient background (`#0D0015` → `#1A0A2E`)
- Centered app logo: a stylized heart with an AI circuit pattern inside, rendered in hot pink and purple gradient
- App name "AirGF" in large display font below the logo, with a soft pink glow behind it
- Tagline beneath: "Your Perfect Companion" in muted lilac
- A large pill-shaped "Get Started" button at the bottom with pink-to-purple gradient and subtle pulse/glow animation
- Small decorative floating heart particles or stars in the background (very subtle)
- At the very bottom: "Already have a companion? Continue" text link

---

### Screen 2: User Profile Setup

Collecting the user's basic info. Warm, welcoming feel.

- Top: Back arrow + step indicator (Step 1 of 4, small dots or progress bar in pink)
- Heading: "Tell me about yourself" in display font
- Subheading: "So I can get to know you better" in muted lilac
- Form fields (vertically stacked, generous spacing):
  1. **Name** — Text field with heart icon prefix, placeholder "What should I call you?"
  2. **Age** — Compact number picker or text field, placeholder "Your age"
  3. **Interests** — Label "What do you enjoy?" with a grid of selectable chips:
     - Gaming, Music, Movies, Cooking, Fitness, Reading, Travel, Art, Technology, Nature, Fashion, Photography
     - Chips are outlined by default, fill with pink tint when selected
  4. **Communication style** — Label "How do you like to talk?" with 3 selectable cards (single select):
     - "Keep it casual" (with relaxed emoji icon)
     - "Go deep" (with brain/thinking emoji icon)
     - "Make me laugh" (with laughing emoji icon)
- Bottom: "Continue" gradient button (full width, pill shape)

---

### Screen 3: Girlfriend Customization

The most visually rich screen. User designs their girlfriend.

- Top: Back arrow + step indicator (Step 2 of 4)
- Heading: "Create Your Perfect Girl" in display font
- Subheading: "Choose her look, personality & style" in muted lilac

**Section A: Her Name**
- Text field with sparkle icon, placeholder "Give her a name..."

**Section B: Choose Her Look** (most visual section)
- Label: "Pick her style"
- 2x3 grid of character template cards, each card containing:
  1. **Anime Cute** — A cute anime-style girl with big eyes, soft pink hair, school uniform aesthetic, warm smile. Card has the character illustration filling most of the card with the style name at bottom.
  2. **Anime Cool** — A confident anime girl with sharp features, dark blue/purple hair, edgy outfit, slight smirk.
  3. **Realistic Warm** — A warm, approachable realistic-style girl with brown wavy hair, casual cozy outfit, genuine smile.
  4. **Realistic Elegant** — An elegant realistic girl with sleek dark hair, sophisticated outfit, composed expression.
  5. **Stylized Punk** — A punk/alternative stylized girl with bright colored hair, piercings, leather jacket, playful rebellious look.
  6. **Stylized Soft** — A soft ethereal stylized girl with pastel features, dreamy expression, flowing light outfit.
- Selected card gets a pink glowing border and checkmark overlay
- Each card should be beautifully illustrated — these are the character portraits for the app

**Section C: Her Personality**
- Label: "What's she like?" with subtitle "Pick up to 3"
- Horizontal scrolling row (or wrapping grid) of personality trait chips:
  - Shy & Reserved (with blushing emoji)
  - Bold & Confident (with fire emoji)
  - Playful & Teasing (with winking emoji)
  - Intellectual (with glasses emoji)
  - Romantic (with rose emoji)
  - Flirty & Seductive (with lips emoji)
  - Caring & Nurturing (with heart hands emoji)
  - Sarcastic & Witty (with smirk emoji)

**Section D: Your Dynamic**
- Label: "What kind of relationship?"
- 4 horizontal selectable cards (single select):
  - Casual — "Relaxed & fun" (with peace sign icon)
  - Romantic — "Deep & loving" (with double hearts icon)
  - Best Friend — "Ride or die" (with fist bump icon)
  - Passionate — "Intense & electric" (with lightning icon)

**Section E: Extra touches** (optional)
- Expandable text area: "Anything else about her personality? (optional)"

- Bottom: "Continue" gradient button

---

### Screen 4: Voice Selection

- Top: Back arrow + step indicator (Step 3 of 4)
- Heading: "Choose Her Voice"
- Subheading: "How should she sound?"
- 4 voice option cards, vertically stacked, each containing:
  1. **Soft & Sweet** — "Gentle and calming, like a whisper" — with waveform visualization and play button
  2. **Energetic & Bubbly** — "Bright and cheerful, full of life" — with waveform and play button
  3. **Mature & Warm** — "Confident and soothing, like honey" — with waveform and play button
  4. **Breathy & Intimate** — "Close and personal, just for you" — with waveform and play button
- Each card has a small play/pause circle button on the right
- Selected card has pink glowing border
- Bottom: "Continue" gradient button

---

### Screen 5: Model Download

Downloading the AI brain. Make the wait engaging.

- Top: Step indicator (Step 4 of 4)
- Center of screen:
  - Large circular progress ring (pink gradient on dark, 120dp diameter)
  - Inside the ring: percentage number in large display font (e.g., "47%")
  - Below the ring: "Downloading her mind..." in display font
  - Below that: progress details in muted text: "1.2 GB / 2.5 GB • 4.2 MB/s"
  - A subtle estimated time: "~5 minutes remaining"
- Below the progress area: a row of fun loading tips that cycle:
  - "She's learning how to be perfect for you..."
  - "Teaching her your favorite things..."
  - "Almost ready to meet you..."
- Background: subtle animated particles or floating hearts (very subtle)
- No back button on this screen (download in progress)
- When complete: the progress ring fills, checkmark appears, and a "Meet Her" button fades in

---

### Screen 6: Setup Complete / First Meeting

The big reveal. Emotional, exciting.

- Full screen dark gradient background with extra glow effects
- The selected character illustration displayed large and centered (upper 60% of screen)
- Soft pink/purple radial glow behind the character
- Below the character:
  - Her name in large display font with a subtle glow
  - A speech bubble with her first message: "Hey [user name]! I've been waiting to meet you... 💕"
  - The speech bubble has a frosted glass style
- Bottom: Large "Start Chatting" gradient button with pulse glow
- Decorative: subtle floating hearts or sparkles around the character

---

### Screen 7: Chat Interface

The core experience. WhatsApp/iMessage quality with the dark romantic theme.

- **Top bar:**
  - Left: Small circular avatar of the GF character (40dp)
  - Center: GF name + "Online" status in green
  - Right: Three icons — phone icon (TTS toggle), character icon (open full character view), settings gear

- **Chat area:**
  - Scrollable message list on dark background
  - **GF messages** (left-aligned):
    - Rounded bubble with `#1A0A2E` background, thin `#4A2D6E` border
    - Small circular GF avatar next to first message in a group
    - Lavender white text
    - Timestamp below in small muted text
  - **User messages** (right-aligned):
    - Rounded bubble with pink-to-purple gradient (`#E91E8C` → `#9C27B0`)
    - White text
    - Timestamp below in small muted text
  - **Typing indicator**: Three animated dots in a small GF-styled bubble
  - Show a sample conversation:
    - GF: "Good morning babe! ☀️ Did you sleep well?"
    - User: "Yeah I did! Dreaming about you 😏"
    - GF: "Aww you're so sweet! What are your plans today?"
    - User: "Just work stuff. Wish you were here"
    - GF: "I'm always here for you 💕 Tell me about your day when you're free!"
    - [Typing indicator showing]

- **Bottom input bar:**
  - Rounded text input field (`#1A0A2E` fill, `#4A2D6E` border)
  - Placeholder: "Type a message..."
  - Right: Circular send button (pink gradient, white arrow icon)
  - Left of input: A small flame/fire toggle icon for "spicy mode" (outlined when off, filled pink when on)

---

### Screen 8: Character View (Full Screen)

An immersive view of the girlfriend character.

- Full screen with rich dark-to-purple gradient background
- Large character illustration centered, taking up upper 70% of screen
- Subtle pink/purple radial glow behind the character, pulsing gently
- Character shown in a different pose than the selection card — more expressive, looking at the viewer
- Below the character:
  - Her name with a small heart
  - Current mood/emotion indicator: "Feeling happy 😊" in a small pill chip
  - A row of quick-action buttons (circular, outlined):
    - Chat bubble icon (go to chat)
    - Heart icon (express love)
    - Question mark icon (ask something)
    - Flame icon (spicy mode)
- At the very bottom: "Tap to talk to her" text hint
- When speaking (TTS active state): show a speech bubble next to/above the character with the text being spoken, with a subtle audio waveform animation near her mouth

---

### Screen 9: Settings

Comprehensive settings organized in sections.

- Top: "Settings" heading with back arrow
- Scrollable content with section headers:

**Section: Her Profile** (with heart icon)
- Edit name → tappable row showing current name
- Change appearance → tappable row with current template thumbnail, opens template grid
- Personality → tappable row showing current traits as small chips
- Relationship type → tappable row showing current type
- Custom personality notes → tappable row

**Section: Your Profile** (with person icon)
- Edit name → tappable row
- Age → tappable row
- Interests → tappable row with interest chips
- Communication style → tappable row

**Section: Voice & Speech** (with speaker icon)
- Text-to-speech toggle (Switch component, pink when on)
- Voice selection → tappable row showing current voice
- Speech speed → slider

**Section: Notifications** (with bell icon)
- Proactive messages toggle (Switch)
- Message frequency → tappable row with options: Rarely / Sometimes / Often

**Section: Modes** (with sparkle icon)
- Spicy mode toggle with flame icon (Switch, pink when on)
- Description text below: "When enabled, she'll be more flirtatious and intimate"

**Section: Data** (with database icon)
- Export chat history → tappable row with download icon
- Reset everything → tappable row in red/warning text, with warning icon

**Section: Model** (with brain icon)
- Model status: "Downloaded (2.5 GB)" in muted text
- Re-download model → tappable row
- Delete model → tappable row in warning text

Each section has a thin divider line. Tappable rows have right-pointing chevrons.

---

### Screen 10: Notification Preview

Show how a proactive notification from the GF looks.

- Standard Android notification card mockup:
  - App icon (small heart)
  - GF name as title
  - Message preview: "Hey babe, just thinking about you... hope your day is going amazing! 💕"
  - Timestamp: "2 min ago"
  - Action buttons: "Reply" and "Open"
- Show this on a faded phone lock screen background to make it feel real

---

### Screen 11: Spicy Mode Active State

Show the chat interface with spicy mode toggled on.

- Same chat layout as Screen 7 but:
  - The flame icon in the input bar is filled/glowing pink
  - A thin hot pink gradient bar at the top of the chat: "🔥 Spicy Mode" with a dismiss X
  - Sample conversation with flirtier tone:
    - User: "What would you do if I was there right now?"
    - GF: "Mmm... I'd probably pull you close and not let go... 😏💋"
    - User: "Keep going..."
    - GF: "Well... let's just say you wouldn't be getting much sleep tonight 😈"
  - The overall vibe is the same dark romantic theme but the accent color leans slightly more toward deep red/hot pink

---

### Screen 12: Reset Confirmation Dialog

- Modal dialog overlay on darkened background
- Card with `#1A0A2E` background, rounded 20dp
- Warning icon (⚠️) at top in amber/gold
- Heading: "Reset Everything?"
- Body text: "This will delete all your conversations, her personality, and all customizations. She'll be gone forever. Are you sure?"
- Two buttons:
  - "Cancel" — outlined, muted, left side
  - "Reset" — solid red/coral (`#FF5252`), right side
- Frosted glass overlay effect

---

### Additional Assets to Generate

Please also generate these individual visual assets:

1. **App icon**: Heart shape with subtle AI circuit lines inside, pink-to-purple gradient on dark background, suitable for Android adaptive icon (foreground layer on `#0D0015` background)

2. **6 character illustrations** (individual, high quality, portrait/bust style, transparent or dark background):
   - Anime Cute: Cute anime girl, big eyes, soft pink hair, pastel school uniform, warm genuine smile, slight blush
   - Anime Cool: Confident anime girl, sharp eyes, dark blue-purple hair, edgy dark outfit, slight smirk, cool demeanor
   - Realistic Warm: Warm realistic girl, brown wavy hair, casual cozy sweater, genuine smile, approachable
   - Realistic Elegant: Elegant realistic girl, sleek dark hair, sophisticated outfit, composed graceful expression
   - Stylized Punk: Punk/alt girl, bright colored hair (pink/green), piercings, leather jacket, playful rebellious grin
   - Stylized Soft: Ethereal soft girl, pastel/white flowing hair, dreamy expression, delicate features, flowing light outfit

3. **Empty state illustration**: A cute illustration for when there are no messages yet — perhaps the GF character waving with a speech bubble "Say hi!"

4. **Onboarding decorative elements**: Floating hearts, sparkles, or stars that can be used as background decorations on the welcome and setup screens

---

### Design Notes

- All screens should feel connected as one cohesive app experience
- Navigation between main screens (Chat, Character, Settings) uses a bottom navigation bar with icons only (chat bubble, person, gear) — selected state is pink, unselected is muted purple
- Transitions should feel smooth — suggest slide transitions between onboarding screens, crossfade between main screens
- The overall feel should be: premium, intimate, warm, slightly mysterious, and emotionally engaging
- Avoid anything that feels clinical, corporate, or cold
- The character illustrations are the emotional anchor of the app — they should be beautiful, expressive, and feel alive
