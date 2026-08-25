package com.oathfall.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The whole persisted run. Serialised to the RuneLite config store as JSON and
 * served verbatim to the companion tracker, so field names here are the public
 * shape of the relay API.
 */
public class Ledger
{
	// --- covenant state -----------------------------------------------------
	public Era era = Era.WAKING;
	public int doom = 0;
	public int grace = 0;
	public int streak = 0;               // consecutive Kept Oaths
	public int tempers = 0;              // banked redraws
	public boolean hollowed = false;
	public boolean hollowSpent = false;  // you may Hollow once
	public boolean vigilUsedThisEra = false;
	/** Scars absolved so far; drives the escalating Absolve cost. Persisted. */
	public int absolved = 0;

	public List<Scar> scars = new ArrayList<>();
	public List<String> keptOaths = new ArrayList<>();
	public List<String> consecrations = new ArrayList<>();

	// --- the table ----------------------------------------------------------
	/** Three Vow ids: two from the deck, the third is the Audience's Card. */
	public List<String> hand = new ArrayList<>();

	// --- the sworn Vow ------------------------------------------------------
	public String activeVowId = null;
	public boolean activeIsAudienceCard = false;
	public long activeSwornAt = 0L;
	public int activeProgress = 0;
	public int activeBaseline = 0;       // counter value at the moment of swearing
	public boolean activeBroken = false;
	public String activeBreakReason = null;

	/** Doom steps that have already been answered, so a Herald fires once each. */
	public List<Integer> heraldsAnswered = new ArrayList<>();

	public int graceCap()
	{
		return scars.contains(Scar.CRACKED_LEDGER) ? 8 : 10;
	}

	/** Absolve costs 6, and one more for every Scar already absolved. */
	public int absolveCost()
	{
		return 6 + absolved;
	}

	public boolean heraldDue()
	{
		int step = effectiveDoom();
		return (step == 3 || step == 6 || step == 9) && !heraldsAnswered.contains(step);
	}

	/** Record that the Herald standing at the current step has been answered. */
	public void markHeraldAnswered()
	{
		int step = effectiveDoom();
		if (!heraldsAnswered.contains(step))
		{
			heraldsAnswered.add(step);
		}
	}

	/** The Marked Scar pulls the next Herald one step closer. */
	public int effectiveDoom()
	{
		return scars.contains(Scar.MARKED) ? Math.min(10, doom + 1) : doom;
	}

	public boolean hasActiveVow()
	{
		return activeVowId != null;
	}

	public void addGrace(int amount)
	{
		grace = Math.min(graceCap(), grace + amount);
	}

	public void clearActive()
	{
		activeVowId = null;
		activeIsAudienceCard = false;
		activeSwornAt = 0L;
		activeProgress = 0;
		activeBaseline = 0;
		activeBroken = false;
		activeBreakReason = null;
	}
}
