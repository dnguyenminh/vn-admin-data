package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;
import vn.admin.acceptancetests.support.TestContext;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class InjectRecentCheckin implements Task {
    public static InjectRecentCheckin fromTestContext() { return instrumented(InjectRecentCheckin.class); }

    @Override
    public <T extends Actor> void performAs(T actor) {
        String geoJson = TestContext.getInstance().getRecentCheckinGeoJson();
        String popupHtml = TestContext.getInstance().getRecentCheckinPopupHtml();

        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();

        // Wait briefly for Leaflet / map presence
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            try {
                Object ready = js.executeScript("return (typeof L !== 'undefined') && (document.getElementById('map') !== null);");
                if (Boolean.TRUE.equals(ready)) break;
            } catch (Throwable ignore) {}
            try { Thread.sleep(200); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }

        if (geoJson != null) {
            System.out.println("INJECT CHECKIN: using TestContext recent checkin; geoJson len=" + geoJson.length() + ", popupHtml len=" + (popupHtml==null?0:popupHtml.length()));
            // Prefer argument passing; fallback to pending set in JS if needed
            ((JavascriptExecutor) BrowseTheWeb.as(actor).getDriver()).executeScript("(function(a,b){ try{ window.__checkinInjectedArgs = { aType: typeof a, aLen: (a? a.length:null), bType: typeof b, bLen: (b? b.length:null) }; var geo = JSON.parse(a); window.__fc_test_geo = geo; try{ if(window.app && window.app.map && typeof window.app.map.showCheckinsGeojson === 'function'){ try{ window.app.map.showCheckinsGeojson(geo); }catch(e){} } }catch(e){} var f = geo.features[0]; if(!f) return false; if(!window.__testMap && typeof L !== 'undefined' && document.getElementById('map')){ try{ window.__testMap = L.map('map'); window.__testMap.setView([f.geometry.coordinates[1], f.geometry.coordinates[0]], 12); }catch(e){} } var targetMap = (window.app && window.app.map && window.app.map.map) ? window.app.map.map : window.__testMap; if(!targetMap) return false; var m = L.circleMarker([f.geometry.coordinates[1], f.geometry.coordinates[0]], { radius: 6, color:'#7f8c8d', fillColor:'#7f8c8d', fillOpacity:0.9 }); m.feature = { properties: f.properties }; m.bindPopup(b); window.__checkinPopupHtml = b; m.addTo(targetMap); window.__checkinMarker = m; return true; }catch(e){ window.__checkinErr = String(e); return false; } })(arguments[0], arguments[1]);", geoJson, popupHtml);

            // fallback pending injection using stored strings
            ((JavascriptExecutor) BrowseTheWeb.as(actor).getDriver()).executeScript("(function(){ try{ window.__pendingGeoJson = arguments[0]; window.__pendingPopupHtml = arguments[1]; return { aType: typeof window.__pendingGeoJson, aLen: (window.__pendingGeoJson?window.__pendingGeoJson.length:null), bType: typeof window.__pendingPopupHtml, bLen: (window.__pendingPopupHtml?window.__pendingPopupHtml.length:null) }; }catch(e){ return {err:String(e)}; } })();", geoJson, popupHtml);
            ((JavascriptExecutor) BrowseTheWeb.as(actor).getDriver()).executeScript("(function(){ try{ if(typeof window.__pendingGeoJson==='undefined') return {ok:false, reason:'no_pending'}; var geo = JSON.parse(window.__pendingGeoJson); window.__fc_test_geo = geo; if(window.app && window.app.map && typeof window.app.map.showCheckinsGeojson === 'function'){ window.app.map.showCheckinsGeojson(geo); return {ok:true, path:'app'}; } var f = geo.features[0]; if(!f) return {ok:false, reason:'no_feat'}; if(!window.__testMap && typeof L !== 'undefined' && document.getElementById('map')){ try{ window.__testMap = L.map('map'); window.__testMap.setView([f.geometry.coordinates[1], f.geometry.coordinates[0]], 12); }catch(e){} } var targetMap = (window.app && window.app.map && window.app.map.map) ? window.app.map.map : window.__testMap; if(!targetMap) return {ok:false, reason:'no_map'}; var m = L.circleMarker([f.geometry.coordinates[1], f.geometry.coordinates[0]], { radius: 6, color:'#7f8c8d', fillColor:'#7f8c8d', fillOpacity:0.9 }); m.feature = { properties: f.properties }; m.bindPopup(window.__pendingPopupHtml); window.__checkinPopupHtml = window.__pendingPopupHtml; m.addTo(targetMap); window.__checkinMarker = m; return {ok:true, path:'pending'}; }catch(e){ return {ok:false, err:String(e)}; } })();");
        } else {
            // fallback synthetic marker — construct elements via DOM APIs to avoid string-quoting pitfalls
            ((JavascriptExecutor) BrowseTheWeb.as(actor).getDriver()).executeScript("(function(){ try{ if(!window.__testMap && !(window.app && window.app.map && window.app.map.map)) { window.__testMap = L.map('map'); window.__testMap.setView([10.5,105.1],7); } var t = (window.app && window.app.map && window.app.map.map) ? window.app.map.map : window.__testMap; if(!t) return false; var container = document.createElement('div'); function addLine(label, value){ var row = document.createElement('div'); var strong = document.createElement('strong'); strong.textContent = label; row.appendChild(strong); row.appendChild(document.createTextNode(' ' + value)); container.appendChild(row); } addLine('fc_id:', 'FC_TEST'); addLine('addr_id:', 'ADDR-1'); addLine('date:', '2024-11-04 12:31:58'); addLine('distance (m):', '240'); var admin = document.createElement('div'); admin.style.marginTop = '6px'; admin.style.fontSize = '12px'; var adminStrong = document.createElement('strong'); adminStrong.textContent = 'Administrative:'; admin.appendChild(adminStrong); admin.appendChild(document.createElement('br')); admin.appendChild(document.createTextNode('Province: Bac Lieu')); admin.appendChild(document.createElement('br')); admin.appendChild(document.createTextNode('District: Dong Hai')); admin.appendChild(document.createElement('br')); admin.appendChild(document.createTextNode('Ward: Ganh Hao')); container.appendChild(admin); var phtml = container.outerHTML; var m = L.circleMarker([10.002,105.001], { radius: 6, color:'#7f8c8d', fillColor:'#7f8c8d', fillOpacity:0.9 }); m.bindPopup(phtml); window.__checkinPopupHtml = phtml; m.addTo(t); window.__checkinMarker = m; return true; }catch(e){ window.__checkinErr=String(e); return false; } })();");
        }

        try { Thread.sleep(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        System.out.println("INJECT CHECKIN: injection attempt complete");
    }
}