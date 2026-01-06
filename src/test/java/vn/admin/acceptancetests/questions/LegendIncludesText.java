package vn.admin.acceptancetests.questions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

public class LegendIncludesText implements Question<Boolean> {
    private final String text;
    private LegendIncludesText(String text) { this.text = text; }
    public static LegendIncludesText includes(String text) { return new LegendIncludesText(text); }

    @Override
    public Boolean answeredBy(Actor actor) {
        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();
        try {
            Object legendText = js.executeScript("(function(){ var el = document.querySelector('.map-legend'); if(!el) return {text:'',html:''}; var txt = el.innerText || el.textContent || ''; var html = el.innerHTML || ''; try{ window.__testLegendText = txt; window.__testLegendHTML = html; }catch(e){} return {text:txt, html:html}; })();");
            if (legendText == null) return false;
            @SuppressWarnings("unchecked") java.util.Map<String,Object> m = (java.util.Map<String,Object>) legendText;
            String txt = String.valueOf(m.getOrDefault("text",""));
            String html = String.valueOf(m.getOrDefault("html",""));
            String lower = txt.toLowerCase() + " " + html.toLowerCase();
            boolean ok = lower.contains(text.toLowerCase());
            if (!ok) {
                System.out.println("DIAG_LEGEND_TEXT: '" + txt + "' expected to include '" + text + "' html='" + html + "'");
            }
            return ok;
        } catch (Throwable t) { System.out.println("DIAG_LEGEND_ERROR: " + t.toString()); return false; }
    }
}
