package es.uca.orderflow.presentation.views;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PrintHashRunner implements CommandLineRunner {
    private final PasswordEncoder encoder;

    public PrintHashRunner(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        System.out.println("BCrypt(admin) = " + encoder.encode("admin"));
    }
}
