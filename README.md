# AngelSustenance

A Minecraft 1.21 plugin that adds a **healthy eating and balanced meal system** to your server. Vanilla food still restores hunger normally, but players who rotate through a varied diet can build sustenance score, trigger balanced meal buffs, and optionally suffer penalties from repetitive eating.[web:107]

---

## Features

- Rolling food history tracking per player
- Category-based balanced meal detection in any order
- Repeated-food value reduction over time
- Configurable positive buffs for healthy eating
- Optional negative effects for repetitive diets
- Sidebar display for player sustenance status
- Per-world enable or disable support
- YAML persistence for player food history and score
- Custom food ID support for future integrations
- Config-driven category system instead of manually defining every combo

---

## Commands

| Command | Description |
|---|---|
| `/sustenance` | View your current sustenance status |
| `/sustenance status` | View your current sustenance status |
| `/sustenance check <player>` | View another online player's sustenance status |
| `/sustenance reload` | Reload the plugin config files |

---

## Permissions

| Permission | Default | Description |
|---|---|---|
| `angelsustenance.use` | Everyone | Access to sustenance status commands |
| `angelsustenance.admin` | OP | Access to admin reload commands |

---

## How the Sustenance System Works

The plugin listens for food consumption and tracks each player's recent meals using a rolling history. Paper exposes food consumption through `PlayerItemConsumeEvent`, which makes it suitable for a diet-history system like this.[web:107]

Instead of requiring hardcoded exact combinations, foods are grouped into categories such as:

- **CARB**
- **PROTEIN**
- **PRODUCE**
- **OTHER**

When a player's recent food history contains the required healthy categories inside the configured lookback window, the plugin marks that as a balanced meal and awards the configured buffs. Because the logic is category-based, players can eat foods in any order and still complete the meal pattern.[web:107]

---

## Repetition and Diet Value

The plugin also checks for repetition inside the player's recent history. Repeated use of the same food gradually reduces score gain, and if enabled, overly repetitive diets can trigger configurable penalty effects.

This gives you a system where vanilla food still works exactly as normal, but varied eating is rewarded and monotonous eating can be discouraged.

---

## Buffs and Stacking

Balanced meal rewards are configured through normal potion effect types and durations. Paper exposes potion effects through `PotionEffectType`, which allows the plugin to apply buffs such as Regeneration, Absorption, Saturation, Speed, or Resistance when a player meets the dietary conditions.[web:124]

Buff stacking is supported in this plugin design, so repeated healthy eating can extend existing effect duration instead of replacing it, depending on your config settings.[web:124]

---

## Sidebar Display

The plugin can show a right-side sidebar with the player's current diet score, balanced state, and most recent food. Paper scoreboards and objectives support sidebar displays for per-player plugin status panels.[web:137][web:14]

This can be disabled in config if you want the plugin to run quietly without a visible UI.[web:137]

---

## Configuration

`config.yml` is generated on first run. Example structure:

```yaml
general:
  sidebar-enabled: true
  save-interval-seconds: 60
  enabled-worlds: []
  use-all-worlds-when-empty: true
  history-size: 6
  repeat-penalty-threshold: 2
  repeat-penalty-enabled: true
  custom-food-support: true

balanced-meal:
  enabled: true
  required-categories:
    - CARB
    - PROTEIN
    - PRODUCE
  lookback-items: 4
  base-score-threshold: 3
  buff-duration-seconds: 600
  allow-stacking: true
```

Buffs and penalties are fully configurable:

```yaml
buffs:
  balanced:
    - type: REGENERATION
      duration-seconds: 30
      amplifier: 0
    - type: ABSORPTION
      duration-seconds: 120
      amplifier: 0

penalties:
  enabled: true
  trigger-duplicate-count: 3
  effects:
    - type: HUNGER
      duration-seconds: 45
      amplifier: 0
```

Food group definitions are also configurable so you do not need to hardcode every balanced meal manually:

```yaml
categories:
  CARB:
    - BREAD
    - BAKED_POTATO
  PROTEIN:
    - COOKED_BEEF
    - COOKED_CHICKEN
  PRODUCE:
    - APPLE
    - CARROT
    - SWEET_BERRIES
```

Custom foods can be mapped by custom ID for future support layers:

```yaml
custom-foods:
  protein_bar:
    category: PROTEIN
  fruit_mix:
    category: PRODUCE
  rice_bowl:
    category: CARB
```

---

## Installation

1. Drop the compiled `.jar` into your server's `plugins/` folder.
2. Start or restart your server.
3. Edit `plugins/AngelSustenance/config.yml` to tune food categories, buffs, penalties, sidebar behavior, and world support.
4. Restart the server or reload the plugin.

---

## Building

Requires Java 21 and Gradle.

```bash
./gradlew jar
```

Output jar will be in `build/libs/`.

---

## Dependencies

- [Paper API 1.21.11 PlayerItemConsumeEvent](https://jd.papermc.io/paper/1.21.11/org/bukkit/event/player/PlayerItemConsumeEvent.html) — used to detect consumed food items.[web:107]
- [Paper PotionEffectType API](https://jd.papermc.io/paper/1.21.11/org/bukkit/potion/PotionEffectType.html) — used for configurable buff and penalty effects.[web:124]
- [Paper Objective API](https://jd.papermc.io/paper/1.21.11/org/bukkit/scoreboard/Objective.html) — used for sidebar scoreboard output.[web:137]
- [Paper plugin.yml docs](https://docs.papermc.io/paper/dev/plugin-yml/) — confirms plugin metadata and `api-version: '1.21'` usage for Paper plugins.[web:7]
