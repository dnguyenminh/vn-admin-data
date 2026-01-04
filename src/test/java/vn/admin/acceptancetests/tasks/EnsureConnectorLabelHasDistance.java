package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class EnsureConnectorLabelHasDistance implements Task {
    public static EnsureConnectorLabelHasDistance now() { return instrumented(EnsureConnectorLabelHasDistance.class); }

    @Override
    public <T extends Actor> void performAs(T actor) {
        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();
        long deadline = System.currentTimeMillis() + 30_000;
        boolean ok = false;
        while (System.currentTimeMillis() < deadline) {
            try {
                Object present = js.executeScript("return document.querySelectorAll('.leaflet-tooltip.connector-label').length > 0 && /\\d+\\s*m/.test(document.querySelector('.leaflet-tooltip.connector-label').innerText);");
                if (Boolean.TRUE.equals(present)) { ok = true; break; }
            } catch (Throwable ignore) {}
            try { Thread.sleep(200); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        if (!ok) throw new AssertionError("Connector label with numeric distance not found");
    }
}