package vn.admin.web;

import org.n52.jackson.datatype.jts.JtsModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan("vn.admin")
@EntityScan("vn.admin.entity")          // Chỉ định nơi chứa các class Entity (Province, District...)
@EnableJpaRepositories("vn.admin.repository") // Chỉ định nơi chứa các interface Repository
public class MapApplication {

    public static void main(String[] args) {
        Logger log = LoggerFactory.getLogger(MapApplication.class);

        SpringApplication app = new SpringApplication(MapApplication.class);
        // Log after Spring's logging system is initialized so the message goes to configured appenders
        app.addListeners((org.springframework.context.ApplicationListener<org.springframework.boot.context.event.ApplicationStartedEvent>)
            event -> log.info("Starting MapApplication..."));
        // Also log on ApplicationReadyEvent to ensure the message is present in the rolling file appender
        app.addListeners((org.springframework.context.ApplicationListener<org.springframework.boot.context.event.ApplicationReadyEvent>)
            event -> log.info("Starting MapApplication..."));

        app.run(args);
    }

    /**
     * Bean này cực kỳ quan trọng khi làm Web GIS.
     * Nó giúp thư viện Jackson (dùng để xuất JSON) hiểu được các kiểu dữ liệu
     * Geometry như Polygon, Point, MultiPolygon từ thư viện JTS.
     */
    @Bean
    public JtsModule jtsModule() {
        return new JtsModule();
    }
}