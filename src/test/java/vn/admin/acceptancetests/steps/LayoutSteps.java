package vn.admin.acceptancetests.steps;

import io.cucumber.java.en.Then;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

public class LayoutSteps {

    @Then("the index.html should contain {string}")
    public void index_html_should_contain(String expectedContent) throws Exception {
        String html = Files.readString(Path.of("src/main/resources/static/index.html"));
        assertThat(html).contains(expectedContent);
    }
}
