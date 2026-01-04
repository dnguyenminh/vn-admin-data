package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class EnsureLeafletPopupShown implements Task {
    public static EnsureLeafletPopupShown now() { return instrumented(EnsureLeafletPopupShown.class); }

    @Override
    public <T extends Actor> void performAs(T actor) {
        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();
        long deadline = System.currentTimeMillis() + 10_000;
        boolean ok = false;
        while (System.currentTimeMillis() < deadline) {
            try {
                Object exists = js.executeScript("return (document.querySelectorAll('.leaflet-popup-content').length>0);");
                if (Boolean.TRUE.equals(exists)) { ok = true; break; }
                Object attached = js.executeScript("var found=false; try{ if(window.app && window.app.map && window.app.map.addressLayer){ window.app.map.addressLayer.eachLayer(function(l){ if(l && l.getPopup()){ found=true; } }); } if(!found && window.app && window.app.map && window.app.map.map && window.app.map.map._popup){ found=true; } }catch(e){} return found;");
                if (Boolean.TRUE.equals(attached)) { ok = true; break; }
            } catch (Throwable ignore) {}
            try { Thread.sleep(200); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        if (!ok) throw new AssertionError("Leaflet popup was not shown");
    }
}
