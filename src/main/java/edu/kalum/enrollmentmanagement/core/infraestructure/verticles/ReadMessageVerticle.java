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
        config.setUser("guest");
        config.setPassword("guest");
        config.setHost("localhost");
        config.setPort(5672);
        config.setVirtualHost("/");
        config.setAutomaticRecoveryEnabled(true);
        this.rabbitMQClient = RabbitMQClient.create(this.vertx, config);
        this.rabbitMQClient.start().onComplete(startHandler -> {
            if(startHandler.succeeded()) {
                logger.info("Conexion exitosa a Rabbit");
                this.rabbitMQClient.basicConsumer("edu.kalum.queue.order",new QueueOptions().setAutoAck(false)).onSuccess(consummer -> {
                    consummer.handler(message -> {
                        logger.info("Envio del mensaje al Event Bus");
                        this.eventBus.request(this.BUS_EVENT_ENROLLMENT_MANAGEMENT,message.body().toJson()).onSuccess(handlerMessage -> {
                            System.out.print(handlerMessage.body().toString());
                        });
                       this.rabbitMQClient.basicAck(message.envelope().getDeliveryTag(),false);
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
