package vn.admin.acceptancetests.questions;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import net.serenitybdd.screenplay.targets.Target;
import vn.admin.acceptancetests.ui.ComboboxControl;

public class SelectedValue implements Question<String> {

    private final Target target;

    public SelectedValue(Target target) {
        this.target = target;
    }

    public static Question<String> of(Target target) {
        return new SelectedValue(target);
    }

    @Override
    public String answeredBy(Actor actor) {
        return target.find(ComboboxControl.INPUT_TEXT).resolveFor(actor).getValue();
    }
}
