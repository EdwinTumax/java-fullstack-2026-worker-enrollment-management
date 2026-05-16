package edu.kalum.enrollmentmanagement.core.infraestructure.verticles;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.rabbitmq.QueueOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ReadMessageVerticle extends AbstractVerticle {

    private final Logger logger = LoggerFactory.getLogger(ReadMessageVerticle.class);
    private final String BUS_EVENT_ENROLLMENT_MANAGEMENT = "BUS_EVENT_ENROLLMENT_MANAGEMENT";
    private EventBus eventBus;

    private RabbitMQClient rabbitMQClient;

    @Override
    public void start() {
        this.eventBus = vertx.eventBus();
        vertx.setTimer(5000, startHandler -> {
           readMessage();
        });

    }

    public void readMessage() {
        RabbitMQOptions config = new RabbitMQOptions();
        config.setUser(config().getJsonObject("rabbit").getString("user"));
        config.setPassword(config().getJsonObject("rabbit").getString("password"));
        config.setHost(config().getJsonObject("rabbit").getString("host"));
        config.setPort(config().getJsonObject("rabbit").getInteger("port"));
        config.setVirtualHost(config().getJsonObject("rabbit").getString("virtualHost"));
        config.setAutomaticRecoveryEnabled(true);
        this.rabbitMQClient = RabbitMQClient.create(this.vertx, config);
        this.rabbitMQClient.start().onComplete(startHandler -> {
            if(startHandler.succeeded()) {
                logger.info("Conexion exitosa a Rabbit");
                this.rabbitMQClient.basicConsumer(config().getJsonObject("rabbit").getString("queue"),new QueueOptions().setAutoAck(false)).onSuccess(consummer -> {
                    consummer.handler(message -> {
                        this.eventBus.request(this.BUS_EVENT_ENROLLMENT_MANAGEMENT,message.body().toJson()).onSuccess(handlerMessage -> {
                            logger.info(handlerMessage.body().toString());
                            this.rabbitMQClient.basicAck(message.envelope().getDeliveryTag(),false);
                        }).onFailure(error -> {
                            logger.error(error.getMessage());
                            this.rabbitMQClient.basicNack(message.envelope().getDeliveryTag(),false,true);
                        });
                    });
                }).onFailure(error -> {
                    logger.error(error.getMessage());
                });
            } else {
                logger.error("Error al conectarse a rabbit");
            }
        });
    }
}
