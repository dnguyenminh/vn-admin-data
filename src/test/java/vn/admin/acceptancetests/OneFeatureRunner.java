package vn.admin.acceptancetests;

import net.serenitybdd.cucumber.CucumberWithSerenity;
import org.junit.runner.RunWith;
import io.cucumber.junit.CucumberOptions;

@RunWith(CucumberWithSerenity.class)
@CucumberOptions(
    features = "src/test/resources/features/map_interaction/fc_focus_prediction.feature",
    glue = "vn.admin.acceptancetests.steps",
    plugin = { "pretty", "json:build/serenity/cucumber-fc-focus.json", "net.serenitybdd.cucumber.core.plugin.SerenityReporter" }
)
public class OneFeatureRunner {
    // Intentionally empty - runs a single feature for diagnostics
}
