package edu.kalum.enrollmentmanagement.core.infraestructure.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ClientEnrollmentVerticle extends AbstractVerticle {
    private final Logger logger = LoggerFactory.getLogger(ClientEnrollmentVerticle.class);
    private final String BUS_EVENT_ENROLLMENT_MANAGEMENT = "BUS_EVENT_ENROLLMENT_MANAGEMENT";
    private EventBus eventBus;

    @Override
    public void start() {
        this.eventBus = vertx.eventBus();
        this.vertx.setTimer(5000, handler -> {
            sendOrder();
        });
    }

    private void sendOrder() {
        this.eventBus.consumer(this.BUS_EVENT_ENROLLMENT_MANAGEMENT, handlerMessage -> {
            logger.info("Lectura del mensaje del event bus");
            logger.info(handlerMessage.body().toString());
            handlerMessage.reply(new JsonObject().put("status","success"));
        });
    }
}
