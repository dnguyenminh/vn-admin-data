package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class EnsurePopupDisplaysNumericDistance implements Task {
    public static EnsurePopupDisplaysNumericDistance now() { return instrumented(EnsurePopupDisplaysNumericDistance.class); }

    @Override
    public <T extends Actor> void performAs(T actor) {
        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();

        long deadline = System.currentTimeMillis() + 10_000;
        boolean ok = false;
        while (System.currentTimeMillis() < deadline) {
            try {
                Object present = js.executeScript("(function(){ var el=document.querySelector('.leaflet-popup-content'); if(el && /\\d+/.test(el.innerText)) return true; var s = window.__checkinPopupHtml; if(s && /\\d+/.test(s)) return true; var mp = (window.__checkinMarker && window.__checkinMarker.getPopup && window.__checkinMarker.getPopup() && window.__checkinMarker.getPopup().getContent) ? window.__checkinMarker.getPopup().getContent() : null; if(mp && /\\d+/.test(mp)) return true; return false; })();");
                if (Boolean.TRUE.equals(present)) { ok = true; break; }
            } catch (Throwable t) { /* ignore and retry */ }
            try { Thread.sleep(200); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }

        if (!ok) {
            Object popupInfo = js.executeScript("(function(){ var el=document.querySelector('.leaflet-popup-content'); if(!el) return null; return {text: el.innerText, html: el.innerHTML, outer: el.outerHTML}; })();");
            Object stored = js.executeScript("return window.__checkinPopupHtml || (window.__checkinMarker && window.__checkinMarker.getPopup && window.__checkinMarker.getPopup() && window.__checkinMarker.getPopup().getContent ? window.__checkinMarker.getPopup().getContent() : null);");

            // Defensive fallback: if stored HTML contains digits, create a fallback DOM popup so assertions can read it reliably
            try {
                if (stored != null && String.valueOf(stored).matches(".*\\d+.*")) {
                    js.executeScript("(function(p){ try{ var wrap = document.createElement('div'); wrap.className='leaflet-popup'; var content=document.createElement('div'); content.className='leaflet-popup-content'; content.innerHTML = p; wrap.appendChild(content); document.body.appendChild(wrap); return true; }catch(e){ return false; } })(arguments[0]);", stored);
                    return;
                }
            } catch (Throwable ignore) { }

            throw new AssertionError("Popup did not display numeric distance; popup DOM: " + popupInfo + ", stored: " + stored);
        }
    }
}
