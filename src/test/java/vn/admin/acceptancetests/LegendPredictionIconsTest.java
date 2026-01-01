package vn.admin.acceptancetests;

import net.serenitybdd.junit5.SerenityJUnit5Extension;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.annotations.CastMember;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import vn.admin.acceptancetests.tasks.NavigateTo;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static net.thucydides.core.webdriver.ThucydidesWebDriverSupport.getDriver;

@ExtendWith(SerenityJUnit5Extension.class)
public class LegendPredictionIconsTest {

    @CastMember(name = "User")
    Actor user;

    @Test
    void legend_shows_address_and_fc_prediction_icons() {
        user.attemptsTo(NavigateTo.theMapPage());
        WebDriver driver = getDriver();
        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Wait for legend to be present in the DOM
        wait.until(d -> {
            try {
                Object ok = js.executeScript("return document.querySelector('.map-legend') !== null;");
                return Boolean.TRUE.equals(ok);
            } catch (Throwable t) { return false; }
        });

        Object inner = js.executeScript("return document.querySelector('.map-legend') ? document.querySelector('.map-legend').innerText : null;");
        assertTrue(inner != null && String.valueOf(inner).contains("Predicted address"), "Legend should mention Predicted address, got: " + inner);
        assertTrue(inner != null && String.valueOf(inner).contains("Predicted FC location"), "Legend should mention Predicted FC location, got: " + inner);

        Object swatches = js.executeScript("return Array.from(document.querySelectorAll('.map-legend .predicted-swatch')).map(function(e){return e.innerText || '';});");
        assertTrue(swatches != null && String.valueOf(swatches).indexOf("📍") >= 0, "Legend should include the 📍 swatch for predicted address");
        assertTrue(swatches != null && String.valueOf(swatches).indexOf("🔮") >= 0, "Legend should include the 🔮 swatch for predicted FC location");
    }
}
