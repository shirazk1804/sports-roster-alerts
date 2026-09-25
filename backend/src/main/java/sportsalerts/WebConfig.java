package sportsalerts;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ApiAuthenticationInterceptor apiAuthenticationInterceptor;

    public WebConfig(
            ApiAuthenticationInterceptor apiAuthenticationInterceptor) {

        this.apiAuthenticationInterceptor = apiAuthenticationInterceptor;
    }

    @Override
    public void addInterceptors(
            InterceptorRegistry registry) {

        registry
                .addInterceptor(
                        apiAuthenticationInterceptor)
                .addPathPatterns(
                        "/api/**")
                .excludePathPatterns(
                        "/api/users/register",
                        "/api/teams");
    }

    @Override
    public void addCorsMappings(
            CorsRegistry registry) {

        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:8081",
                        "https://gentle-meerkat-daba19.netlify.app")
                .allowedMethods(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS")
                .allowedHeaders(
                        "Authorization",
                        "Content-Type",
                        "Accept")
                .allowCredentials(false)
                .maxAge(3600);
    }
}