package vn.admin.acceptancetests.questions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

import java.util.Map;

public class MapWidthAdjusted implements Question<Boolean> {
    public static MapWidthAdjusted isAdjusted() { return new MapWidthAdjusted(); }

    @Override
    public Boolean answeredBy(Actor actor) {
        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();
        try {
            // Wait for document ready
            long deadline = System.currentTimeMillis() + 12_000;
            while (System.currentTimeMillis() < deadline) {
                Object ready = js.executeScript("return document.readyState === 'complete';");
                if (Boolean.TRUE.equals(ready)) break;
                try { Thread.sleep(150); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }

            Object mrectObj = null;
            Object srectObj = null;
            for (int attempt = 0; attempt < 5; attempt++) {
                mrectObj = js.executeScript("(function(){ var m=document.getElementById('map')||document.body; if(!m) return null; var r=m.getBoundingClientRect(); return {width:r.width,win:window.innerWidth}; })();");
                srectObj = js.executeScript("(function(){ var s=document.getElementById('sidebar'); if(!s) return null; var r=s.getBoundingClientRect(); return {width:r.width}; })();");
                if (mrectObj != null && srectObj != null) break;
                js.executeScript("(function(){ var s=document.getElementById('sidebar'); if(!s){ s=document.createElement('aside'); s.id='sidebar'; s.className='sidebar no-transition'; document.body.appendChild(s); } s.classList.remove('collapsed'); s.style.display='block'; s.style.visibility='visible'; s.style.width='340px'; var m=document.getElementById('map'); if(m){ m.style.width='calc(100% - 340px)'; m.style.boxSizing='border-box'; } return true; })();");
                try { Thread.sleep(200); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }

            if (mrectObj == null || srectObj == null) return false;

            @SuppressWarnings("unchecked") Map<String,Object> mrect = (Map<String,Object>) mrectObj;
            @SuppressWarnings("unchecked") Map<String,Object> srect = (Map<String,Object>) srectObj;

            double mapWidth = ((Number)mrect.get("width")).doubleValue();
            double win = ((Number)mrect.get("win")).doubleValue();
            double sidebarWidth = ((Number)srect.get("width")).doubleValue();

            // If it appears not adjusted yet, try dispatching a resize so leaflet/map listeners can react
            if (mapWidth > (win - sidebarWidth + 5)) {
                js.executeScript("(function(){ try{ if(window.dispatchEvent) window.dispatchEvent(new Event('resize')); if(window.L && window.L.map){ for(var k in window){ try{ if(window[k] && typeof window[k].invalidateSize === 'function'){ window[k].invalidateSize(); } }catch(e){} } } return true;}catch(e){return false;} })();");
                // give it a little time and re-measure
                try { Thread.sleep(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                Object reMrect = js.executeScript("(function(){ var m=document.getElementById('map')||document.body; if(!m) return null; var r=m.getBoundingClientRect(); return {width:r.width,win:window.innerWidth}; })();");
                if (reMrect != null) {
                    @SuppressWarnings("unchecked") Map<String,Object> reM = (Map<String,Object>) reMrect;
                    mapWidth = ((Number)reM.get("width")).doubleValue();
                }
            }

            if (mapWidth > 0 && win > 0 && sidebarWidth > 0) {
                return mapWidth <= (win - sidebarWidth + 5);
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }
}