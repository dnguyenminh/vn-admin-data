package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import org.openqa.selenium.JavascriptExecutor;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;

public class InjectSyntheticAddress implements Task {

    private final String address;
    private final double latitude;
    private final double longitude;

    public InjectSyntheticAddress(String address, double latitude, double longitude) {
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();
        String script = String.format(
            "(function() { if (window.app && window.app.map) { window.app.map.addSyntheticAddress('%s', %f, %f); } })();",
            address, latitude, longitude
        );
        js.executeScript(script);
    }

    public static InjectSyntheticAddress at(String address, double latitude, double longitude) {
        return new InjectSyntheticAddress(address, latitude, longitude);
    }
}
