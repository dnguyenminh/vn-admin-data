package vn.admin.acceptancetests.questions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

public class FocusControlsPresent implements Question<Boolean> {
    public static FocusControlsPresent arePresent() { return new FocusControlsPresent(); }

    @Override
    public Boolean answeredBy(Actor actor) {
        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();
        try {
            Object exists = js.executeScript("return (document.getElementById('focusAddressBtn')!==null && document.getElementById('focusFcBtn')!==null && document.getElementById('showAllCheckins')!==null);");
            return Boolean.TRUE.equals(exists);
        } catch (Throwable t) { return false; }
    }
}
