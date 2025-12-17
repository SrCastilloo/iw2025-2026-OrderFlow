package es.uca.orderflow;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class OrderflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderflowApplication.class, args);
	}

	@Bean
    public boolean loginView()
    {
        return true;
    }
	//CommandLineRunner runDemo(DemoScriptService demoScriptService) {
	//	return args -> {
			//System.out.println(">>> TestRegistroLogin RUN empieza");
			//demoScriptService.runDemo();
			//System.out.println(">>> TestRegistroLogin RUN termina");
	//	};
	//}

}
