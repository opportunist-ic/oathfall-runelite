package com.oathfall.relay;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A loopback-only HTTP relay that serves the Oathfall companion tracker and a
 * live ledger feed.
 *
 * Deliberate constraints, because this is what Plugin Hub review will ask about:
 *   - binds to 127.0.0.1, so nothing on the network can reach it;
 *   - every request must carry a token minted fresh each session;
 *   - it is off by default and never contacts anything outbound.
 */
@Slf4j
public class RelayServer
{
	/** Actions the tracker page can ask the plugin to perform. */
	public interface Handler
	{
		String ledgerJson();

		void deal();

		void swear(String vowId);

		void settleKept();

		void settleBroken(String reason);

		void spend(String rite, String argument);
	}

	private final Handler handler;
	private final String token;
	private final List<HttpExchange> streams = new CopyOnWriteArrayList<>();

	private HttpServer server;
	private ExecutorService executor;
	private int port;

	public RelayServer(Handler handler)
	{
		this.handler = handler;
		byte[] raw = new byte[18];
		new SecureRandom().nextBytes(raw);
		this.token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
	}

	public void start(int port) throws IOException
	{
		stop();
		this.port = port;

		server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 0);
		executor = Executors.newFixedThreadPool(2);
		server.setExecutor(executor);

		server.createContext("/", this::serveTracker);
		server.createContext("/api/ledger", this::serveLedger);
		server.createContext("/api/stream", this::serveStream);
		server.createContext("/api/action", this::serveAction);

