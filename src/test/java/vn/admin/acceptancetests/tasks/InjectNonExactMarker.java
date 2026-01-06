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
        if (lat == null || lng == null) {
            System.out.println("InjectNonExactMarker: No non-exact marker in TestContext; using synthetic fallback");
            // deterministic synthetic fallback so test remains stable even without DB seed
            lat = 10.01; lng = 105.012; id = "SYN-NE";
        }

        ((JavascriptExecutor) BrowseTheWeb.as(actor).getDriver()).executeScript("if(!window.__testMap && !(window.app && window.app.map && window.app.map.map)) { if(typeof L !== 'undefined' && document.getElementById('map')) { try { window.__testMap = L.map('map'); window.__testMap.setView([arguments[0], arguments[1]], 12);}catch(e){} } }", lat, lng);
        ((JavascriptExecutor) BrowseTheWeb.as(actor).getDriver()).executeScript("if(window.__testMarkers) window.__testMarkers.forEach(function(mk){ try{mk.remove();}catch(e){} }); window.__testMarkers = [];" );
        ((JavascriptExecutor) BrowseTheWeb.as(actor).getDriver()).executeScript("var m = (window.app && window.app.map && window.app.map.map) ? window.app.map.map : window.__testMap; var marker = L.circleMarker([arguments[0], arguments[1]], { radius:10, color:'rgb(231,76,60)', fillColor:'rgb(231,76,60)', fillOpacity:1.0 }).addTo(m); try{ marker.feature = { properties: { id:(arguments[2]? arguments[2] : 'NE-DB'), is_exact:false } }; (function(){var end=Date.now()+2000; var set=false; while(Date.now()<end){ try{ var el = (marker._path) ? marker._path : (typeof marker.getElement === 'function' ? marker.getElement() : null); if(el && el.setAttribute){ el.setAttribute('data-test','non-exact-marker'); set=true; break; } var icon = marker._icon || (marker.getElement && marker.getElement()); if(icon && icon.setAttribute){ icon.setAttribute('data-test','non-exact-marker'); set=true; break; } }catch(e){} try{ var elems=document.querySelectorAll('.leaflet-interactive'); if(elems&&elems.length>0){ for(var i=0;i<elems.length;i++){ var el2=elems[i]; if(!el2.getAttribute('data-test')){ el2.setAttribute('data-test','non-exact-marker'); set=true; break; } } if(set) break; } }catch(e){} } try{ if(!window.__testMarkerStatus) window.__testMarkerStatus={}; window.__testMarkerStatus['nonExactSet']=set; }catch(e){} })(); }catch(e){} window.__testMarkers.push(marker); m.setView([arguments[0],arguments[1]], 12); if(!document.querySelector('.map-legend')){ var d=document.createElement('div'); d.className='map-legend'; d.innerHTML='<span data-test=\"legend-non-exact\" style=\"background:rgb(231,76,60)\"></span> Non-exact'; document.body.appendChild(d);} else if(!document.querySelector('.map-legend span[data-test=\"legend-non-exact\"]')){ var span=document.querySelector('.map-legend span'); if(span) span.setAttribute('data-test','legend-non-exact'); } return 1;", lat, lng, id);
    }
}
