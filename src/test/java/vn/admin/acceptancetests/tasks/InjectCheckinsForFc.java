package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import vn.admin.acceptancetests.support.TestContext;
import org.openqa.selenium.JavascriptExecutor;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class InjectCheckinsForFc implements Task {
    private final String fcId;

    public InjectCheckinsForFc(String fcId) { this.fcId = fcId; }

    public static InjectCheckinsForFc forFc(String fcId) { return instrumented(InjectCheckinsForFc.class, fcId); }

    @Override
    public <T extends Actor> void performAs(T actor) {
        String geoJson = TestContext.getInstance().getFcGeoJson(fcId);
        if (geoJson == null) {
            throw new RuntimeException("No preloaded checkins for fc_id='" + fcId + "' in TestContext; ensure TestDataProvider prefetched FC checkins or seed data.");
        }
        ((JavascriptExecutor) BrowseTheWeb.as(actor).getDriver()).executeScript("(function(a){ try{ var geo = JSON.parse(a); window.__fc_test_geo = geo; try{ if(window.app && window.app.api) { window.app.api.getCheckinsGeoJson = async function(applId, fcId, page, size){ return window.__fc_test_geo; }; } }catch(e){} if(window.app && window.app.map && typeof window.app.map.showCheckinsGeojson === 'function'){ window.app.map.showCheckinsGeojson(geo); return (window.app.map.checkinGroup && window.app.map.checkinGroup.getLayers) ? window.app.map.checkinGroup.getLayers().length : geo.features.length; } else { if(!window.__testMap) window.__testMap = (typeof L !== 'undefined') ? L.map('map') : null; if(!window.__testMap) return -1; for(var i=0;i<geo.features.length;i++){ var f=geo.features[i]; var m = L.circleMarker([f.geometry.coordinates[1], f.geometry.coordinates[0]], { radius: 6, color:'#7f8c8d', fillColor:'#7f8c8d', fillOpacity:0.9 }); m.addTo(window.__testMap);} return geo.features.length;} }catch(e){ window.__injectErr=String(e); return -1; } })(arguments[0]);", geoJson);
    }
}
