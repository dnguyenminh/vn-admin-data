package vn.admin.acceptancetests.questions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

public class IsPredictedMarkerVisible implements Question<Boolean> {
    public static IsPredictedMarkerVisible isVisible() { return new IsPredictedMarkerVisible(); }

    @Override
    public Boolean answeredBy(Actor actor) {
        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();
        try {
            Object ok = js.executeScript("return ( (window.app && window.app.map && window.app.map.predictedLayer && window.app.map.predictedLayer.getLayers && window.app.map.predictedLayer.getLayers().length>0) || (document.querySelectorAll('.predicted-marker')&&document.querySelectorAll('.predicted-marker').length>0) || (window.__testMap && window.__testMap._layers && Object.keys(window.__testMap._layers).length>0) );");
            return Boolean.TRUE.equals(ok);
        } catch (Throwable t) {
            return false;
        }
    }
}
