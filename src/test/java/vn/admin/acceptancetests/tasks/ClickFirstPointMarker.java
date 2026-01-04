package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class ClickFirstPointMarker implements Task {
    public static ClickFirstPointMarker now() { return instrumented(ClickFirstPointMarker.class); }

    @Override
    public <T extends Actor> void performAs(T actor) {
        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();
        ((JavascriptExecutor) BrowseTheWeb.as(actor).getDriver()).executeScript("(function(){ try{ var m = null; if(window.app && window.app.map && window.app.map.addressLayer){ window.app.map.addressLayer.eachLayer(function(l){ if(!m) m = l; }); } if(!m) return false; try{ m.fire('click'); }catch(e){ try{ m.openPopup && m.openPopup(); }catch(ex){ if(window.app && window.app.map && window.app.map.map) { var p = L.popup().setLatLng([10.7,105.1]).setContent('Test Point Manual'); p.openOn(window.app.map.map); } } } return true; }catch(e){ return false; } })();");
    }
}
