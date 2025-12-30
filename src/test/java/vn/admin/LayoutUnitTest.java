package vn.admin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class LayoutUnitTest {

    @Test
    void map_container_should_use_percent_width_so_legend_stays_visible_when_sidebar_open() throws Exception {
        String html = Files.readString(Path.of("src/main/resources/static/index.html"));
        assertThat(html).contains("#map");
        // Ensure we set width to 100% (not 100vw) so the map respects the flex layout
        assertThat(html).contains("width: 100%");
    }

    @Test
    void map_legend_css_should_exist_in_index_html() throws Exception {
        String html = Files.readString(Path.of("src/main/resources/static/index.html"));
        assertThat(html).contains(".map-legend");
        assertThat(html).contains(".map-legend .legend-item");
    }
}
