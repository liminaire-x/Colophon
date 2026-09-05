package kr.guinnessgroup.colophon.web;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import kr.guinnessgroup.colophon.runtime.ColophonRuntime;
import kr.guinnessgroup.colophon.runtime.NodeRegistry;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP server that hosts the Colophon web editor and its API.
 * <p>
 * v0 uses the JDK built-in {@link HttpServer} so the mod needs no third-party
 * dependencies. It runs on its own daemon thread; publish hands the graph to the
 * {@link ColophonRuntime}, which applies it on the main thread.
 */
public final class ColophonWebServer {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String EDITOR_INDEX = "/colophon/web/index.html";
    public static final int PORT = 8080;

    private final ColophonRuntime runtime;
    private HttpServer server;

    public ColophonWebServer(ColophonRuntime runtime) {
        this.runtime = runtime;
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
            server.createContext("/api/health", this::handleHealth);
            server.createContext("/api/schema", this::handleSchema);
            server.createContext("/api/publish", this::handlePublish);
            server.start();
            LOGGER.info("[Colophon] Web editor server started on http://localhost:{}", PORT);
        } catch (IOException e) {
            LOGGER.error("[Colophon] Failed to start web server on port {}", PORT, e);
            server = null;
        }
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            LOGGER.info("[Colophon] Web editor server stopped");
        }
    }

    // --- handlers ---

    private void handleRoot(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) {
            send(ex, 405, "text/plain; charset=utf-8", "Method Not Allowed");
            return;
        }
        byte[] page = readResource(EDITOR_INDEX);
        if (page == null) {
            String fallback = "<!doctype html><meta charset=\"utf-8\"><h1>Colophon</h1>"
                    + "<p>Editor build not found on the classpath (" + EDITOR_INDEX + "). "
                    + "Build the editor and place index.html under src/main/resources.</p>";
            send(ex, 200, "text/html; charset=utf-8", fallback);
            return;
        }
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(200, page.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(page);
        }
    }

    private void handleHealth(HttpExchange ex) throws IOException {
        send(ex, 200, "application/json; charset=utf-8", "{\"status\":\"ok\",\"mod\":\"colophon\"}");
    }

    private void handleSchema(HttpExchange ex) throws IOException {
        send(ex, 200, "application/json; charset=utf-8", NodeRegistry.schemaJson());
    }

    private void handlePublish(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            send(ex, 405, "text/plain; charset=utf-8", "Method Not Allowed");
            return;
        }
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        try {
            runtime.publish(body);
            send(ex, 202, "application/json; charset=utf-8", "{\"accepted\":true}");
        } catch (Exception e) {
            LOGGER.warn("[Colophon] Publish rejected: {}", e.getMessage());
            JsonObject resp = new JsonObject();
            resp.addProperty("accepted", false);
            resp.addProperty("error", String.valueOf(e.getMessage()));
            send(ex, 400, "application/json; charset=utf-8", resp.toString());
        }
    }

    // --- helpers ---

    private byte[] readResource(String path) {
        try (InputStream in = ColophonWebServer.class.getResourceAsStream(path)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            LOGGER.error("[Colophon] Failed to read resource {}", path, e);
            return null;
        }
    }

    private void send(HttpExchange ex, int code, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
