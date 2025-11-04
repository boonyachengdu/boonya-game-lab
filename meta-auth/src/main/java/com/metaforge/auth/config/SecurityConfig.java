package com.metaforge.auth.config;

import com.metaforge.auth.service.DbUserDetailsService;
import com.metaforge.auth.utils.RoleTyeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final DbUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/", "/home", "/login", "/register", "/forgot-password",
                                "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        .requestMatchers("/api/public/**", "/api/login", "/api/test/**").permitAll()
                        .requestMatchers("/mcp.html", "/api/rag/**").authenticated()
                        .requestMatchers("/chat", "/chat/**", "/documents").authenticated()
                        .requestMatchers("/admin/**").hasRole(RoleTyeUtils.ROLE_ADMIN)
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login") // 处理登录表单提交的URL
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error=true")
                        .successHandler(authenticationSuccessHandler())
                        .failureHandler(authenticationFailureHandler())
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                        .expiredUrl("/login?expired=true")
                )
                .authenticationProvider(authenticationProvider())
                .userDetailsService(userDetailsService)
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        authProvider.setHideUserNotFoundExceptions(false); // 显示用户不存在异常
        log.debug("AuthenticationProvider configured with PasswordEncoder: {}", passwordEncoder().getClass().getSimpleName());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            HttpSecurity http,
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) throws Exception {

        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);

        authenticationManagerBuilder
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder);

        return authenticationManagerBuilder.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            String username = authentication.getName();
            log.info("User logged in successfully: {}", username);

            // 可以在这里记录登录成功日志、更新最后登录时间等
            response.sendRedirect("/");
        };
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            String username = request.getParameter("username");
            log.warn("Login failed for user: {}, reason: {}", username, exception.getMessage());

            // 可以在这里记录登录失败、增加失败计数等
            response.sendRedirect("/login?error=true");
        };
    }
}