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

public class FetchSelectedCustomerAddress extends AbstractFetchCustomerAddress implements Task {

    private final String applId;
    private final String address;

    public FetchSelectedCustomerAddress(String applId, String address) {
        this.applId = applId;
        this.address = address;
    }

    public static FetchSelectedCustomerAddress forAddress(String applId, String address) {
        return instrumented(FetchSelectedCustomerAddress.class, applId, address);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        Map<String, JsonNode> addressMap = actor.recall("customer_addresses_" + applId);
        if (addressMap != null && addressMap.containsKey(address)) {
            JsonNode feature = addressMap.get(address);
            actor.remember("last_selected_address", feature);
        }

    }
}
