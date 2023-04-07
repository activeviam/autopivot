package com.av.autopivot.spring;


import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author ActiveViam
 */
@Configuration
public class ActivePivotWebMvcConfigurer implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/ui/env*.js").addResourceLocations("classpath:/static/activeui/");
        registry.addResourceHandler("/admin/ui/env*.js").addResourceLocations("classpath:/static/admin-ui/");

        registry.addResourceHandler("/ui/extensions*.json")
                .addResourceLocations("classpath:/static/activeui/");

        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }

}
