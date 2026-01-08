package vn.admin.acceptancetests.questions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.ensure.Ensure;

public class CheckinMarkersCount implements Question<Integer> {

    public static CheckinMarkersCount now() { return new CheckinMarkersCount(); }

    @Override
    public Integer answeredBy(Actor actor) {
        Object res = ((org.openqa.selenium.JavascriptExecutor) net.serenitybdd.screenplay.abilities.BrowseTheWeb.as(actor).getDriver())
                .executeScript("return (window.app && window.app.map && window.app.map._allCheckinMarkers) ? window.app.map._allCheckinMarkers.filter(function(o){ try { return o.marker && o.marker._map; } catch(e) { return false; } }).length : 0;");
        try {
            if (res instanceof Number) return ((Number) res).intValue();
            return Integer.parseInt(String.valueOf(res));
        } catch (Exception e) {
            return 0;
        }
    }
}
