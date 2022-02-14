package dev.vality.shumway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@ServletComponentScan
@SpringBootApplication(scanBasePackages = "dev.vality.shumway")
public class ShumwayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShumwayApplication.class, args);
    }
}
