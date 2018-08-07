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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

import static ai.quantumsense.tgmonitor.ipc.messenger.Shared.REQUEST_QUEUE;
import static ai.quantumsense.tgmonitor.ipc.messenger.Shared.KEY_LOGIN_CODE_REQUEST_QUEUE;


public class RabbitMqUiMessenger implements UiMessenger {

    private Logger logger = LoggerFactory.getLogger(RabbitMqUiMessenger.class);

    private Connection connection;
    private Channel channel;
    private Serializer serializer;

    private String responseQueue = "responses-" + makeUuid();
    private String loginCodeRequestQueue;

    public RabbitMqUiMessenger(Serializer serializer) {
        this.serializer = serializer;
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            logger.debug("Connecting to RabbitMQ on " + factory.getHost());
            connection = factory.newConnection();
            channel = connection.createChannel();
            logger.debug("Declaring request queue \"" + REQUEST_QUEUE + "\"");
            channel.queueDeclare(REQUEST_QUEUE, false, false, false, null);
            logger.debug("Declaring response queue \"" + responseQueue + "\"");
            channel.queueDeclare(responseQueue, false, true, false, null);
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
        String correlationId = sendLoginRequest(request);
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
     * unique correlation ID with this request and returns it. The correlation
     * ID is needed to identify the correct response to this request.
     *
     * @param request A Request object.
     *
     * @return Correlation ID.
     */
    private String sendRequest(Request request) {
        String correlationId = makeUuid();
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .correlationId(correlationId)
                .replyTo(responseQueue)
                .build();
        try {
            logger.debug("Sending request " + request + " with correlation ID " + correlationId);
            channel.basicPublish("", REQUEST_QUEUE, props, serializer.serialize(request));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return correlationId;
    }

    /**
     * Like 'sendRequest' but for sending the special login request. In addition
     * to what 'sendRequest' does, this method creates an ad-hoc queue for the
     * login code request that will be made from the core back to the UI, and
     * includes the name of this queue in a message header.
     *
     * @param request A Request object containing a login request.
     *
     * @return Correlation ID.
     */
    private String sendLoginRequest(Request request) {
        String correlationId = makeUuid();
        loginCodeRequestQueue = createAutoNamedQueue();
        Map<String, Object> headers = new HashMap<>();
        headers.put(KEY_LOGIN_CODE_REQUEST_QUEUE, loginCodeRequestQueue);
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .correlationId(correlationId)
                .replyTo(responseQueue)
                .headers(headers)
                .build();
        try {
            logger.debug("Sending login request " + request + " on queue " + loginCodeRequestQueue + " with correlation ID " + correlationId);
            channel.basicPublish("", REQUEST_QUEUE, props, serializer.serialize(request));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return correlationId;
    }

    /**
     * Wait for the response for a previously made request. The response is
     * expected on the unique response queue of this process.
     *
     * @param correlationId Correlation ID returned by 'sendRequest' or 'sendLoginRequest'
     *
     * @return The core's response for the request.
     */
    private Response waitForResponse(String correlationId) {
        final BlockingQueue<byte[]> wait = new ArrayBlockingQueue<>(1);
        try {
            logger.debug("Start listening for response on queue \"" + responseQueue + "\"");
            channel.basicConsume(responseQueue, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties responseProps, byte[] body) {
                    if (responseProps.getCorrelationId().equals(correlationId)) {
                        wait.offer(body);
                        logger.debug("Received a message with matching correlation ID: " + correlationId);
                        try {
                            channel.basicCancel(consumerTag);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        logger.debug("Received a response but correlation IDs don't match: expected " + correlationId + ", but received " + responseProps.getCorrelationId());
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
        logger.debug("Received response: " + response);
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
            logger.debug("Start listening for login code request on queue \"" + loginCodeRequestQueue + "\"");
            channel.basicConsume(loginCodeRequestQueue, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties requestProps, byte[] body) {
                    logger.debug("Received login code request");
                    Response response = new Response(loginCodePrompt.promptLoginCode());
                    AMQP.BasicProperties responseProps = new AMQP.BasicProperties
                            .Builder()
                            .correlationId(requestProps.getCorrelationId())
                            .build();
                    try {
                        logger.debug("Sending back response to login code request: " + response);
                        channel.basicPublish("", requestProps.getReplyTo(), responseProps, serializer.serialize(response));
                        channel.basicCancel(consumerTag);  // Cancel this consumer
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private String makeUuid() {
        return UUID.randomUUID().toString();
    }
}