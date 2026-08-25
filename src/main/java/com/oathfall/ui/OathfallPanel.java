package com.oathfall.ui;

import com.oathfall.OathfallPlugin;
import com.oathfall.model.Ledger;
import com.oathfall.model.Scar;
import com.oathfall.model.Vow;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;

/** The side panel: the ledger up top, the table below, rites at the bottom. */
public class OathfallPanel extends PluginPanel
{
	private static final Color EMBER = new Color(0xC9, 0x94, 0x35);
	private static final Color DOOM = new Color(0xC1, 0x5C, 0x72);
	private static final Color INK_DIM = new Color(0x9A, 0xA0, 0x96);

	private final OathfallPlugin plugin;
	private final JPanel body = new JPanel();

	public OathfallPanel(OathfallPlugin plugin)
	{
		super(false);
		this.plugin = plugin;

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		add(body, BorderLayout.NORTH);
	}

	public void refresh()
	{
		SwingUtilities.invokeLater(this::rebuild);
	}

	private void rebuild()
	{
		body.removeAll();

		Ledger ledger = plugin.getLedger();

		body.add(title("OATHFALL"));
		body.add(caption("Era " + ledger.era.getNumeral() + " — " + ledger.era.getTitle()
			+ (ledger.hollowed ? " · HOLLOWED" : "")));
		body.add(Box.createVerticalStrut(10));

		body.add(statRow(ledger));
		body.add(Box.createVerticalStrut(12));

		Vow active = plugin.activeVow();
		if (active != null)
		{
			body.add(sectionLabel("THE SWORN VOW"));
			body.add(wrapped(active.getObjective(), Color.WHITE));
			body.add(wrapped(active.getBinding().getTitle() + " — " + active.getBinding().getRule(), DOOM));

			if (active.isAutoTracked())
			{
				body.add(caption("Progress " + ledger.activeProgress + " / " + active.getGoalAmount()));
			}
			else
			{
				body.add(caption("Not machine-trackable. Settle by hand."));
			}

			if (ledger.activeBroken)
			{
				body.add(Box.createVerticalStrut(6));
				body.add(wrapped("BROKEN: " + ledger.activeBreakReason, DOOM));
			}

			body.add(Box.createVerticalStrut(8));
			body.add(button("Settle as Kept", EMBER, e -> plugin.settleKept()));
			body.add(button("Settle as Broken", DOOM,
				e -> plugin.settleBroken(ledger.activeBreakReason == null ? "Settled by hand" : ledger.activeBreakReason)));
		}
		else
		{
			body.add(sectionLabel("THE TABLE"));

			if (plugin.currentHand().isEmpty())
			{
				body.add(caption("No cards dealt."));
			}
			else
			{
				int i = 0;
				for (Vow vow : plugin.currentHand())
				{
					final String id = vow.getId();
					boolean audience = i == 2;
					body.add(Box.createVerticalStrut(6));
					body.add(wrapped((audience ? "[AUDIENCE] " : "") + vow.getId() + " · " + vow.getObjective(), Color.WHITE));
					body.add(wrapped(vow.getBinding().getTitle() + " · " + vow.getLength().getTitle()
						+ " · " + vow.graceValue(audience) + " Grace", INK_DIM));
					body.add(button("Swear this", EMBER, e -> plugin.swear(id)));
					i++;
				}
			}

			body.add(Box.createVerticalStrut(8));
			body.add(button("Deal the Vows", EMBER, e -> plugin.deal()));
		}

		if (!ledger.scars.isEmpty())
		{
			body.add(Box.createVerticalStrut(12));
			body.add(sectionLabel("SCARS CARRIED (" + ledger.scars.size() + ")"));
			for (Scar scar : ledger.scars)
			{
				body.add(wrapped("· " + scar.getTitle() + " — " + scar.getRule(), DOOM));
			}
		}

		body.add(Box.createVerticalStrut(12));
		body.add(sectionLabel("THE RELIQUARY"));
		body.add(button("Temper — 2", EMBER, e -> plugin.spend("temper", "")));
		body.add(button("Vigil — 4", EMBER, e -> plugin.spend("vigil", "")));
		body.add(button("Draw a Scar", DOOM, e -> plugin.drawScar()));

		String url = plugin.relayUrl();
		body.add(Box.createVerticalStrut(12));
		body.add(sectionLabel("COMPANION TRACKER"));
		body.add(caption(url == null ? "Relay off — enable it in settings." : url));
		if (url != null)
		{
			body.add(button("Copy tracker link", EMBER, e -> copy(url)));
		}
		body.add(button("Copy ledger JSON", EMBER, e -> copy(plugin.ledgerJson())));

		body.revalidate();
		body.repaint();
	}

	// ------------------------------------------------------------- components

	private JPanel statRow(Ledger ledger)
	{
		JPanel row = new JPanel(new GridLayout(1, 3, 4, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.add(stat("DOOM", String.valueOf(ledger.doom), DOOM));
		row.add(stat("GRACE", ledger.grace + "/" + ledger.graceCap(), EMBER));
		row.add(stat("SCARS", String.valueOf(ledger.scars.size()), INK_DIM));
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
		return row;
	}

	private JPanel stat(String label, String value, Color colour)
	{
		JPanel cell = new JPanel(new BorderLayout());
		cell.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		cell.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

		JLabel l = new JLabel(label);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(INK_DIM);

		JLabel v = new JLabel(value);
		v.setFont(FontManager.getRunescapeBoldFont());
		v.setForeground(colour);

		cell.add(l, BorderLayout.NORTH);
		cell.add(v, BorderLayout.CENTER);
		return cell;
	}

	private JLabel title(String text)
	{
		JLabel l = new JLabel(text);
		l.setFont(FontManager.getRunescapeBoldFont());
		l.setForeground(EMBER);
		l.setAlignmentX(Component.LEFT_ALIGNMENT);
		return l;
	}

	private JLabel sectionLabel(String text)
	{
		JLabel l = new JLabel(text);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(EMBER);
		l.setAlignmentX(Component.LEFT_ALIGNMENT);
		l.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
		return l;
	}

	private JLabel caption(String text)
	{
		JLabel l = new JLabel(text);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(INK_DIM);
		l.setAlignmentX(Component.LEFT_ALIGNMENT);
		return l;
	}

	/** JLabel has no wrapping, so lean on the HTML renderer for body text. */
	private JLabel wrapped(String text, Color colour)
	{
		JLabel l = new JLabel("<html><body style='width:150px'>" + escape(text) + "</body></html>");
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(colour);
		l.setAlignmentX(Component.LEFT_ALIGNMENT);
		l.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		return l;
	}

	private JButton button(String text, Color colour, java.awt.event.ActionListener action)
	{
		JButton b = new JButton(text);
		b.setFont(FontManager.getRunescapeSmallFont());
		b.setForeground(colour);
		b.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		b.setFocusPainted(false);
		b.setAlignmentX(Component.LEFT_ALIGNMENT);
		b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		b.addActionListener(action);
		return b;
	}

	private static void copy(String text)
	{
		java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
			.setContents(new java.awt.datatransfer.StringSelection(text), null);
	}

	private static String escape(String s)
	{
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
