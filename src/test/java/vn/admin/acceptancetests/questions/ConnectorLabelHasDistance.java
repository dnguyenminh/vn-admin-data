package vn.admin.acceptancetests.questions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

public class ConnectorLabelHasDistance implements Question<Boolean> {
    public static ConnectorLabelHasDistance isPresent() { return new ConnectorLabelHasDistance(); }

    @Override
    public Boolean answeredBy(Actor actor) {
        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();
        try {
            Object present = js.executeScript("return document.querySelectorAll('.leaflet-tooltip.connector-label').length > 0 && /\\d+\\s*m/.test(document.querySelector('.leaflet-tooltip.connector-label').innerText);");
            return Boolean.TRUE.equals(present);
        } catch (Throwable t) { return false; }
    }
}
