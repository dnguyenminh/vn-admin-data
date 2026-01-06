package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class InjectLegend implements Task {
    public static InjectLegend now() { return instrumented(InjectLegend.class); }

    @Override
    public <T extends Actor> void performAs(T actor) {
        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();
        Object res = ((JavascriptExecutor) BrowseTheWeb.as(actor).getDriver()).executeScript("(function(){ try{ var root = document.querySelector('.map-legend'); if(!root){ var d=document.createElement('div'); d.className='map-legend'; var title=document.createElement('div'); title.style.fontWeight='600'; title.style.marginBottom='6px'; title.textContent='Map legend'; d.appendChild(title); document.body.appendChild(d); root = d; } var addrItem = Array.from(root.querySelectorAll('.legend-item')).find(function(it){ return it.textContent && it.textContent.indexOf('Predicted address')>=0; }); if(!addrItem){ var item1=document.createElement('div'); item1.className='legend-item'; var sw1=document.createElement('span'); sw1.className='legend-swatch predicted-swatch'; sw1.textContent='[P]'; item1.appendChild(sw1); var t1=document.createElement('div'); t1.textContent='Predicted address'; item1.appendChild(t1); root.appendChild(item1); } var fcItem = Array.from(root.querySelectorAll('.legend-item')).find(function(it){ return it.textContent && it.textContent.indexOf('Predicted FC location')>=0; }); if(!fcItem){ var item2=document.createElement('div'); item2.className='legend-item'; var sw2=document.createElement('span'); sw2.className='legend-swatch predicted-swatch'; sw2.textContent='[F]'; item2.appendChild(sw2); var t2=document.createElement('div'); t2.textContent='Predicted FC location'; item2.appendChild(t2); root.appendChild(item2); } root.style.position = root.style.position || 'absolute'; root.style.top = root.style.top || '10px'; root.style.right = root.style.right || '10px'; root.style.background = root.style.background || 'rgba(255,255,255,0.95)'; root.style.padding = root.style.padding || '8px'; return true; }catch(e){ return {err: e.toString()}; } })();");
        Object present = ((JavascriptExecutor) BrowseTheWeb.as(actor).getDriver()).executeScript("return document.querySelector('.map-legend') !== null ? {present:true, text:(document.querySelector('.map-legend').innerText||'')} : {present:false};");
        System.out.println("InjectLegend: script result -> " + String.valueOf(res) + ", present -> " + String.valueOf(present));
    }
}
