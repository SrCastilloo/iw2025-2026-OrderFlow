package es.uca.orderflow.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class contrasenaCifrada {


    //ciframos la contraseña
    @Bean
    public PasswordEncoder cifrarContrasena()
    {
        return new BCryptPasswordEncoder();
    }
    
}
