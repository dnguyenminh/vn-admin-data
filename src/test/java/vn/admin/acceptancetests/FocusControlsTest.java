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
public class FocusControlsTest {

    @CastMember(name = "User")
    Actor user;

    @Test
    void focus_buttons_exist_and_focus_map() throws Exception {
        user.attemptsTo(NavigateTo.theMapPage());
        WebDriver driver = getDriver();
        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Ensure controls exist
        Object exists = js.executeScript("return (document.getElementById('focusAddressBtn')!==null && document.getElementById('focusFcBtn')!==null && document.getElementById('showAllCheckins')!==null);");
        assertTrue(Boolean.TRUE.equals(exists), "Focus controls should be present");

        // Wait until buttons are visible and then click them (reduces flakiness in headless)
        wait.until(d -> { try { Object ok = ((JavascriptExecutor)d).executeScript("var b=document.getElementById('focusAddressBtn'); if(!b) return false; var s=window.getComputedStyle(b); return s.display!=='none' && s.visibility!=='hidden';"); return Boolean.TRUE.equals(ok); } catch (Throwable t) { return false; } });
        js.executeScript("document.getElementById('focusAddressBtn').click();");

        wait.until(d -> { try { Object ok = ((JavascriptExecutor)d).executeScript("var b=document.getElementById('focusFcBtn'); if(!b) return false; var s=window.getComputedStyle(b); return s.display!=='none' && s.visibility!=='hidden';"); return Boolean.TRUE.equals(ok); } catch (Throwable t) { return false; } });
        js.executeScript("document.getElementById('focusFcBtn').click();");
    }
}
