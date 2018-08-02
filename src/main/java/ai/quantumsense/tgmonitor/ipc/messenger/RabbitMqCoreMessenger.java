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

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

import static ai.quantumsense.tgmonitor.ipc.messenger.Shared.LOGIN_CODE_REQUEST_QUEUE_KEY;
import static ai.quantumsense.tgmonitor.ipc.messenger.Shared.REQUEST_QUEUE;

public class RabbitMqCoreMessenger implements CoreMessenger {

    private Connection connection;
    private Channel channel;
    private Serializer serializer;

    private String loginCodeRequestQueue;

    public RabbitMqCoreMessenger(Serializer serializer) {
        this.serializer = serializer;
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.queueDeclare(REQUEST_QUEUE, false, false, true, null);
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startRequestListener(OnRequestReceivedCallback callback) {
        try {
            channel.basicConsume(REQUEST_QUEUE, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties requestProps, byte[] body) {
                    // If this is a login request, get the queue to use for the login code request
                    if (requestProps.getHeaders() != null && requestProps.getHeaders().containsKey(LOGIN_CODE_REQUEST_QUEUE_KEY))
                        loginCodeRequestQueue = (String) requestProps.getHeaders().get(LOGIN_CODE_REQUEST_QUEUE_KEY);
                    Request request = serializer.deserializeRequest(body);
                    Response response = callback.onRequestReceived(request);
                    try {
                        channel.basicPublish("",
                                requestProps.getReplyTo(),
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
            channel.basicPublish("", loginCodeRequestQueue, requestProps, serializer.serialize(request));
            // Create response listener
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
        return name;
    }

    private String createCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
