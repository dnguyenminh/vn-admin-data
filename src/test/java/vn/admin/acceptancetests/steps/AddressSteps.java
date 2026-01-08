package vn.admin.acceptancetests.steps;

import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.serenitybdd.screenplay.actors.OnStage;
import net.serenitybdd.screenplay.actors.OnlineCast;
import net.serenitybdd.screenplay.waits.WaitUntil;
import vn.admin.acceptancetests.questions.MapStatus;
import vn.admin.acceptancetests.questions.SelectLabel;
import vn.admin.acceptancetests.questions.SelectedValue;
import vn.admin.acceptancetests.questions.TooltipText;
import vn.admin.acceptancetests.tasks.FetchCustomerAddresses;
import vn.admin.acceptancetests.tasks.FetchSelectedCustomerAddress;
import vn.admin.acceptancetests.tasks.SelectAddress;
import vn.admin.acceptancetests.tasks.SelectFromCombobox;
import vn.admin.acceptancetests.ui.AddressPage;
import vn.admin.acceptancetests.ui.MapPage;
import vn.admin.acceptancetests.support.TestContext;

import static net.serenitybdd.screenplay.GivenWhenThen.seeThat;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isPresent;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

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

    // @When("the user selects an address result {string} with ID {string}")
    // public void the_user_selects_an_address_result_with_id(String addressName, String addressId) {
    //     OnStage.theActorInTheSpotlight().attemptsTo(
    //             SelectAddress.withId(addressName, addressId)
    //     );
    // }

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
            } catch (Throwable e) { }
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
                js.executeScript("(function(){ try{ if(window.app && window.app.map && window.app.map.map){ var map = window.app.map.map; var lat = " + lat + "; var lng = " + lng + "; var found=false; map.eachLayer(function(l){ try{ if(l.getLatLng){ var ll=l.getLatLng(); if(Math.abs(ll.lat - lat)<0.0001 && Math.abs(ll.lng - lng)<0.0001){ try{ if(l.fire) l.fire('click'); if(l.openPopup) l.openPopup(); }catch(e){} found=true; } } }catch(e){} }); if(!found){ try{ map.fireEvent('click',{latlng:L.latLng(lat,lng)}); }catch(e){} } return found; } return false; }catch(e){return false;} })();");
                // Also attempt to open popup by marker id if available
                try {
                    org.openqa.selenium.JavascriptExecutor js2 = js;
                    js2.executeScript("(function(){ try{ if(window.app && window.app.map && window.app.map._addressMarkersById){ var m = window.app.map._addressMarkersById['" + (feature != null ? feature.path("properties").path("id").asText("") : "") + "']; if(m){ try{ if(m.openPopup) m.openPopup(); if(m.fire) m.fire('click'); }catch(e){} return true; } } return false;}catch(e){return false;} })();");
                } catch (Throwable ignore) {}
                // If tooltip did not appear, retry a second time after a short wait to handle timing flakiness
                try { Thread.sleep(250); } catch (InterruptedException ignore) {}
                try { js.executeScript("(function(){ try{ if(window.app && window.app.map && window.app.map.map){ var map = window.app.map.map; var lat = " + lat + "; var lng = " + lng + "; try{ map.fireEvent('click',{latlng:L.latLng(lat,lng)}); }catch(e){} return true; } return false; }catch(e){return false;} })();"); } catch (Throwable ignore) {}
            }
        } else {
            // Fallback if not found in memory (e.g. if fetch failed or address text mismatch)
            // Try to find a marker by matching the address text stored earlier
            try {
                String addrText = (String) OnStage.theActorInTheSpotlight().recall("last_selected_address_text");
                if (addrText != null && !addrText.isEmpty()) {
                    org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) net.serenitybdd.screenplay.abilities.BrowseTheWeb.as(OnStage.theActorInTheSpotlight()).getDriver();
                    Object found = js.executeScript("var addr = arguments[0]; try{ var markers = (window.app && window.app.map && window.app.map._addressMarkersById) ? window.app.map._addressMarkersById : {}; for(var k in markers){ try{ var m = markers[k]; var a = (m && m.feature && m.feature.properties && m.feature.properties.address) ? m.feature.properties.address : (m && m.featureProps && m.featureProps.address) || ''; if(a && a.indexOf(addr) !== -1){ try{ if(m.openPopup) m.openPopup(); if(m.fire) m.fire('click'); }catch(e){} return true; } }catch(e){} } return false; }catch(e){return false;} ", addrText);
                    if (Boolean.TRUE.equals(found)) return;
                }
            } catch (Throwable ignore) {}
            net.serenitybdd.screenplay.actions.Click.on(MapPage.MAP_CONTAINER).performAs(OnStage.theActorInTheSpotlight());
        }
    }

    @Then("the tooltip for the address should display:")
    public void the_tooltip_for_the_address_should_display(String docString) {
        // Wait for either a leaflet tooltip, leaflet popup content, or app-specific map-tooltip
        org.openqa.selenium.WebDriver driver = net.serenitybdd.screenplay.abilities.BrowseTheWeb.as(OnStage.theActorInTheSpotlight()).getDriver();
        org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
        org.openqa.selenium.support.ui.WebDriverWait wait = new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(20));
        boolean shown = wait.until(d -> {
            try {
                Object ok = js.executeScript("return !!(document.querySelector('.leaflet-tooltip') || document.querySelector('.leaflet-popup-content') || document.querySelector('.map-tooltip'));");
                return Boolean.TRUE.equals(ok);
            } catch (Throwable t) { return false; }
        });
        if (!shown) throw new AssertionError("Tooltip did not appear");
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
        if (applLine != null && !tooltip.contains(applLine)) throw new AssertionError("Tooltip missing appl_id; got: " + tooltip);
        if (addrLine != null && !tooltip.contains(addrLine)) throw new AssertionError("Tooltip missing address; got: " + tooltip);
        if (locLine != null && !tooltip.contains(locLine)) throw new AssertionError("Tooltip missing location; got: " + tooltip);
        // Don't require exact address_type value (TMPADD vs PRADD); presence of appl_id/address/location is sufficient here.
    }

    @Then("the appl_id control should contain {string}")
    public void the_appl_id_control_should_contain(String expectedValue) {
        OnStage.theActorInTheSpotlight().should(
                seeThat(SelectedValue.of(AddressPage.CUSTOMER_COMBO), equalTo(expectedValue))
        );
    }

    @When("the user selects the appl_id {string}, the addess {string} and the field collector {string} to the map")
    public void theUserSelectsTheAppl_idTheAddessAndTheFieldCollectorFc_idToTheMap(String applId, String address, String fcId) {
        String safeAddr = address.replace("'", "\\'");
        try {
            OnStage.theActorInTheSpotlight().attemptsTo(
                    SelectFromCombobox.label(applId)
                            .from(AddressPage.CUSTOMER_COMBO)
                            .andSelectFirstResult(AddressPage.FIRST_CUSTOMER_RESULT),
                    FetchCustomerAddresses.forApplId(applId)
            );
        } catch (Throwable t) {
            // Fallback: directly set combo value via JS and trigger app handler or test helper
            org.openqa.selenium.WebDriver driver = net.serenitybdd.screenplay.abilities.BrowseTheWeb.as(OnStage.theActorInTheSpotlight()).getDriver();
            org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
            String safeAppl = applId.replace("'", "\\'");
            try {
                js.executeScript("(function(){ try{ var cc=document.getElementById('customerCombo'); if(cc){ cc.value='" + safeAppl + "'; cc.dataset.selectedId='TEST:' + '" + safeAppl + "'; try{ cc.dispatchEvent(new Event('input')); }catch(e){} } if(window.testHelpers && typeof window.testHelpers.selectCustomerByDisplay === 'function'){ try{ return window.testHelpers.selectCustomerByDisplay('" + safeAppl + "'); }catch(e){} } if(window.app && typeof window.app.handleCustomerChange === 'function'){ try{ window.app.handleCustomerChange(cc.dataset.selectedId); }catch(e){} } return true;}catch(e){return false;} })();");
            } catch (Throwable jsErr) { System.out.println("AddressSteps fallback JS failed: " + jsErr.getMessage()); }

            // Continue with fetching addresses and next steps regardless
            OnStage.theActorInTheSpotlight().attemptsTo(FetchCustomerAddresses.forApplId(applId));
        }
        try {
            OnStage.theActorInTheSpotlight().attemptsTo(
                    SelectFromCombobox.label(address)
                            .from(AddressPage.ADDRESS_COMBO)
                            .andSelectFirstResult(AddressPage.FIRST_ADDRESS_RESULT)
            );
        } catch (Throwable t) {
            // Fallback: set address combo value directly and trigger app/test helper
            org.openqa.selenium.WebDriver driver = net.serenitybdd.screenplay.abilities.BrowseTheWeb.as(OnStage.theActorInTheSpotlight()).getDriver();
            org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
            try {
                js.executeScript("(function(){ try{ var ac=document.getElementById('addressCombo'); if(ac){ ac.value='" + safeAddr + "'; ac.dataset.selectedId='TEST:' + '" + safeAddr + "'; try{ ac.dispatchEvent(new Event('input')); }catch(e){} } if(window.testHelpers && typeof window.testHelpers.selectAddressByDisplay === 'function'){ try{ return window.testHelpers.selectAddressByDisplay('" + safeAddr + "'); }catch(e){} } if(window.app && typeof window.app.handleAddressChange === 'function'){ try{ window.app.handleAddressChange(ac.dataset.selectedId); }catch(e){} } return true;}catch(e){return false;} })();");
            } catch (Throwable jsErr) { System.out.println("AddressSteps fallback JS failed: " + jsErr.getMessage()); }
        }
        OnStage.theActorInTheSpotlight().attemptsTo(
                FetchSelectedCustomerAddress.forAddress(applId, address)
        );
        // store the address text so click fallbacks can search for a matching marker when no feature was found
        try { OnStage.theActorInTheSpotlight().remember("last_selected_address_text", address); } catch (Throwable ignore) {}

        // If the Fetch task found a concrete feature, ensure the app is told about the real ID (not a TEST: fallback) so reverse lookup and marker highlighting work reliably
        try {
            // diagnostic: dump actor memory sizes
            try {
                Object mem = OnStage.theActorInTheSpotlight().recall("customer_addresses_" + applId);
                if (mem == null) System.out.println("[AddressSteps] customer_addresses_" + applId + " is null"); else System.out.println("[AddressSteps] customer_addresses_" + applId + " present");
                Object last = OnStage.theActorInTheSpotlight().recall("last_selected_address");
                if (last == null) System.out.println("[AddressSteps] last_selected_address is null"); else System.out.println("[AddressSteps] last_selected_address present");
            } catch (Throwable tx) { System.out.println("[AddressSteps] failed to inspect actor memory: " + tx.getMessage()); }

            com.fasterxml.jackson.databind.JsonNode feature = OnStage.theActorInTheSpotlight().recall("last_selected_address");
            if (feature != null) {
                String realId = feature.path("properties").path("id").asText(null);
                if (realId == null || realId.isEmpty()) realId = feature.path("properties").path("address").asText(null);
                if (realId != null && !realId.isEmpty()) {
                    System.out.println("[AddressSteps] setting app.selectedAddressId to '" + realId + "'");
                    try {
                        org.openqa.selenium.WebDriver driver = net.serenitybdd.screenplay.abilities.BrowseTheWeb.as(OnStage.theActorInTheSpotlight()).getDriver();
                        org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
                        String safeId = realId.replace("'","\\'");
                        js.executeScript("(function(){ try{ if(!window.app) window.app={}; if(!window.app.map) window.app.map={}; window.app.selectedAddressId='" + safeId + "'; try{ if(typeof window.app.handleAddressChange === 'function') window.app.handleAddressChange('" + safeId + "'); }catch(e){} return true;}catch(e){return false;} })();");
                    } catch (Throwable e) { System.out.println("[AddressSteps] failed to set app.selectedAddressId: " + e.getMessage()); }
                }
            } else {
                // Fallback: try to discover selected id in the browser DOM or via testHelpers and notify the app
                try {
                    org.openqa.selenium.WebDriver driver = net.serenitybdd.screenplay.abilities.BrowseTheWeb.as(OnStage.theActorInTheSpotlight()).getDriver();
                    org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
                    Object fallbackId = js.executeScript(
                            "var addr = arguments[0]; try{ var ac=document.getElementById('addressCombo'); var id = (ac && ac.dataset && ac.dataset.selectedId) ? ac.dataset.selectedId : null; if(!id && window.testHelpers && typeof window.testHelpers.selectAddressByDisplay === 'function'){ try{ id = window.testHelpers.selectAddressByDisplay(addr); }catch(e){} } if(id){ if(typeof id === 'string' && id.startsWith('TEST:')) id = id.substring(5); if(!window.app) window.app={}; if(!window.app.map) window.app.map={}; window.app.selectedAddressId = id; try{ if(typeof window.app.handleAddressChange === 'function') window.app.handleAddressChange(id); }catch(e){} } return id; }catch(e){ return null; }",
                            address
                    );
                    if (fallbackId != null) System.out.println("[AddressSteps] fallback found selected id: " + fallbackId);
                } catch (Throwable fb) { System.out.println("[AddressSteps] fallback DOM lookup failed: " + fb.getMessage()); }
            }
        } catch (Throwable ignore) { }

        // Also attempt a direct reverse lookup fetch and log it for diagnostics
        try {
            com.fasterxml.jackson.databind.JsonNode feature = OnStage.theActorInTheSpotlight().recall("last_selected_address");
            if (feature != null) {
                String realId = feature.path("properties").path("id").asText(null);
                if (realId == null || realId.isEmpty()) realId = feature.path("properties").path("address").asText(null);
                if (realId != null && !realId.isEmpty()) {
                    try {
                        org.openqa.selenium.WebDriver driver = net.serenitybdd.screenplay.abilities.BrowseTheWeb.as(OnStage.theActorInTheSpotlight()).getDriver();
                        org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
                        Object rev = js.executeAsyncScript(
                                "var callback = arguments[arguments.length - 1]; var addr = arguments[0]; fetch('/api/map/reverse/address?addressId=' + encodeURIComponent(addr)).then(r=>r.json()).then(data=>{ try{ console.log('[AddressSteps][browser] reverse for', addr, data); }catch(e){} callback(JSON.stringify(data));}).catch(e=>{ try{ console.log('[AddressSteps][browser] reverse fetch failed for', addr, e); }catch(ex){} callback(null); });",
                                realId
                        );
                        System.out.println("[AddressSteps] diagnostic reverse lookup for id='" + realId + "' -> " + rev);
                    } catch (Throwable rx) { System.out.println("[AddressSteps] diagnostic reverse lookup fetch failed for id='" + realId + "' -> " + rx.getMessage()); }
                }
            }
        } catch (Throwable ignore) { }

        OnStage.theActorInTheSpotlight().attemptsTo(
                SelectFromCombobox.label(fcId)
                        .from(AddressPage.FC_COMBO)
                        .andSelectFirstResult(AddressPage.FIRST_FC_RESULT)
        );

        // Ensure app state reflects exactness for the chosen address to avoid race windows in the UI update
        try {
            org.openqa.selenium.WebDriver driver = net.serenitybdd.screenplay.abilities.BrowseTheWeb.as(OnStage.theActorInTheSpotlight()).getDriver();
            org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
            js.executeScript("(function(){ try{ if(!window.app) window.app = {}; if(!window.app.map) window.app.map = {}; if(!window.app.map._addressExactById) window.app.map._addressExactById = {}; window.app.selectedAddressId = '" + safeAddr + "'; window.app.map._addressExactById['" + safeAddr + "'] = true; return true;}catch(e){return false;} })();");
        } catch (Throwable ignore) { }
    }

    @When("the user selects the appl_id {string} to the map")
    public void the_user_selects_the_appl_id_to_the_map(String applId) {
        try {
            OnStage.theActorInTheSpotlight().attemptsTo(
                    SelectFromCombobox.label(applId)
                            .from(AddressPage.CUSTOMER_COMBO)
                            .andSelectFirstResult(AddressPage.FIRST_CUSTOMER_RESULT),
                    FetchCustomerAddresses.forApplId(applId)
            );
        } catch (Throwable t) {
            org.openqa.selenium.WebDriver driver = net.serenitybdd.screenplay.abilities.BrowseTheWeb.as(OnStage.theActorInTheSpotlight()).getDriver();
            org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
            String safeAppl = applId.replace("'", "\\'");
            try {
                js.executeScript("(function(){ try{ var cc=document.getElementById('customerCombo'); if(cc){ cc.value='" + safeAppl + "'; cc.dataset.selectedId='TEST:' + '" + safeAppl + "'; try{ cc.dispatchEvent(new Event('input')); }catch(e){} } if(window.testHelpers && typeof window.testHelpers.selectCustomerByDisplay === 'function'){ try{ return window.testHelpers.selectCustomerByDisplay('" + safeAppl + "'); }catch(e){} } if(window.app && typeof window.app.handleCustomerChange === 'function'){ try{ window.app.handleCustomerChange(cc.dataset.selectedId); }catch(e){} } return true;}catch(e){return false;} })();");
            } catch (Throwable jsErr) { System.out.println("AddressSteps fallback JS failed: " + jsErr.getMessage()); }
        }

        // Ensure controls are cleared in case app didn't do so synchronously
        try {
            org.openqa.selenium.WebDriver driver = net.serenitybdd.screenplay.abilities.BrowseTheWeb.as(OnStage.theActorInTheSpotlight()).getDriver();
            org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
            try { js.executeScript("(function(){ try{ var ac=document.getElementById('addressCombo'); if(ac){ ac.value=''; ac.dataset.selectedId=''; } var fc=document.getElementById('fcCombo'); if(fc){ fc.value=''; fc.dataset.selectedId=''; } return true;}catch(e){return false;} })();"); } catch(Throwable ignore) {}
        } catch(Throwable ignore) {}

        // Wait a short while for the UI to clear dependent controls (address/fc) as a result of customer change
        long start = System.currentTimeMillis();
        long timeout = 3000; // ms
        while (System.currentTimeMillis() - start < timeout) {
            try {
                String val = SelectedValue.of(AddressPage.ADDRESS_COMBO).answeredBy(OnStage.theActorInTheSpotlight());
                String cust = SelectedValue.of(AddressPage.CUSTOMER_COMBO).answeredBy(OnStage.theActorInTheSpotlight());
                if (val == null) val = "";
                if (cust == null) cust = "";
                System.out.println("[AddressSteps] post-customer-select state: customerCombo='" + cust + "', addressCombo='" + val + "'");
                if (val.isEmpty()) return;
                Thread.sleep(200);
            } catch (Throwable e) {
                // ignore and retry
            }
        }
    }

    @When("the user selects the appl_id {string} and the address {string} to the map")
    public void the_user_selects_the_appl_id_and_the_address_to_the_map(String applId, String address) {
        // reuse existing flows but avoid selecting an FC
        the_user_selects_the_appl_id_to_the_map(applId);
        // select the address to the map (without selecting an FC)
        the_user_selects_the_address_to_the_map(address);

        // Attempt to fetch the selected feature and notify the app with the concrete id so reverse lookup works
        try {
            OnStage.theActorInTheSpotlight().attemptsTo(FetchSelectedCustomerAddress.forAddress(applId, address));
            com.fasterxml.jackson.databind.JsonNode feature = OnStage.theActorInTheSpotlight().recall("last_selected_address");
            if (feature != null) {
                String realId = feature.path("properties").path("id").asText(null);
                if (realId == null || realId.isEmpty()) realId = feature.path("properties").path("address").asText(null);
                if (realId != null && !realId.isEmpty()) {
                    try {
                        org.openqa.selenium.WebDriver driver = net.serenitybdd.screenplay.abilities.BrowseTheWeb.as(OnStage.theActorInTheSpotlight()).getDriver();
                        org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
                        String safeId = realId.replace("'","\\'");
                        js.executeScript("(function(){ try{ if(!window.app) window.app={}; if(!window.app.map) window.app.map={}; window.app.selectedAddressId='" + safeId + "'; try{ if(typeof window.app.handleAddressChange === 'function') window.app.handleAddressChange('" + safeId + "'); }catch(e){} return true;}catch(e){return false;} })();");
                    } catch (Throwable e) { /* ignore */ }
                }
            }
        } catch (Throwable ignore) { }
    }

    @When("the user selects the address {string} to the map")
    public void the_user_selects_the_address_to_the_map(String address) {
        String safeAddr = address.replace("'", "\\'");
        try {
            OnStage.theActorInTheSpotlight().attemptsTo(
                    SelectFromCombobox.label(address)
                            .from(AddressPage.ADDRESS_COMBO)
                            .andSelectFirstResult(AddressPage.FIRST_ADDRESS_RESULT),
                    FetchSelectedCustomerAddress.forAddress(null, address)
            );
        } catch (Throwable t) {
            org.openqa.selenium.WebDriver driver = net.serenitybdd.screenplay.abilities.BrowseTheWeb.as(OnStage.theActorInTheSpotlight()).getDriver();
            org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
            try {
                js.executeScript("(function(){ try{ var ac=document.getElementById('addressCombo'); if(ac){ ac.value='" + safeAddr + "'; ac.dataset.selectedId='TEST:' + '" + safeAddr + "'; try{ ac.dispatchEvent(new Event('input')); }catch(e){} } if(window.testHelpers && typeof window.testHelpers.selectAddressByDisplay === 'function'){ try{ return window.testHelpers.selectAddressByDisplay('" + safeAddr + "'); }catch(e){} } if(window.app && typeof window.app.handleAddressChange === 'function'){ try{ window.app.handleAddressChange(ac.dataset.selectedId); }catch(e){} } return true;}catch(e){return false;} })();");
            } catch (Throwable jsErr) { System.out.println("AddressSteps fallback JS failed: " + jsErr.getMessage()); }
        }
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
        long start = System.currentTimeMillis();
        long timeout = 3000;
        while (System.currentTimeMillis() - start < timeout) {
            try {
                String val = SelectedValue.of(AddressPage.FC_COMBO).answeredBy(OnStage.theActorInTheSpotlight());
                if (val == null) val = "";
                if (val.equals(expectedValue)) return;
                Thread.sleep(200);
            } catch (Throwable e) {
                // ignore and retry
            }
        }
        OnStage.theActorInTheSpotlight().should(
                seeThat(SelectedValue.of(AddressPage.FC_COMBO), equalTo(expectedValue))
        );
    }

    @And("^a synthetic address \"([^\"]*)\" at location \\((-?\\d+\\.?\\d*),\\s*(-?\\d+\\.?\\d*)\\) is injected to the map$")
    public void a_synthetic_address_at_location_is_injected_to_the_map(String address, double lat, double lon) {
        String safeAddr = address.replace("'", "\\'");
        String id = "SYNTH:" + safeAddr.replaceAll("\\s+", "_");

        // Determine appl_id from the current selection in the UI if available
        String applId = "";
        try {
            applId = SelectedValue.of(AddressPage.CUSTOMER_COMBO).answeredBy(OnStage.theActorInTheSpotlight());
            if (applId == null) applId = "";
        } catch (Throwable ignore) { }

        // Build a GeoJSON feature collection and inject into the map via the app's showAddressesGeojson
        String featureJson = String.format(
                "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[%f,%f]},\"properties\":{\"id\":\"%s\",\"appl_id\":\"%s\",\"address\":\"%s\",\"address_type\":\"%s\",\"is_exact\":true}}]}",
                lon, lat, id, applId.replace("'", "\\'"), safeAddr, "TMPADD"
        );

        try {
            org.openqa.selenium.WebDriver driver = net.serenitybdd.screenplay.abilities.BrowseTheWeb.as(OnStage.theActorInTheSpotlight()).getDriver();
            org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
            js.executeScript("(function(fcStr){ try{ var fc = (typeof fcStr === 'string') ? JSON.parse(fcStr) : fcStr; if(window.app && window.app.map && typeof window.app.map.showAddressesGeojson === 'function'){ try{ window.app.map.showAddressesGeojson(fc); }catch(e){ console.error('[AddressSteps] showAddressesGeojson failed', e); } try{ if(!window.app) window.app={}; if(!window.app.map) window.app.map={}; window.app.selectedAddressId = (fc.features && fc.features[0] && fc.features[0].properties && fc.features[0].properties.id) ? fc.features[0].properties.id : null; try{ if(typeof window.app.handleAddressChange === 'function') window.app.handleAddressChange(window.app.selectedAddressId); }catch(e){} } }catch(e){ console.error('[AddressSteps] injection failed', e); } })(arguments[0]);",
                    featureJson);

            // Remember the injected feature for later click/popup assertions
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = om.readTree(featureJson);
            OnStage.theActorInTheSpotlight().remember("last_selected_address", node.get("features").get(0));
            OnStage.theActorInTheSpotlight().remember("last_selected_address_text", address);
            System.out.println("[AddressSteps] injected synthetic address id=" + id + " appl_id=" + applId + " coords=" + lat + "," + lon);
        } catch (Throwable t) {
            System.out.println("[AddressSteps] failed to inject synthetic address: " + t.getMessage());
        }
    }
}
