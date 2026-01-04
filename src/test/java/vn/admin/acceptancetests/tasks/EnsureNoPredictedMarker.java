package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class EnsureNoPredictedMarker implements Task {
    public static EnsureNoPredictedMarker now() { return instrumented(EnsureNoPredictedMarker.class); }

    @Override
    public <T extends Actor> void performAs(T actor) {
        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();
        long deadline = System.currentTimeMillis() + 5000;
        boolean ok = false;
        while (System.currentTimeMillis() < deadline) {
            try {
                Object present = js.executeScript("return document.querySelector('.predicted-marker') === null;");
                if (Boolean.TRUE.equals(present)) { ok = true; break; }
            } catch (Throwable ignore) {}
            try { Thread.sleep(200); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        if (!ok) throw new AssertionError("Predicted marker is present when it should not be");
    }
}
