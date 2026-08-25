package com.oathfall.deck;

import com.oathfall.model.Binding;
import com.oathfall.model.Era;
import com.oathfall.model.GoalType;
import com.oathfall.model.Length;
import com.oathfall.model.Vow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The five Era decks, matching the published Oathfall codex card for card.
 * Each Vow carries a machine-readable goal so the tracker can follow progress
 * without the player typing anything.
 */
public final class Decks
{
	private static final Map<Era, List<Vow>> DECKS = new EnumMap<>(Era.class);
	private static final Map<String, Vow> BY_ID = new HashMap<>();

	private Decks()
	{
	}

	static
	{
		build(Era.WAKING, new Object[][]{
			{"Reach 43 Prayer and unlock Protect from Melee", Binding.GROUNDED, Length.STANDARD, GoalType.SKILL_LEVEL, "PRAYER", 43},
			{"Complete Dragon Slayer I", Binding.STARVING, Length.STANDARD, GoalType.QUEST, "DRAGON_SLAYER_I", 1},
			{"Bury 200 big bones from Hill Giants", Binding.BAREFOOT, Length.SHORT, GoalType.MANUAL, "", 200},
			{"Mine and bank 200 iron ore", Binding.SUNLESS, Length.STANDARD, GoalType.ITEM, "Iron ore", 200},
			{"Kill Obor", Binding.COLD_IRON, Length.STANDARD, GoalType.KILL_COUNT, "Obor", 1},
			{"Complete Waterfall Quest and claim the reward", Binding.BLIND, Length.STANDARD, GoalType.QUEST, "WATERFALL_QUEST", 1},
			{"Complete five Beginner clue scrolls", Binding.NAMED, Length.SHORT, GoalType.CLUE_COUNT, "beginner", 5},
			{"Reach 40 Attack, Strength and Defence", Binding.ONE_HANDED, Length.STANDARD, GoalType.MANUAL, "", 3},
			{"Cook and bank 100 trout", Binding.TITHED, Length.SHORT, GoalType.ITEM, "Trout", 100},
			{"Survive thirty minutes above level 20 Wilderness", Binding.UNBROKEN, Length.SHORT, GoalType.MANUAL, "", 1},
			{"Complete the Cook's Assistant to Rune Mysteries chain", Binding.SUNLESS, Length.SHORT, GoalType.QUEST, "RUNE_MYSTERIES", 1},
			{"Reach 40 Ranged using only self-made arrows", Binding.COLD_IRON, Length.STANDARD, GoalType.SKILL_LEVEL, "RANGED", 40},
		});

		build(Era.WANDERING, new Object[][]{
			{"Open a full Barrows chest", Binding.GROUNDED, Length.STANDARD, GoalType.KILL_COUNT, "Barrows Chest", 1},
			{"Kill Sarachnis ten times", Binding.STARVING, Length.STANDARD, GoalType.KILL_COUNT, "Sarachnis", 10},
			{"Reach 70 Slayer", Binding.RUSTED, Length.STANDARD, GoalType.SKILL_LEVEL, "SLAYER", 70},
			{"Complete Monkey Madness I", Binding.BLIND, Length.STANDARD, GoalType.QUEST, "MONKEY_MADNESS_I", 1},
			{"Complete five Medium clue scrolls", Binding.ONE_HANDED, Length.STANDARD, GoalType.CLUE_COUNT, "medium", 5},
			{"Kill the Giant Mole fifteen times", Binding.COLD_IRON, Length.STANDARD, GoalType.KILL_COUNT, "Giant Mole", 15},
			{"Complete the Fremennik Trials and claim your name", Binding.NAMED, Length.STANDARD, GoalType.QUEST, "THE_FREMENNIK_TRIALS", 1},
			{"Reach 61 Crafting for the Slayer helmet", Binding.SUNLESS, Length.STANDARD, GoalType.SKILL_LEVEL, "CRAFTING", 61},
			{"Obtain a Dragon defender", Binding.UNBROKEN, Length.STANDARD, GoalType.ITEM, "Dragon defender", 1},
			{"Complete the Varrock Medium diary", Binding.TITHED, Length.STANDARD, GoalType.MANUAL, "", 1},
			{"Kill Bryophyta and bank the staff", Binding.BAREFOOT, Length.STANDARD, GoalType.ITEM, "Bryophyta's staff", 1},
			{"Reach 70 Magic and unlock Ancient Magicks", Binding.GROUNDED, Length.STANDARD, GoalType.SKILL_LEVEL, "MAGIC", 70},
		});

		build(Era.ASCENT, new Object[][]{
			{"Earn the Fire cape", Binding.COLD_IRON, Length.LONG, GoalType.ITEM, "Fire cape", 1},
			{"Kill Zulrah fifty times", Binding.RUSTED, Length.LONG, GoalType.KILL_COUNT, "Zulrah", 50},
			{"Complete the Gauntlet", Binding.BLIND, Length.STANDARD, GoalType.KILL_COUNT, "Gauntlet", 1},
			{"Obtain any Armadyl unique", Binding.STARVING, Length.LONG, GoalType.MANUAL, "", 1},
			{"Reach 85 Slayer", Binding.TITHED, Length.LONG, GoalType.SKILL_LEVEL, "SLAYER", 85},
			{"Kill Vorkath twenty-five times", Binding.ONE_HANDED, Length.STANDARD, GoalType.KILL_COUNT, "Vorkath", 25},
			{"Complete a solo Chambers of Xeric", Binding.NAMED, Length.LONG, GoalType.KILL_COUNT, "Chambers of Xeric", 1},
			{"Complete Song of the Elves", Binding.GROUNDED, Length.LONG, GoalType.QUEST, "SONG_OF_THE_ELVES", 1},
			{"Complete three Hard clue scrolls", Binding.SUNLESS, Length.STANDARD, GoalType.CLUE_COUNT, "hard", 3},
			{"Kill the Abyssal Sire twenty times", Binding.UNBROKEN, Length.STANDARD, GoalType.KILL_COUNT, "Abyssal Sire", 20},
			{"Obtain a Dragon warhammer or Voidwaker shard", Binding.BAREFOOT, Length.LONG, GoalType.ITEM, "Dragon warhammer", 1},
			{"Reach 90 Ranged", Binding.SILENT, Length.LONG, GoalType.SKILL_LEVEL, "RANGED", 90},
		});

		build(Era.CRUCIBLE, new Object[][]{
			{"Complete the Corrupted Gauntlet", Binding.COLD_IRON, Length.LONG, GoalType.KILL_COUNT, "Corrupted Gauntlet", 1},
			{"Complete a Theatre of Blood raid", Binding.NAMED, Length.LONG, GoalType.KILL_COUNT, "Theatre of Blood", 1},
			{"Kill Nex thirty times", Binding.RUSTED, Length.LONG, GoalType.KILL_COUNT, "Nex", 30},
			{"Complete Tombs of Amascut at level 300 or above", Binding.BLIND, Length.LONG, GoalType.KILL_COUNT, "Tombs of Amascut", 1},
			{"Reach 99 in your lowest combat skill", Binding.TITHED, Length.LONG, GoalType.MANUAL, "", 1},
			{"Complete five Elite clue scrolls", Binding.GROUNDED, Length.STANDARD, GoalType.CLUE_COUNT, "elite", 5},
			{"Kill Cerberus fifty times", Binding.STARVING, Length.LONG, GoalType.KILL_COUNT, "Cerberus", 50},
			{"Complete the Western Provinces Elite diary", Binding.ONE_HANDED, Length.LONG, GoalType.MANUAL, "", 1},
			{"Complete ten Chambers of Xeric raids", Binding.SILENT, Length.LONG, GoalType.KILL_COUNT, "Chambers of Xeric", 10},
			{"Obtain a Voidwaker piece", Binding.BAREFOOT, Length.LONG, GoalType.MANUAL, "", 1},
			{"Kill the Alchemical Hydra one hundred times", Binding.UNBROKEN, Length.LONG, GoalType.KILL_COUNT, "Alchemical Hydra", 100},
			{"Reach 90 Slayer and unlock every task extension", Binding.SUNLESS, Length.LONG, GoalType.SKILL_LEVEL, "SLAYER", 90},
		});

		build(Era.RECKONING, new Object[][]{
			{"Complete the Inferno", Binding.UNBROKEN, Length.LONG, GoalType.ITEM, "Infernal cape", 1},
			{"Absolve every Scar you currently carry", Binding.TITHED, Length.LONG, GoalType.MANUAL, "", 1},
			{"Complete Theatre of Blood Hard Mode", Binding.COLD_IRON, Length.LONG, GoalType.KILL_COUNT, "Theatre of Blood Hard Mode", 1},
			{"Kill an Awakened Desert Treasure II boss", Binding.BLIND, Length.LONG, GoalType.MANUAL, "", 1},
			{"Complete Tombs of Amascut at level 500", Binding.NAMED, Length.LONG, GoalType.KILL_COUNT, "Tombs of Amascut", 1},
			{"Obtain any boss pet", Binding.GROUNDED, Length.LONG, GoalType.MANUAL, "", 1},
			{"Reach 2000 total level", Binding.RUSTED, Length.LONG, GoalType.TOTAL_LEVEL, "", 2000},
			{"Earn the Quest cape", Binding.SUNLESS, Length.LONG, GoalType.ITEM, "Quest point cape", 1},
			{"Reach Colosseum wave twelve", Binding.STARVING, Length.LONG, GoalType.MANUAL, "", 12},
			{"Slay every Herald that ever escaped you", Binding.UNBROKEN, Length.LONG, GoalType.MANUAL, "", 1},
			{"Complete Chambers of Xeric Challenge Mode", Binding.SILENT, Length.LONG, GoalType.KILL_COUNT, "Chambers of Xeric Challenge Mode", 1},
			{"Achieve a maximum hit with three combat styles in one trip", Binding.BAREFOOT, Length.LONG, GoalType.MANUAL, "", 3},
		});
	}

