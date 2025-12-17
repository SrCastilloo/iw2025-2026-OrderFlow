// src/main/java/es/uca/orderflow/security/SecurityConfig.java
package es.uca.orderflow.security;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import es.uca.orderflow.presentation.views.LoginView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends VaadinWebSecurity {

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.ignoringRequestMatchers(
                new AntPathRequestMatcher("/api/pedidos/**")
        ));

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/product-photos/**",
                        "/company-logos/**",
                        "/images/**",
                        "/favicon.ico"
                ).permitAll()
                // Solo si mantienes la ruta de prueba:
                .requestMatchers("/__static__/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/pedidos/*/factura.pdf").permitAll()
        );

        super.configure(http);

        setLoginView(http, LoginView.class);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                "/product-photos/**",
                "/company-logos/**",
                "/images/**",
                "/__static__/**",
                "/frontend-resources/**",
                "/favicon.ico"
        );
    }
}
