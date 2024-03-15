package io.nuvalence.workmanager.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Simple web configuration to disable cors for local demo.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry
                .addViewController("/swagger-ui/")
                .setViewName("forward:/swagger-ui/index.html");
        registry
                .addRedirectViewController("/swagger-ui.html", "/swagger-ui/index.html");
    }

    /**
     * This override passes in the new ObjectMapper properties assigned below.
     *
     * @param converters refers to the spring http message converters.
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new MappingJackson2HttpMessageConverter(objectMapper()));
    }

    /**
     * This object mapper disables the serializeable feature WRITE_DATES_AS_TIMESTAMPS.
     * This ensures all object mappers correctly format datetime stamps as defined in their local env.
     */
    private ObjectMapper objectMapper() {
        return SpringConfig.getMapper();
    }
}
