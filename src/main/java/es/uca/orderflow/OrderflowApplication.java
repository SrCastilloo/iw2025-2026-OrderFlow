package es.uca.orderflow;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

import es.uca.orderflow.business.entities.Cliente;
import es.uca.orderflow.business.services.IdentificarCliente;
import es.uca.orderflow.business.services.RegistrarCliente;

@SpringBootApplication
public class OrderflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderflowApplication.class, args);
	}


	@Bean //para probar el registro y el login
	@Order(1) //para q se ejecute primero
	CommandLineRunner testRegistroLogin(RegistrarCliente registrar, IdentificarCliente identificar) {
		return args -> {
			System.out.println(">>> TestRegistroLogin RUN empieza");

			Cliente c = new Cliente();
			c.setNombre("Test");
			c.setApellidos("User");
			// evita colisión con UNIQUE de correo
			c.setCorreo("test+" + System.currentTimeMillis() + "@demo.com");
			c.setContrasena("123456");

			try {
				registrar.registroCliente(c);
				Cliente encontrado = identificar.identificaCliente(c.getCorreo(), "123456");
				System.out.println(" Cliente autenticado: " + encontrado.getNombre());
			} catch (Exception e) {
				System.err.println(" Error: " + e.getMessage());
			}
		};
	}

}
