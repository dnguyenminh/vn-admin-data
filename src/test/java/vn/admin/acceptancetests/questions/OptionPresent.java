package vn.admin.acceptancetests.questions;

import net.serenitybdd.core.pages.WebElementFacade;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.targets.Target;
import org.openqa.selenium.By;

// explicit java.util.List not required (we use fully-qualified type for clarity)

public class OptionPresent implements Question<Boolean> {
    private final Target select;
    private final String optionText;

    private OptionPresent(Target select, String optionText) {
        this.select = select;
        this.optionText = optionText;
    }

    public static OptionPresent hasOption(Target select, String optionText) {
        return new OptionPresent(select, optionText);
    }

    @Override
    public Boolean answeredBy(Actor actor) {
        WebElementFacade element = select.resolveFor(actor);
        java.util.List<org.openqa.selenium.WebElement> options = element.findElements(By.xpath(".//option[normalize-space(text())='" + optionText + "']"));
        return !options.isEmpty();
    }
}
