package com.oathfall;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Launches a development RuneLite client with Oathfall loaded.
 * Run it with the {@code run} Gradle task.
 */
public class OathfallPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(OathfallPlugin.class);
		RuneLite.main(args);
	}
}
