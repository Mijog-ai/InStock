---
name: Industrial Precision
colors:
  surface: '#f8f9ff'
  surface-dim: '#cbdbf5'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e5eeff'
  surface-container-high: '#dce9ff'
  surface-container-highest: '#d3e4fe'
  on-surface: '#0b1c30'
  on-surface-variant: '#45474c'
  inverse-surface: '#213145'
  inverse-on-surface: '#eaf1ff'
  outline: '#75777d'
  outline-variant: '#c5c6cd'
  surface-tint: '#545f73'
  primary: '#091426'
  on-primary: '#ffffff'
  primary-container: '#1e293b'
  on-primary-container: '#8590a6'
  inverse-primary: '#bcc7de'
  secondary: '#9d4300'
  on-secondary: '#ffffff'
  secondary-container: '#fd761a'
  on-secondary-container: '#5c2400'
  tertiary: '#001906'
  on-tertiary: '#ffffff'
  tertiary-container: '#003010'
  on-tertiary-container: '#00a64a'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d8e3fb'
  primary-fixed-dim: '#bcc7de'
  on-primary-fixed: '#111c2d'
  on-primary-fixed-variant: '#3c475a'
  secondary-fixed: '#ffdbca'
  secondary-fixed-dim: '#ffb690'
  on-secondary-fixed: '#341100'
  on-secondary-fixed-variant: '#783200'
  tertiary-fixed: '#6bff8f'
  tertiary-fixed-dim: '#4ae176'
  on-tertiary-fixed: '#002109'
  on-tertiary-fixed-variant: '#005321'
  background: '#f8f9ff'
  on-background: '#0b1c30'
  surface-variant: '#d3e4fe'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  title-lg:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
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
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  code-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.05em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 4px
  xs: 8px
  sm: 12px
  md: 16px
  lg: 24px
  xl: 32px
  touch-target: 48px
  gutter: 16px
  container-margin: 24px
---

## Brand & Style

The design system is engineered for high-velocity industrial environments where legibility, speed of recognition, and physical interaction reliability are paramount. The personality is **utilitarian, authoritative, and precise**. It draws from **Corporate/Modern** aesthetics but integrates **Tactile** elements to provide haptic-like visual feedback for workers operating in high-pressure warehouse settings.

The interface prioritizes information density without sacrificing clarity. It utilizes a "Utility-First" approach, ensuring that every element serves a functional purpose, reducing cognitive load during long shifts.

**Design Style: Clean Industrial Dashboard**
- **Light Theme Efficiency:** High-clarity backgrounds to reduce eye strain under warehouse lighting.
- **Tactile Feedback:** Interactive elements use subtle inner shadows and depth to mimic physical buttons, ensuring the user knows exactly when a "press" has been registered.
- **High-Contrast Indicators:** Critical data points use saturated status colors to remain legible even at a distance or on lower-quality handheld displays.

## Colors

This color palette is grounded in professional stability with high-visibility accents for operational cues.

- **Primary (Slate Blue):** Used for structural navigation, headers, and primary branding. It provides a "heavy" anchor for the layout.
- **Accent (Orange):** Reserved for primary actions, current selections, and focus states. It ensures that the "next step" is always obvious.
- **Semantic Palette:** Green, Red, and Amber are used strictly for status reporting (Success, Error, Warning). These colors must maintain a contrast ratio of at least 4.5:1 against the background.
- **Neutrals:** A range of Slate grays is used for borders, secondary text, and inactive states to maintain the industrial feel.

## Typography

The design system exclusively utilizes **Inter** to ensure maximum legibility across various screen resolutions, including ruggedized tablets and handheld scanners.

- **Scale:** Sizes are slightly oversized compared to standard SaaS products to accommodate "at-a-glance" reading from a distance of 2-3 feet.
- **Hierarchy:** Use `Title-LG` for card headers and `Label-MD` (All Caps) for metadata and table headers to create a distinct visual break from content.
- **Numerical Data:** For inventory counts and grid coordinates, use `Medium` or `Semi-Bold` weights to ensure numbers are never misread.

## Layout & Spacing

The layout uses a **Fluid Grid** system designed for 12 columns on desktop and 4 columns on mobile/handheld devices. 

- **Touch Safety:** A minimum touch target of `48px` is enforced for all interactive elements to accommodate gloved hands or rapid movement.
- **The 4px Baseline:** All spacing and sizing must be a multiple of `4px` to maintain a strict industrial rhythm.
- **Data Tables:** Use `sm` (12px) padding for high-density tables and `md` (16px) for standard workflows.
- **Breakpoints:**
  - **Mobile:** < 600px (Single column stacked cards)
  - **Tablet:** 600px - 1024px (2-column layout for dashboard)
  - **Desktop:** > 1024px (Full 12-column grid with persistent sidebar)

## Elevation & Depth

This design system uses a hybrid approach of **Tonal Layers** and **Tactile Depth** to indicate interactivity.

- **Base Surface:** Background color (#F8FAFC).
- **Cards/Containers:** Elevated via a 1px border (#E2E8F0) and a subtle 2px drop shadow.
- **Interactive Cells (The "Press" Look):**
  - **Default:** Subtle 1px bottom border for a "raised" appearance.
  - **Hover:** Slightly darkened background and enhanced outer shadow.
  - **Pressed/Active:** Uses an `inner-shadow` (inset) to create a "pushed-in" physical effect, signifying the bin or shelf is selected.
- **Modals:** Use a heavy backdrop blur (8px) and a large 24px shadow to focus attention on critical inputs.

## Shapes

The design system utilizes **Rounded (Level 2)** shapes to balance industrial rigidity with modern software ergonomics.

- **Standard Elements:** Buttons, inputs, and shelf grid cells use an **8px (0.5rem)** radius.
- **Large Containers:** Zone cards and modals use a **16px (1rem)** radius to clearly frame content groups.
- **Status Badges:** Use a fully rounded/pill shape (999px) to distinguish them from functional buttons.

## Components

### Navigation Headers
The primary header uses the Dark Slate-Blue (#1E293B) background. Icons should be white with a 24px bounding box. The header should be fixed to the top of the viewport to maintain global navigation access.

### Zone Cards & Progress Bars
Cards contain "Zone" summaries. Progress bars for "Capacity" should use a 12px height with rounded ends. The track color is #E2E8F0, while the fill uses Primary or Success colors depending on the metric.

### Interactive Shelf Grid
The core of the WMS. Each cell represents a warehouse location.
- **States:** 
  - *Occupied:* Light gray fill with a #64748B border.
  - *Empty:* White fill.
  - *Selected:* Orange (#F97316) border with a subtle inner glow.
  - *Action:* On click, the cell must show an `inset` shadow.

### Industrial-Style Modals
Modals occupy 90% of screen width on mobile and 600px on desktop. 
- **Header:** Slate-blue top border (4px).
- **Inputs:** Extra-large (56px height) with 18px font size for rapid entry.
- **Buttons:** Primary action buttons should span the full width of the modal bottom on mobile devices.

### Status Badges
High-contrast indicators using semantic colors. Text should be bold and all-caps (Label-MD).
- **Format:** Background at 15% opacity of the semantic color with a 100% opacity text color of the same hue for maximum accessibility.