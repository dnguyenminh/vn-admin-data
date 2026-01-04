package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class ClickElementById implements Task {
    private final String id;
    public ClickElementById(String id) { this.id = id; }
    public static ClickElementById withId(String id) { return instrumented(ClickElementById.class, id); }

    @Override
    public <T extends Actor> void performAs(T actor) {
        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();
        try {
            js.executeScript("(function(){ var el=document.getElementById(arguments[0]); if(!el) return false; try{ el.click(); }catch(e){ try{ el.dispatchEvent(new MouseEvent('click',{bubbles:true,cancelable:true})); }catch(e){} } return true; })();", id);
        } catch (Throwable t) { throw new RuntimeException("Failed to click element by id '" + id + "': " + t.getMessage(), t); }
    }
}
