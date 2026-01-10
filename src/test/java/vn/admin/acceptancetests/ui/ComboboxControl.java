package vn.admin.acceptancetests.ui;

import net.serenitybdd.screenplay.targets.Target;

public class ComboboxControl {
    public static final Target SEARCH_ITEM = Target.the("Item {0}")
            .locatedBy("./div[concat(' ', normalize-space(@class), ' ')=' search-results ']" +
                    "/div[contains(concat(' ', normalize-space(@class), ' '), ' search-item ') " +
                    "and text() = {0}]");
    public static final Target INPUT_TEXT = Target.the("Search input {0}")
            .locatedBy("./div/input");
}