---
name: Midnight Muse
colors:
  surface: '#181021'
  surface-dim: '#181021'
  surface-bright: '#3f3548'
  surface-container-lowest: '#120b1b'
  surface-container-low: '#201829'
  surface-container: '#241c2e'
  surface-container-high: '#2f2638'
  surface-container-highest: '#3a3144'
  on-surface: '#ecddf6'
  on-surface-variant: '#e1bdc8'
  inverse-surface: '#ecddf6'
  inverse-on-surface: '#362d3f'
  outline: '#a88892'
  outline-variant: '#594048'
  surface-tint: '#ffb0cc'
  primary: '#ffb0cc'
  on-primary: '#640038'
  primary-container: '#ff46a0'
  on-primary-container: '#580030'
  inverse-primary: '#b8006c'
  secondary: '#f9abff'
  on-secondary: '#570066'
  secondary-container: '#86039c'
  on-secondary-container: '#f7a0ff'
  tertiary: '#ffaed9'
  on-tertiary: '#610046'
  tertiary-container: '#e95cb4'
  on-tertiary-container: '#55003d'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffd9e4'
  primary-fixed-dim: '#ffb0cc'
  on-primary-fixed: '#3e0021'
  on-primary-fixed-variant: '#8d0051'
  secondary-fixed: '#ffd6fe'
  secondary-fixed-dim: '#f9abff'
  on-secondary-fixed: '#35003f'
  on-secondary-fixed-variant: '#7b008f'
  tertiary-fixed: '#ffd8ea'
  tertiary-fixed-dim: '#ffaed9'
  on-tertiary-fixed: '#3c002a'
  on-tertiary-fixed-variant: '#890064'
  background: '#181021'
  on-background: '#ecddf6'
  surface-variant: '#3a3144'
typography:
  display-lg:
    fontFamily: Nunito Sans
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  display-sm:
    fontFamily: Nunito Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg:
    fontFamily: Nunito Sans
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
  headline-sm:
    fontFamily: Nunito Sans
    fontSize: 20px
    fontWeight: '700'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.05em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  container-margin: 20px
  gutter: 16px
---

## Brand & Style

The design system is centered on the "Dark Romantic" aesthetic, tailored for an intimate, premium AI companion experience. It balances the depth of a midnight sky with the vibrant energy of neon pulses, evoking a sense of luxury, privacy, and emotional warmth.

The style is a hybrid of **Glassmorphism** and **Modern Corporate**, utilizing translucent layers and deep atmospheric blurs to create a sense of digital presence that feels tangible yet ethereal. The interface should feel like a high-end, private sanctuary. Visuals prioritize high-quality depth through radial glows and soft-lit surfaces rather than flat containers.

## Colors

The palette is anchored in a "Deep Midnight" foundation, moving away from pure blacks to a very dark violet-black. This ensures the primary "Hot Pink" accent feels like a glowing light source rather than just a flat color.

- **Backgrounds:** Use the vertical gradient from `#0D0015` to `#1A0A2E` for main views to provide a sense of vertical depth.
- **Accents:** The primary Hot Pink is reserved for high-intent actions and active states. Soft neon pink highlights are used for subtle UI feedback and notifications.
- **Typography:** Avoid pure white. Soft lavender and muted lilac maintain the romantic atmosphere and reduce eye strain in low-light environments.

## Typography

This design system uses a dual-font strategy. **Nunito Sans** provides a soft, rounded, and welcoming feel for all headings, reinforcing the "girlfriend" persona's warmth. **Inter** handles the functional body text, ensuring maximum legibility and a modern, high-tech feel for the AI interface.

For mobile-specific views, `display-lg` should scale down to `display-sm` to prevent text wrapping issues on smaller devices. Use `body-sm` with a weight of 500 for secondary information to maintain clarity against dark backgrounds.

## Layout & Spacing

The layout follows a fluid-first approach with a focus on centered, intimate content containers. 

- **Grid:** Use a 4-column grid for mobile and a 12-column grid for tablet/desktop. 
- **Rhythm:** An 8px linear scale drives the vertical rhythm.
- **Atmospheric Spacing:** Content should be given generous margins (24px+) to allow the background radial glows and glassmorphic effects to "breathe" without feeling cluttered.
- **Safe Areas:** Ensure interactive elements maintain a 44px minimum touch target, especially for primary chat actions.

## Elevation & Depth

Depth is communicated through light and transparency rather than heavy shadows.

- **Glassmorphism:** Secondary surfaces (like chat bubbles or sidebars) use a 10% opacity purple tint with a 20px backdrop blur. 
- **Tonal Layers:** The background is the lowest layer. Surface cards (`#1A0A2E`) sit above, and interactive elements/modals use the Elevated Surface (`#2D1B4E`).
- **Luminance:** Use soft pink or purple radial glows behind key characters or important CTAs to draw the eye.
- **Shadows:** When necessary, use a "Soft Purple" shadow: `0px 8px 24px rgba(74, 45, 110, 0.4)`. This creates a subtle lift that feels integrated with the purple theme.

## Shapes

The shape language is consistently rounded to evoke friendliness and comfort. 

- **Cards:** 16px radius for a balanced container look.
- **Buttons:** 24px radius (semi-pill) to make actions feel soft and inviting.
- **Inputs:** 28px radius (full-pill) for text entry fields, creating a distinct "capsule" look that differentiates them from structural cards.
- **Chat Bubbles:** Use a 16px radius with a sharp corner on the tail-side to indicate the speaker.

## Components

### Buttons
- **Primary:** Hot Pink (`#E91E8C`) background with Lavender White (`#F5E6FF`) text. Weight 700.
- **Secondary:** Ghost style with a Dark Purple (`#4A2D6E`) border and 24px radius. 

### Chat Bubbles
- **AI Response:** Glassmorphic background (10% tint) with Lavender White text.
- **User Message:** Deep Purple (`#9C27B0`) background with subtle inner glow.

### Input Fields
- Fully pill-shaped (28px).
- Dark Purple (`#1A0A2E`) background with a 1px border of `#4A2D6E`.
- On focus, the border glows with the Primary Hot Pink.

### Cards
- Use 16px rounding.
- Apply a subtle vertical gradient from Surface to Elevated Surface to give a metallic, premium feel.

### Chips/Tags
- Small, pill-shaped elements for interest tags or personality traits.
- Use the Secondary Accent (`#9C27B0`) at 20% opacity with a solid border of the same color.

### Navigation Bar
- Situated at the bottom, using a heavy backdrop blur (30px) and a subtle top border of `#4A2D6E`. Icons should glow in Primary Pink when active.