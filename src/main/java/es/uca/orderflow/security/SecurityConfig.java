// src/main/java/es/uca/orderflow/security/SecurityConfig.java
package es.uca.orderflow.security;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig extends VaadinWebSecurity {
    /*
    @Override
    protected void configure(org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
        // Deja que Vaadin configure sus paths internos
        super.configure(http);
        // Usa TU vista Vaadin como pantalla de login
        setLoginView(http, LoginView.class);
        // Con @AnonymousAllowed en RegistroView y LoginView, serán públicas
    }

    */

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
