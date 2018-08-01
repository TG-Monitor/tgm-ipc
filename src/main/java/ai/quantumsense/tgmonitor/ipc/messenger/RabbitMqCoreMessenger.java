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

import static ai.quantumsense.tgmonitor.ipc.messenger.Constants.REQUEST_QUEUE;
import static ai.quantumsense.tgmonitor.ipc.messenger.Constants.LOGIN_CODE_REQUEST_QUEUE;
import static ai.quantumsense.tgmonitor.ipc.messenger.Constants.LOGIN_CODE_RESPONSE_QUEUE;

public class RabbitMqCoreMessenger implements CoreMessenger {

    private Connection connection;
    private Channel channel;
    private Serializer serializer;

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
        // Send request
        AMQP.BasicProperties requestProps = getRequestProps();
        try {
            channel.basicPublish("", LOGIN_CODE_REQUEST_QUEUE, requestProps, serializer.serialize(request));
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Create response listener
        final BlockingQueue<byte[]> wait = new ArrayBlockingQueue<>(1);
        try {
            channel.basicConsume(LOGIN_CODE_RESPONSE_QUEUE, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties responseProps, byte[] body) {
                    if (responseProps.getCorrelationId().equals(requestProps.getCorrelationId())) {
                        wait.offer(body);
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Wait for response
        Response response = null;
        try {
            response = serializer.deserializeResponse(wait.take());
        } catch (InterruptedException e) {
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

    private AMQP.BasicProperties getRequestProps() {
        return new AMQP.BasicProperties.Builder()
                .correlationId(getCorrelationId())
                .build();
    }

    private String getCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
