package com.oathfall.ui;

import com.oathfall.OathfallPlugin;
import com.oathfall.model.Length;
import com.oathfall.model.Vow;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * The draw table, rendered over the game window: three cards dealt face down,
 * each turned by clicking it, then sworn by clicking the revealed face.
 *
 * The flip is a horizontal squash — the card narrows to nothing, swaps face at
 * the midpoint and opens again — which reads as a card turning without needing
 * any real 3D.
 */
public class VowDrawOverlay extends Overlay
{
	private static final int CARD_W = 150;
	private static final int CARD_H = 210;
	private static final int GAP = 16;
	private static final long FLIP_MS = 420;

	private static final Color GROUND = new Color(0x0E, 0x13, 0x10, 235);
	private static final Color SURFACE = new Color(0x16, 0x1C, 0x18);
	private static final Color RULE = new Color(0x2A, 0x34, 0x2D);
	private static final Color EMBER = new Color(0xC9, 0x94, 0x35);
	private static final Color EMBER_HI = new Color(0xED, 0xBB, 0x59);
	private static final Color DOOM_HI = new Color(0xCB, 0x69, 0x7F);
	private static final Color INK = new Color(0xE9, 0xE5, 0xD8);
	private static final Color INK_DIM = new Color(0x8B, 0x92, 0x86);
	private static final Color SEALED = new Color(0x6E, 0x93, 0xA2);

