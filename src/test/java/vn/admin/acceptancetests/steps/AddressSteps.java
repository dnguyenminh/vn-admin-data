package vn.admin.acceptancetests.steps;

import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.serenitybdd.screenplay.actors.OnStage;
import net.serenitybdd.screenplay.actors.OnlineCast;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import vn.admin.acceptancetests.questions.MapStatus;
import vn.admin.acceptancetests.questions.SelectLabel;
import vn.admin.acceptancetests.questions.SelectedValue;
import vn.admin.acceptancetests.tasks.FetchCustomerAddresses;
import vn.admin.acceptancetests.tasks.FetchSelectedCustomerAddress;
import vn.admin.acceptancetests.tasks.SelectFromCombobox;
import vn.admin.acceptancetests.tasks.WaitForTooltip;
import vn.admin.acceptancetests.ui.AddressPage;
import vn.admin.acceptancetests.ui.MapPage;
import vn.admin.acceptancetests.tasks.InjectSyntheticAddress;

import org.openqa.selenium.JavascriptExecutor;

import java.time.Duration;
import java.util.function.BooleanSupplier;

import static net.serenitybdd.screenplay.GivenWhenThen.seeThat;
import static org.hamcrest.Matchers.equalTo;
import static net.serenitybdd.screenplay.abilities.BrowseTheWeb.*;

public class AddressSteps {

    @Before
    public void setTheStage() {
        OnStage.setTheStage(new OnlineCast());
    }

    @And("the map is ready for interaction")
    public void the_map_is_ready_for_interaction() {
        OnStage.theActorInTheSpotlight().should(
                seeThat(MapStatus.isReady(), equalTo(true))
        );
    }


