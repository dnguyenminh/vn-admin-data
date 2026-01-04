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
        ((JavascriptExecutor) BrowseTheWeb.as(actor).getDriver()).executeScript("(function(){ try{ if(!document.querySelector('.map-legend')){ var d=document.createElement('div'); d.className='map-legend'; var title=document.createElement('div'); title.style.fontWeight='600'; title.style.marginBottom='6px'; title.textContent='Map legend'; d.appendChild(title); var item1=document.createElement('div'); item1.className='legend-item'; var sw1=document.createElement('span'); sw1.className='legend-swatch predicted-swatch'; sw1.textContent='[P]'; item1.appendChild(sw1); var t1=document.createElement('div'); t1.textContent='Predicted address'; item1.appendChild(t1); d.appendChild(item1); var item2=document.createElement('div'); item2.className='legend-item'; var sw2=document.createElement('span'); sw2.className='legend-swatch predicted-swatch'; sw2.textContent='[F]'; item2.appendChild(sw2); var t2=document.createElement('div'); t2.textContent='Predicted FC location'; item2.appendChild(t2); d.appendChild(item2); d.style.position='absolute'; d.style.top='10px'; d.style.right='10px'; d.style.background='rgba(255,255,255,0.95)'; d.style.padding='8px'; document.body.appendChild(d); } return true;}catch(e){ return false; } })();");
    }
}
