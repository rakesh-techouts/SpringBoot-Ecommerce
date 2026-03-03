package com.techouts.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF disabled because authentication is handled via custom HttpSession logic
            .csrf(csrf -> csrf.disable())
            // Public endpoints only; everything else requires logged-in session
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(
                    "/", "/index",
                    "/login", "/register",
                    "/product/**",
                    "/cart/add", "/cart/count",
                    "/uploads/**",
                    "/css/**", "/js/**", "/images/**", "/webjars/**",
                    "/error", "/favicon.ico"
                ).permitAll()
                .requestMatchers("/admin/**").access(this::sessionAdminAuthorized)
                .anyRequest().access(this::sessionUserAuthorized)
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request,
                                           response,
                                           authException) -> response.sendRedirect("/login"))
            )
            // Disable all default security features
            .formLogin(form -> form.disable())
            .logout(logout -> logout.disable())
            .httpBasic(httpBasic -> httpBasic.disable());

        return http.build();
    }

    private AuthorizationDecision sessionUserAuthorized(
            java.util.function.Supplier<org.springframework.security.core.Authentication> authentication,
            RequestAuthorizationContext context
    ) {
        HttpSession session = context.getRequest().getSession(false);
        boolean loggedIn = session != null && session.getAttribute("USER_ID") instanceof Long;
        return new AuthorizationDecision(loggedIn);
    }

    private AuthorizationDecision sessionAdminAuthorized(
            java.util.function.Supplier<org.springframework.security.core.Authentication> authentication,
            RequestAuthorizationContext context
    ) {
        HttpSession session = context.getRequest().getSession(false);
        boolean loggedIn = session != null && session.getAttribute("USER_ID") instanceof Long;
        boolean isAdmin = session != null && Boolean.TRUE.equals(session.getAttribute("IS_ADMIN"));
        return new AuthorizationDecision(loggedIn && isAdmin);
    }
}
