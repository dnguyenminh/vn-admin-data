package vn.admin.acceptancetests.ui;

import net.serenitybdd.screenplay.targets.Target;
import org.openqa.selenium.By;

public class AddressPage {
    public static final Target CUSTOMER_COMBO = Target.the("Customer combo")
            .located(By.id("customerCombo"));

    public static final Target CUSTOMER_RESULTS = Target.the("Customer results container")
            .located(By.id("customerResults"));

    public static final Target FIRST_CUSTOMER_RESULT = Target.the("First customer result")
            .located(By.cssSelector(".search-item:nth-child(1)"));

    public static final Target ADDRESS_COMBO = Target.the("Address combo")
            .located(By.id("addressCombo"));

    public static final Target ADDRESS_RESULTS = Target.the("Address results container")
            .located(By.id("addressResults"));

    public static final Target FIRST_ADDRESS_RESULT = Target.the("First address result")
            .located(By.cssSelector("#addressResults > .search-item"));

    public static final Target FC_COMBO = Target.the("Field Collector combo")
            .located(By.id("fcCombo"));

    public static final Target FC_RESULTS = Target.the("FC results container")
            .located(By.id("fcResults"));

    public static final Target FIRST_FC_RESULT = Target.the("First FC result")
            .located(By.cssSelector("#fcResults > .search-item"));

    public static final Target LEAFLET_TOOLTIP = Target.the("Leaflet tooltip")
            .located(By.cssSelector(".leaflet-tooltip"));
}