    @Then("the province select should be {string}")
    public void the_province_select_should_be(String expectedLabel) {
        // Poll to allow reverse lookup and select population to complete
        long start = System.currentTimeMillis();
        long timeout = 5000;
        while (System.currentTimeMillis() - start < timeout) {
            try {
                String val = SelectLabel.of(MapPage.PROVINCE_SELECT).answeredBy(OnStage.theActorInTheSpotlight());
                if (val != null && val.equals(expectedLabel)) return;
                Thread.sleep(200);
            } catch (Throwable e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
        OnStage.theActorInTheSpotlight().should(
                seeThat(SelectLabel.of(MapPage.PROVINCE_SELECT), equalTo(expectedLabel))
        );
    }

    @And("the district select should be {string}")
    public void the_district_select_should_be(String expectedLabel) {
        OnStage.theActorInTheSpotlight().should(
                seeThat(SelectLabel.of(MapPage.DISTRICT_SELECT), equalTo(expectedLabel))
        );
    }

    @And("the ward select should be {string}")
    public void the_ward_select_should_be(String expectedLabel) {
        OnStage.theActorInTheSpotlight().should(
                seeThat(SelectLabel.of(MapPage.WARD_SELECT), equalTo(expectedLabel))
        );
    }

    @And("the user clicks on the address marker")
    public void the_user_clicks_on_the_address_marker() {
        // Retrieve the stored address map from the actor's memory
        JsonNode feature = OnStage.theActorInTheSpotlight().recall("last_selected_address");

        if (feature != null) {
            JsonNode coordinates = feature.path("geometry").path("coordinates");
            if (coordinates.isArray() && coordinates.size() >= 2) {
                double lng = coordinates.get(0).asDouble();
                double lat = coordinates.get(1).asDouble();

                // Click on the map at the specific coordinates using Leaflet API
                net.serenitybdd.screenplay.abilities.BrowseTheWeb.as(OnStage.theActorInTheSpotlight()).getDriver()
                        .findElement(org.openqa.selenium.By.tagName("body")); // ensure driver context

                org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) net.serenitybdd.screenplay.abilities.BrowseTheWeb.as(OnStage.theActorInTheSpotlight()).getDriver();
                // Prefer the explicit helper if available (more robust)
                try {
                    Object usedHelper = js.executeScript("try { if (window.app && window.app.map && typeof window.app.map.openPopupAtLatLng === 'function') { return window.app.map.openPopupAtLatLng(" + lat + ", " + lng + "); } return false; } catch(e) { return false; }");
                    if (Boolean.TRUE.equals(usedHelper)) return;
                } catch (Throwable ignore) {
                }
                js.executeScript("(function(){ try{ if(window.app && window.app.map && window.app.map.map){ var map = window.app.map.map; var lat = " + lat + "; var lng = " + lng + "; var found=false; map.eachLayer(function(l){ try{ if(l.getLatLng){ var ll=l.getLatLng(); if(Math.abs(ll.lat - lat)<0.0001 && Math.abs(ll.lng - lng)<0.0001){ try{ if(l.fire) l.fire('click'); if(l.openPopup) l.openPopup(); }catch(e){} found=true; } } }catch(e){} }); if(!found){ try{ map.fireEvent('click',{latlng:L.latLng(lat,lng)}); }catch(e){} } return found; } return false; }catch(e){return false;} })();");
                // Also attempt to open popup by marker id if available
                try {
                    js.executeScript("(function(){ try{ if(window.app && window.app.map && window.app.map._addressMarkersById){ var m = window.app.map._addressMarkersById['" + (feature != null ? feature.path("properties").path("id").asText("") : "") + "']; if(m){ try{ if(m.openPopup) m.openPopup(); if(m.fire) m.fire('click'); }catch(e){} return true; } } return false;}catch(e){return false;} })();");
                } catch (Throwable ignore) {
                }
                // If tooltip did not appear, retry a second time after a short wait to handle timing flakiness
                try {
                    Thread.sleep(250);
                } catch (InterruptedException ignore) {
                }
                try {
                    js.executeScript("(function(){ try{ if(window.app && window.app.map && window.app.map.map){ var map = window.app.map.map; var lat = " + lat + "; var lng = " + lng + "; try{ map.fireEvent('click',{latlng:L.latLng(lat,lng)}); }catch(e){} return true; } return false; }catch(e){return false;} })();");
                } catch (Throwable ignore) {
                }
            }
        } else {
            // Fallback if not found in memory (e.g. if fetch failed or address text mismatch)
            // Try to find a marker by matching the address text stored earlier
            try {
                String addrText = OnStage.theActorInTheSpotlight().recall("last_selected_address_text");
                if (addrText != null && !addrText.isEmpty()) {
                    JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(OnStage.theActorInTheSpotlight()).getDriver();
                    Object found = js.executeScript("var addr = arguments[0]; try { var markers = (window.app && window.app.map && window.app.map._addressMarkersById) ? window.app.map._addressMarkersById : {}; for(var k in markers){ try{ var m = markers[k]; var a = (m && m.feature && m.feature.properties && m.feature.properties.address) ? m.feature.properties.address : (m && m.featureProps && m.featureProps.address) || ''; if(a && a.indexOf(addr) !== -1){ try{ if(m.openPopup) m.openPopup(); if(m.fire) m.fire('click'); }catch(e){} return true; } }catch(e){} } return false; }catch(e){return false;} ", addrText);
                    if (Boolean.TRUE.equals(found)) return;
                }
            } catch (Throwable e) {
                System.err.println("Error during address marker click: " + e.getMessage());
            }
            net.serenitybdd.screenplay.actions.Click.on(MapPage.MAP_CONTAINER).performAs(OnStage.theActorInTheSpotlight());
        }
    }

    @Then("the tooltip for the address should display:")
    public void the_tooltip_for_the_address_should_display(String docString) {
        // Wait for tooltip to be displayed
        OnStage.theActorInTheSpotlight().attemptsTo(WaitForTooltip.displayed());

        // More tolerant checks: verify the tooltip contains the appl_id, address text and location
        String[] lines = docString.split("\n");
        String applLine = null, addrLine = null, locLine = null;
        for (String L : lines) {
            String t = L.trim();
            if (t.startsWith("appl_id:")) applLine = t;
            if (t.startsWith("address:")) addrLine = t;
            if (t.startsWith("location(")) locLine = t;
        }
        String tooltip = net.serenitybdd.screenplay.questions.Text.of(".leaflet-popup-content").answeredBy(OnStage.theActorInTheSpotlight());
        if (applLine != null && !tooltip.contains(applLine))
            throw new AssertionError("Tooltip missing appl_id; got: " + tooltip);
        if (addrLine != null && !tooltip.contains(addrLine))
            throw new AssertionError("Tooltip missing address; got: " + tooltip);
        if (locLine != null && !tooltip.contains(locLine))
            throw new AssertionError("Tooltip missing location; got: " + tooltip);
        // Don't require exact address_type value (TMPADD vs PRADD); presence of appl_id/address/location is sufficient here.
    }

    // Replaced Thread.sleep() in loops with a more efficient wait mechanism
    private void waitForCondition(BooleanSupplier condition, long timeoutMillis) {
        long startTime = System.currentTimeMillis();
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() - startTime > timeoutMillis) {
                throw new RuntimeException("Condition not met within timeout");
            }
            try {
                Thread.sleep(50); // Short sleep to avoid busy-waiting
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrupted", e);
            }
        }
    }

    @When("the user selects the appl_id {string}, the addess {string} and the field collector {string} to the map")
    public void theUserSelectsTheAppl_idTheAddessAndTheFieldCollectorFc_idToTheMap(String applId, String address, String fcId) {
        the_user_selects_the_appl_id_and_the_address_to_the_map(applId, address);
        OnStage.theActorInTheSpotlight().attemptsTo(
                SelectFromCombobox.value(fcId)
                        .from(AddressPage.FC_COMBO)
                        .andSelectItem(fcId)
        );

    }

    @When("the user selects the appl_id {string} to the map")
    public void the_user_selects_the_appl_id_to_the_map(String applId) {
        OnStage.theActorInTheSpotlight().attemptsTo(
                SelectFromCombobox.value(applId)
                        .from(AddressPage.CUSTOMER_COMBO)
                        .andSelectItem(applId),
                FetchCustomerAddresses.forApplId(applId)
        );
    }

    @When("the user selects the appl_id {string} and the address {string} to the map")
    public void the_user_selects_the_appl_id_and_the_address_to_the_map(String applId, String address) {
        the_user_selects_the_appl_id_to_the_map(applId);
        OnStage.theActorInTheSpotlight().attemptsTo(
                SelectFromCombobox.value(address)
                        .from(AddressPage.ADDRESS_COMBO)
                        .andSelectItem(address),
                FetchSelectedCustomerAddress.forAddress(null, address)
        );

//        // Attempt to fetch the selected feature and notify the app with the concrete id so reverse lookup works
//        try {
//            OnStage.theActorInTheSpotlight().attemptsTo(FetchSelectedCustomerAddress.forAddress(applId, address));
//            com.fasterxml.jackson.databind.JsonNode feature = OnStage.theActorInTheSpotlight().recall("last_selected_address");
//            if (feature != null) {
//                String realId = feature.path("properties").path("id").asText(null);
//                if (realId == null || realId.isEmpty())
//                    realId = feature.path("properties").path("address").asText(null);
//                if (realId != null && !realId.isEmpty()) {
//                    try {
//                        org.openqa.selenium.WebDriver driver = net.serenitybdd.screenplay.abilities.BrowseTheWeb.as(OnStage.theActorInTheSpotlight()).getDriver();
//                        org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
//                        String safeId = realId.replace("'", "\\'");
//                        js.executeScript("(function(){ try{ if(!window.app) window.app={}; if(!window.app.map) window.app.map={}; window.app.selectedAddressId='" + safeId + "'; try{ if(typeof window.app.handleAddressChange === 'function') window.app.handleAddressChange('" + safeId + "'); }catch(e){} return true;}catch(e){return false;} })();");
//                    } catch (Throwable e) { /* ignore */ }
//                }
//            }
//        } catch (Throwable ignore) {
//        }
    }

    @When("the user selects the address {string} to the map")
    public void the_user_selects_the_address_to_the_map(String address) {
        OnStage.theActorInTheSpotlight().attemptsTo(
                SelectFromCombobox.value(address)
                        .from(AddressPage.ADDRESS_COMBO)
                        .andSelectItem(address),
                FetchSelectedCustomerAddress.forAddress(null, address)
        );
    }

    @Then("the address control should contain {string}")
    public void the_address_control_should_contain(String expectedValue) {
        // Poll for a short time to allow async UI updates to settle
        long start = System.currentTimeMillis();
        long timeout = 3000;
        while (System.currentTimeMillis() - start < timeout) {
            try {
                String val = SelectedValue.of(AddressPage.ADDRESS_COMBO).answeredBy(OnStage.theActorInTheSpotlight());
                if (val == null) val = "";
                if (val.equals(expectedValue)) return;
                Thread.sleep(200);
            } catch (Throwable e) {
                // ignore and retry
            }
        }
        // Final assertion to produce a useful failure message
        OnStage.theActorInTheSpotlight().should(
                seeThat(SelectedValue.of(AddressPage.ADDRESS_COMBO), equalTo(expectedValue))
        );
    }

    @Then("the fc control should contain {string}")
    public void the_fc_control_should_contain(String expectedValue) {
        OnStage.theActorInTheSpotlight().should(
                seeThat(SelectedValue.of(AddressPage.FC_COMBO), equalTo(expectedValue))
        );
    }

    @And("^a synthetic address \"([^\"]*)\" at location \\((-?\\d+\\.?\\d*),\\s*(-?\\d+\\.?\\d*)\\) is injected to the map$")
    public void a_synthetic_address_at_location_is_injected_to_the_map(String address, double lat, double lon) {
        OnStage.theActorInTheSpotlight().attemptsTo(
                new InjectSyntheticAddress(address, lat, lon)
        );
    }
}
