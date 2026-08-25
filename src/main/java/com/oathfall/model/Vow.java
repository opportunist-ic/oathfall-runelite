package com.oathfall.model;

/** One card in an Era deck. Immutable; live progress lives on the Ledger. */
public class Vow
{
	private final String id;
	private final Era era;
	private final int index;
	private final String objective;
	private final Binding binding;
	private final Length length;
	private final GoalType goalType;
	private final String goalTarget;
	private final int goalAmount;

	public Vow(Era era, int index, String objective, Binding binding, Length length,
			   GoalType goalType, String goalTarget, int goalAmount)
	{
		this.era = era;
		this.index = index;
		this.id = String.format("%s-%02d", era.getCode(), index + 1);
		this.objective = objective;
		this.binding = binding;
		this.length = length;
		this.goalType = goalType;
		this.goalTarget = goalTarget;
		this.goalAmount = goalAmount;
	}

	public String getId()
	{
		return id;
	}

	public Era getEra()
	{
		return era;
	}

	public int getIndex()
	{
		return index;
	}

	public String getObjective()
	{
		return objective;
	}

	public Binding getBinding()
	{
		return binding;
	}

	public Length getLength()
	{
		return length;
	}

	public GoalType getGoalType()
	{
		return goalType;
	}

	public String getGoalTarget()
	{
		return goalTarget;
	}

	public int getGoalAmount()
	{
		return goalAmount;
	}

	public boolean isAutoTracked()
	{
		return goalType != GoalType.MANUAL;
	}

	/** Grace paid on a Kept Oath. The Audience's Card is worth one more. */
	public int graceValue(boolean audienceCard)
	{
		return length.getGrace() + (audienceCard ? 1 : 0);
	}

	@Override
	public String toString()
	{
		return id + " " + objective + " [" + binding.getTitle() + "]";
	}
}
