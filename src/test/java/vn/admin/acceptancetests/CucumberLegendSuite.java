package vn.admin.acceptancetests;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/legend/legend_features.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "vn.admin.acceptancetests.steps")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "net.serenitybdd.cucumber.core.plugin.SerenityReporter,pretty,html:build/reports/cucumber/legend-report.html")
@org.springframework.boot.test.context.SpringBootTest(classes = vn.admin.web.MapApplication.class, webEnvironment = org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.context.ActiveProfiles("test")
@org.junit.jupiter.api.TestInstance(org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS)
public class CucumberLegendSuite {

    @org.springframework.boot.test.web.server.LocalServerPort
    private int port;

    private static com.sun.net.httpserver.HttpServer staticServer;

    @org.junit.jupiter.api.BeforeAll
    public void beforeAll() {
        // Ensure tests can reach the embedded server via the NavigateTo helper
        String existing = System.getProperty("test.url");
        if (existing != null && !existing.isBlank() && !existing.endsWith(":8080")) {
            System.out.println("CucumberLegendSuite: using existing test.url=" + existing);
            return;
        }
        // Try to use Spring Boot assigned port, if present
        String base = "http://localhost:" + port;
        try {
            if (port > 0) {
                System.setProperty("test.url", base);
                System.setProperty("webdriver.base.url", base);
                System.out.println("CucumberLegendSuite: test.url=" + base);
                return;
            }
        } catch (Throwable ignored) {}

        // As a fallback, start a simple static file server serving src/main/resources/static
        try {
            java.nio.file.Path staticDir = java.nio.file.Paths.get("src/main/resources/static");
            staticServer = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(0), 0);
            staticServer.createContext("/", new com.sun.net.httpserver.HttpHandler() {
                @Override
                public void handle(com.sun.net.httpserver.HttpExchange exchange) throws java.io.IOException {
                    String path = exchange.getRequestURI().getPath();
                    if (path.equals("/")) path = "/index.html";
                    java.nio.file.Path file = staticDir.resolve(path.substring(1)).normalize();
                    if (!file.startsWith(staticDir) || !java.nio.file.Files.exists(file) || java.nio.file.Files.isDirectory(file)) {
                        byte[] notFound = "Not Found".getBytes();
                        exchange.sendResponseHeaders(404, notFound.length);
                        try (java.io.OutputStream os = exchange.getResponseBody()) { os.write(notFound); }
                        return;
                    }
                    String contentType = guessContentType(file);
                    byte[] bytes = java.nio.file.Files.readAllBytes(file);
                    exchange.getResponseHeaders().add("Content-Type", contentType);
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (java.io.OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                }
                private String guessContentType(java.nio.file.Path file) {
                    String fn = file.getFileName().toString();
                    if (fn.endsWith(".html")) return "text/html; charset=utf-8";
                    if (fn.endsWith(".js")) return "application/javascript; charset=utf-8";
                    if (fn.endsWith(".css")) return "text/css; charset=utf-8";
                    if (fn.endsWith(".json")) return "application/json; charset=utf-8";
                    if (fn.endsWith(".svg")) return "image/svg+xml";
                    return "application/octet-stream";
                }
            });
            staticServer.start();
            int sp = staticServer.getAddress().getPort();
            String fallback = "http://localhost:" + sp;
            System.setProperty("test.url", fallback);
            System.setProperty("webdriver.base.url", fallback);
            System.out.println("CucumberLegendSuite: started static server at " + fallback);
        } catch (Throwable t) {
            System.out.println("CucumberLegendSuite: failed to start static server: " + t.getMessage());
        }
    }

    @org.junit.jupiter.api.AfterAll
    public void afterAll() {
        if (staticServer != null) {
            staticServer.stop(0);
            System.out.println("CucumberLegendSuite: stopped static server");
        }
    }

}

