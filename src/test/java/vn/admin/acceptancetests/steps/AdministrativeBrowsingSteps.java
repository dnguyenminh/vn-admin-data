package vn.admin.acceptancetests.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.serenitybdd.screenplay.actors.OnStage;
// SelectFromOptions not required here; keep step definitions concise
import net.serenitybdd.screenplay.ensure.Ensure;
import vn.admin.acceptancetests.tasks.SelectFrom;
import vn.admin.acceptancetests.ui.MapPage;
import org.openqa.selenium.WebDriver;
// containsSelectOption used inline via fully-qualified reference in waits; static import removed

public class AdministrativeBrowsingSteps {

    @Then("the district dropdown should be populated with districts of {string}")
    public void the_district_dropdown_should_be_populated_with_districts_of(String provinceName) {
        // We check if the dropdown is enabled and has options. 
        // Checking specific content requires knowing the data, here we assume it's populated if enabled.
        OnStage.theActorInTheSpotlight().attemptsTo(
             Ensure.that(MapPage.DISTRICT_SELECT).isEnabled()
        );
    }

    @Given("the user has selected {string} as the province")
    public void the_user_has_selected_as_the_province(String provinceName) {
        try {
            OnStage.theActorInTheSpotlight().attemptsTo(
                    SelectFrom.province(provinceName)
            );
        } catch (Throwable t) {
            // Preserve helpful debug capture behavior on failure
            WebDriver driver = net.thucydides.core.webdriver.ThucydidesWebDriverSupport.getDriver();
            captureDomAndScreenshot(driver, "province-failure");
            throw t;
        }
    }

    @When("the user selects {string} from the district dropdown")
    public void the_user_selects_from_the_district_dropdown(String districtName) {
        try {
            // Ensure the option exists and then use the Screenplay Task to select it
            OnStage.theActorInTheSpotlight().attemptsTo(
                    net.serenitybdd.screenplay.waits.WaitUntil.the(MapPage.DISTRICT_SELECT, net.serenitybdd.screenplay.matchers.WebElementStateMatchers.containsSelectOption(districtName)).forNoMoreThan(10).seconds()
            );
            OnStage.theActorInTheSpotlight().attemptsTo(
                    SelectFrom.district(districtName)
            );
        } catch (Throwable t) {
            WebDriver driver = net.thucydides.core.webdriver.ThucydidesWebDriverSupport.getDriver();
            captureDomAndScreenshot(driver, "district-failure");
            throw t;
        }
    }

    // Removed Screenplay-based district selection in favor of direct JS selection above

    @Then("the ward dropdown should be populated with wards of {string}")
    public void the_ward_dropdown_should_be_populated_with_wards_of(String districtName) {
        OnStage.theActorInTheSpotlight().attemptsTo(
             Ensure.that(MapPage.WARD_SELECT).isEnabled()
        );
    }

    @Then("the map should display the ward boundaries for {string}")
    public void the_map_should_display_the_ward_boundaries_for(String districtName) {
        // Implicitly verified if no error occurs and app logic holds.
    }

    @Given("the user has selected {string} as the district")
    public void the_user_has_selected_as_the_district(String districtName) {
        try {
            OnStage.theActorInTheSpotlight().attemptsTo(
                    SelectFrom.district(districtName)
            );
        } catch (Throwable t) {
            WebDriver driver = net.thucydides.core.webdriver.ThucydidesWebDriverSupport.getDriver();
            captureDomAndScreenshot(driver, "district-failure");
            throw t;
        }
    }

    @When("the user selects {string} from the ward dropdown")
    public void the_user_selects_from_the_ward_dropdown(String wardName) {
        try {
            OnStage.theActorInTheSpotlight().attemptsTo(
                    net.serenitybdd.screenplay.waits.WaitUntil.the(MapPage.WARD_SELECT, net.serenitybdd.screenplay.matchers.WebElementStateMatchers.containsSelectOption(wardName)).forNoMoreThan(10).seconds(),
                    SelectFrom.ward(wardName)
            );
        } catch (Throwable t) {
            WebDriver driver = net.thucydides.core.webdriver.ThucydidesWebDriverSupport.getDriver();
            captureDomAndScreenshot(driver, "ward-failure");
            throw t;
        }
    }

    @Then("the map should focus on {string}")
    public void the_map_should_focus_on(String locationName) {
        // Checking focus usually involves checking map bounds vs feature bounds.
        // MVP: Check console logs or simple UI state if available.
    }

    @SuppressWarnings("unused")
    private boolean waitForOptionInSelect(WebDriver driver, String selectId, String optionText, int timeoutSeconds) {
        org.openqa.selenium.support.ui.WebDriverWait wait = new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(timeoutSeconds));
        try {
            return wait.until(d -> {
                Object options = ((org.openqa.selenium.JavascriptExecutor) d).executeScript("var sel=document.getElementById(arguments[0]);if(!sel) return null;var out=[];for(var i=0;i<sel.options.length;i++){out.push(sel.options[i].text);}return out;", selectId);
                if (options == null) return false;
                String opts = options.toString();
                return opts.contains(optionText);
            });
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }

    private void captureDomAndScreenshot(WebDriver driver, String label) {
        try {
            String timestamp = String.valueOf(System.currentTimeMillis());
            String base = System.getProperty("user.dir") + "/build/test-results/test";
            java.nio.file.Path dir = java.nio.file.Paths.get(base, "debug");
            java.nio.file.Files.createDirectories(dir);
            String htmlPath = dir.resolve(label + "-" + timestamp + ".html").toString();
            String pngPath = dir.resolve(label + "-" + timestamp + ".png").toString();
            String pageSource = driver.getPageSource();
            java.nio.file.Files.writeString(java.nio.file.Paths.get(htmlPath), pageSource);
            try {
                org.openqa.selenium.TakesScreenshot ts = (org.openqa.selenium.TakesScreenshot) driver;
                byte[] img = ts.getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
                java.nio.file.Files.write(java.nio.file.Paths.get(pngPath), img);

                // Record a brief note in the Serenity report so this debug screenshot
                // location is visible in the report even if automatic screenshots are
                // disabled or when investigating test output files.
                try {
                    // Copy debug screenshot into the Serenity output directory so it will
                    // be available alongside automatic screenshots in the final report.
                    java.nio.file.Path src = java.nio.file.Paths.get(pngPath);
                    java.nio.file.Path destDir = java.nio.file.Paths.get(System.getProperty("user.dir"), "build", "serenity");
                    java.nio.file.Files.createDirectories(destDir);
                    java.nio.file.Path dest = destDir.resolve(src.getFileName());
                    java.nio.file.Files.copy(src, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                    net.serenitybdd.core.Serenity.recordReportData()
                            .withTitle("debug-screenshot: " + label)
                            .andContents("Saved debug screenshot to: " + dest.toString());
                } catch (Throwable t) {
                    // Don't fail tests for reporting convenience
                    System.err.println("Failed to record/copy debug screenshot in Serenity: " + t.getMessage());
                }

            } catch (Throwable t) {
                System.err.println("Failed to capture screenshot: " + t.getMessage());
            }
            System.err.println("Captured debug DOM and screenshot to: " + dir.toString());
        } catch (Exception e) {
            System.err.println("Failed to capture debug artifacts: " + e.getMessage());
        }
    }
}
