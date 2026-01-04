package vn.admin.acceptancetests.questions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

public class PopupContainsProvince implements Question<Boolean> {
    private final String province;
    private PopupContainsProvince(String province) { this.province = province; }
    public static PopupContainsProvince withName(String province) { return new PopupContainsProvince(province); }

    @Override
    public Boolean answeredBy(Actor actor) {
        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();
        try {
            Object present = js.executeScript("return document.querySelector('.leaflet-popup-content') !== null && document.querySelector('.leaflet-popup-content').innerText.indexOf('Province: ' + arguments[0]) >= 0;", province);
            return Boolean.TRUE.equals(present);
        } catch (Throwable t) { return false; }
    }
}
