# MiBandMCP V1 UI Direction

## 1. Design Intent

MiBandMCP should feel like a calm Android system utility, not a fitness platform and not a developer console.

The UI should communicate:

- trust
- clarity
- low friction
- lightweight control

The app should be understandable for normal users while still feeling precise and technical.

## 2. Visual Personality

Working style name:

- Calm Utility

Keywords:

- quiet
- precise
- trustworthy
- lightweight

Avoid:

- sporty energy
- social fitness aesthetics
- dense dashboard aesthetics
- hacker-terminal aesthetics
- playful consumer wellness styling

## 3. Layout Principles

- Prefer one clear vertical flow per screen
- Keep navigation shallow
- Use cards sparingly and intentionally
- Surface the most important state first
- Let actions stay near the state they affect
- Avoid decorative sections without functional value

## 4. Screen Structure

V1 should have two screens:

### Home

Purpose:

- show service state
- show data availability
- show current health summary
- expose primary actions

Recommended order:

1. app title and subtle status summary
2. MCP service card
3. Gadgetbridge / data source card
4. health summary card
5. primary actions

### Settings

Purpose:

- configure basic service behavior
- configure data access
- expose diagnostics entry points

Recommended sections:

- network
- data source
- refresh behavior
- language/about

## 5. Component Style

### Cards

- medium corner radius
- soft tonal separation rather than heavy shadow
- compact but breathable spacing

### Buttons

- one prominent primary action at a time
- secondary actions should be quieter
- destructive or stop actions should not dominate visually

### Status Chips / Badges

- use for service state, sync state, and data freshness
- color should support state, not carry the full meaning alone

### Lists

- settings should use standard Material list patterns
- keep descriptions short and plain

## 6. Typography

- Use Material 3 type scale defaults as a base
- Prioritize readable numeric presentation for health values
- Do not overuse large display styles
- Labels should be short
- Body text should stay concise

Numeric data should feel stable and easy to scan:

- steps: large and prominent
- heart rate: prominent, but secondary to service state
- sleep summary: compact and readable

## 7. Color Direction

Primary direction:

- cool green-teal accent
- soft neutral surfaces
- non-harsh background

Intent:

- health-adjacent without looking like a sports tracker
- technical without looking cold or sterile

### Suggested role behavior

- primary: teal family
- success: soft green
- warning: amber
- error: muted brick red
- background: off-white or light cool gray
- surface: slightly elevated neutral tint

### Theme behavior

- light theme is the primary designed mode
- dark theme should exist and follow system settings
- dynamic color may be supported, but the default static palette should still look intentional

## 8. Motion

V1 motion should be minimal.

Allowed motion:

- subtle screen fade/slide on entry
- low-key refresh transition for changing values
- smooth service-state color/icon transition

Avoid:

- bouncing
- spring-heavy transitions
- ornamental motion

## 9. Iconography

- Use Material Symbols only in V1
- Prefer outlined or rounded variants consistently
- Use icons to reinforce meaning, not to decorate every row

## 10. Bilingual UX

V1 should support Chinese and English from the start.

Rules:

- Use system locale
- Do not show both languages at once in normal UI
- Keep copy short so both languages fit well
- Prefer simple nouns and verbs over product jargon

## 11. Content Tone

Copy should be:

- direct
- calm
- plain
- non-technical when shown to normal users

Good examples:

- Service running
- Last sync 2 minutes ago
- Gadgetbridge not configured
- Refresh now

Avoid:

- verbose diagnostics in the main UI
- protocol jargon in user-facing copy
- overly cheerful product language

## 12. Home Screen Content Contract

The home screen must make these questions answerable at a glance:

1. Is the MCP service running?
2. What address can I use?
3. Is Gadgetbridge connected well enough to provide data?
4. Are the shown values recent?
5. What are today's key values?
6. What action should I take next if something is wrong?

## 13. Settings Screen Content Contract

The settings screen must make these tasks easy:

1. Confirm or update LAN port
2. Grant or re-grant data access
3. See refresh behavior
4. Understand current app and integration state

## 14. Design Constraint

If a UI choice improves visual novelty but harms clarity or makes the app feel larger than it is, reject it.
