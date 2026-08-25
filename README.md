# Loadouts
Loadouts is a [Meteor Client](https://meteorclient.com) addon that remembers how your inventory should look. Save your current layout as a loadout and Loadouts sorts your items back into place whenever you want. If something is missing it tells you exactly what instead of silently failing.

Think of it as a saved inventory snapshot for things like pvp kits, mining setups or your daily driver layout.

## Features
- Covers the whole inventory screen: hotbar, backpack rows, armor and offhand
- Save as many loadouts as you want, stored as small JSON files
- Inventory style editor with a searchable item picker
- One click apply that moves items into their designated slots
- Flexible item matching: any planks works for oak planks, iron sword works for diamond sword
- Chat and toast notifications listing missing items
- Full `.loadout` command set plus an apply keybind

## Requirements
- Minecraft 26.2
- Fabric Loader 0.19+
- Java 25
- Meteor Client 26.2 snapshot

## Installation
1. Download the latest JAR from the [Releases](https://github.com/PacketByte/Minecraft-Loadouts/releases) page.
2. Place the JAR in the `mods` folder of your Minecraft installation.
3. Make sure Meteor Client is installed in the same `mods` folder.
4. Launch the game with the Fabric loader.

## Quick start
1. Join a world and arrange your inventory the way you like it.
2. Run `.loadout save mykit`.
3. Mess up your inventory (it happens).
4. Run `.loadout apply mykit` and watch everything snap back into place.
5. Anything you do not have right now shows up as `Missing: ...` in chat.

## Meteor menu
Open the Meteor GUI (right shift by default) and you will find a `Loadouts` tab next to Modules, Friends and Macros. It lists every saved loadout with buttons right there:

- `Apply` sorts your items into that loadout
- `Edit` opens the full editor screen
- the minus button deletes the loadout after a confirm click

At the bottom there is a name box plus a `Capture current` button, which snapshots your inventory into a fresh loadout without typing any commands.

## Commands
| Command | Description |
| --- | --- |
| `.loadout save <name>` | Snapshot your current inventory into a loadout |
| `.loadout apply [name]` | Sort items into place, defaults to the active loadout |
| `.loadout edit [name]` | Open the editor GUI |
| `.loadout delete <name>` | Delete a loadout |
| `.loadout list` | Show all loadouts |
| `.loadout set <name>` | Set the active loadout used by the keybind |

## Editor
Open it with `.loadout edit`. It shows your saved loadout on top and your live inventory below.

- Left click an item in the results panel to put it into the selected slot
- Drag items between slots to rearrange them
- Right click a slot to clear it
- Capture copies your current inventory into the grid
- Save writes the grid under the name in the text field
- Apply runs the sort and closes the editor

The search panel lists every item in the game, scroll to page through it.

## Settings
- `active-loadout`: which loadout the apply keybind uses
- `notify-mode`: how you get notified about missing items, `Chat`, `Toast`, `Both` or `Off`
- `report-substitutions`: mention when a similar item was sorted into a slot instead of the exact one
- `apply-bind`: press to apply the active loadout

## How matching works
Loadouts matches items by type first. If the exact item is not in your inventory it falls back to a loose family match, so birch planks can fill an oak planks slot and an iron pickaxe can fill a diamond pickaxe slot. Quantities do not matter, three planks are enough to claim a plank slot.

Items are never taken from slots that already hold what they are supposed to hold, so applying twice is safe.

## Storage
Loadouts live in `<game folder>/meteor-client/loadouts/` as one JSON file per loadout. Feel free to copy them between machines.

## Notes
- Applying only works while no chest or other container is open
- The sort clicks through your real inventory, so servers see normal inventory interactions

## Credits
- [Meteor Client](https://github.com/MeteorDevelopment/meteor-client), the hack client this addon extends.
- The [Meteor Addon API](https://github.com/MeteorDevelopment/meteor-addon-template), which provides the module and command systems.

## License
This project is released under the [CC0 1.0 Universal](LICENSE) license.
