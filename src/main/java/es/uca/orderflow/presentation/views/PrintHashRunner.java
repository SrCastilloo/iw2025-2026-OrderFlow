package es.uca.orderflow.presentation.views;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class PrintHashRunner implements CommandLineRunner {

    private final PasswordEncoder encoder;
    private final Environment env;

    public PrintHashRunner(PasswordEncoder encoder, Environment env) {
        this.encoder = encoder;
        this.env = env;
    }

    @Override
    public void run(String... args) {
        // Seguridad: solo permitir en perfil "dev"
        if (!isDevProfileActive()) {
            return;
        }


        String raw = firstNonBlank(
                System.getenv("ORDERFLOW_HASH_PASSWORD"),
                env.getProperty("orderflow.hash.password")
        );

        if (raw == null) {
            // No hacemos nada si no se proporciona una contraseña explícitamente
            return;
        }

        System.out.println("BCrypt(password) = " + encoder.encode(raw));
    }

    private boolean isDevProfileActive() {
        return Arrays.stream(env.getActiveProfiles())
                .anyMatch(p -> "dev".equalsIgnoreCase(p));
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
