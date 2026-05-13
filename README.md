# mini-adventure
A text-based fantasy RPG built in JavaFX. You've been hired by the king to retrieve something stolen by a dragon. Classic quest. Except the king is secretly evil and the dragon isn't.

The story is maybe two pages of flavor text total. I'd genuinely encourage you to open MiniAdventure.java and scroll down to read it. World-building was not the priority here, but I hope my love for creative writing and humor comes across in the limited bit that's actually there.

**What I actually built**

The entire game runs on a linked-list scene graph. Every location, dialogue beat, and combat encounter is a node, with buttons that navigate the links between them and update state on screen and behind the scenes.

The core architectural challenge was node recycling: the scene graph has to be initialized once on first load, but turn-based combat requires cycling through the same nodes repeatedly while correctly reflecting updated state each time. Getting JavaFX to refresh SceneNodes at the right moment (and ensuring what was displayed matched what was actually happening internally) was the central engineering problem of this project.

**Combat System**

Turn-based, party-based combat with a surprising amount of depth for a solo project:

_Unique character AI_ — every party member and enemy has manually designed, distinct behavior. Characters have dialogue reactions to being buffed, hurt, healed, or when a character-specific mechanic triggers.
_Full status system_ — buffs, debuffs, and crowd control all apply, stack, and expire correctly across turns
_Player spells_ — damage, a protection spell granting temporary immunity, and a stun, each with working cooldowns
_Targeting_ — spells and attacks correctly target chosen enemies or allies
_Death handling_ — dead party members correctly disable turns and dialogue. You can still win if your party survives without you. Edge cases like temporary death states are handled without incorrectly triggering a loss.
_Party swap menu_ — functional pre-combat party management
_Back buttons_ — probably the most painful feature to implement. Changing your mind mid-turn correctly restores state without firing cooldowns, skipping turns, or corrupting buff and debuff timers.

A significant amount of time went into balancing character stats to make the combat actually feel fair.

**Source**

All logic lives in MiniAdventure.java. The full runnable JavaFX project isn't included (setup is a pain, trust me) but the architecture, combat system, and story are all readable directly in the source. This was built entirely for fun, and I never intended to finish it, but it was an absolute joy to work on.
