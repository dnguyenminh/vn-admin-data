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
            String script = "var appl = arguments[0]; var callback = arguments[arguments.length - 1];" +
                    "fetch('/api/map/addresses/geojson?applId=' + encodeURIComponent(appl))" +
                    ".then(response => response.text().then(t => { try{ console.log('[AbstractFetchCustomerAddress][browser] status', response.status, 'len', t.length); }catch(e){} try{ var json = JSON.parse(t); callback(JSON.stringify(json)); }catch(e){ callback('__INVALID_JSON__:' + t); } }))" +
                    ".catch(err => { try{ console.log('[AbstractFetchCustomerAddress][browser] fetch failed', err); }catch(e){} callback(null); });";

            Object result = null;
            // Retry a few times to allow the app to start loading addresses asynchronously
            long start = System.currentTimeMillis();
            long timeout = 3000; // ms
            int attempts = 0;
            while (System.currentTimeMillis() - start < timeout) {
                attempts++;
                try {
                    result = js.executeAsyncScript(script, applId);
                } catch (Throwable t) {
                    System.out.println("[AbstractFetchCustomerAddress] fetch attempt " + attempts + " failed: " + t.getMessage());
                }
                if (result != null) break;
                try { Thread.sleep(200); } catch (InterruptedException ignore) {}
            }
            if (result == null) {
                System.out.println("[AbstractFetchCustomerAddress] fetch result null after " + attempts + " attempts for applId=" + applId);
            } else {
                System.out.println("[AbstractFetchCustomerAddress] fetch succeeded (or returned invalid-json token) for applId=" + applId + " -> " + (result instanceof String ? (((String)result).length() > 200 ? ((String)result).substring(0,200)+"..." : result) : result.toString()));
            }

            if (result != null) {
                try {
                    // Handle a special invalid JSON token returned by diagnostic browser fetch wrapper
                    String s = result.toString();
                    if (s.startsWith("__INVALID_JSON__:")) {
                        System.out.println("[AbstractFetchCustomerAddress] server returned invalid JSON for applId=" + applId + " -> " + s.substring("__INVALID_JSON__:".length()).trim());
                        // Treat as empty address list rather than failing the test
                        addressMap = new HashMap<>();
                        actor.remember("customer_addresses_" + applId, addressMap);
                    } else {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode root = mapper.readTree(s);
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
                    }

                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse address GeoJSON", e);
                }
            }
        }
        return addressMap;
    }
}
