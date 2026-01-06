package vn.admin.acceptancetests.questions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

public class TooltipText implements Question<String> {

    @Override
    public String answeredBy(Actor actor) {
        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();
        Object tooltipText = js.executeScript("return (function(){ var t = document.querySelector('.leaflet-tooltip'); if(t) return t.innerText; var p = document.querySelector('.leaflet-popup-content'); if(p) return p.innerText; var alt = document.querySelector('.map-tooltip'); if(alt) return alt.innerText; return null; })();");
        return tooltipText == null ? null : String.valueOf(tooltipText);
    }

    public static Question<String> value() {
        return new TooltipText();
    }
}
