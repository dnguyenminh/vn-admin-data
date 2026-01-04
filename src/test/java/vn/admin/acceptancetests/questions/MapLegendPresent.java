package vn.admin.acceptancetests.questions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

public class MapLegendPresent implements Question<Boolean> {
    public static MapLegendPresent isPresent() { return new MapLegendPresent(); }

    @Override
    public Boolean answeredBy(Actor actor) {
        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();
        try {
            Object present = js.executeScript("return document.querySelector('.map-legend') !== null;");
            return Boolean.TRUE.equals(present);
        } catch (Throwable t) { return false; }
    }
}
