package com.oathfall.ui;

import com.oathfall.OathfallPlugin;
import com.oathfall.model.Vow;
import net.runelite.client.input.MouseAdapter;

import java.awt.event.MouseEvent;

/**
 * Mouse handling for the draw table.
 *
 * Clicks are only consumed while the table is actually up and the pointer is
 * over a card, so the plugin never steals input from the game otherwise.
 */
public class VowDrawInput extends MouseAdapter
{
	private final OathfallPlugin plugin;
	private final VowDrawOverlay overlay;

	public VowDrawInput(OathfallPlugin plugin, VowDrawOverlay overlay)
	{
		this.plugin = plugin;
		this.overlay = overlay;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent event)
	{
		if (!plugin.isDrawOverlayEnabled() || !overlay.isTableUp())
		{
			return event;
		}

		int seat = overlay.seatAt(event.getX(), event.getY());
		if (seat < 0)
		{
			return event;
		}

		Vow chosen = overlay.click(seat);
		if (chosen != null)
		{
			// swear() marshals onto the client thread itself.
			plugin.swear(chosen.getId());
		}

		event.consume();
		return event;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent event)
	{
		if (plugin.isDrawOverlayEnabled() && overlay.isTableUp())
		{
			overlay.setHovered(overlay.seatAt(event.getX(), event.getY()));
		}
		return event;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent event)
	{
		// Swallow the release that belongs to a click we already consumed,
		// otherwise the game receives a stray release over the card.
		if (plugin.isDrawOverlayEnabled() && overlay.isTableUp()
			&& overlay.seatAt(event.getX(), event.getY()) >= 0)
		{
			event.consume();
		}
		return event;
	}
}
