package vn.admin.acceptancetests;

import net.serenitybdd.junit5.SerenityJUnit5Extension;
import net.serenitybdd.core.Serenity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SerenityJUnit5Extension.class)
public class SerenityDiagnosticTest {

    @Test
    public void simple_sanity_test() {
        // simple no-op test to exercise Serenity JUnit5 extension
        Serenity.recordReportData().withTitle("diagnostic").andContents("hello from diagnostic test");
    }
}
