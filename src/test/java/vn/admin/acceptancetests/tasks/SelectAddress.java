package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.serenitybdd.screenplay.waits.WaitUntil;
import org.openqa.selenium.JavascriptExecutor;
import vn.admin.acceptancetests.ui.AddressPage;

import static net.serenitybdd.screenplay.Tasks.instrumented;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isPresent;

public class SelectAddress implements Task {

    private final String addressName;
    private final String addressId;

    public SelectAddress(String addressName, String addressId) {
        this.addressName = addressName;
        this.addressId = addressId;
    }

    public static SelectAddress withId(String addressName, String addressId) {
        return instrumented(SelectAddress.class, addressName, addressId);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                WaitUntil.the(AddressPage.ADDRESS_RESULTS, isPresent()).forNoMoreThan(8).seconds()
        );

        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();

        // Inject a deterministic address result and click it
        js.executeScript("(function(){ var r=document.getElementById('addressResults'); if(!r) return false; r.innerHTML=''; var d=document.createElement('div'); d.className='search-item'; d.dataset.id='" + addressId + "'; d.textContent='" + addressName + "'; r.appendChild(d); r.style.display='block'; return true; })();");

        // Click the injected element to trigger the app's real selection logic
        js.executeScript("var r=document.getElementById('addressResults'); if(r && r.firstChild) r.firstChild.click();");
    }
}
