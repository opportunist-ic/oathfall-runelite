package com.oathfall.model;

/**
 * The twelve Bindings. {@link #enforceable} marks the ones the client can
 * actually observe; the rest are honour-only and the panel says so plainly
 * rather than pretending to police them.
 */
public enum Binding
{
	STARVING("Starving", "No food. Potions and Prayer only.", true),
	SILENT("Silent", "No Prayer of any kind.", true),
	COLD_IRON("Cold Iron", "No potions, no boosts, no stat drinks.", true),
	GROUNDED("Grounded", "No teleports. Walk, sail, or do not go.", true),
	BAREFOOT("Barefoot", "Boots, gloves and cape slots stay empty.", true),
	ONE_HANDED("One-Handed", "Shield slot stays empty throughout.", true),
	UNBROKEN("Unbroken", "One death voids the Vow instantly.", true),
	SUNLESS("Sunless", "No run energy. You walk the entire Vow.", true),
	BLIND("Blind", "No wiki, no guides, no helper plugins.", false),
	RUSTED("Rusted", "Nothing obtained in the last seven days.", false),
	TITHED("Tithed", "Destroy a tenth of your coins on completion.", false),
	NAMED("Named", "Announce the Vow in public chat before you begin.", false);

	private final String title;
	private final String rule;
	private final boolean enforceable;

	Binding(String title, String rule, boolean enforceable)
	{
		this.title = title;
		this.rule = rule;
		this.enforceable = enforceable;
	}

	public String getTitle()
	{
		return title;
	}

	public String getRule()
	{
		return rule;
	}

	/** True when {@code BindingMonitor} can detect a break from game events. */
	public boolean isEnforceable()
	{
		return enforceable;
	}
}
