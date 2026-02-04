package com.directchat.api;

import com.directchat.DirectChatPlugin;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP server for DirectChat API.
 */
public class WebServer {

    private final DirectChatPlugin plugin;
    private final int port;
    private HttpServer server;
    private final ApiHandler apiHandler;

    public WebServer(DirectChatPlugin plugin, int port) {
        this.plugin = plugin;
        this.port = port;
        this.apiHandler = new ApiHandler(plugin);
    }

    /**
     * Start the HTTP server.
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Register endpoints
        server.createContext("/api/auth", new AuthHandler());
        server.createContext("/api/send", new SendHandler());
        server.createContext("/api/fetch", new FetchHandler());
        server.createContext("/api/health", new HealthHandler());

        // Use a thread pool for handling requests
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        plugin.getLogger().info("API endpoints registered: /api/auth, /api/send, /api/fetch, /api/health");
    }

    /**
     * Stop the HTTP server.
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    /**
     * Handler for /api/auth endpoint.
     */
    private class AuthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"status\":\"ERROR\",\"message\":\"Method not allowed\"}");
                return;
            }

            String body = readRequestBody(exchange);
            String response = apiHandler.handleAuth(body);
            sendResponse(exchange, 200, response);
        }
    }

    /**
     * Handler for /api/send endpoint.
     */
    private class SendHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"status\":\"ERROR\",\"message\":\"Method not allowed\"}");
                return;
            }

            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                sendResponse(exchange, 401, "{\"status\":\"ERROR\",\"message\":\"Missing or invalid authorization\"}");
                return;
            }

            String token = authHeader.substring(7);
            String body = readRequestBody(exchange);
            String response = apiHandler.handleSend(token, body);
            sendResponse(exchange, 200, response);
        }
    }

    /**
     * Handler for /api/fetch endpoint.
     */
    private class FetchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"status\":\"ERROR\",\"message\":\"Method not allowed\"}");
                return;
            }

            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                sendResponse(exchange, 401, "{\"status\":\"ERROR\",\"message\":\"Missing or invalid authorization\"}");
                return;
            }

            String token = authHeader.substring(7);

            // Parse query parameter 'since'
            String query = exchange.getRequestURI().getQuery();
            long since = 0;
            if (query != null && query.startsWith("since=")) {
                try {
                    since = Long.parseLong(query.substring(6));
                } catch (NumberFormatException ignored) {
                }
            }

            String response = apiHandler.handleFetch(token, since);
            sendResponse(exchange, 200, response);
        }
    }

    /**
     * Handler for /api/health endpoint.
     */
    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendResponse(exchange, 200, "{\"status\":\"OK\",\"plugin\":\"DirectChat\",\"version\":\"1.0.0\"}");
        }
    }

    /**
     * Read request body as string.
     */
    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Send HTTP response.
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
