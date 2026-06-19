<p align="center">
<img src="https://raw.githubusercontent.com/TetraTheta/FarmManager/main/.etc/farmmanager_icon.png" alt="FarmManager Logo">
</p>

# FarmManager
**FarmManager protects and replants crops in selected WorldGuard regions and customizes composters on Paper servers**

![Version](https://img.shields.io/modrinth/v/farmmanager?style=for-the-badge&label=Plugin%20Version)
![Game Version](https://img.shields.io/modrinth/game-versions/farmmanager?style=for-the-badge&label=Minecraft%20Version)<br>
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/farmmanager?style=for-the-badge&label=Modrinth%20Downloads)](https://modrinth.com/plugin/farmmanager)

[![Discord](https://img.shields.io/discord/1514516278226845726?style=for-the-badge)](https://discord.gg/eS8tCEkecp)

## Introduction

FarmManager helps server owners turn selected WorldGuard regions into managed farm areas on Paper servers with WorldEdit and WorldGuard installed. Inside watched regions, immature crops can be protected from accidental breaking, while mature crops can be harvested, delivered to the player, and replanted automatically.

Crop protection, crop harvesting, and farmland trampling protection only act in WorldGuard regions that you explicitly add with `/fm region add`. This keeps ordinary survival farming unchanged outside managed farm areas and lets each server decide exactly where automatic crop behavior belongs.

FarmManager also applies configurable composter rules server-wide. Server owners can add new compostable items or override the compost chance of existing compostable items for both player right-clicks and hopper automation.

By default, FarmManager supports wheat, carrots, potatoes, beetroots, and nether wart for crop management, and rotten flesh as a custom composter item. Server owners can disable individual crop materials in the configuration, adjust Creative mode behavior for protection and harvesting separately, choose how full inventories are handled, protect farmland from trampling, allow sneaking players to bypass immature crop protection, and configure composter item chances.

Use `/fm reload` after editing the configuration to apply changes without restarting the server.

## Configuration

```yml
# Temporary bypass rules.
bypass:
  # Allows sneaking players to break immature crops even when protection is enabled.
  sneak-to-break-immature: true

# Custom composter behavior applied server-wide.
composter:
  # Handles configured items with FarmManager's custom composter chance rules.
  enabled: true
  # Bukkit Material names and their level increase chance from 0.0 to 1.0.
  # Example: 0.65 means 65%, while 1.0 means always.
  # Level 0 composters always increase to level 1 when a configured item is accepted.
  items:
    POISONOUS_POTATO: 0.65
    ROTTEN_FLESH: 0.65

# Bukkit Material names that are allowed to be handled as crops.
# Material.matchMaterial() is used, then FarmManager keeps only supported crop descriptors.
crops:
  materials:
    - BEETROOTS
    - CARROTS
    - NETHER_WART
    - POTATOES
    - WHEAT

# Automatic harvest and replant behavior for mature crops.
harvest:
  # Adds drops to the player's inventory before applying the overflow policy.
  add-to-inventory: true
  # Applies automatic harvest and replanting to Creative mode players.
  creative: false
  # Enables custom drop handling and next-tick replanting.
  enabled: true
  # Handles drops that do not fit into the inventory.
  # drop: keep inserted items and drop only the leftovers at the broken crop.
  # discard: keep inserted items and delete only the leftovers.
  # keep: if any item would not fit, drop the whole harvest at the broken crop instead.
  overflow: drop

# Server-wide language code used by bundled files under languages/.
language: 'ko'

# Notification behavior for repeated gameplay messages.
notification:
  # Player-facing gameplay notification location.
  # chat: sends notifications as chat messages.
  # action-bar: sends notifications above the player's hotbar.
  channel: action-bar
  # Chat notifications are throttled per player and message key. Action bar and command replies are never throttled.
  chat-cooldown-ticks: 40

# Protection rules inside watched regions.
protection:
  farmland:
    trampling:
      # Cancels entity trampling that would turn farmland into dirt inside watched regions.
      enabled: true
  immature:
    # Applies immature crop protection to Creative mode players.
    creative: false
    # Cancels immature crop breaks inside watched regions.
    enabled: true

# WorldGuard regions where FarmManager should adjust crop breaking.
# Entries are normalized as "world:region". Commands may add bare region names for a player's current world.
regions:
  watched: []
```

FarmManager validates recoverable configuration values during startup and reload.
- Invalid overflow policies fall back to `drop`.
- Invalid notification channels fall back to `action-bar`.
- Unsupported crop materials are removed from `crops.materials`.
- Invalid composter materials are removed from `composter.items`, while chances outside `0.0` to `1.0` are clamped to the nearest valid value.
- Watched regions that no longer exist in WorldGuard are removed from the saved list.

## Commands

- `/fm`: Lists watched WorldGuard regions.
- `/fm region`: Lists watched WorldGuard regions.
- `/fm region list`: Lists watched WorldGuard regions.
- `/fm region add <region>`: Adds a WorldGuard region to the watched list.
- `/fm region remove <region>`: Removes a WorldGuard region from the watched list.
- `/fm region delete <region>`: Removes a WorldGuard region from the watched list.
- `/fm region del <region>`: Removes a WorldGuard region from the watched list.
- `/fm reload`: Reloads configuration from disk.

Region arguments can be written as `world:region`. Players may also use a bare region name for their current world. Console senders can use a bare region name only when exactly one loaded world has that region ID.

## Technical Notes

FarmManager listens for crop block breaks at high priority and ignores events already cancelled by other plugins. It checks the broken block against the configured crop registry, then verifies that the block location is inside one of the watched WorldGuard regions.

Immature crop protection cancels the break before the crop is destroyed. If `bypass.sneak-to-break-immature` is enabled, sneaking players can still break immature crops intentionally.

Farmland trampling protection cancels entity-caused farmland-to-dirt changes inside watched regions when `protection.farmland.trampling.enabled` is enabled. This cancellation is silent and does not send a chat notification.

For mature crops, FarmManager calculates Bukkit drops using the player's held tool and player context, disables the original block drops, then finishes the harvest on the next tick only if the crop block actually became air. One replant item is consumed from the calculated drops before the crop is planted again at age zero.

Custom composter handling is server-wide, not limited to watched WorldGuard regions. When a configured item is inserted by player right-click or hopper, FarmManager cancels the vanilla insertion and applies the configured chance. A level `0` composter always increases to level `1` when it accepts a configured item. Levels `1` through `6` use the configured chance, failed rolls still consume one item, and a successful fill from level `6` makes the composter ready for bone meal. Items not listed under `composter.items` keep their vanilla composter behavior.

Supported crop materials:

- `WHEAT`
- `CARROTS`
- `POTATOES`
- `BEETROOTS`
- `NETHER_WART`

Permissions:

- `farmmanager.admin`: Allows managing watched regions and reloading configuration.
