package edu.kalum.enrollmentmanagement.core.infraestructure.verticles;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class ClientEnrollmentVerticle extends AbstractVerticle {
    private final Logger logger = LoggerFactory.getLogger(ClientEnrollmentVerticle.class);
    private final String BUS_EVENT_ENROLLMENT_MANAGEMENT = "BUS_EVENT_ENROLLMENT_MANAGEMENT";
    private final String CIRCUIT_BREAKER_ENROLLMENTS = "CIRCUIT_BREAKER_ENROLLMENTS";
    private EventBus eventBus;
    private WebClient webClient;
    private CircuitBreaker circuitBreaker;

    @Override
    public void start() {
        this.eventBus = vertx.eventBus();

        this.circuitBreaker = CircuitBreaker.create(CIRCUIT_BREAKER_ENROLLMENTS,vertx,
                new CircuitBreakerOptions()
                        .setMaxFailures(5)
                        .setTimeout(5000)
                        .setFallbackOnFailure(false)
                        .setResetTimeout(10000)
        )
        .openHandler(log -> logger.error("Circuit breaker opened"))
        .halfOpenHandler(log -> logger.info("Circuit Breaker half open"))
        .closeHandler(log -> logger.info("Circuit breaker closed"));

        this.webClient = WebClient.create(vertx);
        this.vertx.setTimer(5000, handler -> {
            sendOrder();
        });
    }

    private void sendOrder() {
        this.eventBus.consumer(this.BUS_EVENT_ENROLLMENT_MANAGEMENT, handlerMessage -> {
            String jsonString = new String(Base64.getDecoder().decode(handlerMessage.body().toString()));
            JsonObject order = new JsonObject(jsonString);
            JsonObject data = order.getJsonObject("data");
            this.circuitBreaker.<JsonObject>execute(promise -> {
                this.webClient.post(9080,"localhost","/enrollment-management/v1/enrollment")
                        .putHeader("Content-Type","application/json")
                        .as(BodyCodec.buffer())
                        .sendBuffer(data.toBuffer())
                        .onSuccess(response -> {
                            if(response.bodyAsJsonObject().getInteger("statusCode") == 201) {
                                order.put("status","COMPLETED");
                                promise.complete(new JsonObject().put("status","success"));
                            } else {
                                order.put("status","FAILED");
                                order.put("errors",response.bodyAsJsonObject());
                                promise.complete(new JsonObject().put("status","failed"));
                            }
                        }).onComplete(resp -> {
                            if(resp.succeeded()) {
                                handlerMessage.reply(order);
                            } else {
                                order.put("status","SERVICE_UNAVAILABLE");
                                handlerMessage.reply(order);
                                promise.fail(new JsonObject().put("status","SERVICE_UNAVAILABLE").encode());
                            }
                        });
            }).onComplete(response -> {
                logger.info(response.toString());
            });
        });
    }
}
