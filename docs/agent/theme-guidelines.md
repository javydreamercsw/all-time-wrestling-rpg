# Theme-Awareness Guidelines (Light/Dark Mode)

How to write UI code that renders correctly in every theme the app supports:
`light`, `dark`, and the custom `retro`, `neon`, and `high-contrast` themes
(see `ThemeService`). Motivated by the ATW-j4r1 audit, which found connector
lines permanently black because a script read a non-existent Lumo token.

## The one rule

**Never hardcode a color in Java UI code or embedded JS.** Reference a Lumo
token instead — tokens carry `light-dark()` variants that flip automatically
when the theme attribute changes.

## Choosing the right token

|                 Need                  |                                                                     Use                                                                     |
|---------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| Text on background                    | `var(--lumo-body-text-color)`, `--lumo-secondary-text-color`, `--lumo-tertiary-text-color`                                                  |
| "Ink" color for lines/borders/strokes | `var(--lumo-contrast)` (flips shade↔tint) — NOT `--lumo-contrast-color`, which **does not exist** in Lumo                                   |
| Subtle borders/dividers               | `var(--lumo-contrast-10pct)` / `--lumo-contrast-20pct`                                                                                      |
| Card/surface background               | `var(--lumo-base-color)`                                                                                                                    |
| Semantic accent                       | `--lumo-error-color`, `--lumo-success-color`, `--lumo-warning-color`, `--lumo-primary-color` (plus `-10pct`/`-50pct` tints for backgrounds) |
| Text on a colored fill                | `--lumo-error-contrast-color`, `--lumo-primary-contrast-color`, etc. (scoped variants only)                                                 |

In Java, prefer `LumoUtility.TextColor.*` / `LumoUtility.Background.*` classes
over inline `getStyle().set(...)` when a utility class exists.

## Embedded JS (executeJs scripts)

`getStyle()`-equivalent tokens do not exist in JS-injected DOM — read them:

```js
const color = getComputedStyle(root).getPropertyValue('--lumo-contrast').trim();
```

Never hardcode `#000`/`#fff` as anything more than a last-resort fallback.
Example: `TournamentBracketComponent.CONNECTOR_SCRIPT`.

## What is exempt

- **Elevation shadows** — `rgba(0,0,0,alpha)` is standard in both themes.
- **Chart series colors** — categorical data colors chosen for contrast against
  any background.
- **Print/PDF stylesheets** — fixed paper output, no theme.
- **Identity badges** (e.g. `wrestler-tier-*`) — saturated brand colors paired
  with explicit white/black text; theme-independent by design.

## Custom themes

Every `[theme~="..."]` block in `frontend/themes/default/styles.css` must
declare **`color-scheme`** explicitly. Lumo's `:root` sets
`color-scheme: light` and `[theme~="dark"]` flips it to dark; a custom theme
that omits the declaration inherits the root default, and every `light-dark()`
token resolves to that variant — the neon theme rendered light-variant borders
on a near-black background until it declared `color-scheme: dark`.

A dark-destined custom theme needs: `color-scheme: dark` plus full overrides of
`--lumo-base-color`, the text-color trio, and the `--lumo-contrast-Npct` scale.

## Checklist for new UI code

1. No hex/rgba colors in Java or JS — Lumo tokens only (or the exemptions above)
2. New custom theme? Declare `color-scheme` and override the full token set
3. Verify the view in both `light` and `dark` (set `document.documentElement`
   `theme` attribute; no restart needed) before merging UI changes
4. Suspicious a token doesn't exist? Check
   `node_modules/@vaadin/vaadin-lumo-styles/src/props/color.css` — reading a
   non-existent token silently falls back to whatever default you supplied

