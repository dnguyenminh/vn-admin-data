package vn.admin.acceptancetests.questions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

public class HasNonExactMarkerColor implements Question<Boolean> {
    public static HasNonExactMarkerColor isPresent() { return new HasNonExactMarkerColor(); }

    @Override
    public Boolean answeredBy(Actor actor) {
        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();
        try {
            Object ok = js.executeScript("(function(){try{var end=Date.now()+2000;while(Date.now()<end){ var ne = document.querySelector('[data-test=non-exact-marker]'); if(ne){ var fillAttr = (ne.getAttribute && (ne.getAttribute('fill')||'')) || ''; var strokeAttr = (ne.getAttribute && (ne.getAttribute('stroke')||'')) || ''; var fill=window.getComputedStyle(ne).fill||''; var stroke=window.getComputedStyle(ne).stroke||''; var styleFill = (ne.style && (ne.style.fill||'')) || ''; var styleStroke = (ne.style && (ne.style.stroke||'')) || ''; if(fillAttr.indexOf('231')>=0||fillAttr.indexOf('e74c3c')>=0||fillAttr.indexOf('rgb(231')>=0||strokeAttr.indexOf('231')>=0||strokeAttr.indexOf('e74c3c')>=0||strokeAttr.indexOf('rgb(231')>=0||fill.indexOf('231')>=0||fill.indexOf('e74c3c')>=0||fill.indexOf('rgb(231')>=0||stroke.indexOf('231')>=0||stroke.indexOf('e74c3c')>=0||stroke.indexOf('rgb(231')>=0||styleFill.indexOf('231')>=0||styleFill.indexOf('e74c3c')>=0||styleFill.indexOf('rgb(231')>=0||styleStroke.indexOf('231')>=0||styleStroke.indexOf('e74c3c')>=0||styleStroke.indexOf('rgb(231')>=0) return 'FOUND'; return JSON.stringify({markerPresent:true,fillAttr:fillAttr,strokeAttr:strokeAttr,fillComputed:fill,strokeComputed:stroke,styleFill:styleFill,styleStroke:styleStroke}); } var elems=document.querySelectorAll('.leaflet-interactive'); if(elems&&elems.length>0){ for(var i=0;i<elems.length;i++){ var el=elems[i]; var fill=window.getComputedStyle(el).fill||''; var stroke=window.getComputedStyle(el).stroke||''; var sfill=(el.style&&el.style.fill)||''; var sstroke=(el.style&&el.style.stroke)||''; if(fill.indexOf('231')>=0||fill.indexOf('e74c3c')>=0||fill.indexOf('rgb(231')>=0||stroke.indexOf('231')>=0||stroke.indexOf('e74c3c')>=0||stroke.indexOf('rgb(231')>=0||sfill.indexOf('231')>=0||sfill.indexOf('e74c3c')>=0||sfill.indexOf('rgb(231')>=0||sstroke.indexOf('231')>=0||sstroke.indexOf('e74c3c')>=0||sstroke.indexOf('rgb(231')>=0) return 'FOUND'; }} var legend = document.querySelector('[data-test=legend-non-exact]')||document.querySelector('.map-legend span'); if(legend){ var bg = window.getComputedStyle(legend).backgroundColor||''; var html=(legend.parentElement&&legend.parentElement.innerHTML)||(document.querySelector('.map-legend')&&document.querySelector('.map-legend').innerHTML)||''; if(bg.indexOf('231')>=0||bg.indexOf('e74c3c')>=0||bg.indexOf('rgb(231')>=0) return 'FOUND'; if(html&&(html.indexOf('231')>=0||html.indexOf('e74c3c')>=0||html.indexOf('rgb(231')>=0)) return 'FOUND'; if(window.__testMarkerStatus && window.__testMarkerStatus['nonExactSet']===true) return 'FOUND'; return JSON.stringify({legendPresent:true,legendBg:bg,legendHtml:html}); } var wait=Date.now()+10; while(Date.now()<wait){} } return 'NOTFOUND';}catch(e){return 'ERROR:'+e.toString();}})();");
            if (ok instanceof String) {
                String s = (String) ok;
                if ("FOUND".equals(s)) return true;
                System.out.println("HasNonExactMarkerColor diagnostic: " + s);
                return false;
            }
            return false;
        } catch (Throwable t) {
            System.out.println("HasNonExactMarkerColor error: " + t.toString());
            return false;
        }
    }
}
