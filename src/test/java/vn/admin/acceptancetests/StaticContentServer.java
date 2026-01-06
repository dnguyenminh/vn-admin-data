package vn.admin.acceptancetests;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class StaticContentServer {
    private static HttpServer server;

    @BeforeAll
    public static void startServer() throws IOException {
        Path staticDir = Paths.get("src/main/resources/static");
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String path = exchange.getRequestURI().getPath();
                if (path.equals("/")) path = "/index.html";
                Path file = staticDir.resolve(path.substring(1)).normalize();
                if (!file.startsWith(staticDir) || !Files.exists(file) || Files.isDirectory(file)) {
                    byte[] notFound = "Not Found".getBytes();
                    exchange.sendResponseHeaders(404, notFound.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(notFound); }
                    return;
                }
                String contentType = guessContentType(file);
                byte[] bytes = Files.readAllBytes(file);
                exchange.getResponseHeaders().add("Content-Type", contentType);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            }
            private String guessContentType(Path file) {
                String fn = file.getFileName().toString();
                if (fn.endsWith(".html")) return "text/html; charset=utf-8";
                if (fn.endsWith(".js")) return "application/javascript; charset=utf-8";
                if (fn.endsWith(".css")) return "text/css; charset=utf-8";
                if (fn.endsWith(".json")) return "application/json; charset=utf-8";
                if (fn.endsWith(".svg")) return "image/svg+xml";
                return "application/octet-stream";
            }
        });
        server.start();
        int port = server.getAddress().getPort();
        String base = "http://localhost:" + port;
        System.setProperty("test.url", base);
        System.setProperty("webdriver.base.url", base);
        System.out.println("StaticContentServer: serving static on " + base);
    }

    @AfterAll
    public static void stopServer() {
        if (server != null) {
            server.stop(0);
            System.out.println("StaticContentServer: stopped");
        }
    }
}
