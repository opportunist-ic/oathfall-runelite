package com.oathfall.model;

public enum Length
{
	SHORT("Short", 1),
	STANDARD("Standard", 2),
	LONG("Long", 4);

	private final String title;
	private final int grace;

	Length(String title, int grace)
	{
		this.title = title;
		this.grace = grace;
	}

	public String getTitle()
	{
		return title;
	}

	/** Base Grace paid on a Kept Oath, before the Audience's Card bonus. */
	public int getGrace()
	{
		return grace;
	}

	public Length escalate()
	{
		return this == SHORT ? STANDARD : LONG;
	}
}
