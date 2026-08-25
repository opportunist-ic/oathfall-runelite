package com.oathfall;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Provides;
import com.oathfall.deck.Decks;
import com.oathfall.model.Binding;
import com.oathfall.model.Era;
import com.oathfall.model.GoalType;
import com.oathfall.model.Ledger;
import com.oathfall.model.Length;
import com.oathfall.model.Scar;
import com.oathfall.model.Vow;
import com.oathfall.relay.RelayServer;
import com.oathfall.track.BindingMonitor;
import com.oathfall.track.ObjectiveTracker;
import com.oathfall.ui.OathfallOverlay;
import com.oathfall.ui.OathfallPanel;
import com.oathfall.ui.VowDrawInput;
import com.oathfall.ui.VowDrawOverlay;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.MouseManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@PluginDescriptor(
	name = "Oathfall",
	description = "Ledger, binding watchdog and companion tracker for the Oathfall hardcore ironman covenant",
	tags = {"ironman", "hardcore", "snowflake", "challenge", "vow", "oathfall", "restriction"}
)
public class OathfallPlugin extends Plugin implements RelayServer.Handler
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OathfallConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private Gson gson;

	private Ledger ledger = new Ledger();
	private BindingMonitor bindingMonitor;
	private ObjectiveTracker objectiveTracker;
	private RelayServer relay;
	private OathfallPanel panel;
	private OathfallOverlay overlay;
	private VowDrawOverlay drawOverlay;
	private VowDrawInput drawInput;
	private NavigationButton navButton;

	private final Random random = new Random();

	@Provides
	OathfallConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(OathfallConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		loadLedger();

		bindingMonitor = new BindingMonitor(client);
		objectiveTracker = new ObjectiveTracker(client);

		panel = new OathfallPanel(this);
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/com/oathfall/panel_icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Oathfall")
			.icon(icon)
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		overlay = new OathfallOverlay(this, config);
		overlayManager.add(overlay);

		drawOverlay = new VowDrawOverlay(this, client);
		overlayManager.add(drawOverlay);
		drawInput = new VowDrawInput(this, drawOverlay);
		mouseManager.registerMouseListener(drawInput);

		relay = new RelayServer(this);
		if (config.relayEnabled())
		{
			startRelay();
		}

		panel.refresh();
	}

	@Override
	protected void shutDown() throws Exception
	{
		saveLedger();

		if (relay != null)
		{
			relay.stop();
			relay = null;
		}
		mouseManager.unregisterMouseListener(drawInput);
		overlayManager.remove(drawOverlay);
		overlayManager.remove(overlay);
		clientToolbar.removeNavigation(navButton);
		drawOverlay = null;
		drawInput = null;
		panel = null;
		overlay = null;
	}

	// ================================================================= events

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(this::maybeAdvanceEra);
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (!ledger.hasActiveVow() || ledger.activeBroken)
		{
			return;
		}

		Vow vow = activeVow();
		if (vow == null)
		{
			return;
		}

		if (withinGracePeriod())
		{
			return;
		}

		String reason = bindingMonitor.checkTick(vow.getBinding());
		if (reason != null)
		{
			flagBreak(reason);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!ledger.hasActiveVow() || ledger.activeBroken || withinGracePeriod())
		{
			return;
		}

		Vow vow = activeVow();
		if (vow == null)
		{
			return;
		}

		String reason = bindingMonitor.checkMenuClick(vow.getBinding(), event.getMenuOption(), event.getMenuTarget());
		if (reason != null)
		{
			flagBreak(reason);
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (event.getActor() != client.getLocalPlayer())
		{
			return;
		}

		// A death always costs a Doom step, sworn Vow or not.
		ledger.doom = Math.min(10, ledger.doom + 1);
		ledger.streak = 0;
		announce("You died. Doom rises to " + ledger.doom + ".");

		if (ledger.hasActiveVow() && !ledger.activeBroken)
		{
			Vow vow = activeVow();
			if (vow != null && vow.getBinding() == Binding.UNBROKEN)
			{
				flagBreak("Died under Unbroken");
			}
		}

		checkHerald();
		persistAndPush();
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		// Fires on every XP drop, so it must stay cheap: only level-shaped goals
		// care, and refreshProgress persists nothing unless a value actually moved.
		refreshProgress(GoalType.SKILL_LEVEL, GoalType.TOTAL_LEVEL, GoalType.QUEST);

		if (config.autoEra())
		{
			maybeAdvanceEra();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		// Item goals scan the bank, so only run them when a container really changed.
		refreshProgress(GoalType.ITEM);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
		{
			return;
		}

		Vow vow = activeVow();
		if (vow == null || ledger.activeBroken)
		{
			return;
		}

		int absolute = objectiveTracker.parseChat(vow, event.getMessage());
		if (absolute < 0)
		{
			return;
		}

		if (ledger.activeBaseline == 0 && ledger.activeProgress == 0)
		{
			// First sighting of this counter — treat it as the baseline.
			ledger.activeBaseline = absolute - 1;
		}

		ledger.activeProgress = Math.max(0, absolute - ledger.activeBaseline);
		checkObjectiveComplete(vow);
		persistAndPush();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!OathfallConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		if ("relayEnabled".equals(event.getKey()) || "relayPort".equals(event.getKey()))
		{
			if (config.relayEnabled())
			{
				startRelay();
			}
			else if (relay != null)
			{
				relay.stop();
			}
		}
	}

	// ================================================================= covenant

	// ---- Entry points called from the Swing panel and from relay HTTP threads.
	// They all hop onto the client thread first: everything below reads the
	// RuneLite client (levels, quests, item containers), which is only safe there.

	@Override
	public void deal()
	{
		clientThread.invoke(this::doDeal);
	}

	@Override
	public void swear(String vowId)
	{
		clientThread.invoke(() -> doSwear(vowId));
	}

	@Override
	public void settleKept()
	{
		clientThread.invoke(this::doSettleKept);
	}

	@Override
	public void settleBroken(String reason)
	{
		clientThread.invoke(() -> doSettleBroken(reason));
	}

	@Override
	public void spend(String rite, String argument)
	{
		clientThread.invoke(() -> doSpend(rite, argument));
	}

	/** Put a mis-sworn Vow back on the table. Admin action, not a covenant rite. */
	public void unswear()
	{
		clientThread.invoke(this::doUnswear);
	}

	/** Wipe the run back to a fresh Era I covenant. */
	public void resetRun()
	{
		clientThread.invoke(this::doResetRun);
	}

	/** Answer the Herald standing at the current Doom step. */
	public void answerHerald(boolean won)
	{
		clientThread.invoke(() -> doAnswerHerald(won));
	}

	public void drawScar()
	{
		clientThread.invoke(this::doDrawScar);
	}

	/** Deal a fresh table of three. */
	private void doDeal()
	{
		if (ledger.hasActiveVow())
		{
			announce("A Vow is already sworn. Settle it before dealing again.");
			return;
		}

		List<Vow> hand = Decks.deal(ledger.era, config.audienceCardId(), random);
		ledger.hand = new ArrayList<>();
		for (Vow vow : hand)
		{
			ledger.hand.add(vow.getId());
		}

		if (drawOverlay != null)
		{
			drawOverlay.reset();
		}

		announce("Three cards on the table. The third is the Audience's Card.");
		persistAndPush();
	}

	private void doSwear(String vowId)
	{
		Vow vow = Decks.byId(vowId);
		if (vow == null || !ledger.hand.contains(vowId))
		{
			announce("That Vow is not on the table.");
			return;
		}

		if (ledger.scars.contains(Scar.DEBTOR) && !isLongestOnTable(vow))
		{
			announce("Debtor: you must swear the longest Vow on the table.");
			return;
		}

		ledger.activeVowId = vowId;
		ledger.activeIsAudienceCard = ledger.hand.indexOf(vowId) == 2;
		ledger.activeSwornAt = System.currentTimeMillis();
		ledger.activeBroken = false;
		ledger.activeBreakReason = null;
		ledger.activeProgress = 0;
		ledger.activeBaseline = objectiveTracker.currentCounter(vow);

		announce("Sworn: " + vow.getObjective() + " — " + vow.getBinding().getTitle() + ".");
		if (!vow.getBinding().isEnforceable())
		{
			announce(vow.getBinding().getTitle() + " cannot be checked by the plugin. That one is on your honour.");
		}

		persistAndPush();
	}

	private void doSettleKept()
	{
		Vow vow = activeVow();
		if (vow == null)
		{
			return;
		}

		int grace = vow.graceValue(ledger.activeIsAudienceCard);
		ledger.addGrace(grace);
		ledger.keptOaths.add(vow.getId());
		ledger.streak++;

		if (ledger.streak >= 3)
		{
			ledger.streak = 0;
			if (ledger.doom > 0)
			{
				ledger.doom--;
				announce("Three Kept Oaths. Doom falls to " + ledger.doom + ".");
			}
		}

		announce("Oath kept. +" + grace + " Grace (" + ledger.grace + "/" + ledger.graceCap() + ").");
		ledger.clearActive();
		ledger.hand.clear();
		maybeAdvanceEra();
		persistAndPush();
	}

	private void doSettleBroken(String reason)
	{
		Vow vow = activeVow();
		if (vow == null)
		{
			return;
		}

		ledger.doom = Math.min(10, ledger.doom + 1);
		ledger.streak = 0;
		announce("Vow broken: " + reason + ". Doom rises to " + ledger.doom + ".");

		if (config.autoScar())
		{
			doDrawScar();
		}
		else
		{
			announce("Draw a Scar from the panel when you are ready.");
		}

		ledger.clearActive();
		ledger.hand.clear();
		checkHerald();
		checkFall();
		persistAndPush();
	}

	/** Draw one Scar at random from those not already carried. */
	private Scar doDrawScar()
	{
		List<Scar> pool = new ArrayList<>();
		for (Scar scar : Scar.values())
		{
			if (!ledger.scars.contains(scar))
			{
				pool.add(scar);
			}
		}

		if (pool.isEmpty())
		{
			announce("Every Scar is already carried. The covenant has nothing left to take.");
			return null;
		}

		Scar drawn = pool.get(random.nextInt(pool.size()));
		ledger.scars.add(drawn);
		announce("Scar drawn: " + drawn.getTitle() + " — " + drawn.getRule());
		persistAndPush();
		return drawn;
	}

	private void doSpend(String rite, String argument)
	{
		switch (rite == null ? "" : rite.toLowerCase())
		{
			case "temper":
				if (pay(2))
				{
					ledger.tempers++;
					announce("Temper banked. You hold " + ledger.tempers + ".");
				}
				break;

			case "consecrate":
				if (pay(3))
				{
					ledger.consecrations.add(argument);
					announce("Consecrated: " + argument + ".");
				}
				break;

			case "vigil":
				if (ledger.vigilUsedThisEra)
				{
					announce("A Vigil has already been taken this Era.");
				}
				else if (pay(4))
				{
					ledger.vigilUsedThisEra = true;
					ledger.clearActive();
					ledger.hand.clear();
					announce("Vigil taken. No Scar, no Doom, no Vow this cycle.");
				}
				break;

			case "absolve":
			{
				int cost = ledger.absolveCost();
				Scar target = scarByTitle(argument);
				if (target == null)
				{
					announce("You do not carry that Scar.");
				}
				else if (pay(cost))
				{
					ledger.scars.remove(target);
					ledger.absolved++;
					announce("Absolved: " + target.getTitle() + ".");
				}
				break;
			}

			case "relic":
				if (pay(8))
				{
					announce("Relic bound: " + argument + " may be broken once without penalty.");
				}
				break;

			default:
				announce("Unknown rite.");
				return;
		}

		persistAndPush();
	}

	private void doUnswear()
	{
		Vow vow = activeVow();
		if (vow == null)
		{
			announce("No Vow is sworn.");
			return;
		}

		// The card goes back on the table exactly as it was dealt. No Grace, no
		// Scar, no Doom — this exists for misclicks, not for escaping a Binding.
		ledger.clearActive();
		announce("Unsworn: " + vow.getObjective() + ". The card is back on the table.");
		if (drawOverlay != null)
		{
			drawOverlay.reset();
		}
		persistAndPush();
	}

	private void doResetRun()
	{
		ledger = new Ledger();
		if (drawOverlay != null)
		{
			drawOverlay.reset();
		}
		announce("The covenant is torn up. A fresh run begins at Era I.");
		persistAndPush();
	}

	private void doAnswerHerald(boolean won)
	{
		if (!ledger.heraldDue())
		{
			announce("No Herald stands at Doom " + ledger.effectiveDoom() + ".");
			return;
		}

		if (won)
		{
			ledger.markHeraldAnswered();
			ledger.doom = Math.max(0, ledger.doom - 2);
			announce("The Herald falls. Doom drops to " + ledger.doom + " and a Relic is bound.");
		}
		else
		{
			doDrawScar();
			announce("The Herald stands. It returns next session, and nothing else counts.");
		}

		checkFall();
		persistAndPush();
	}

	/** Losing Hardcore status, or Doom 10, Hollows the account. */
	public void hollow()
	{
		if (ledger.hollowSpent)
		{
			announce("The covenant is closed. This account is retired to the Barrow.");
			return;
		}

		ledger.hollowed = true;
		ledger.hollowSpent = true;
		ledger.grace = 0;
		ledger.doom = 5;
		ledger.streak = 0;

		for (int i = 0; i < 3; i++)
		{
			doDrawScar();
		}

		announce("The account is Hollowed. Doom resets to 5. The Atonement begins.");
		persistAndPush();
	}

	// ================================================================= internals

	private void checkObjectiveComplete(Vow vow)
	{
		if (vow.getGoalType() == GoalType.MANUAL)
		{
			return;
		}

		int measured = objectiveTracker.isAbsolute(vow)
			? objectiveTracker.currentCounter(vow)
			: ledger.activeProgress;

		if (measured >= vow.getGoalAmount())
		{
			announce("Objective met. Settle the Vow when you are ready.");
		}
	}

	/**
	 * Recompute progress for the sworn Vow, but only when its goal is one of
	 * {@code types}, and only persist when the measured value actually changed.
	 * Without that gate this ran a bank scan and a config write on every XP drop.
	 */
	private void refreshProgress(GoalType... types)
	{
		Vow vow = activeVow();
		if (vow == null || ledger.activeBroken || !vow.isAutoTracked() || !objectiveTracker.isAbsolute(vow))
		{
			return;
		}

		boolean relevant = false;
		for (GoalType type : types)
		{
			if (vow.getGoalType() == type)
			{
				relevant = true;
				break;
			}
		}
		if (!relevant)
		{
			return;
		}

		int measured = objectiveTracker.currentCounter(vow);
		if (measured == ledger.activeProgress)
		{
			return;
		}

		ledger.activeProgress = measured;
		checkObjectiveComplete(vow);
		persistAndPush();
	}

	private void flagBreak(String reason)
	{
		ledger.activeBroken = true;
		ledger.activeBreakReason = reason;

		if (config.autoBreak())
		{
			doSettleBroken(reason);
		}
		else
		{
			announce("Binding broken — " + reason + ". Confirm in the panel to settle.");
			persistAndPush();
		}
	}

	private boolean withinGracePeriod()
	{
		long elapsed = System.currentTimeMillis() - ledger.activeSwornAt;
		return elapsed < config.graceSeconds() * 1000L;
	}

	private void maybeAdvanceEra()
	{
		if (!config.autoEra())
		{
			return;
		}

		Era satisfied = Era.highestSatisfied(client);
		if (satisfied.ordinal() > ledger.era.ordinal())
		{
			ledger.era = satisfied;
			ledger.vigilUsedThisEra = false;
			ledger.hand.clear();
			announce("Era " + satisfied.getNumeral() + " — " + satisfied.getTitle()
				+ ". The old deck is retired. Herald: " + satisfied.getHerald() + ".");
			persistAndPush();
		}
	}

	private void checkHerald()
	{
		if (ledger.heraldDue())
		{
			int step = ledger.effectiveDoom();
			announce("A Herald stands at Doom " + step + ": " + ledger.era.getHerald()
				+ ". It returns every session until it is answered.");
		}
	}

	private void checkFall()
	{
		if (ledger.doom >= 10)
		{
			announce("Doom 10. The Fall.");
			hollow();
		}
	}

	private boolean pay(int cost)
	{
		if (ledger.grace < cost)
		{
			announce("Not enough Grace — " + cost + " required, " + ledger.grace + " held.");
			return false;
		}
		ledger.grace -= cost;
		return true;
	}

	private boolean isLongestOnTable(Vow candidate)
	{
		Length longest = Length.SHORT;
		for (String id : ledger.hand)
		{
			Vow vow = Decks.byId(id);
			if (vow != null && vow.getLength().ordinal() > longest.ordinal())
			{
				longest = vow.getLength();
			}
		}
		return candidate.getLength() == longest;
	}

	private Scar scarByTitle(String title)
	{
		for (Scar scar : ledger.scars)
		{
			if (scar.getTitle().equalsIgnoreCase(title) || scar.name().equalsIgnoreCase(title))
			{
				return scar;
			}
		}
		return null;
	}

	private void startRelay()
	{
		try
		{
			relay.start(config.relayPort());
			announce("Companion tracker at " + relay.url());
		}
		catch (IOException e)
		{
			log.warn("Oathfall relay could not bind to port {}", config.relayPort(), e);
			announce("Could not open the tracker on port " + config.relayPort() + ". Try another port.");
		}
	}

	private void persistAndPush()
	{
		saveLedger();

		if (relay != null && relay.isRunning())
		{
			relay.broadcast(ledgerJson());
		}

		if (panel != null)
		{
			panel.refresh();
		}
	}

	private void loadLedger()
	{
		String json = config.ledger();
		if (json == null || json.isEmpty())
		{
			ledger = new Ledger();
			return;
		}

		try
		{
			Ledger loaded = gson.fromJson(json, Ledger.class);
			ledger = loaded == null ? new Ledger() : loaded;
		}
		catch (JsonSyntaxException e)
		{
			log.warn("Oathfall ledger was unreadable; starting a fresh covenant", e);
			ledger = new Ledger();
		}
	}

	private void saveLedger()
	{
		config.setLedger(gson.toJson(ledger));
	}

	private void announce(String message)
	{
		log.debug("Oathfall: {}", message);

		if (!config.chatFeedback())
		{
			return;
		}

		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage("[Oathfall] " + message)
			.build());
	}

	// ================================================================= accessors

	@Override
	public String ledgerJson()
	{
		return gson.toJson(ledger);
	}

	public Ledger getLedger()
	{
		return ledger;
	}

	public Vow activeVow()
	{
		return Decks.byId(ledger.activeVowId);
	}

	public List<Vow> currentHand()
	{
		List<Vow> hand = new ArrayList<>();
		for (String id : ledger.hand)
		{
			Vow vow = Decks.byId(id);
			if (vow != null)
			{
				hand.add(vow);
			}
		}
		return hand;
	}

	public boolean isDrawOverlayEnabled()
	{
		return config.showDrawOverlay();
	}

	public String relayUrl()
	{
		return relay != null && relay.isRunning() ? relay.url() : null;
	}
}
