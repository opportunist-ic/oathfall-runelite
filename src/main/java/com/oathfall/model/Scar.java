package com.oathfall.model;

/**
 * The Scar deck. {@link #watched} marks Scars whose restriction the plugin can
 * keep an eye on for the rest of the run, not just at the moment it is drawn.
 */
public enum Scar
{
	THE_WEIGHT("The Weight", "One inventory slot must remain empty. Forever.", true),
	ASHEN_TONGUE("Ashen Tongue", "Your highest unlocked Prayer is sealed shut.", true),
	HOLLOW_GUT("Hollow Gut", "Never more than eight food in an inventory.", true),
	CRACKED_LEDGER("Cracked Ledger", "Your Grace cap falls from ten to eight.", true),
	IRONSHOD("Ironshod", "The boots slot is sealed. Barefoot, permanently.", true),
	THE_LONG_ROAD("The Long Road", "Roll one teleport method. It is closed to you.", false),
	BRITTLE("Brittle", "One type of combat potion per trip.", false),
	DEBTOR("Debtor", "Your next three Vows must be the longest on the table.", true),
	MARKED("Marked", "The next Herald arrives one Doom step early.", true),
	VOWED_TO_ASH("Vowed to Ash", "Roll a non-combat skill. It gains no experience.", true),
	SLEEPLESS("Sleepless", "No house teleport, no pool, no restoration between trips.", false),
	THE_ECHO("The Echo", "Swear the Vow you just broke again, one Length tier longer.", true);

	private final String title;
	private final String rule;
	private final boolean watched;

	Scar(String title, String rule, boolean watched)
	{
		this.title = title;
		this.rule = rule;
		this.watched = watched;
	}

	public String getTitle()
	{
		return title;
	}

	public String getRule()
	{
		return rule;
	}

	public boolean isWatched()
	{
		return watched;
	}
}
