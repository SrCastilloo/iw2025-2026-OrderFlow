package es.uca.orderflow;


import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import es.uca.orderflow.business.services.DemoScriptService;
import org.springframework.core.annotation.Order; 

@SpringBootApplication
public class OrderflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderflowApplication.class, args);
	}

	@Bean
	@Order(1)
	CommandLineRunner runDemo(DemoScriptService demoScriptService) {
		return args -> {
			System.out.println(">>> TestRegistroLogin RUN empieza");
			demoScriptService.runDemo(); 
			System.out.println(">>> TestRegistroLogin RUN termina");
		};
	}

}
