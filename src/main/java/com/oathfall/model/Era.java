package com.oathfall.model;

import net.runelite.api.Client;
import net.runelite.api.Skill;

/**
 * Era gates. {@link #satisfiedBy(Client)} is evaluated against live stats so the
 * plugin can advance the deck by itself the moment the account crosses a gate.
 */
public enum Era
{
	WAKING("I", "The Waking", "WAK", 0, 0, "Obor"),
	WANDERING("II", "The Wandering", "WAN", 40, 500, "Sarachnis"),
	ASCENT("III", "The Ascent", "ASC", 55, 900, "Abyssal Sire"),
	CRUCIBLE("IV", "The Crucible", "CRU", 70, 1400, "Nex"),
	RECKONING("V", "The Reckoning", "REC", 85, 1750, "The Inferno");

	private final String numeral;
	private final String title;
	private final String code;
	private final int baseCombat;
	private final int totalLevel;
	private final String herald;

	Era(String numeral, String title, String code, int baseCombat, int totalLevel, String herald)
	{
		this.numeral = numeral;
		this.title = title;
		this.code = code;
		this.baseCombat = baseCombat;
		this.totalLevel = totalLevel;
		this.herald = herald;
	}

	public String getNumeral()
	{
		return numeral;
	}

	public String getTitle()
	{
		return title;
	}

	public String getCode()
	{
		return code;
	}

	public int getBaseCombat()
	{
		return baseCombat;
	}

	public int getTotalLevel()
	{
		return totalLevel;
	}

	public String getHerald()
	{
		return herald;
	}

	/** Long Vows only exist from Era III, where the account can carry them. */
	public boolean allowsLongVows()
	{
		return ordinal() >= ASCENT.ordinal();
	}

	public Era next()
	{
		return this == RECKONING ? RECKONING : values()[ordinal() + 1];
	}

	/**
	 * A gate is met when the lowest of the four melee/ranged/magic base levels
	 * clears {@link #baseCombat} and total level clears {@link #totalLevel}.
	 */
	public boolean satisfiedBy(Client client)
	{
		if (client == null)
		{
			return false;
		}

		int lowestBase = Math.min(
			Math.min(client.getRealSkillLevel(Skill.ATTACK), client.getRealSkillLevel(Skill.STRENGTH)),
			Math.min(client.getRealSkillLevel(Skill.DEFENCE),
				Math.min(client.getRealSkillLevel(Skill.RANGED), client.getRealSkillLevel(Skill.MAGIC))));

		return lowestBase >= baseCombat && client.getTotalLevel() >= totalLevel;
	}

	/** The highest Era whose gate the account currently satisfies. */
	public static Era highestSatisfied(Client client)
	{
		Era best = WAKING;
		for (Era era : values())
		{
			if (era.satisfiedBy(client))
			{
				best = era;
			}
		}
		return best;
	}
}
