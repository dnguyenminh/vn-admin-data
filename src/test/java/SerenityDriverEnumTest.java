import org.junit.Test;
import net.thucydides.core.webdriver.SupportedWebDriver;

public class SerenityDriverEnumTest {
    @Test
    public void printSupportedDriverValues() {
        System.out.println("SupportedWebDriver values:");
        for (SupportedWebDriver v : SupportedWebDriver.values()) {
            System.out.println(" - " + v.name());
        }
    }
}
