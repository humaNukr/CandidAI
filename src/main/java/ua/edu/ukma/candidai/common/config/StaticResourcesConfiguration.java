package ua.edu.ukma.candidai.common.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.EncodedResourceResolver;
import org.springframework.web.servlet.resource.VersionResourceResolver;

import java.util.concurrent.TimeUnit;

/**
 * Static resource handling with long-term caching:
 * - Content-based versioning for CSS with pre-compressed gzip support.
 * - Long-term caching for webjars.
 * - Shallow ETag generation for REST API endpoints.
 */
@Configuration(proxyBeanMethods = false)
public class StaticResourcesConfiguration implements WebMvcConfigurer {

    private static final int CACHE_DAYS = 365;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCacheControl(CacheControl.maxAge(CACHE_DAYS, TimeUnit.DAYS).cachePublic())
                .resourceChain(true)
                .addResolver(new EncodedResourceResolver())
                .addResolver(new VersionResourceResolver().addContentVersionStrategy("/**"));

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .setCacheControl(CacheControl.maxAge(CACHE_DAYS, TimeUnit.DAYS).cachePublic());
    }

    /**
     * ETag filter applies only to /api/* JSON responses to avoid invalidating CSRF on Thymeleaf views.
     */
    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> apiEtagFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> registration
                = new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        registration.addUrlPatterns("/api/*");
        return registration;
    }
}
