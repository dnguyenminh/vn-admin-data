package vn.admin.acceptancetests.tasks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

import java.util.HashMap;
import java.util.Map;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class FetchCustomerAddresses extends AbstractFetchCustomerAddress implements Task {

    private final String applId;

    public FetchCustomerAddresses(String applId) {
        this.applId = applId;
    }

    public static FetchCustomerAddresses forApplId(String applId) {
        return instrumented(FetchCustomerAddresses.class, applId);
    }

//    public String getApplId() {
//        return applId;
//    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        fetchCustomerAddress(actor, this.applId);
        actor.remember("current_appl_id", applId);
    }

}