	private static void build(Era era, Object[][] rows)
	{
		List<Vow> vows = new ArrayList<>(rows.length);
		for (int i = 0; i < rows.length; i++)
		{
			Object[] r = rows[i];
			Vow vow = new Vow(era, i, (String) r[0], (Binding) r[1], (Length) r[2],
				(GoalType) r[3], (String) r[4], (Integer) r[5]);
			vows.add(vow);
			BY_ID.put(vow.getId(), vow);
		}
		DECKS.put(era, Collections.unmodifiableList(vows));
	}

	public static List<Vow> forEra(Era era)
	{
		return DECKS.getOrDefault(era, Collections.emptyList());
	}

	public static Vow byId(String id)
	{
		return id == null ? null : BY_ID.get(id);
	}

	/**
	 * Deal a table of three: two from the Era deck plus the Audience's Card in
	 * the third seat. {@code audienceCardId} is whatever the community voted in
	 * last episode; a null or unknown id falls back to a third random draw.
	 */
	public static List<Vow> deal(Era era, String audienceCardId, Random random)
	{
		List<Vow> pool = new ArrayList<>(forEra(era));
		Vow audience = byId(audienceCardId);
		if (audience != null)
		{
			pool.remove(audience);
		}

		Collections.shuffle(pool, random);

		List<Vow> hand = new ArrayList<>(3);
		hand.add(pool.get(0));
		hand.add(pool.get(1));
		hand.add(audience != null ? audience : pool.get(2));
		return hand;
	}
}
