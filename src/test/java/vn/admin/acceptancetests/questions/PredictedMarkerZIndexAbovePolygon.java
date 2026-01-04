package vn.admin.acceptancetests.questions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

public class PredictedMarkerZIndexAbovePolygon implements Question<Boolean> {
    public static PredictedMarkerZIndexAbovePolygon isAbove() { return new PredictedMarkerZIndexAbovePolygon(); }

    @Override
    public Boolean answeredBy(Actor actor) {
        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();
        try {
            Object zOk = js.executeScript("var p = window.__poly.getPane(); var pr = window.__pred.getPane(); return parseInt(window.getComputedStyle(pr).zIndex) > parseInt(window.getComputedStyle(p).zIndex);");
            return Boolean.TRUE.equals(zOk);
        } catch (Throwable t) { return false; }
    }
}
