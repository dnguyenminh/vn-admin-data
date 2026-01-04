package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import vn.admin.acceptancetests.support.TestContext;

import java.util.List;
import org.openqa.selenium.JavascriptExecutor;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class InjectCustomerResults implements Task {
    public static InjectCustomerResults fromTestContext() { return instrumented(InjectCustomerResults.class); }

    @Override
    public <T extends Actor> void performAs(T actor) {
        List<String> appls = TestContext.getInstance().getDistinctApplIds();
        if (appls == null || appls.isEmpty()) throw new RuntimeException("No customers found in TestContext (customer_address). Seed test data.");
        ((JavascriptExecutor) BrowseTheWeb.as(actor).getDriver()).executeScript("(function(a){ var r=document.getElementById('customerResults'); if(!r) return false; r.innerHTML=''; a.forEach(function(n,i){ var d=document.createElement('div'); d.className='search-item'; d.dataset.id=String(i+1); d.textContent=n; r.appendChild(d); }); r.style.display='block'; return true; })(arguments[0]);", appls);
    }
}
