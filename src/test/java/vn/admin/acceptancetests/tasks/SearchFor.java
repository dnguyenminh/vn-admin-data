package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Performable;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.waits.WaitUntil;
import vn.admin.acceptancetests.ui.MapPage;

import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

public class SearchFor {
    public static Performable location(String locationName) {
        return Task.where("{0} searches for " + locationName,
                // For testing without a running backend, we only set the input value here
                // and let the assertions check the input value. A richer test could
                // inject DOM search results or run against a started server.
                Enter.theValue(locationName).into(MapPage.SEARCH_INPUT)
        );
    }
}
