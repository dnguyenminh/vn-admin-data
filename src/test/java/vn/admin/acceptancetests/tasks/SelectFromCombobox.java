package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.actions.SendKeys;
import net.serenitybdd.screenplay.targets.Target;
import net.serenitybdd.screenplay.waits.WaitUntil;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

import static net.serenitybdd.screenplay.Tasks.instrumented;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isPresent;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isClickable;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isNotVisible;

public class SelectFromCombobox implements Task {

    private final String value;
    private final Target combobox;
    private final Target firstResult;
    private final boolean selectByLabel;

    public SelectFromCombobox(String value, Target combobox, Target firstResult, boolean selectByLabel) {
        this.value = value;
        this.combobox = combobox;
        this.firstResult = firstResult;
        this.selectByLabel = selectByLabel;
    }

    public static SelectFromComboboxBuilder value(String value) {
        return new SelectFromComboboxBuilder(value);
    }

    public static SelectFromComboboxBuilder label(String label) {
        return new SelectFromComboboxBuilder(label, true);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        // Ensure any previous dropdowns are closed or not obscuring the view
        actor.attemptsTo(
                WaitUntil.the(combobox, isClickable()).forNoMoreThan(10).seconds(),
                Click.on(combobox),
                Enter.theValue(value).into(combobox),
                SendKeys.of(Keys.ARROW_DOWN).into(combobox)
        );

        Target resultTarget = firstResult;
        if (selectByLabel) {
            // If selecting by label, we try to find the item with the specific text
            // Assuming the results are in a container with class 'search-item' or similar
            // This might need adjustment based on the exact DOM structure of the results
            // For now, we'll try to find an element containing the text within the results container
            // If firstResult is a specific item, we might need to look at its parent or a sibling
            
            // However, the current usage passes FIRST_CUSTOMER_RESULT which is a specific element.
            // To support label selection, we might need to construct a dynamic target.
            // Since the user asked for "SelectFromCombobox.label", we can assume they want to pick the item that matches the label.
            
            // Let's try to find the element by text content.
            // We assume the results are visible.
            resultTarget = Target.the("Result with label " + value)
                    .located(By.xpath("//div[contains(@class, 'search-item') and contains(text(), '" + value + "')]"));
        }

        actor.attemptsTo(
                WaitUntil.the(resultTarget, isPresent()).forNoMoreThan(10).seconds(),
                WaitUntil.the(resultTarget, isClickable()).forNoMoreThan(10).seconds(),
                Click.on(resultTarget),
                WaitUntil.the(resultTarget, isNotVisible()).forNoMoreThan(5).seconds()
        );
    }

    public static class SelectFromComboboxBuilder {
        private final String value;
        private final boolean selectByLabel;

        public SelectFromComboboxBuilder(String value) {
            this(value, false);
        }

        public SelectFromComboboxBuilder(String value, boolean selectByLabel) {
            this.value = value;
            this.selectByLabel = selectByLabel;
        }

        public SelectFromComboboxTargetBuilder from(Target combobox) {
            return new SelectFromComboboxTargetBuilder(value, combobox, selectByLabel);
        }
    }

    public static class SelectFromComboboxTargetBuilder {
        private final String value;
        private final Target combobox;
        private final boolean selectByLabel;

        public SelectFromComboboxTargetBuilder(String value, Target combobox, boolean selectByLabel) {
            this.value = value;
            this.combobox = combobox;
            this.selectByLabel = selectByLabel;
        }

        public SelectFromCombobox andSelectFirstResult(Target firstResult) {
            return instrumented(SelectFromCombobox.class, value, combobox, firstResult, selectByLabel);
        }
        
        // Overload to allow omitting the specific target if we are selecting by label and finding it dynamically
        // But the existing pattern requires a target. We can keep using it as a fallback or reference.
    }
}
