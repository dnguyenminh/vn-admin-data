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
public class SidebarVisualTest {

    @CastMember(name = "User")
    Actor user;

    @Test
    void sidebar_uses_full_height_and_toggle_does_not_overlap_zoom() {
        // Open page with the acceptanceTest flag (NavigateTo already appends it)
        user.attemptsTo(NavigateTo.theMapPage());

        WebDriver driver = getDriver();

        // Wait until sidebar element appears and is open
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(6));
        wait.until(d -> {
            try {
                Object ok = ((JavascriptExecutor) d).executeScript(
                        "var s=document.getElementById('sidebar'); if(!s) return false; var cls = s.classList; if(cls && cls.contains('collapsed')) return false; return true;");
                return Boolean.TRUE.equals(ok);
            } catch (Throwable t) { return false; }
        });

        // Check: sidebar height >= viewport height - 1 (allow minor rounding)
        Object fullHeight = ((JavascriptExecutor) driver).executeScript(
            "var s=document.getElementById('sidebar'); if(!s) return false; var r=s.getBoundingClientRect(); return r.height >= (window.innerHeight - 1);"
        );
        assertTrue(Boolean.TRUE.equals(fullHeight), "Sidebar should use full viewport height when open");

        // Check: sidebar toggle does not overlap Leaflet zoom control
        WebDriverWait overlapWait = new WebDriverWait(driver, java.time.Duration.ofSeconds(3));
        boolean noOverlap = overlapWait.until(d -> {
            try {
                Object ok = ((JavascriptExecutor) d).executeScript(
                        "var t=document.getElementById('sidebarToggle'); if(!t) return false; var z=document.querySelector('.leaflet-control-zoom'); if(!z) return true; var r1=t.getBoundingClientRect(); var r2=z.getBoundingClientRect(); var overlap = (r1.right>r2.left && r1.left<r2.right && r1.bottom>r2.top && r1.top<r2.bottom); return !overlap;"
                );
                return Boolean.TRUE.equals(ok);
            } catch (Throwable t) { return false; }
        });
        assertTrue(noOverlap, "Sidebar toggle should not overlap Leaflet zoom control");
    }
}
