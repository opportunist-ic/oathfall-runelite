# Oathfall — RuneLite plugin

**Author:** daniel k · **Support:** Discord `khentimentu` (or open a GitHub issue)

Ledger, binding watchdog and companion tracker for the [Oathfall](https://claude.ai/code/artifact/d82f0909-58b0-4fcc-a47f-258acdba388c)
hardcore ironman covenant.

The plugin keeps the run's state, deals the Vow table, watches the sworn Binding
for observable breaks, moves the Doom track, and serves a live tracker page you
can put on a second monitor or straight into a stream layout.

---

## What it does not do

**No gameplay automation.** Nothing here clicks, types, walks, or plays for you.
The Plugin Hub rejects input automation outright and Jagex bans accounts for it.
Every mechanic below is passive: it reads game state and menu options you have
already clicked. "Automation" in this plugin means automatic *bookkeeping* —
progress detection, break detection, Era advancement, and syncing.

---

## Features

| | |
|---|---|
| **The deck** | All 60 Vows across the five Eras, matching the codex card for card. |
| **Dealing** | Two cards from your Era deck plus the Audience's Card in seat three. |
| **Binding watchdog** | Detects breaks of the eight observable Bindings (below). |
| **Objective tracking** | 46 of the 60 Vows report progress with no typing at all. |
| **Doom track** | Rises on breaks and deaths, falls after three Kept Oaths, fires Heralds at 3 / 6 / 9. |
| **Scars** | Drawn at random on a break, persisted for the life of the run. |
| **Era gates** | Advances the deck automatically the moment base combat and total level clear the gate. |
| **The Hollowing** | Resets Doom to 5, draws three Scars, flips the win condition to the Atonement. |
| **Companion tracker** | A live page served from the client, off by default. |

### Which Bindings the plugin can actually police

Watched (a break is detected from game events):

| Binding | Signal |
|---|---|
| Starving | `MenuOptionClicked` "Eat", plus brews/stews/wine on "Drink" |
| Cold Iron | `MenuOptionClicked` "Drink" |
| Grounded | teleport menu options and targets |
| Silent | any active prayer on the game tick |
| Sunless | run toggle varp |
| Barefoot | boots / gloves / cape equipment slots |
| One-Handed | shield slot |
| Unbroken | `ActorDeath` on the local player |

Honour-only — the panel and overlay label these `honour`, and the plugin never
pretends to check them: **Blind**, **Rusted**, **Tithed**, **Named**.

A configurable grace period (default 20s) after swearing keeps banking and
gearing up from voiding a Vow before it starts.

---

## Connecting it to the web app

This is the part worth reading carefully, because one obvious route is closed.

**The published Oathfall artifact cannot call the plugin.** Artifact pages run
under a strict Content Security Policy that blocks `fetch`, `XHR` and WebSockets
to any host other than the page's own origin — and `http://127.0.0.1` counts as
another host. No amount of CORS headers on the plugin's side changes that; the
block is in the browser, before the request is made.

So there are two bridges instead, and both work:

### 1. The served tracker (live, recommended)

Turn on **Serve the companion tracker** in the plugin settings. The plugin binds
a small HTTP server to `127.0.0.1` and serves its own copy of the tracker. Because
the page is served *by the plugin*, it is same-origin with the relay and can hold
an open `EventSource` stream — state updates land the instant they happen.

Click **Copy tracker link** in the side panel and open the URL. It looks like:

```
http://127.0.0.1:7373/?t=<session-token>
```

| Endpoint | Purpose |
|---|---|
| `GET /` | the tracker page |
| `GET /api/ledger` | current ledger as JSON |
| `GET /api/stream` | server-sent events, pushed on every change |
| `POST /api/action?do=deal\|swear\|kept\|broken\|spend` | actions from the page |

Security posture, since Plugin Hub review will ask:

- binds to `127.0.0.1` only, so nothing on your network can reach it;
- every request must carry a token minted fresh each session;
- **off by default**;
- makes no outbound connections, ever.

### 2. The clipboard ledger (works with the hosted artifact)

**Copy ledger JSON** in the side panel puts the whole run on your clipboard.
Paste it into the *Companion* section of the published codex page and it renders
your live Doom, Grace, Era, Scars and sworn Vow. No network involved, so the CSP
has nothing to object to. Good enough for an end-of-episode ledger card.

---

## Building

```bash
./gradlew build
```

To launch a development client with the plugin loaded, double-click
**`run-dev-client.bat`** (Windows) or run **`./run-dev-client.sh`**. Both are
thin wrappers around:

```bash
./gradlew run
```

The first launch downloads Gradle and the RuneLite client and takes a few
minutes; later launches are quick. Once the client is up, open the **Oathfall**
tab in the right-hand sidebar to deal a hand. If you use a Jagex account, the
dev client needs [an extra login step](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

Verified building against **RuneLite 1.12.36** on **JDK 11**, with zero
deprecation warnings.

### Submitting to the Plugin Hub

1. Push this repository to GitHub as a **public** repo.
2. Fork [runelite/plugin-hub](https://github.com/runelite/plugin-hub).
3. Add a file named `oathfall` under `plugins/` containing:

   ```
   repository=https://github.com/opportunist-ic/oathfall-runelite.git
   commit=<full 40-character commit hash>
   ```

4. Open a pull request against `runelite/plugin-hub` and watch the CI result.

Note on `support=`: the Plugin Hub renders that field as the plugin's "Report an
issue" link, so it needs a URL rather than a Discord handle. It points at this
repository's issue tracker; the Discord contact is carried in the plugin
description and at the top of this README.

---

## Layout

```
src/main/java/com/oathfall/
  OathfallPlugin.java      lifecycle, event wiring, covenant rules
  OathfallConfig.java      settings
  model/                   Binding, Scar, Era, Vow, Length, GoalType, Ledger
  deck/Decks.java          all 60 Vows with machine-readable goals
  track/BindingMonitor     observable Binding breaks
  track/ObjectiveTracker   skill / quest / item / kill-count / clue progress
  relay/RelayServer        loopback HTTP + SSE
  ui/OathfallPanel         side panel
  ui/OathfallOverlay       in-game overlay
src/main/resources/com/oathfall/
  tracker.html             the served companion page
  panel_icon.png           navigation button icon
```

## Licence

BSD 2-Clause. See `LICENSE`.
