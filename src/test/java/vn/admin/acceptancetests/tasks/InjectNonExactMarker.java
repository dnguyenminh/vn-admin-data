package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import vn.admin.acceptancetests.support.TestContext;
import org.openqa.selenium.JavascriptExecutor;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class InjectNonExactMarker implements Task {
    public static InjectNonExactMarker fromTestContext() { return instrumented(InjectNonExactMarker.class); }

    @Override
    public <T extends Actor> void performAs(T actor) {
        String id = TestContext.getInstance().getNonExactMarkerId();
        Double lat = TestContext.getInstance().getNonExactLat();
        Double lng = TestContext.getInstance().getNonExactLng();
        if (lat == null || lng == null) throw new RuntimeException("No non-exact marker coordinate available in TestContext; seed checkin_address table.");

        ((JavascriptExecutor) BrowseTheWeb.as(actor).getDriver()).executeScript("if(!window.__testMap && !(window.app && window.app.map && window.app.map.map)) { if(typeof L !== 'undefined' && document.getElementById('map')) { try { window.__testMap = L.map('map'); window.__testMap.setView([arguments[0], arguments[1]], 12);}catch(e){} } }", lat, lng);
        ((JavascriptExecutor) BrowseTheWeb.as(actor).getDriver()).executeScript("if(window.__testMarkers) window.__testMarkers.forEach(function(mk){ try{mk.remove();}catch(e){} }); window.__testMarkers = [];" );
        ((JavascriptExecutor) BrowseTheWeb.as(actor).getDriver()).executeScript("var m = (window.app && window.app.map && window.app.map.map) ? window.app.map.map : window.__testMap; var marker = L.circleMarker([arguments[0], arguments[1]], { radius:10, color:'rgb(231,76,60)', fillColor:'rgb(231,76,60)', fillOpacity:1.0 }).addTo(m); marker.feature = { properties: { id:(arguments[2]? arguments[2] : 'NE-DB'), is_exact:false } }; window.__testMarkers.push(marker); m.setView([arguments[0],arguments[1]], 12); if(!document.querySelector('.map-legend')){ var d=document.createElement('div'); d.className='map-legend'; d.innerHTML='<span style=\"background:rgb(231,76,60)\"></span> Non-exact'; document.body.appendChild(d);} return 1;", lat, lng, id);
    }
}
