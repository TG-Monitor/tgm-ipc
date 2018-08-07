package ai.quantumsense.tgmonitor.ipc.messenger;

import ai.quantumsense.tgmonitor.ipc.CoreMessenger;
import ai.quantumsense.tgmonitor.ipc.payload.Request;
import ai.quantumsense.tgmonitor.ipc.payload.Response;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

import static ai.quantumsense.tgmonitor.ipc.messenger.Shared.KEY_LOGIN_CODE_REQUEST_QUEUE;
import static ai.quantumsense.tgmonitor.ipc.messenger.Shared.REQUEST_QUEUE;

public class RabbitMqCoreMessenger implements CoreMessenger {

    private Logger logger = LoggerFactory.getLogger(RabbitMqCoreMessenger.class);

    private Connection connection;
    private Channel channel;
    private Serializer serializer;

    private String loginCodeRequestQueue;

    public RabbitMqCoreMessenger(Serializer serializer) {
        this.serializer = serializer;
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            logger.debug("Connecting to RabbitMQ on " + factory.getHost());
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.queueDeclare(REQUEST_QUEUE, false, false, true, null);
            logger.debug("Declared request queue \"" + REQUEST_QUEUE + "\"");
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startRequestListener(OnRequestReceivedCallback callback) {
        try {
            logger.debug("Start listening for requests on queue \"" + REQUEST_QUEUE + "\"");
            channel.basicConsume(REQUEST_QUEUE, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties requestProps, byte[] body) {
                    // If this is a login request, get the queue to use for the login code request
                    if (requestProps.getHeaders() != null && requestProps.getHeaders().containsKey(KEY_LOGIN_CODE_REQUEST_QUEUE))
                        loginCodeRequestQueue = (String) requestProps.getHeaders().get(KEY_LOGIN_CODE_REQUEST_QUEUE);
                    Request request = serializer.deserializeRequest(body);
                    logger.debug("Received request on queue \"" + REQUEST_QUEUE + "\": " + request);
                    Response response = callback.onRequestReceived(request);
                    try {
                        String responseQueue = requestProps.getReplyTo();
                        logger.debug("Sending back response on queue \"" + responseQueue + "\": " + response);
                        channel.basicPublish("",
                                responseQueue,
                                getResponseProps(requestProps.getCorrelationId()),
                                serializer.serialize(response));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Response loginCodeRequest(Request request) {
        Response response = null;
        try {
            // Send request
            String replyToQueue = createAutoNamedQueue();
            AMQP.BasicProperties requestProps = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(createCorrelationId())
                    .replyTo(replyToQueue)
                    .build();
            logger.debug("Sending login code request ");
            channel.basicPublish("", loginCodeRequestQueue, requestProps, serializer.serialize(request));
            // Create response listener
            logger.debug("Start listening for response to login code request on queue \"" + replyToQueue + "\"");
            final BlockingQueue<byte[]> wait = new ArrayBlockingQueue<>(1);
            channel.basicConsume(replyToQueue, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties responseProps, byte[] body) {
                    if (responseProps.getCorrelationId().equals(requestProps.getCorrelationId()))
                        wait.offer(body);
                    try {
                        channel.basicCancel(consumerTag);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            // Wait for response
            response = serializer.deserializeResponse(wait.take());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        logger.debug("Received response to login code request: " + response);
        return response;
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private AMQP.BasicProperties getResponseProps(String correlationId) {
        return new AMQP.BasicProperties.Builder()
                .correlationId(correlationId)
                .build();
    }

    private String createAutoNamedQueue() {
        String name = null;
        try {
            name = channel.queueDeclare().getQueue();
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.debug("Created auto-named queue: " + name);
        return name;
    }

    private String createCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
