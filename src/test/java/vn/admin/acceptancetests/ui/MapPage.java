package vn.admin.acceptancetests.ui;

import net.serenitybdd.screenplay.targets.Target;
import org.openqa.selenium.By;

public class MapPage {
    public static final Target SEARCH_INPUT = Target.the("Search input field")
            .located(By.id("searchInput"));
    
    public static final Target SEARCH_RESULTS = Target.the("Search results container")
            .located(By.id("searchResults"));
    
    public static final Target FIRST_SEARCH_RESULT = Target.the("First search result")
            .located(By.cssSelector(".search-item:first-child"));

    public static final Target PROVINCE_SELECT = Target.the("Province dropdown")
            .located(By.id("provinceSelect"));
            
    public static final Target DISTRICT_SELECT = Target.the("District dropdown")
            .located(By.id("districtSelect"));
            
    public static final Target WARD_SELECT = Target.the("Ward dropdown")
            .located(By.id("wardSelect"));
            
    public static final Target MAP_CONTAINER = Target.the("Map container")
            .located(By.id("map"));
}