package vn.admin.acceptancetests;

import io.cucumber.junit.CucumberOptions;
import net.serenitybdd.cucumber.CucumberWithSerenity;
import org.junit.runner.RunWith;

@RunWith(CucumberWithSerenity.class)
@CucumberOptions(
        features = "src/test/resources/features",
        glue = "vn.admin.acceptancetests.steps",
        plugin = {"pretty"}
)
public class SerenityCucumberLegacyRunner {
    // Runs the Cucumber feature suite via Serenity's JUnit4 runner to ensure
    // the Serenity adapter writes outcome files for aggregation.
}
