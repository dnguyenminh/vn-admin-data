package vn.admin.acceptancetests.ui;

import net.serenitybdd.screenplay.targets.Target;
import org.openqa.selenium.By;

public class AddressPage {
    public static final Target CUSTOMER_COMBO = Target.the("Customer combo")
            .located(By.cssSelector(".customer-combobox"));

    public static final Target ADDRESS_COMBO = Target.the("Address combo")
            .located(By.cssSelector(".address-combobox"));

    public static final Target FC_COMBO = Target.the("Field Collector combo")
            .located(By.cssSelector(".fc-combobox"));

}
