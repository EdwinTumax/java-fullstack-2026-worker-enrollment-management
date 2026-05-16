package edu.kalum.enrollmentmanagement.core;

import edu.kalum.enrollmentmanagement.core.infraestructure.verticles.ClientEnrollmentVerticle;
import edu.kalum.enrollmentmanagement.core.infraestructure.verticles.ReadMessageVerticle;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.config.spi.ConfigStore;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class WorkerEnrollmentManagementApplication implements CommandLineRunner {

	private Vertx vertx;
	@Autowired
	private ReadMessageVerticle readMessageVerticle;
	@Autowired
	private ClientEnrollmentVerticle clientEnrollmentVerticle;
	@Autowired
	private Environment env;

	public static void main(String[] args) {
		SpringApplication.run(WorkerEnrollmentManagementApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

	}

	@PostConstruct
	public void deploymentVerticles() {
		this.vertx = Vertx.vertx();
		String configEnv = env.getProperty("SPRING_PROFILES_ACTIVE") != null ? env.getProperty("SPRING_PROFILES_ACTIVE") : "dev";
		ConfigStoreOptions configStoreOptionsEnv = new ConfigStoreOptions().setType("file").setConfig(new JsonObject().put("path",configEnv.concat(".json")));
		ConfigStoreOptions configStoreOptionsSys = new ConfigStoreOptions().setType("sys");
		ConfigRetrieverOptions configRetrieverOptions = new ConfigRetrieverOptions().addStore(configStoreOptionsEnv).addStore(configStoreOptionsSys);
		ConfigRetriever configRetriever = ConfigRetriever.create(vertx,configRetrieverOptions);
		configRetriever.getConfig().onSuccess(config -> {
			this.vertx.deployVerticle(readMessageVerticle, new DeploymentOptions().setConfig(config));
			this.vertx.deployVerticle(clientEnrollmentVerticle, new DeploymentOptions().setConfig(config));
		}).onFailure(error -> {
			System.out.println("Error al cargar la configuracion del worker");
			error.printStackTrace();
		});
	}
}
