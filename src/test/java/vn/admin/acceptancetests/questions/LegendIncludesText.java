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
            Object swatches = js.executeScript("return Array.from(document.querySelectorAll('.map-legend .predicted-swatch')).map(function(e){return e.innerText || '';});");
            String s = String.valueOf(swatches);
            return (swatches != null && s.indexOf(text) >= 0);
        } catch (Throwable t) { return false; }
    }
}
