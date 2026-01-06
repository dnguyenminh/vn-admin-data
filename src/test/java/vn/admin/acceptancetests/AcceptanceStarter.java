package vn.admin.acceptancetests;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(classes = vn.admin.web.MapApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.context.ActiveProfiles("test")
public class AcceptanceStarter {

    @LocalServerPort
    int port;

    @Test
    void startServerAndExposePort() {
        String base = "http://localhost:" + port;
        System.setProperty("test.url", base);
        System.setProperty("webdriver.base.url", base);
        System.out.println("AcceptanceStarter: server at " + base);
        // no assertions; this test simply ensures the Spring context (server) is started
    }
}
