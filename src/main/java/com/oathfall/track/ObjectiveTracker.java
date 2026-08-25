package com.oathfall.track;

import com.oathfall.model.GoalType;
import com.oathfall.model.Vow;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InventoryID;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Measures how far along a sworn Vow is, from signals the client already
 * produces. Counting objectives are measured as a delta against the baseline
 * captured when the Vow was sworn, so previous progress never counts twice.
 */
public class ObjectiveTracker
{
	/** "Your Zulrah kill count is: 143." and the Barrows/raids variants. */
	private static final Pattern KILL_COUNT = Pattern.compile(
		"Your (?:completed )?(.+?) (?:kill )?count is: <col=[^>]+>([\\d,]+)</col>", Pattern.CASE_INSENSITIVE);

	/** "You have completed 42 medium Treasure Trails." */
	private static final Pattern CLUE_COUNT = Pattern.compile(
		"You have completed ([\\d,]+) (\\w+) Treasure Trails", Pattern.CASE_INSENSITIVE);

	private final Client client;

	public ObjectiveTracker(Client client)
	{
		this.client = client;
	}

	/**
	 * The absolute counter a Vow is measured against. Called once at swearing
	 * time for the baseline, then again whenever a relevant event fires.
	 */
	public int currentCounter(Vow vow)
	{
		switch (vow.getGoalType())
		{
			case SKILL_LEVEL:
				Skill skill = skill(vow.getGoalTarget());
				return skill == null ? 0 : client.getRealSkillLevel(skill);

			case TOTAL_LEVEL:
				return client.getTotalLevel();

			case QUEST:
				Quest quest = quest(vow.getGoalTarget());
				return quest != null && quest.getState(client) == QuestState.FINISHED ? 1 : 0;

			case ITEM:
				return countItem(vow.getGoalTarget());

			default:
				// KILL_COUNT and CLUE_COUNT arrive by chat message, not by polling.
				return 0;
		}
	}

	/**
	 * Level and total-level goals are absolute targets, not deltas — reaching 70
	 * Slayer means 70, regardless of where you started.
	 */
	public boolean isAbsolute(Vow vow)
	{
		GoalType type = vow.getGoalType();
		return type == GoalType.SKILL_LEVEL || type == GoalType.TOTAL_LEVEL || type == GoalType.QUEST;
	}

	/**
	 * Parse a chat message for a counter relevant to this Vow.
	 *
	 * @return the new absolute count, or -1 when the message is irrelevant.
	 */
	public int parseChat(Vow vow, String message)
	{
		String clean = message == null ? "" : message;

		if (vow.getGoalType() == GoalType.KILL_COUNT)
		{
			Matcher m = KILL_COUNT.matcher(clean);
			if (m.find() && m.group(1).equalsIgnoreCase(vow.getGoalTarget()))
			{
				return parseInt(m.group(2));
			}
		}

		if (vow.getGoalType() == GoalType.CLUE_COUNT)
		{
			Matcher m = CLUE_COUNT.matcher(clean);
			if (m.find() && m.group(2).equalsIgnoreCase(vow.getGoalTarget()))
			{
				return parseInt(m.group(1));
			}
		}

		return -1;
	}

	private int countItem(String name)
	{
		int total = 0;
		total += countIn(InventoryID.INV, name);
		total += countIn(InventoryID.WORN, name);
		total += countIn(InventoryID.BANK, name);
		return total;
	}

	private int countIn(int containerId, String name)
	{
		ItemContainer container = client.getItemContainer(containerId);
		if (container == null || name == null || name.isEmpty())
		{
			return 0;
		}

		int total = 0;
		for (Item item : container.getItems())
		{
			if (item == null || item.getId() <= 0)
			{
				continue;
			}
			String itemName = client.getItemDefinition(item.getId()).getName();
			if (itemName != null && itemName.equalsIgnoreCase(name))
			{
				total += Math.max(1, item.getQuantity());
			}
		}
		return total;
	}

	private static Skill skill(String name)
	{
		try
		{
			return Skill.valueOf(name.toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}

	private static Quest quest(String name)
	{
		try
		{
			return Quest.valueOf(name.toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}

	private static int parseInt(String raw)
	{
		try
		{
			return Integer.parseInt(raw.replace(",", ""));
		}
		catch (NumberFormatException e)
		{
			return -1;
		}
	}
}
