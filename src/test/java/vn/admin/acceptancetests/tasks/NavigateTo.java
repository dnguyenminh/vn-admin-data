package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Performable;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Open;

public class NavigateTo {
    public static Performable theMapPage() {
        String defaultFileUrl = "file://" + System.getProperty("user.dir") + "/src/main/resources/static/index.html";
        String resolvedUrl = System.getProperty("test.url", System.getProperty("webdriver.base.url", defaultFileUrl));
        System.out.println("NavigateTo: default resolved URL -> " + resolvedUrl);

        return Task.where("{0} opens the map page",
            Task.where("open url", actor -> System.out.println("NavigateTo: opening URL -> " + resolvedUrl)),
            Open.url(resolvedUrl)
        );
    }
}
