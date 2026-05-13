package com.directchat.tunnel;

import com.directchat.DirectChatPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages a Cloudflare Quick Tunnel using the cloudflared CLI.
 * Used when the server cannot open ports (e.g., Aternos, Play.hosting).
 *
 * The plugin starts its API on localhost:{port}, then cloudflared creates
 * a public HTTPS tunnel to that local port automatically.
 */
public class CloudflaredTunnelManager {

    // Pattern to extract the tunnel URL from cloudflared output
    // Matches lines like: "https://abc-xyz-123.trycloudflare.com"
    private static final Pattern TUNNEL_URL_PATTERN =
            Pattern.compile("https://[a-zA-Z0-9\\-]+\\.trycloudflare\\.com");

    private final DirectChatPlugin plugin;
    private Process tunnelProcess;
    private String tunnelUrl;
    private boolean running = false;

    // Custom cloudflared binary path (null = use system PATH)
    private final String cloudflaredPath;

    public CloudflaredTunnelManager(DirectChatPlugin plugin, String cloudflaredPath) {
        this.plugin = plugin;
        this.cloudflaredPath = cloudflaredPath;
    }

    /**
     * Start a Cloudflare Quick Tunnel.
     * This tunnels the local API port through Cloudflare's network,
     * making it accessible via a public HTTPS URL.
     *
     * @param localPort The local port the API server is listening on
     * @return CompletableFuture that resolves with the tunnel URL, or fails with an error
     */
    public CompletableFuture<String> startTunnel(int localPort) {
        CompletableFuture<String> future = new CompletableFuture<>();

        if (running) {
            future.completeExceptionally(new IllegalStateException("Tunnel is already running"));
            return future;
        }

        String localUrl = "http://localhost:" + localPort;
        plugin.getLogger().info("[Cloudflared] Starting Quick Tunnel for " + localUrl + "...");

        try {
            ProcessBuilder pb;
            if (cloudflaredPath != null && !cloudflaredPath.isEmpty()) {
                pb = new ProcessBuilder(cloudflaredPath, "tunnel", "--url", localUrl);
            } else {
                pb = new ProcessBuilder("cloudflared", "tunnel", "--url", localUrl);
            }

            pb.redirectErrorStream(true);
            pb.environment().put("NO_COLOR", "1");

            tunnelProcess = pb.start();

            // Read output in a separate thread to capture the tunnel URL
            Thread readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(tunnelProcess.getInputStream()))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        plugin.debug("[Cloudflared] " + line);

                        // Check for tunnel URL in output
                        Matcher matcher = TUNNEL_URL_PATTERN.matcher(line);
                        if (matcher.find()) {
                            String url = matcher.group();
                            tunnelUrl = url;
                            running = true;

                            plugin.getLogger().info("[Cloudflared] Tunnel established!");
                            plugin.getLogger().info("[Cloudflared] Public URL: " + url);
                            plugin.getLogger().info("[Cloudflared] Connect with: /directchat connect " + url + " <password>");

                            // Broadcast to admins in-game
                            plugin.getChatManager().broadcastSystemMessage(
                                    "\u00a76[Cloudflared] Tunnel started! Connect via: \u00a7a" + url);

                            future.complete(url);
                        }

                        // Check for error indicators
                        if (line.contains("failed") || line.contains("error") || line.contains("ERROR")) {
                            plugin.getLogger().warning("[Cloudflared] " + line);
                        }
                    }

                    // Process exited
                    int exitCode = tunnelProcess.waitFor();
                    running = false;

                    if (exitCode != 0) {
                        String msg = "cloudflared exited with code " + exitCode;
                        plugin.getLogger().warning("[Cloudflared] " + msg);
                        if (!future.isDone()) {
                            future.completeExceptionally(new IOException(msg));
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    running = false;
                    plugin.getLogger().warning("[Cloudflared] Error reading tunnel output: " + e.getMessage());
                    if (!future.isDone()) {
                        future.completeExceptionally(e);
                    }
                }
            }, "DirectChat-CloudflaredReader");

            readerThread.setDaemon(true);
            readerThread.start();

            // Timeout after 30 seconds if no URL is found
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!future.isDone()) {
                    String msg = "Timed out waiting for cloudflared tunnel URL (30s)";
                    future.completeExceptionally(new IOException(msg));
                    stopTunnel();
                }
            }, 600L); // 600 ticks = 30 seconds

        } catch (IOException e) {
            future.completeExceptionally(e);
            plugin.getLogger().severe("[Cloudflared] Failed to start tunnel: " + e.getMessage());
        }

        return future;
    }

    /**
     * Stop the Cloudflare tunnel.
     */
    public void stopTunnel() {
        if (tunnelProcess != null && tunnelProcess.isAlive()) {
            plugin.getLogger().info("[Cloudflared] Stopping tunnel...");
            tunnelProcess.destroy();

            // Wait up to 5 seconds for graceful shutdown
            try {
                if (!tunnelProcess.waitFor(5, TimeUnit.SECONDS)) {
                    tunnelProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                tunnelProcess.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }

        tunnelProcess = null;
        tunnelUrl = null;
        running = false;
        plugin.getLogger().info("[Cloudflared] Tunnel stopped");
    }

    /**
     * Check if the tunnel is currently running.
     */
    public boolean isRunning() {
        return running && tunnelProcess != null && tunnelProcess.isAlive();
    }

    /**
     * Get the public tunnel URL, or null if not running.
     */
    public String getTunnelUrl() {
        return tunnelUrl;
    }

    /**
     * Check if cloudflared is available on the system.
     *
     * @return true if cloudflared binary is found
     */
    public boolean isCloudflaredAvailable() {
        try {
            ProcessBuilder pb;
            if (cloudflaredPath != null && !cloudflaredPath.isEmpty()) {
                pb = new ProcessBuilder(cloudflaredPath, "--version");
            } else {
                pb = new ProcessBuilder("cloudflared", "--version");
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String version = reader.readLine();
                    plugin.getLogger().info("[Cloudflared] Found: " + version);
                }
                return true;
            }
        } catch (IOException | InterruptedException e) {
            return false;
        }
        return false;
    }
}
