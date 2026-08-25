package com.oathfall;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(OathfallConfig.GROUP)
public interface OathfallConfig extends Config
{
	String GROUP = "oathfall";

	@ConfigSection(name = "Enforcement", description = "How strictly the covenant is policed", position = 0)
	String enforcement = "enforcement";

	@ConfigSection(name = "Companion tracker", description = "The local relay the tracker page reads", position = 1)
	String relay = "relay";

	@ConfigSection(name = "Display", description = "Overlay and chat feedback", position = 2)
	String display = "display";

	// ---------------- enforcement ----------------

	@ConfigItem(
		keyName = "autoBreak",
		name = "Break Vows automatically",
		description = "When a Binding is observably broken, settle the Vow as Broken without asking.<br>"
			+ "Off: the plugin flags the break and you confirm it yourself.",
		section = enforcement,
		position = 0
	)
	default boolean autoBreak()
	{
		return false;
	}

	@ConfigItem(
		keyName = "autoScar",
		name = "Draw the Scar automatically",
		description = "On a Broken Vow, draw a random Scar immediately instead of prompting.",
		section = enforcement,
		position = 1
	)
	default boolean autoScar()
	{
		return true;
	}

	@ConfigItem(
		keyName = "autoEra",
		name = "Advance Eras automatically",
		description = "Move to the next Era the moment its gate is satisfied and retire the old deck.",
		section = enforcement,
		position = 2
	)
	default boolean autoEra()
	{
		return true;
	}

	@ConfigItem(
		keyName = "graceSeconds",
		name = "Grace period (seconds)",
		description = "Ignore Binding breaks for this long after swearing, so banking and gearing up<br>"
			+ "do not void a Vow before it starts.",
		section = enforcement,
		position = 3
	)
	@Range(min = 0, max = 300)
	default int graceSeconds()
	{
		return 20;
	}

	// ---------------- relay ----------------

	@ConfigItem(
		keyName = "relayEnabled",
		name = "Serve the companion tracker",
		description = "Run a small HTTP server bound to 127.0.0.1 only, serving the Oathfall tracker<br>"
			+ "and a live ledger feed. Nothing is sent off your machine.",
		section = relay,
		position = 0
	)
	default boolean relayEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "relayPort",
		name = "Port",
		description = "Loopback port for the tracker. Change it if something else already uses this one.",
		section = relay,
		position = 1
	)
	@Range(min = 1024, max = 65535)
	default int relayPort()
	{
		return 7373;
	}

	// ---------------- display ----------------

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show the Vow overlay",
		description = "Draw the sworn Vow, its Binding and live progress over the game window.",
		section = display,
		position = 0
	)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDrawOverlay",
		name = "Show the draw table",
		description = "Deal the three Vows as cards over the game window. Click a card to turn it,<br>"
			+ "then click it again to swear it.",
		section = display,
		position = 1
	)
	default boolean showDrawOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "chatFeedback",
		name = "Announce in chat",
		description = "Print covenant events to your own chatbox. Local only — nothing is sent publicly.",
		section = display,
		position = 2
	)
	default boolean chatFeedback()
	{
		return true;
	}

	@ConfigItem(
		keyName = "audienceCardId",
		name = "Audience's Card",
		description = "The Vow id the comments voted in for the next draw, e.g. WAN-04.<br>"
			+ "Leave blank to deal a third random card instead.",
		section = display,
		position = 3
	)
	default String audienceCardId()
	{
		return "";
	}

	// ---------------- persisted run (hidden) ----------------

	@ConfigItem(keyName = "ledger", name = "", description = "", hidden = true)
	default String ledger()
	{
		return "";
	}

	@ConfigItem(keyName = "ledger", name = "", description = "", hidden = true)
	void setLedger(String json);
}
