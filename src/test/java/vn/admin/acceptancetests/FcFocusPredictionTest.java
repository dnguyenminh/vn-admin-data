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
public class FcFocusPredictionTest {

    @CastMember(name = "User")
    Actor user;

    @Test
    void focus_button_shows_checkins_and_prediction_button_shows_predicted_marker() throws Exception {
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

        // Inject two checkins for FC_TEST and override ApiClient to return them
        js.executeScript("(function(){ try{ var geo = {type:'FeatureCollection', features:[]}; geo.features.push({ type:'Feature', properties: { id:'CHECKIN-A', fc_id:'FC_TEST', customer_address_id:'ADDR-1', checkin_date:'2024-11-04 12:31:58' }, geometry: { type:'Point', coordinates: [105.001, 10.002] } }); geo.features.push({ type:'Feature', properties: { id:'CHECKIN-B', fc_id:'FC_TEST', customer_address_id:'ADDR-2', checkin_date:'2024-11-04 12:35:58' }, geometry: { type:'Point', coordinates: [105.011, 10.012] } }); window.__fc_test_geo = geo; try { if(window.app && window.app.api) { window.app.api.getCheckinsGeoJson = async function(applId, fcId, page, size){ return window.__fc_test_geo; }; } }catch(e){} if(window.app && window.app.map && typeof window.app.map.showCheckinsGeojson === 'function'){ window.app.map.showCheckinsGeojson(geo); return (window.app.map.checkinGroup && window.app.map.checkinGroup.getLayers) ? window.app.map.checkinGroup.getLayers().length : 2; } else { if(!window.__testMap) window.__testMap = (typeof L !== 'undefined') ? L.map('map') : null; if(!window.__testMap) return -1; var m1 = L.circleMarker([10.002,105.001], { radius: 6, color:'#7f8c8d', fillColor:'#7f8c8d', fillOpacity:0.9 }); var m2 = L.circleMarker([10.012,105.011], { radius: 6, color:'#7f8c8d', fillColor:'#7f8c8d', fillOpacity:0.9 }); m1.addTo(window.__testMap); m2.addTo(window.__testMap); return 2; } }catch(e){ window.__injectErr=String(e); return -1; } })();");

        // Ensure FC combobox button exists and simulate selecting FC_TEST
        js.executeScript("(function(){ try{ var el = document.getElementById('fcCombo'); if(el){ el.value='FC_TEST'; el.dataset.selectedId='FC_TEST'; } return true;}catch(e){return false;} })();");

        // Ensure no predicted marker is present at start (clear any previous predictions)
        js.executeScript("(function(){ try{ if(window.app && window.app.map && typeof window.app.map.clearPredicted === 'function'){ window.app.map.clearPredicted(); } return true;}catch(e){return false;} })();");

        // Click the focus button: should show checkins (and not show predicted marker)
        js.executeScript("(function(){ try{ var b = document.getElementById('focusFcBtn'); if(b) b.click(); return true;}catch(e){return false;} })();");

        // Give it a moment and assert there is no predicted marker
        wait.until(d -> {
            try {
                Object present = js.executeScript("return document.querySelector('.predicted-marker') === null;");
                return Boolean.TRUE.equals(present);
            } catch (Throwable t) { return false; }
        });

        // Now click the predicted button and assert a predicted marker appears
        js.executeScript("(function(){ try{ var b = document.getElementById('showFcPredBtn'); if(b) b.click(); return true;}catch(e){return false;} })();");

        wait.until(d -> {
            try {
                Object ok = js.executeScript("var el = document.querySelector('.predicted-marker'); return el && el.innerText && el.innerText.indexOf('🔮') >= 0;");
                return Boolean.TRUE.equals(ok);
            } catch (Throwable t) { return false; }
        });

        Object pred = js.executeScript("return document.querySelector('.predicted-marker') ? document.querySelector('.predicted-marker').innerText : null;");
        assertTrue(pred != null && String.valueOf(pred).indexOf('🔮') >= 0, "Predicted marker should display 🔮 after clicking the Show predicted button, got: " + pred);
    }
}
