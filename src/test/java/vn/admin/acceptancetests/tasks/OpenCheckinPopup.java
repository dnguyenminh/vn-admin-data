package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class OpenCheckinPopup implements Task {
    public static OpenCheckinPopup now() { return instrumented(OpenCheckinPopup.class); }

    @Override
    public <T extends Actor> void performAs(T actor) {
        ((JavascriptExecutor) BrowseTheWeb.as(actor).getDriver()).executeScript("(function(){try{ if(window.__checkinMarker && typeof window.__checkinMarker.openPopup === 'function'){ window.__checkinMarker.openPopup(); return 'checkinMarker'; } if(window.__popupMarker && typeof window.__popupMarker.openPopup === 'function'){ window.__popupMarker.openPopup(); return '__popupMarker'; } if(window.app && window.app.map && window.app.map.checkinGroup && window.app.map.checkinGroup.getLayers){ var ls = window.app.map.checkinGroup.getLayers(); if(ls && ls.length>0 && typeof ls[0].openPopup === 'function'){ ls[0].openPopup(); return 'checkinGroup'; } } if(typeof L !== 'undefined' && window.__checkinMarker && window.__checkinPopupHtml){ try{ var latlng = window.__checkinMarker.getLatLng ? window.__checkinMarker.getLatLng() : null; var mapObj = (window.app && window.app.map && window.app.map.map) ? window.app.map.map : window.__testMap; if(latlng && mapObj){ L.popup().setLatLng(latlng).setContent(window.__checkinPopupHtml).openOn(mapObj); return 'popup-created'; } } catch(e){} } var icons = document.querySelectorAll('.leaflet-interactive, .leaflet-marker-icon, .leaflet-circle-marker'); if(icons && icons.length>0){ try { icons[0].dispatchEvent(new MouseEvent('click',{bubbles:true,cancelable:true})); return 'dom-icon'; } catch(e) { try { icons[0].click(); return 'dom-icon-click'; } catch(e2){} } } if(window.__checkinPopupHtml){ try{ var wrap = document.createElement('div'); wrap.className = 'leaflet-popup'; var content = document.createElement('div'); content.className = 'leaflet-popup-content'; content.innerHTML = window.__checkinPopupHtml; wrap.appendChild(content); document.body.appendChild(wrap); return 'dom-created'; }catch(e){} } return 'none'; }catch(e){ return 'err:' + String(e); } })();");
    }
}
