package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class InjectPredictedMarkerAndPolygon implements Task {
    public static InjectPredictedMarkerAndPolygon now() { return instrumented(InjectPredictedMarkerAndPolygon.class); }

    @Override
    public <T extends Actor> void performAs(T actor) {
        ((JavascriptExecutor) BrowseTheWeb.as(actor).getDriver()).executeScript("(function(){ try{ var m = window.app.map.map; m.createPane('polyPane'); m.getPane('polyPane').style.zIndex = 400; m.createPane('predPane'); m.getPane('predPane').style.zIndex = 600; var poly = L.polygon([[10,105],[10,106],[11,106],[11,105]], {pane:'polyPane'}).addTo(m); var pred = L.circleMarker([10.5,105.5], {pane:'predPane'}).addTo(m); window.__poly = poly; window.__pred = pred; return true; }catch(e){ return false; } })();");
    }
}
