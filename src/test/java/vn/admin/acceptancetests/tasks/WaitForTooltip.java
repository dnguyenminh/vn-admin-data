package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.targets.Target;
import net.serenitybdd.screenplay.waits.WaitUntil;
import org.openqa.selenium.By;

import static net.serenitybdd.screenplay.Tasks.instrumented;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

public class WaitForTooltip implements Task {

    private static final Target TOOLTIP = Target.the("Leaflet tooltip")
            .located(By.cssSelector(".leaflet-tooltip, .leaflet-popup-content, .map-tooltip"));

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
            WaitUntil.the(TOOLTIP, isVisible()).forNoMoreThan(20).seconds()
        );
    }

    public static WaitForTooltip displayed() {
        return instrumented(WaitForTooltip.class);
    }
}
