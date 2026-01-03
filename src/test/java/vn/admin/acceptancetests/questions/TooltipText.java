package vn.admin.acceptancetests.questions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

public class TooltipText implements Question<String> {

    @Override
    public String answeredBy(Actor actor) {
        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();
        Object tooltipText = js.executeScript("return document.querySelector('.leaflet-tooltip') ? document.querySelector('.leaflet-tooltip').innerText : null;");
        return tooltipText == null ? null : String.valueOf(tooltipText);
    }

    public static Question<String> value() {
        return new TooltipText();
    }
}
