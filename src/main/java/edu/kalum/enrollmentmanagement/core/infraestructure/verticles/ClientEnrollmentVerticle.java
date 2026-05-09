package edu.kalum.enrollmentmanagement.core.infraestructure.verticles;

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
    private EventBus eventBus;
    private WebClient webClient;

    @Override
    public void start() {
        this.eventBus = vertx.eventBus();
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
            this.webClient.post(9080,"localhost","/enrollment-management/v1/enrollment")
                    .putHeader("Content-Type","application/json")
                    .as(BodyCodec.buffer())
                    .sendBuffer(data.toBuffer())
                    .onSuccess(response -> {
                        if(response.bodyAsJsonObject().getInteger("statusCode") == 201) {
                            order.put("status","COMPLETED");
                        } else {
                            order.put("status","FAILED");
                            order.put("errors",response.bodyAsJsonObject());
                        }
                    }).onComplete(resp -> {
                        if(resp.succeeded()) {
                            handlerMessage.reply(order);
                        } else {
                            order.put("status","SERVICE_UNAVAILABLE");
                            handlerMessage.reply(order);
                        }
                    });
        });
    }
}
