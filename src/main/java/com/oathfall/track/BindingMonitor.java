package com.oathfall.track;

import com.oathfall.model.Binding;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Prayer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Watches for observable Binding breaks.
 *
 * Every check is passive: it reads game state and menu options the player has
 * already clicked. Nothing here sends input to the client — the Plugin Hub
 * forbids gameplay automation, and so does Jagex.
 */
public class BindingMonitor
{
	private final Client client;

	public BindingMonitor(Client client)
	{
		this.client = client;
	}

	/**
	 * Per-tick state checks. Returns a human-readable break reason, or null.
	 */
	@Nullable
	public String checkTick(Binding binding)
	{
		switch (binding)
		{
			case SILENT:
				Prayer active = activePrayer();
				return active == null ? null : "Prayer used: " + pretty(active.name());

			case SUNLESS:
				return client.getVarpValue(VarPlayerID.OPTION_RUN) == 1 ? "Run energy enabled" : null;

			case BAREFOOT:
				String worn = firstWorn(EquipmentInventorySlot.BOOTS, EquipmentInventorySlot.GLOVES, EquipmentInventorySlot.CAPE);
				return worn == null ? null : "Equipped in a sealed slot: " + worn;

			case ONE_HANDED:
				String shield = firstWorn(EquipmentInventorySlot.SHIELD);
				return shield == null ? null : "Shield slot filled: " + shield;

			default:
				return null;
		}
	}

	/**
	 * Menu-click checks. {@code option} and {@code target} come straight from the
	 * MenuOptionClicked event.
	 */
	@Nullable
	public String checkMenuClick(Binding binding, String option, String target)
	{
		String opt = option == null ? "" : option.toLowerCase(Locale.ROOT);
		String tgt = target == null ? "" : target.toLowerCase(Locale.ROOT);

		switch (binding)
		{
			case STARVING:
				if (opt.equals("eat"))
				{
					return "Ate " + target;
				}
				// Drinking a brew or a stew still counts as food.
				if (opt.equals("drink") && (tgt.contains("brew") || tgt.contains("stew") || tgt.contains("wine")))
				{
					return "Drank " + target;
				}
				return null;

			case COLD_IRON:
				if (opt.equals("drink"))
				{
					return "Drank " + target;
				}
				return null;

			case GROUNDED:
				if (opt.contains("teleport") || tgt.contains("teleport") || opt.equals("rub") && tgt.contains("glory"))
				{
					return "Teleported: " + (target == null || target.isEmpty() ? option : target);
				}
				return null;

			default:
				return null;
		}
	}

	@Nullable
	private Prayer activePrayer()
	{
		for (Prayer prayer : Prayer.values())
		{
			try
			{
				if (client.getVarbitValue(prayer.getVarbit()) == 1)
				{
					return prayer;
				}
			}
			catch (RuntimeException ignored)
			{
				// Prayer enum gains entries across game updates; skip unknowns.
			}
		}
		return null;
	}

	@Nullable
	private String firstWorn(EquipmentInventorySlot... slots)
	{
		ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
		if (equipment == null)
		{
			return null;
		}

		for (EquipmentInventorySlot slot : slots)
		{
			Item item = equipment.getItem(slot.getSlotIdx());
			if (item != null && item.getId() > 0)
			{
				return client.getItemDefinition(item.getId()).getName();
			}
		}
		return null;
	}

	private static String pretty(String enumName)
	{
		String s = enumName.toLowerCase(Locale.ROOT).replace('_', ' ');
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
}
