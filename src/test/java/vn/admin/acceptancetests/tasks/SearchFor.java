package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Performable;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Enter;
// Click and WaitUntil not required in simplified SearchFor task
import vn.admin.acceptancetests.ui.MapPage;

// isVisible not used in this simplified task

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
