package vn.admin.acceptancetests;

import net.serenitybdd.junit5.SerenityJUnit5Extension;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.annotations.CastMember;
import net.serenitybdd.screenplay.ensure.Ensure;
import net.serenitybdd.screenplay.questions.Text;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import vn.admin.acceptancetests.tasks.NavigateTo;
import vn.admin.acceptancetests.tasks.SearchFor;
import vn.admin.acceptancetests.ui.MapPage;

@ExtendWith(SerenityJUnit5Extension.class)
class MapSearchTestSuite {

    @CastMember(name = "User")
    Actor user;

    @Test
    void search_for_province_should_auto_select_it() {
        user.attemptsTo(
                NavigateTo.theMapPage(),
                Ensure.that(MapPage.MAP_CONTAINER).isDisplayed(),
                
                // Search for a known location (assuming seeded data or mock)
                // Using "Hà Nội" as a likely example
                SearchFor.location("Hà Nội"),
                
                // Verify the search input is updated (logic in index.html updates it on select)
                Ensure.that(MapPage.SEARCH_INPUT).value().contains("Hà Nội")
        );
    }

    @Test
    void map_should_load_successfully() {
        user.attemptsTo(
                NavigateTo.theMapPage(),
                Ensure.that(MapPage.MAP_CONTAINER).isDisplayed(),
                Ensure.that(MapPage.PROVINCE_SELECT).isDisplayed()
        );
    }
}
