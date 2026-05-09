package edu.kalum.enrollmentmanagement.core;

import edu.kalum.enrollmentmanagement.core.infraestructure.verticles.ClientEnrollmentVerticle;
import edu.kalum.enrollmentmanagement.core.infraestructure.verticles.ReadMessageVerticle;
import io.vertx.core.Vertx;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WorkerEnrollmentManagementApplication implements CommandLineRunner {

	private Vertx vertx;
	@Autowired
	private ReadMessageVerticle readMessageVerticle;
	@Autowired
	private ClientEnrollmentVerticle clientEnrollmentVerticle;

	public static void main(String[] args) {
		SpringApplication.run(WorkerEnrollmentManagementApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

	}

	@PostConstruct
	public void deploymentVerticles() {
		this.vertx = Vertx.vertx();
		this.vertx.deployVerticle(readMessageVerticle);
		this.vertx.deployVerticle(clientEnrollmentVerticle);
	}
}