	private static final Font F_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 15);
	private static final Font F_BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
	private static final Font F_SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
	private static final Font F_CODE = new Font(Font.MONOSPACED, Font.BOLD, 10);

	private final OathfallPlugin plugin;
	private final Client client;

	/** Hit boxes for the three seats, refreshed every frame the table is up. */
	private final Rectangle[] seats = new Rectangle[3];
	private final boolean[] revealed = new boolean[3];
	private final long[] flipStart = new long[3];

	private String shownHandKey = "";
	private int hovered = -1;

	public VowDrawOverlay(OathfallPlugin plugin, Client client)
	{
		this.plugin = plugin;
		this.client = client;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(100.0f);
	}

	/** True while three cards are on the table and nothing is sworn. */
	public boolean isTableUp()
	{
		return !plugin.getLedger().hasActiveVow() && plugin.currentHand().size() == 3;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		if (!isTableUp() || !plugin.isDrawOverlayEnabled())
		{
			return null;
		}

		List<Vow> hand = plugin.currentHand();

		// A fresh deal resets every card to face down.
		String key = hand.get(0).getId() + hand.get(1).getId() + hand.get(2).getId();
		if (!key.equals(shownHandKey))
		{
			shownHandKey = key;
			for (int i = 0; i < 3; i++)
			{
				revealed[i] = false;
				flipStart[i] = 0L;
			}
		}

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		Rectangle view = viewport();
		int totalW = CARD_W * 3 + GAP * 2;
		int x0 = view.x + (view.width - totalW) / 2;
		int y0 = view.y + (view.height - CARD_H) / 2 - 10;

		drawBackdrop(g, view, x0, y0, totalW);

		for (int i = 0; i < 3; i++)
		{
			Rectangle seat = new Rectangle(x0 + i * (CARD_W + GAP), y0, CARD_W, CARD_H);
			seats[i] = seat;
			drawCard(g, seat, hand.get(i), i);
		}

		drawFooter(g, x0, y0 + CARD_H + 14, totalW);
		return null;
	}

	private Rectangle viewport()
	{
		int w = client.getViewportWidth();
		int h = client.getViewportHeight();
		if (w <= 0 || h <= 0)
		{
			return new Rectangle(0, 0, client.getCanvasWidth(), client.getCanvasHeight());
		}
		return new Rectangle(client.getViewportXOffset(), client.getViewportYOffset(), w, h);
	}

	private void drawBackdrop(Graphics2D g, Rectangle view, int x0, int y0, int totalW)
	{
		g.setColor(new Color(0, 0, 0, 150));
		g.fill(view);

		String title = "THE TABLE";
		g.setFont(F_CODE);
		FontMetrics fm = g.getFontMetrics();
		g.setColor(EMBER);
		g.drawString(title, x0 + (totalW - fm.stringWidth(title)) / 2, y0 - 26);

		g.setColor(RULE);
		g.drawLine(x0, y0 - 16, x0 + totalW, y0 - 16);
	}

	private void drawFooter(Graphics2D g, int x0, int y, int totalW)
	{
		boolean anyHidden = !revealed[0] || !revealed[1] || !revealed[2];
		String msg = anyHidden
			? "Click a card to turn it."
			: "Click a card again to swear it.";

		g.setFont(F_SMALL);
		FontMetrics fm = g.getFontMetrics();
		g.setColor(INK_DIM);
		g.drawString(msg, x0 + (totalW - fm.stringWidth(msg)) / 2, y + 12);
	}

	private void drawCard(Graphics2D g, Rectangle seat, Vow vow, int index)
	{
		float phase = flipPhase(index);
		// squash to zero width at the midpoint, then open again
		double scaleX = Math.abs(1.0 - 2.0 * phase);
		if (scaleX < 0.02)
		{
			scaleX = 0.02;
		}

		boolean showFace = revealed[index] ? phase >= 0.5f : false;

		AffineTransform old = g.getTransform();
		g.translate(seat.getCenterX(), seat.getCenterY());
		g.scale(scaleX, 1.0);
		g.translate(-seat.getCenterX(), -seat.getCenterY());

		Shape card = new RoundRectangle2D.Float(seat.x, seat.y, seat.width, seat.height, 6, 6);

		if (showFace)
		{
			drawFace(g, seat, card, vow, index);
		}
		else
		{
			drawBack(g, seat, card, index);
		}

		g.setTransform(old);
	}

	private void drawBack(Graphics2D g, Rectangle seat, Shape card, int index)
	{
		g.setPaint(new GradientPaint(seat.x, seat.y, new Color(0x17, 0x1E, 0x19),
			seat.x, seat.y + seat.height, new Color(0x11, 0x16, 0x13)));
		g.fill(card);

		// hatching, clipped to the card
		Shape clip = g.getClip();
		g.setClip(card);
		g.setColor(new Color(0xC9, 0x94, 0x35, 26));
		for (int d = -seat.height; d < seat.width; d += 10)
		{
			g.drawLine(seat.x + d, seat.y, seat.x + d + seat.height, seat.y + seat.height);
		}
		g.setClip(clip);

		g.setColor(index == hovered ? EMBER_HI : RULE);
		g.setStroke(new BasicStroke(index == hovered ? 2f : 1f));
		g.draw(card);

		// the sigil
		int cx = (int) seat.getCenterX();
		int cy = (int) seat.getCenterY();
		g.setColor(new Color(0xC9, 0x94, 0x35, 150));
		g.setStroke(new BasicStroke(1.4f));
		g.drawOval(cx - 26, cy - 26, 52, 52);
		int[] px = {cx, cx + 17, cx, cx - 17};
		int[] py = {cy - 24, cy, cy + 24, cy};
		g.drawPolygon(px, py, 4);
		g.setColor(EMBER);
		g.fillOval(cx - 4, cy - 4, 8, 8);
	}

	private void drawFace(Graphics2D g, Rectangle seat, Shape card, Vow vow, int index)
	{
		boolean audience = index == 2;

		g.setColor(SURFACE);
		g.fill(card);
		g.setColor(index == hovered ? EMBER_HI : (audience ? SEALED : RULE));
		g.setStroke(new BasicStroke(index == hovered ? 2f : 1f));
		g.draw(card);

		int x = seat.x + 10;
		int y = seat.y + 18;
		int w = seat.width - 20;

		g.setFont(F_CODE);
		g.setColor(EMBER);
		g.drawString(vow.getId(), x, y);

		if (audience)
		{
			g.setColor(SEALED);
			FontMetrics fm = g.getFontMetrics();
			String tag = "AUDIENCE";
			g.drawString(tag, seat.x + seat.width - 10 - fm.stringWidth(tag), y);
		}

		y += 8;
		g.setColor(RULE);
		g.drawLine(x, y, x + w, y);

		// objective
		y += 16;
		g.setFont(F_TITLE);
		g.setColor(INK);
		y = drawWrapped(g, vow.getObjective(), x, y, w, 16);

		// binding, pinned near the bottom
		int bindY = seat.y + seat.height - 54;
		g.setColor(RULE);
		g.drawLine(x, bindY - 12, x + w, bindY - 12);

		g.setFont(F_BODY);
		g.setColor(DOOM_HI);
		g.drawString(vow.getBinding().getTitle(), x, bindY);

		g.setFont(F_SMALL);
		g.setColor(INK_DIM);
		drawWrapped(g, vow.getBinding().getRule(), x, bindY + 13, w, 11);

		// length + grace
		int footY = seat.y + seat.height - 9;
		g.setFont(F_CODE);
		Length len = vow.getLength();
		g.setColor(len == Length.LONG ? DOOM_HI : INK_DIM);
		g.drawString(len.getTitle().toUpperCase(), x, footY);

		String grace = vow.graceValue(audience) + "G";
		FontMetrics fm = g.getFontMetrics();
		g.setColor(EMBER_HI);
		g.drawString(grace, seat.x + seat.width - 10 - fm.stringWidth(grace), footY);
	}

	/** Word-wraps to the card width and returns the y after the last line. */
	private int drawWrapped(Graphics2D g, String text, int x, int y, int maxW, int lineH)
	{
		FontMetrics fm = g.getFontMetrics();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" "))
		{
			String candidate = line.length() == 0 ? word : line + " " + word;
			if (fm.stringWidth(candidate) > maxW && line.length() > 0)
			{
				g.drawString(line.toString(), x, y);
				y += lineH;
				line = new StringBuilder(word);
			}
			else
			{
				line = new StringBuilder(candidate);
			}
		}
		if (line.length() > 0)
		{
			g.drawString(line.toString(), x, y);
			y += lineH;
		}
		return y;
	}

	private float flipPhase(int index)
	{
		if (flipStart[index] == 0L)
		{
			return revealed[index] ? 1f : 0f;
		}
		long elapsed = System.currentTimeMillis() - flipStart[index];
		if (elapsed >= FLIP_MS)
		{
			return 1f;
		}
		float t = elapsed / (float) FLIP_MS;
		// ease-in-out so the turn has weight
		return t < 0.5f ? 2f * t * t : -1f + (4f - 2f * t) * t;
	}

	// ------------------------------------------------------------------ input

	/** @return the seat under the point, or -1. */
	public int seatAt(int x, int y)
	{
		if (!isTableUp())
		{
			return -1;
		}
		for (int i = 0; i < seats.length; i++)
		{
			if (seats[i] != null && seats[i].contains(x, y))
			{
				return i;
			}
		}
		return -1;
	}

	public void setHovered(int seat)
	{
		hovered = seat;
	}

	/**
	 * Handle a click on a seat.
	 *
	 * @return the Vow to swear, or null when the click only turned a card.
	 */
	public Vow click(int seat)
	{
		List<Vow> hand = plugin.currentHand();
		if (seat < 0 || seat >= hand.size())
		{
			return null;
		}

		if (!revealed[seat])
		{
			revealed[seat] = true;
			flipStart[seat] = System.currentTimeMillis();
			return null;
		}

		// A second click on a face-up card swears it.
		return hand.get(seat);
	}

	/** Forget the current table so the next deal starts face down. */
	public void reset()
	{
		shownHandKey = "";
		hovered = -1;
		for (int i = 0; i < 3; i++)
		{
			revealed[i] = false;
			flipStart[i] = 0L;
			seats[i] = null;
		}
	}

	public List<Rectangle> debugSeats()
	{
		List<Rectangle> out = new ArrayList<>();
		for (Rectangle r : seats)
		{
			if (r != null)
			{
				out.add(r);
			}
		}
		return out;
	}
}
