package vn.admin.acceptancetests.questions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.targets.Target;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public class SelectLabel implements Question<String> {

    private final Target target;

    public SelectLabel(Target target) {
        this.target = target;
    }

    public static Question<String> of(Target target) {
        return new SelectLabel(target);
    }

    @Override
    public String answeredBy(Actor actor) {
        WebElement element = target.resolveFor(actor);
        if (element.getTagName().equalsIgnoreCase("select")) {
            try {
                Select select = new Select(element);
                return select.getFirstSelectedOption().getText();
            } catch (Exception e) {
                return "";
            }
        }
        // For non-select elements, return the visible text
        return element.getText();
    }
}
