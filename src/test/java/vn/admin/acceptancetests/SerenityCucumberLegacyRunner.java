// Legacy JUnit4 runner to ensure Serenity Cucumber reporter emits outcome files when
// the JUnit Platform engine may not produce them. This runner is used only for
// diagnostic / reporting purposes and is executed explicitly when needed.
package vn.admin.acceptancetests;

import net.serenitybdd.cucumber.CucumberWithSerenity;
import org.junit.runner.RunWith;
import io.cucumber.junit.CucumberOptions;

@RunWith(CucumberWithSerenity.class)
@CucumberOptions(
    features = "src/test/resources/features",
    glue = "vn.admin.acceptancetests.steps",
    plugin = {
        "pretty",
        "json:build/serenity/cucumber.json",
        "net.serenitybdd.cucumber.core.plugin.SerenityReporter"
    }
)
public class SerenityCucumberLegacyRunner {
    // Intentionally empty. Runner class triggers legacy Cucumber+Serenity integration.
}
