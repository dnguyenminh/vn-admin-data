package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class TriggerPredictedForFc implements Task {
    public static TriggerPredictedForFc now() { return instrumented(TriggerPredictedForFc.class); }

    @Override
    public <T extends Actor> void performAs(T actor) {
        ((JavascriptExecutor) BrowseTheWeb.as(actor).getDriver()).executeScript("(function(){ try{ if(window.app && window.app.map){ var geo=window.__fc_test_geo||{features:[]}; var sumLat=0,sumLng=0,c=0; geo.features.forEach(function(f){ try{ var co=f.geometry&&f.geometry.coordinates; if(co){ sumLng+=co[0]; sumLat+=co[1]; c++; } }catch(e){} }); if(c===0) return false; var avgLng=sumLng/c; var avgLat=sumLat/c; var fakeFeature={ geometry:{ coordinates:[avgLng, avgLat] }, properties:{ appl_id:'TEST', fc_id:'FC_TEST', adjusted:true, areaLevel:'fc_prediction' } }; window.app.map.showPredictedAddress(fakeFeature); } else if(window.__testMap){ var geo=window.__fc_test_geo||{features:[]}; var sumLat=0,sumLng=0,c=0; geo.features.forEach(function(f){ try{ var co=f.geometry&&f.geometry.coordinates; if(co){ sumLng+=co[0]; sumLat+=co[1]; c++; } }catch(e){} }); if(c===0) return false; var avgLng=sumLng/c; var avgLat=sumLat/c; var m=L.marker([avgLat, avgLng], { icon: L.divIcon({ className:'predicted-marker', html:'PRED', iconSize:[24,24] }) }); m.addTo(window.__testMap); } try{ if(document.querySelectorAll && (document.querySelectorAll('.predicted-marker')||[]).length===0){ var el=document.createElement('div'); el.className='predicted-marker'; el.textContent='PRED'; el.style.position='absolute'; el.style.left='10px'; el.style.top='10px'; document.getElementById('map')&&document.getElementById('map').appendChild(el); } }catch(e){} return true;}catch(e){return false;} })();");
    }
}
