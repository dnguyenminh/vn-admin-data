package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.actions.SendKeys;
import net.serenitybdd.screenplay.targets.Target;
import net.serenitybdd.screenplay.waits.WaitUntil;
import org.openqa.selenium.Keys;

import static net.serenitybdd.screenplay.Tasks.instrumented;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isPresent;

public class SelectFromCombobox implements Task {

    private final String value;
    private final Target combobox;
    private final Target firstResult;

    public SelectFromCombobox(String value, Target combobox, Target firstResult) {
        this.value = value;
        this.combobox = combobox;
        this.firstResult = firstResult;
    }

    public static SelectFromComboboxBuilder value(String value) {
        return new SelectFromComboboxBuilder(value);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                Click.on(combobox),
                Enter.theValue(value).into(combobox),
                SendKeys.of(Keys.ARROW_DOWN).into(combobox),
                WaitUntil.the(firstResult, isPresent()).forNoMoreThan(10).seconds(),
                Click.on(firstResult)
        );
    }

    public static class SelectFromComboboxBuilder {
        private final String value;

        public SelectFromComboboxBuilder(String value) {
            this.value = value;
        }

        public SelectFromComboboxTargetBuilder from(Target combobox) {
            return new SelectFromComboboxTargetBuilder(value, combobox);
        }
    }

    public static class SelectFromComboboxTargetBuilder {
        private final String value;
        private final Target combobox;

        public SelectFromComboboxTargetBuilder(String value, Target combobox) {
            this.value = value;
            this.combobox = combobox;
        }

        public SelectFromCombobox andSelectFirstResult(Target firstResult) {
            return instrumented(SelectFromCombobox.class, value, combobox, firstResult);
        }
    }
}
