package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.actions.SendKeys;
import net.serenitybdd.screenplay.targets.Target;
import net.serenitybdd.screenplay.waits.WaitUntil;
import vn.admin.acceptancetests.ui.ComboboxControl;

import static net.serenitybdd.screenplay.Tasks.instrumented;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isClickable;

public class SelectFromCombobox implements Task {

    private final String value;
    private final String selectingItem;
    private final Target combobox;

    public SelectFromCombobox(String value, Target combobox, String selectingItem) {
        this.value = value;
        this.combobox = combobox;
        this.selectingItem = selectingItem;
    }

    public static SelectFromComboboxBuilder value(String value) {
        return new SelectFromComboboxBuilder(value);
    }

    public String escapeXPath(String text) {
        if (!text.contains("'")) {
            return "'" + text + "'";
        }
        if (!text.contains("\"")) {
            return "\"" + text + "\"";
        }
        return "concat('" + text.replace("'", "', \"'\", '") + "')";
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        // Ensure any previous dropdowns are closed or not obscuring the view
        Target inputText = this.combobox.find(ComboboxControl.INPUT_TEXT);
        actor.attemptsTo(
                WaitUntil.the(inputText, isClickable()).forNoMoreThan(10).seconds(),
                Enter.theValue(this.value).into(inputText)
//                Enter.theValue("").into(inputText),
//                SendKeys.of(this.value).into(inputText)
        );

        if (null != this.selectingItem && !this.selectingItem.trim().isEmpty()) {
            String safeValue = escapeXPath(this.selectingItem.trim());
            Target resultTarget = this.combobox.find(ComboboxControl.SEARCH_ITEM.of(safeValue));
            try {
                actor.attemptsTo(
                        WaitUntil.the(resultTarget, isClickable()).forNoMoreThan(20).seconds(),
                        Click.on(resultTarget)
                );
            } catch (Exception e) {
                // Retry: try focusing the input and re-attempt selection
                try {
                    actor.attemptsTo(Click.on(inputText));
                    Thread.sleep(250);
                    actor.attemptsTo(SendKeys.of(this.selectingItem).into(inputText));
                    actor.attemptsTo(Click.on(resultTarget));
                } catch (Exception ex) {
                    // Final fallback: use JS to scroll into view and click the input element directly,
                    // then enter the value and retry the selection. This helps when overlays or
                    // CSS transforms prevent WebDriver's click from working.
                    try {
                        org.openqa.selenium.WebDriver driver = net.serenitybdd.screenplay.abilities.BrowseTheWeb.as(actor).getDriver();
                        org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
                        Object el = inputText.resolveFor(actor);
                        js.executeScript("arguments[0].scrollIntoView(true); arguments[0].click();", el);
                        Thread.sleep(200);
                        actor.attemptsTo(Enter.theValue(this.value).into(inputText));
                        Thread.sleep(200);
                        actor.attemptsTo(Click.on(resultTarget));
                    } catch (Exception ex2) {
                        throw e;
                    }
                }
            }
        }


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


        public SelectFromCombobox andSelectItem(String selectingItem) {
            return instrumented(SelectFromCombobox.class, value, combobox, selectingItem);
        }

    }
}