		server.start();
		log.info("Oathfall relay listening on {}", url());
	}

	public void stop()
	{
		for (HttpExchange ex : streams)
		{
			try
			{
				ex.close();
			}
			catch (RuntimeException ignored)
			{
				// client already gone
			}
		}
		streams.clear();

		if (server != null)
		{
			server.stop(0);
			server = null;
		}

		// HttpServer.stop does not touch the executor, so toggling the relay would
		// otherwise leak its threads every time it was switched back on.
		if (executor != null)
		{
			executor.shutdownNow();
			executor = null;
		}
	}

	public boolean isRunning()
	{
		return server != null;
	}

	/** The address to open in a browser, token included. */
	public String url()
	{
		return "http://127.0.0.1:" + port + "/?t=" + token;
	}

	/** Push the current ledger to every connected tracker page. */
	public void broadcast(String json)
	{
		byte[] payload = ("data: " + json.replace("\n", " ") + "\n\n").getBytes(StandardCharsets.UTF_8);

		for (HttpExchange ex : streams)
		{
			try
			{
				OutputStream out = ex.getResponseBody();
				out.write(payload);
				out.flush();
			}
			catch (IOException e)
			{
				streams.remove(ex);
				try
				{
					ex.close();
				}
				catch (RuntimeException ignored)
				{
					// nothing to do
				}
			}
		}
	}

	// ------------------------------------------------------------------ routes

	private void serveTracker(HttpExchange ex) throws IOException
	{
		if (!authorised(ex))
		{
			return;
		}

		byte[] page;
		try (InputStream in = getClass().getResourceAsStream("/com/oathfall/tracker.html"))
		{
			if (in == null)
			{
				send(ex, 500, "text/plain", "tracker.html missing from the plugin jar".getBytes(StandardCharsets.UTF_8));
				return;
			}
			page = readAll(in);
		}

		// The page needs the token to call back into the relay.
		String html = new String(page, StandardCharsets.UTF_8).replace("__OATHFALL_TOKEN__", token);
		send(ex, 200, "text/html; charset=utf-8", html.getBytes(StandardCharsets.UTF_8));
	}

	private void serveLedger(HttpExchange ex) throws IOException
	{
		if (!authorised(ex))
		{
			return;
		}
		send(ex, 200, "application/json; charset=utf-8", handler.ledgerJson().getBytes(StandardCharsets.UTF_8));
	}

	private void serveStream(HttpExchange ex) throws IOException
	{
		if (!authorised(ex))
		{
			return;
		}

		ex.getResponseHeaders().add("Content-Type", "text/event-stream; charset=utf-8");
		ex.getResponseHeaders().add("Cache-Control", "no-store");
		ex.getResponseHeaders().add("Connection", "keep-alive");
		ex.sendResponseHeaders(200, 0);

		streams.add(ex);

		OutputStream out = ex.getResponseBody();
		out.write(("data: " + handler.ledgerJson().replace("\n", " ") + "\n\n").getBytes(StandardCharsets.UTF_8));
		out.flush();
	}

	private void serveAction(HttpExchange ex) throws IOException
	{
		if (!authorised(ex))
		{
			return;
		}

		if (!"POST".equalsIgnoreCase(ex.getRequestMethod()))
		{
			send(ex, 405, "text/plain", "POST only".getBytes(StandardCharsets.UTF_8));
			return;
		}

		Map<String, String> q = query(ex.getRequestURI());
		String action = q.getOrDefault("do", "");
		String arg = q.getOrDefault("arg", "");

		try
		{
			switch (action)
			{
				case "deal":
					handler.deal();
					break;
				case "swear":
					handler.swear(arg);
					break;
				case "kept":
					handler.settleKept();
					break;
				case "broken":
					handler.settleBroken(arg.isEmpty() ? "Settled from the tracker" : arg);
					break;
				case "spend":
					handler.spend(arg, q.getOrDefault("arg2", ""));
					break;
				default:
					send(ex, 400, "text/plain", "unknown action".getBytes(StandardCharsets.UTF_8));
					return;
			}
		}
		catch (RuntimeException e)
		{
			log.warn("Oathfall relay action '{}' failed", action, e);
			send(ex, 500, "text/plain", "action failed".getBytes(StandardCharsets.UTF_8));
			return;
		}

		send(ex, 200, "application/json; charset=utf-8", handler.ledgerJson().getBytes(StandardCharsets.UTF_8));
	}

	// ------------------------------------------------------------------ plumbing

	private boolean authorised(HttpExchange ex) throws IOException
	{
		String supplied = query(ex.getRequestURI()).get("t");
		if (token.equals(supplied))
		{
			return true;
		}

		send(ex, 403, "text/plain", "Oathfall relay: bad or missing token".getBytes(StandardCharsets.UTF_8));
		return false;
	}

	private static Map<String, String> query(URI uri)
	{
		Map<String, String> out = new HashMap<>();
		String raw = uri.getRawQuery();
		if (raw == null || raw.isEmpty())
		{
			return out;
		}

		for (String pair : raw.split("&"))
		{
			int eq = pair.indexOf('=');
			if (eq <= 0)
			{
				continue;
			}
			out.put(decode(pair.substring(0, eq)), decode(pair.substring(eq + 1)));
		}
		return out;
	}

	private static String decode(String s)
	{
		try
		{
			return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8.name());
		}
		catch (Exception e)
		{
			return s;
		}
	}

	private static void send(HttpExchange ex, int status, String contentType, byte[] body) throws IOException
	{
		ex.getResponseHeaders().add("Content-Type", contentType);
		ex.getResponseHeaders().add("Cache-Control", "no-store");
		ex.sendResponseHeaders(status, body.length);
		try (OutputStream out = ex.getResponseBody())
		{
			out.write(body);
		}
	}

	private static byte[] readAll(InputStream in) throws IOException
	{
		List<byte[]> chunks = new ArrayList<>();
		byte[] buf = new byte[8192];
		int total = 0, read;
		while ((read = in.read(buf)) > 0)
		{
			byte[] chunk = new byte[read];
			System.arraycopy(buf, 0, chunk, 0, read);
			chunks.add(chunk);
			total += read;
		}

		byte[] all = new byte[total];
		int at = 0;
		for (byte[] chunk : chunks)
		{
			System.arraycopy(chunk, 0, all, at, chunk.length);
			at += chunk.length;
		}
		return all;
	}
}
