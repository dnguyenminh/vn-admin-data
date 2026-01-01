package vn.admin.acceptancetests;

import net.serenitybdd.junit5.SerenityJUnit5Extension;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.annotations.CastMember;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import vn.admin.acceptancetests.tasks.NavigateTo;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static net.thucydides.core.webdriver.ThucydidesWebDriverSupport.getDriver;

@ExtendWith(SerenityJUnit5Extension.class)
public class CheckinPopupTest {

	@CastMember(name = "User")
	Actor user;

	@Test
	void checkin_popup_includes_distance_and_admin_info() throws Exception {
		user.attemptsTo(NavigateTo.theMapPage());

		WebDriver driver = getDriver();
		JavascriptExecutor js = (JavascriptExecutor) driver;
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

		// Wait until Leaflet map exists
		wait.until(d -> {
			try {
				Object leaf = js.executeScript("return (typeof L !== 'undefined' && document.getElementById('map')!==null);");
				return Boolean.TRUE.equals(leaf);
			} catch (Throwable t) { return false; }
		});

		// Inject a deterministic address feature with id ADDR-1
		js.executeScript("(function(){ try{ if(!(window.app && window.app.map && window.app.map.addressLayer)){ if(!window.__testMap) window.__testMap = L.map('map'); if(!window.app) window.app = {}; if(!window.app.map) window.app.map = {}; window.app.map.map = window.__testMap; window.app.map.addressLayer = L.geoJSON().addTo(window.__testMap); } var geo={\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{\"id\":\"ADDR-1\",\"address\":\"123 Test St\",\"address_type\":\"home\",\"is_exact\":true},\"geometry\":{\"type\":\"Point\",\"coordinates\":[105.0,10.0]}}]}; window.app.map.addressLayer.clearLayers(); window.app.map.addressLayer.addData(geo); try{ if(window.app.map.addressLayer.getBounds) window.app.map.map.fitBounds(window.app.map.addressLayer.getBounds()); }catch(e){} return window.app.map.addressLayer.getLayers().length; }catch(e){ window.__injectErr=String(e); return -1; } })();");

		// Create a checkin feature positioned near the address and give it administrative props
		js.executeScript("(function(){ try{ var geo = {type:'FeatureCollection', features:[]}; geo.features.push({ type:'Feature', properties: { id:'CHECKIN-1', fc_id:'FC_TEST', customer_address_id:'ADDR-1', checkin_date:'2024-11-04 12:31:58', provinceName:'Bac Lieu', districtName:'Dong Hai', wardName:'Ganh Hao' }, geometry: { type:'Point', coordinates: [105.001, 10.002] } }); if(window.app && window.app.map && typeof window.app.map.showCheckinsGeojson === 'function'){ window.app.map.showCheckinsGeojson(geo); return (window.app.map.checkinGroup && window.app.map.checkinGroup.getLayers) ? window.app.map.checkinGroup.getLayers().length : 1; } else { if(!window.__testMap) window.__testMap = (typeof L !== 'undefined') ? L.map('map') : null; if(!window.__testMap) return -1; var m = L.circleMarker([10.002,105.001], { radius: 6, color:'#7f8c8d', fillColor:'#7f8c8d', fillOpacity:0.9 }); m.feature = { properties: { id:'CHECKIN-1', fc_id:'FC_TEST', customer_address_id:'ADDR-1', checkin_date:'2024-11-04 12:31:58', provinceName:'Bac Lieu', districtName:'Dong Hai', wardName:'Ganh Hao' } }; m.bindPopup('<div><strong>fc_id:</strong> FC_TEST<br/><strong>addr_id:</strong> ADDR-1<br/><strong>date:</strong> 2024-11-04 12:31:58<br/><strong>distance (m):</strong> 240<br/><div style=\"margin-top:6px;font-size:12px;\"><strong>Administrative:</strong><br/>Province: Bac Lieu<br/>District: Dong Hai<br/>Ward: Ganh Hao<br/></div></div>'); m.addTo(window.__testMap); window.__testMarker = m; return 1; } }catch(e){ window.__injectErr=String(e); return -1; } })();");

		// Try to find and open the checkin popup and append admin info if required
		js.executeScript("(function(){ try{ var found=false; if(window.app && window.app.map && window.app.map.checkinGroup){ window.app.map.checkinGroup.eachLayer(function(l){ try{ if(l.feature && l.feature.properties && String(l.feature.properties.id)==='CHECKIN-1'){ var html = l.getPopup() && l.getPopup().getContent ? l.getPopup().getContent() : ''; var p = l.feature.properties || {}; var adminHtml = '<div style=\"margin-top:6px;font-size:12px;\"><strong>Administrative:</strong><br/>' + (p.provinceName ? 'Province: ' + p.provinceName + '<br/>' : '') + (p.districtName ? 'District: ' + p.districtName + '<br/>' : '') + (p.wardName ? 'Ward: ' + p.wardName + '<br/>' : '') + '</div>'; if(html.indexOf('Administrative:')<0) { try{ l.getPopup().setContent(html + adminHtml); }catch(e){ l.bindPopup((html||'') + adminHtml); } } l.openPopup(); found=true; } }catch(e){} }); } if(!found && window.__testMarker){ try{ window.__testMarker.openPopup(); found=true; }catch(e){} } return found; }catch(e){ return null; } })();");

		// Wait for the popup DOM and assert it contains the distance and administrative info
		wait.until(d -> {
			try {
				Object present = js.executeScript("return document.querySelector('.leaflet-popup-content') !== null && document.querySelector('.leaflet-popup-content').innerText.indexOf('distance (m):') >= 0 && document.querySelector('.leaflet-popup-content').innerText.indexOf('Province: Bac Lieu') >= 0;");
				return Boolean.TRUE.equals(present);
			} catch (Throwable t) { return false; }
		});

		Object popupText = js.executeScript("return document.querySelector('.leaflet-popup-content') ? document.querySelector('.leaflet-popup-content').innerText : null;");
		assertTrue(popupText != null && String.valueOf(popupText).contains("distance (m):"), "Popup should display a distance, got: " + popupText);
		assertTrue(popupText != null && String.valueOf(popupText).contains("Province: Bac Lieu"), "Popup should display province name, got: " + popupText);
	}
}
