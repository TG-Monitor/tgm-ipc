package ai.quantumsense.tgmonitor.ipc.messenger;

import ai.quantumsense.tgmonitor.ipc.UiMessenger;
import ai.quantumsense.tgmonitor.ipc.payload.Request;
import ai.quantumsense.tgmonitor.ipc.payload.Response;
import ai.quantumsense.tgmonitor.logincodeprompt.LoginCodePrompt;
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



public class RabbitMqUiMessenger implements UiMessenger {

    private final String RESPONSE_QUEUE = "response-" + UUID.randomUUID().toString();

    private Connection connection;
    private Channel channel;
    private Serializer serializer;

    public RabbitMqUiMessenger(Serializer serializer) {
        this.serializer = serializer;
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.queueDeclare(REQUEST_QUEUE, false, false, true, null);
            channel.queueDeclare(RESPONSE_QUEUE, false, false, true, null);
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Response request(Request request) {
        String correlationId = sendRequest(request);
        return waitForResponse(correlationId);
    }

    @Override
    public Response loginRequest(Request request, LoginCodePrompt loginCodePrompt) {
        String correlationId = sendRequest(request);
        handleLoginCodeRequest(loginCodePrompt);
        return waitForResponse(correlationId);
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send a request to the core process on the request queue. Associates a
     * unique correlation ID with this request and returns this ID. The
     * correlation ID is needed for waiting for the response to this request.
     *
     * @param request A request object.
     *
     * @return The correlation ID associated with this request.
     */
    private String sendRequest(Request request) {
        AMQP.BasicProperties props = getRequestProps();
        try {
            channel.basicPublish("", REQUEST_QUEUE, props, serializer.serialize(request));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return props.getCorrelationId();
    }

    /**
     * Wait for the response from the core process to a previously made request.
     * The response is waited for on this process' response queue which was
     * communicated to the core process in the request's properties.
     *
     * @param correlationId The correlation ID returned by sendRequest().
     *
     * @return The response object.
     */
    private Response waitForResponse(String correlationId) {
        final BlockingQueue<byte[]> wait = new ArrayBlockingQueue<>(1);
        try {
            channel.basicConsume(RESPONSE_QUEUE, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties responseProps, byte[] body) {
                    if (responseProps.getCorrelationId().equals(correlationId)) {
                        wait.offer(body);
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        Response response = null;
        try {
            response = serializer.deserializeResponse(wait.take());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * Create listener for login code request from the core process on the
     * dedicated login code request queue. When the request is received, this
     * method triggers the login code prompt and sends the login code back to
     * the core process on the dedicated login code response queue.
     *
     * This will allow the login procedure complete, and the response from the
     * core process to the initial login request to be sent.
     *
     * @param loginCodePrompt The login code prompt implemented by the UI.
     */
    private void handleLoginCodeRequest(LoginCodePrompt loginCodePrompt) {
        try {
            channel.queueDeclare(LOGIN_CODE_REQUEST_QUEUE, false, false, true, null);
            channel.queueDeclare(LOGIN_CODE_RESPONSE_QUEUE, false, false, true, null);
            channel.basicConsume(LOGIN_CODE_REQUEST_QUEUE, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties requestProps, byte[] body) {
                    String loginCode = loginCodePrompt.promptLoginCode();
                    Response response = new Response(loginCode);
                    AMQP.BasicProperties responseProps = getResponseProps(requestProps.getCorrelationId());
                    try {
                        channel.basicPublish("", LOGIN_CODE_RESPONSE_QUEUE, responseProps, serializer.serialize(response));
                        channel.basicCancel(consumerTag);  // Cancel consumer
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private AMQP.BasicProperties getRequestProps() {
        return new AMQP.BasicProperties.Builder()
                .correlationId(getCorrelationId())
                .replyTo(RESPONSE_QUEUE)
                .build();
    }

    private AMQP.BasicProperties getResponseProps(String correlationId) {
        return new AMQP.BasicProperties.Builder()
                .correlationId(correlationId)
                .build();
    }

    private String getCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
