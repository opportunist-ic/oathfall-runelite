package com.oathfall.ui;

import com.oathfall.OathfallConfig;
import com.oathfall.OathfallPlugin;
import com.oathfall.model.Ledger;
import com.oathfall.model.Vow;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

/** The sworn Vow, its Binding, and the Doom clock, over the game window. */
public class OathfallOverlay extends OverlayPanel
{
	private static final Color EMBER = new Color(0xED, 0xBB, 0x59);
	private static final Color DOOM = new Color(0xCB, 0x69, 0x7F);
	private static final Color INK = new Color(0xE9, 0xE5, 0xD8);

	private final OathfallPlugin plugin;
	private final OathfallConfig config;

	public OathfallOverlay(OathfallPlugin plugin, OathfallConfig config)
	{
		this.plugin = plugin;
		this.config = config;

		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		panelComponent.setPreferredSize(new Dimension(210, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverlay())
		{
			return null;
		}

		Ledger ledger = plugin.getLedger();
		Vow vow = plugin.activeVow();

		panelComponent.getChildren().clear();
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Oathfall · Era " + ledger.era.getNumeral())
			.color(EMBER)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Doom")
			.right(ledger.doom + " / 10")
			.rightColor(ledger.doom >= 8 ? DOOM : INK)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Grace")
			.right(ledger.grace + " / " + ledger.graceCap())
			.rightColor(EMBER)
			.build());

		if (!ledger.scars.isEmpty())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Scars")
				.right(String.valueOf(ledger.scars.size()))
				.rightColor(DOOM)
				.build());
		}

		if (vow == null)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("No Vow sworn")
				.leftColor(DOOM)
				.build());
			return super.render(graphics);
		}

		panelComponent.getChildren().add(LineComponent.builder()
			.left(vow.getObjective())
			.leftColor(INK)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left(vow.getBinding().getTitle())
			.right(vow.getBinding().isEnforceable() ? "watched" : "honour")
			.leftColor(DOOM)
			.build());

		if (vow.isAutoTracked())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Progress")
				.right(ledger.activeProgress + " / " + vow.getGoalAmount())
				.rightColor(EMBER)
				.build());
		}

		if (ledger.activeBroken)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("BROKEN")
				.right(ledger.activeBreakReason)
				.leftColor(DOOM)
				.rightColor(DOOM)
				.build());
		}

		if (ledger.heraldDue())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("HERALD")
				.right(ledger.era.getHerald())
				.leftColor(DOOM)
				.rightColor(DOOM)
				.build());
		}

		return super.render(graphics);
	}
}
