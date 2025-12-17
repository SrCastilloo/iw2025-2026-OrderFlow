// es.uca.orderflow.config.JpaAuditingConfig.java
package es.uca.orderflow.config;

import java.util.Optional;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import es.uca.orderflow.business.entities.Duenno;
import es.uca.orderflow.business.services.DuennoSesionService;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    AuditorAware<String> auditorAware(DuennoSesionService duennoSesionService) {
        return () -> {
            Duenno d = duennoSesionService.getActual();
            return Optional.ofNullable(d == null ? null : d.getCorreo()); // o d.getId().toString()
        };
    }
}
