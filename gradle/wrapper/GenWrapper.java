import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.net.URL;

public class GenWrapper {
    public static void main(String[] args) throws Exception {
        String url = "https://github.com/gradle/gradle/raw/v8.5.0/gradle/wrapper/gradle-wrapper.jar";
        //Files.createDirectories(Paths.get("gradle/wrapper"));
        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, Paths.get("gradle-wrapper.jar"));
            System.out.println("Thành công! File gradle-wrapper.jar đã được tạo.");
        }
    }
}
