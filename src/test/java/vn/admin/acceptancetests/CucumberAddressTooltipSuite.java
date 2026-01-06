package vn.admin.acceptancetests;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/address/address_tooltip.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "vn.admin.acceptancetests.steps")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "net.serenitybdd.cucumber.core.plugin.SerenityReporter,pretty,html:build/reports/cucumber/address-tooltip-report.html")
@org.springframework.boot.test.context.SpringBootTest(classes = vn.admin.web.MapApplication.class, webEnvironment = org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.context.ActiveProfiles("test")
@org.junit.jupiter.api.TestInstance(org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS)
public class CucumberAddressTooltipSuite {

    @org.springframework.boot.test.web.server.LocalServerPort
    private int port;

}
