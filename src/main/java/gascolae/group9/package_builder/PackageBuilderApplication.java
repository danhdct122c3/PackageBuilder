package gascolae.group9.package_builder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class PackageBuilderApplication {

	public static void main(String[] args) {
		SpringApplication.run(PackageBuilderApplication.class, args);
	}

}
