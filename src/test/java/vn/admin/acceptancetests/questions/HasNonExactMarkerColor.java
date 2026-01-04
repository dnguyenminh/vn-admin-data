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
            Object ok = js.executeScript("(function(){ try{ var paths = document.querySelectorAll('path.leaflet-interactive'); if(!paths) return false; for(var i=0;i<paths.length;i++){ var fill = window.getComputedStyle(paths[i]).fill || ''; if(fill.indexOf('231')>=0 || fill.indexOf('e74c3c')>=0 || fill.indexOf('rgb(231')>=0) return true; } return false; }catch(e){return false;} })();");
            return Boolean.TRUE.equals(ok);
        } catch (Throwable t) {
            return false;
        }
    }
}
