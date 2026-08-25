package com.oathfall.model;

/**
 * How {@code ObjectiveTracker} measures a Vow's progress. MANUAL means the
 * objective has no reliable client-side signal and the player ticks it off
 * themselves — the plugin never guesses at those.
 */
public enum GoalType
{
	/** Reach a level in a named skill. target = skill name, amount = level. */
	SKILL_LEVEL,
	/** Reach a total level. amount = total. */
	TOTAL_LEVEL,
	/** Kill count deltas parsed from the game's own kill-count messages. */
	KILL_COUNT,
	/** Completed clue scrolls of a tier. target = tier name, amount = count. */
	CLUE_COUNT,
	/** A quest reaching FINISHED. target = Quest enum name. */
	QUEST,
	/** An item appearing in inventory, equipment or bank. target = item name. */
	ITEM,
	/** Player-confirmed only. */
	MANUAL
}
