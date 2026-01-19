package com.uop.qrvehicle.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration
 * Configures view controllers and static resource handling
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.certificate-path}")
    private String certificatePath;

    @Value("${app.upload.qrcode-path}")
    private String qrcodePath;

    @Value("${app.upload.image-path}")
    private String imagePath;

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("redirect:/login");
        registry.addViewController("/error/403").setViewName("error/403");
        registry.addViewController("/error/404").setViewName("error/404");
        registry.addViewController("/error/500").setViewName("error/500");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve uploaded certificates
        registry.addResourceHandler("/uploads/certificates/**")
                .addResourceLocations("file:" + certificatePath);

        // Serve QR codes
        registry.addResourceHandler("/uploads/qrcodes/**")
                .addResourceLocations("file:" + qrcodePath);

        // Serve uploaded images
        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations("file:" + imagePath);
    }
}
