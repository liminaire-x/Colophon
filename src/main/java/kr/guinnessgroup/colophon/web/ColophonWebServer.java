/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import kr.guinnessgroup.colophon.DocumentException;
import kr.guinnessgroup.colophon.npc.Npcs;
import kr.guinnessgroup.colophon.runtime.ColophonRuntime;
import kr.guinnessgroup.colophon.runtime.NodeRegistry;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Serves the web editor and its API on port 8080:
 * <ul>
 *   <li>{@code GET /api/health}</li>
 *   <li>{@code GET /api/schema} — node types for the palette</li>
 *   <li>{@code GET /api/graphs} — the current graph document</li>
 *   <li>{@code GET /api/npcs} — the current NPC document</li>
 *   <li>{@code GET /api/npc-placements} — where each NPC stands</li>
 *   <li>{@code POST /api/publish} — replace both documents: {@code {"graphs": ..., "npcs": ...}}</li>
 * </ul>
 */
public final class ColophonWebServer {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String EDITOR_INDEX = "/colophon/web/index.html";
    private static final String JSON = "application/json; charset=utf-8";
    public static final int PORT = 8080;

    private final ColophonRuntime runtime;
    private final NodeRegistry registry;
    private final Npcs npcs;
    private HttpServer server;

    public ColophonWebServer(ColophonRuntime runtime, NodeRegistry registry, Npcs npcs) {
        this.runtime = runtime;
        this.registry = registry;
        this.npcs = npcs;
    }

    public synchronized void start() {
        if (server != null) {
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.setExecutor(Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "Colophon-Web");
                t.setDaemon(true);
                return t;
            }));
            server.createContext("/", this::handleRoot);
            server.createContext("/api/health", ex -> send(ex, 200, JSON, "{\"status\":\"ok\"}"));
            server.createContext("/api/schema", ex -> send(ex, 200, JSON, registry.schemaJson()));
            server.createContext("/api/graphs", ex -> getOnly(ex, runtime.graphsJson()));
            server.createContext("/api/npcs", ex -> getOnly(ex, runtime.npcsJson()));
            server.createContext("/api/npc-placements", ex -> getOnly(ex, npcs.placementsJson()));
            server.createContext("/api/publish", this::handlePublish);
            server.start();
            LOGGER.info("[Colophon] Web editor at http://localhost:{}", PORT);
        } catch (IOException e) {
            LOGGER.error("[Colophon] Failed to start web server on port {}", PORT, e);
            server = null;
        }
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private void handleRoot(HttpExchange ex) throws IOException {
        byte[] page = readResource(EDITOR_INDEX);
        if (page == null) {
            send(ex, 200, "text/html; charset=utf-8",
                    "<!doctype html><meta charset=\"utf-8\"><h1>Colophon</h1><p>Editor build not found.</p>");
            return;
        }
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(200, page.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(page);
        }
    }

    private static void getOnly(HttpExchange ex, String json) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) {
            send(ex, 405, "text/plain; charset=utf-8", "Method Not Allowed");
            return;
        }
        send(ex, 200, JSON, json);
    }

    private void handlePublish(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            send(ex, 405, "text/plain; charset=utf-8", "Method Not Allowed");
            return;
        }
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        try {
            runtime.publish(body);
            send(ex, 200, JSON, "{\"accepted\":true}");
        } catch (DocumentException e) {
            LOGGER.warn("[Colophon] Publish rejected: {}", e.errors());
            send(ex, 400, JSON, rejected(e.errors()));
        } catch (RuntimeException e) {
            LOGGER.error("[Colophon] Publish failed", e);
            send(ex, 500, JSON, rejected(List.of("server error: " + e.getMessage())));
        }
    }

    private static String rejected(List<String> errors) {
        JsonArray arr = new JsonArray();
        errors.forEach(arr::add);
        JsonObject o = new JsonObject();
        o.addProperty("accepted", false);
        o.add("errors", arr);
        return o.toString();
    }

    private byte[] readResource(String path) {
        try (InputStream in = ColophonWebServer.class.getResourceAsStream(path)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            LOGGER.error("[Colophon] Failed to read {}", path, e);
            return null;
        }
    }

    private static void send(HttpExchange ex, int code, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
