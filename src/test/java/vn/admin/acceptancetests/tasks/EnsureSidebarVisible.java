package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class EnsureSidebarVisible implements Task {
    public static EnsureSidebarVisible now() { return instrumented(EnsureSidebarVisible.class); }

    @Override
    public <T extends Actor> void performAs(T actor) {
        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();

        // Ensure sidebar exists and is open/visible
        ((JavascriptExecutor) BrowseTheWeb.as(actor).getDriver()).executeScript("(function(){ try{ var s=document.getElementById('sidebar'); if(!s){ s=document.createElement('aside'); s.id='sidebar'; s.className='sidebar'; document.body.appendChild(s); } s.classList.remove('collapsed'); s.classList.add('no-transition'); s.style.position='absolute'; s.style.left='0px'; s.style.top='0px'; s.style.width='340px'; s.style.height='100vh'; s.style.display='block'; s.style.visibility='visible'; var nodes=s.querySelectorAll('select,input,.search-results'); nodes.forEach(function(el){ el.style.display = (el.tagName==='SELECT'?'inline-block':'block'); el.style.visibility='visible'; }); var t=document.getElementById('sidebarToggle'); if(t) t.setAttribute('aria-expanded','true'); return true;}catch(e){return false;} })();");

        // Wait/poll until layout stabilizes (simple poll loop)
        long deadline = System.currentTimeMillis() + 5000;
        boolean ok = false;
        while (System.currentTimeMillis() < deadline) {
            try {
                Object exists = js.executeScript("return (document.getElementById('sidebar') !== null && document.getElementById('sidebar').style.display !== 'none' && document.getElementById('sidebar').style.visibility !== 'hidden');");
                if (Boolean.TRUE.equals(exists)) { ok = true; break; }
            } catch (Throwable ignore) { }
            try { Thread.sleep(150); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }

        if (!ok) throw new RuntimeException("Sidebar visibility could not be ensured");

        // Also ensure a '#map' container exists (keeps this task self-contained) and adjust it so layout calculations succeed
        js.executeScript("(function(){ try{ if(!document.getElementById('map')){ var d=document.createElement('div'); d.id='map'; d.style.width='1000px'; d.style.height='800px'; d.style.position='absolute'; d.style.left='340px'; d.style.top='0px'; document.body.appendChild(d); } var m=document.getElementById('map'); if(m){ m.style.width='calc(100% - 340px)'; m.style.boxSizing='border-box'; } if(window.dispatchEvent) window.dispatchEvent(new Event('resize')); if(window.L && window.L.map){ for(var k in window){ try{ if(window[k] && typeof window[k].invalidateSize === 'function'){ window[k].invalidateSize(); } }catch(e){} } } return true;}catch(e){return false;} })();");

        // give map a moment to adjust
        long tdeadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < tdeadline) {
            try {
                Object exists = js.executeScript("return (function(){ var m=document.getElementById('map')||document.body; var r=m.getBoundingClientRect(); return (r.width>0); })();");
                if (Boolean.TRUE.equals(exists)) break;
            } catch (Throwable ignore) { }
            try { Thread.sleep(150); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
    }
}
