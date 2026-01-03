package vn.admin.acceptancetests.questions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

public class MapStatus implements Question<Boolean> {

    @Override
    public Boolean answeredBy(Actor actor) {
        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();
        Object leaf = js.executeScript("return (typeof L !== 'undefined' && document.getElementById('map')!==null);");
        return Boolean.TRUE.equals(leaf);
    }

    public static Question<Boolean> isReady() {
        return new MapStatus();
    }
}
