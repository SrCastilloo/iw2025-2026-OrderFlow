// src/main/java/es/uca/orderflow/config/StaticResourceConfig.java
package es.uca.orderflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        Path productsDir = Paths.get("frontend-resources", "products").toAbsolutePath().normalize();
        registry.addResourceHandler("/product-photos/**")
                .addResourceLocations("file:" + productsDir + "/")
                .setCachePeriod(0);

        Path logosDir = Paths.get("frontend-resources", "company-logos").toAbsolutePath().normalize();
        registry.addResourceHandler("/company-logos/**")
                .addResourceLocations("file:" + logosDir + "/")
                .setCachePeriod(0);

        System.out.println("CONFIG: Product photos -> file:" + productsDir + "/");
        System.out.println("CONFIG: Company logos  -> file:" + logosDir + "/");
    }
}
