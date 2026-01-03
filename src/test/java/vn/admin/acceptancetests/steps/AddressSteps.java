package vn.admin.acceptancetests.steps;

import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.serenitybdd.screenplay.actors.OnStage;
import net.serenitybdd.screenplay.waits.WaitUntil;
import vn.admin.acceptancetests.questions.MapStatus;
import vn.admin.acceptancetests.questions.SelectedValue;
import vn.admin.acceptancetests.questions.TooltipText;
import vn.admin.acceptancetests.tasks.FetchCustomerAddresses;
import vn.admin.acceptancetests.tasks.FetchSelectedCustomerAddress;
import vn.admin.acceptancetests.tasks.SelectAddress;
import vn.admin.acceptancetests.tasks.SelectFromCombobox;
import vn.admin.acceptancetests.ui.AddressPage;
import vn.admin.acceptancetests.ui.MapPage;

import static net.serenitybdd.screenplay.GivenWhenThen.seeThat;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isPresent;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class AddressSteps {

    @And("the map is ready for interaction")
    public void the_map_is_ready_for_interaction() {
        OnStage.theActorInTheSpotlight().should(
                seeThat(MapStatus.isReady(), equalTo(true))
        );
    }

    @When("the user selects an address result {string} with ID {string}")
    public void the_user_selects_an_address_result_with_id(String addressName, String addressId) {
        OnStage.theActorInTheSpotlight().attemptsTo(
                SelectAddress.withId(addressName, addressId)
        );
    }

    @Then("the province select should be {string}")
    public void the_province_select_should_be(String expectedValue) {
        OnStage.theActorInTheSpotlight().should(
                seeThat(SelectedValue.of(MapPage.PROVINCE_SELECT), equalTo(expectedValue))
        );
    }

    @And("the district select should be {string}")
    public void the_district_select_should_be(String expectedValue) {
        OnStage.theActorInTheSpotlight().should(
                seeThat(SelectedValue.of(MapPage.DISTRICT_SELECT), equalTo(expectedValue))
        );
    }

    @And("the ward select should be {string}")
    public void the_ward_select_should_be(String expectedValue) {
        OnStage.theActorInTheSpotlight().should(
                seeThat(SelectedValue.of(MapPage.WARD_SELECT), equalTo(expectedValue))
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

                ((org.openqa.selenium.JavascriptExecutor) net.serenitybdd.screenplay.abilities.BrowseTheWeb.as(OnStage.theActorInTheSpotlight()).getDriver())
                        .executeScript("if(window.app && window.app.map && window.app.map.map) { window.app.map.map.fireEvent('click', { latlng: L.latLng(" + lat + ", " + lng + ") }); }");
            }
        } else {
            // Fallback if not found in memory (e.g. if fetch failed or address text mismatch)
            net.serenitybdd.screenplay.actions.Click.on(MapPage.MAP_CONTAINER).performAs(OnStage.theActorInTheSpotlight());
        }
    }

    @Then("the tooltip for the address should display:")
    public void the_tooltip_for_the_address_should_display(String docString) {
        OnStage.theActorInTheSpotlight().attemptsTo(
                WaitUntil.the(AddressPage.LEAFLET_TOOLTIP, isPresent()).forNoMoreThan(20).seconds()
        );
        OnStage.theActorInTheSpotlight().should(
                seeThat(TooltipText.value(), containsString(docString))
        );
    }

    @Then("the appl_id control should contain {string}")
    public void the_appl_id_control_should_contain(String expectedValue) {
        OnStage.theActorInTheSpotlight().should(
                seeThat(SelectedValue.of(AddressPage.CUSTOMER_COMBO), equalTo(expectedValue))
        );
    }

    @When("the user selects the appl_id {string}, the addess {string} and the field collector {string} to the map")
    public void theUserSelectsTheAppl_idTheAddessAndTheFieldCollectorFc_idToTheMap(String applId, String address, String fcId) {
        OnStage.theActorInTheSpotlight().attemptsTo(
                SelectFromCombobox.value(applId)
                        .from(AddressPage.CUSTOMER_COMBO)
                        .andSelectFirstResult(AddressPage.FIRST_CUSTOMER_RESULT),
                FetchCustomerAddresses.forApplId(applId),
                SelectFromCombobox.value(address)
                        .from(AddressPage.ADDRESS_COMBO)
                        .andSelectFirstResult(AddressPage.FIRST_ADDRESS_RESULT),
                FetchSelectedCustomerAddress.forAddress(applId, address),
                SelectFromCombobox.value(fcId)
                        .from(AddressPage.FC_COMBO)
                        .andSelectFirstResult(AddressPage.FIRST_FC_RESULT)
        );
    }
}
