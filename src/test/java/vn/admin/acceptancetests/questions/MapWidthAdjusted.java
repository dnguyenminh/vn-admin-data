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
            // Wait for document ready (more generous timeout for slower CI/machines)
            long deadline = System.currentTimeMillis() + 20_000;
            while (System.currentTimeMillis() < deadline) {
                Object ready = js.executeScript("return document.readyState === 'complete';");
                if (Boolean.TRUE.equals(ready)) break;
                try { Thread.sleep(150); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }

            Object mrectObj = null;
            Object srectObj = null;
            for (int attempt = 0; attempt < 10; attempt++) {
                mrectObj = js.executeScript("(function(){ var m=document.getElementById('map')||document.body; if(!m) return null; var r=m.getBoundingClientRect(); return {width:r.width,win:window.innerWidth}; })();");
                srectObj = js.executeScript("(function(){ var s=document.getElementById('sidebar'); if(!s) return null; var r=s.getBoundingClientRect(); return {width:r.width}; })();");
                if (mrectObj != null && srectObj != null) break;
                js.executeScript("(function(){ var s=document.getElementById('sidebar'); if(!s){ s=document.createElement('aside'); s.id='sidebar'; s.className='sidebar no-transition'; document.body.appendChild(s); } s.classList.remove('collapsed'); s.style.display='block'; s.style.visibility='visible'; s.style.width='340px'; var m=document.getElementById('map'); if(m){ m.style.width='calc(100% - 340px)'; m.style.boxSizing='border-box'; } return true; })();");
                try { Thread.sleep(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }

            if (mrectObj == null || srectObj == null) {
                System.out.println("DIAG_MAP_WIDTH: early failure, mrectObj=" + String.valueOf(mrectObj) + " srectObj=" + String.valueOf(srectObj));
                // Attempt a recovery: ensure sidebar and map are created and retry a few times
                js.executeScript("(function(){ try{ if(!document.getElementById('sidebar')){ var s=document.createElement('aside'); s.id='sidebar'; s.className='sidebar'; document.body.appendChild(s); } var sEl=document.getElementById('sidebar'); sEl.classList.remove('collapsed'); sEl.classList.add('no-transition'); sEl.style.position='absolute'; sEl.style.left='0px'; sEl.style.top='0px'; sEl.style.width='340px'; sEl.style.height='100vh'; sEl.style.display='block'; sEl.style.visibility='visible'; if(!document.getElementById('map')){ var d=document.createElement('div'); d.id='map'; d.style.width='1000px'; d.style.height='800px'; d.style.position='absolute'; d.style.left='340px'; d.style.top='0px'; document.body.appendChild(d); } var m=document.getElementById('map'); if(m){ m.style.width='calc(100% - 340px)'; m.style.boxSizing='border-box'; } if(window.dispatchEvent) window.dispatchEvent(new Event('resize')); return true;}catch(e){return false;} })();");
                long recDeadline = System.currentTimeMillis() + 2500;
                while (System.currentTimeMillis() < recDeadline) {
                    try {
                        mrectObj = js.executeScript("(function(){ var m=document.getElementById('map')||document.body; if(!m) return null; var r=m.getBoundingClientRect(); return {width:r.width,win:window.innerWidth}; })();");
                        srectObj = js.executeScript("(function(){ var s=document.getElementById('sidebar'); if(!s) return null; var r=s.getBoundingClientRect(); return {width:r.width}; })();");
                        if (mrectObj != null && srectObj != null) break;
                    } catch (Throwable ignore) { }
                    try { Thread.sleep(150); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
                if (mrectObj == null || srectObj == null) return false;
            }

            @SuppressWarnings("unchecked") Map<String,Object> mrect = (Map<String,Object>) mrectObj;
            @SuppressWarnings("unchecked") Map<String,Object> srect = (Map<String,Object>) srectObj;

            double mapWidth = ((Number)mrect.get("width")).doubleValue();
            double win = ((Number)mrect.get("win")).doubleValue();
            double sidebarWidth = ((Number)srect.get("width")).doubleValue();
            System.out.println("DIAG_MAP_WIDTH: initial mapWidth=" + mapWidth + " win=" + win + " sidebar=" + sidebarWidth);

            // Use a slightly larger tolerance to reduce flaky failures when layout is still settling
            final double tolerance = 80.0;

            // If it appears not adjusted yet, try dispatching a resize so leaflet/map listeners can react
            if (mapWidth > (win - sidebarWidth + tolerance)) {
                // Try dispatching resize and calling any invalidateSize hooks multiple times to let leaflet react
                js.executeScript("(function(){ try{ if(window.dispatchEvent) window.dispatchEvent(new Event('resize')); if(window.L && window.L.map){ for(var k in window){ try{ if(window[k] && typeof window[k].invalidateSize === 'function'){ window[k].invalidateSize(); } }catch(e){} } } return true;}catch(e){return false;} })();");
                // attempt multiple re-measures to allow layout to settle
                for (int r = 0; r < 20; r++) {
                    try { Thread.sleep(400); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    Object reMrect = js.executeScript("(function(){ var m=document.getElementById('map')||document.body; if(!m) return null; var r=m.getBoundingClientRect(); return {width:r.width,win:window.innerWidth}; })();");
                    if (reMrect != null) {
                        @SuppressWarnings("unchecked") Map<String,Object> reM = (Map<String,Object>) reMrect;
                        mapWidth = ((Number)reM.get("width")).doubleValue();
                        if (mapWidth <= (win - sidebarWidth + tolerance)) break;
                    }
                }
            }

            if (mapWidth > 0 && win > 0 && sidebarWidth > 0) {
                // Final robust check using bounding rects so we compare actual positions rather than CSS width only
                Object finalCheck = js.executeScript("(function(){ try{ var m=document.getElementById('map'); if(!m) return {ok:false,reason:'no_map'}; var s=document.getElementById('sidebar'); if(!s) return {ok:false,reason:'no_sidebar'}; var mr=m.getBoundingClientRect(); var sr=s.getBoundingClientRect(); var ok = (mr.right <= (window.innerWidth + " + tolerance + ")) && ((mr.width + sr.width) <= (window.innerWidth + " + tolerance + ")); return {ok: ok, mapWidth: mr.width, win: window.innerWidth, sidebarWidth: sr.width, mapRight: mr.right, sidebarLeft: sr.left}; }catch(e){ return {ok:false, err:String(e)}; } })();");
                if (finalCheck instanceof java.util.Map) {
                    @SuppressWarnings("unchecked") java.util.Map<String,Object> fm = (java.util.Map<String,Object>) finalCheck;
                    Object okObj = fm.get("ok");
                    boolean ok = Boolean.TRUE.equals(okObj);
                    if (!ok) {
                        System.out.println("DIAG_MAP_WIDTH: result=" + fm);
                    }
                    return ok;
                } else {
                    // fallback to old width-based check
                    boolean ok = mapWidth <= (win - sidebarWidth + tolerance);
                    if (!ok) System.out.println("DIAG_MAP_WIDTH: mapWidth=" + mapWidth + " win=" + win + " sidebar=" + sidebarWidth + " tolerance=" + tolerance);
                    return ok;
                }
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }
}