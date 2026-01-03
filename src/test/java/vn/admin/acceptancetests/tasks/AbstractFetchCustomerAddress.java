package vn.admin.acceptancetests.tasks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

import java.util.HashMap;
import java.util.Map;

public class AbstractFetchCustomerAddress {
    protected <T extends Actor> Map<String, JsonNode> fetchCustomerAddress(T actor, String applId) {
        Map<String, JsonNode> addressMap = actor.recall("customer_addresses_" + applId);
        if (null == addressMap) {
            JavascriptExecutor js = (JavascriptExecutor) actor.getAbilityThatExtends(BrowseTheWeb.class).getDriver();
            // Execute fetch in the browser context to reuse the session/cookies
            String script = "var callback = arguments[arguments.length - 1];" +
                    "fetch('/api/map/addresses/geojson?applId=" + applId + "')" +
                    ".then(response => response.json())" +
                    ".then(data => callback(JSON.stringify(data)))" +
                    ".catch(err => callback(null));";

            Object result = js.executeAsyncScript(script);

            if (result != null) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(result.toString());
                    JsonNode features = root.path("features");

                    addressMap = new HashMap<>();
                    if (features.isArray()) {
                        for (JsonNode feature : features) {
                            String id = feature.path("properties").path("id").asText();
                            if (!id.isEmpty()) {
                                addressMap.put(id, feature);
                            }
                            // Also map by address text if needed, or store the whole list
                            String addressText = feature.path("properties").path("address").asText();
                            if (!addressText.isEmpty()) {
                                addressMap.put(addressText, feature);
                            }
                        }
                    }

                    actor.remember("customer_addresses_" + applId, addressMap);

                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse address GeoJSON", e);
                }
            }
        }
        return addressMap;
    }
}
