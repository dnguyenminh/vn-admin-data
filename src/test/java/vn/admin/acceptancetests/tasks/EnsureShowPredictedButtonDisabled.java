package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class EnsureShowPredictedButtonDisabled implements Task {
    public static EnsureShowPredictedButtonDisabled now() { return instrumented(EnsureShowPredictedButtonDisabled.class); }

    @Override
    public <T extends Actor> void performAs(T actor) {
        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();
        long deadline = System.currentTimeMillis() + 10_000;
        boolean ok = false;
        while (System.currentTimeMillis() < deadline) {
            try {
                Object disabled = js.executeScript("var b=document.getElementById('showFcPredBtn'); return b ? b.disabled : null;");
                if (Boolean.TRUE.equals(disabled)) { ok = true; break; }
            } catch (Throwable ignore) {}
            try { Thread.sleep(150); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        if (!ok) throw new AssertionError("showFcPredBtn did not become disabled");
    }
}