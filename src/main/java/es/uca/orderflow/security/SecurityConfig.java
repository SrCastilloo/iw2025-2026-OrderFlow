// src/main/java/es/uca/orderflow/security/SecurityConfig.java
package es.uca.orderflow.security;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import es.uca.orderflow.presentation.views.LoginView; // ajusta si tu clase se llama distinto
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig extends VaadinWebSecurity {

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        // 1) Reglas propias (DEBEN ir antes de super.configure)
        http.csrf(csrf -> csrf.ignoringRequestMatchers(
                new AntPathRequestMatcher("/api/pedidos/**")
        ));

        http.authorizeHttpRequests(auth -> auth
                // Factura: requiere sesión (cámbialo a .permitAll() si la quieres pública)
                .requestMatchers(HttpMethod.GET, "/api/pedidos/*/factura.pdf").permitAll() //solo de momento, para ver si funciona

                // Recursos estáticos típicos
                .requestMatchers("/images/**", "/favicon.ico").permitAll()
        );

        // 2) Config por defecto de Vaadin (cierra la cadena con anyRequest)
        super.configure(http);

        // 3) Vista de login Vaadin
        setLoginView(http, LoginView.class);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
