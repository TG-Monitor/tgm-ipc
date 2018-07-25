package ai.quantumsense.tgmonitor.ipcadapter.rpc;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.tools.jsonrpc.JsonRpcClient;
import com.rabbitmq.tools.jsonrpc.JsonRpcException;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitMqRpcClient implements RpcClient {

    private Connection connection;
    private JsonRpcClient rpcClient;

    public RabbitMqRpcClient(String queueName) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(queueName, false, false, true, null);
            rpcClient = new JsonRpcClient(channel, "", queueName);
        } catch (IOException | TimeoutException | JsonRpcException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object call(String method, Object... args) {
        Object response = null;
        try {
            response = rpcClient.call(method, args);
        } catch (IOException | JsonRpcException | TimeoutException e) {
            System.err.println(e.getMessage());
        }
        return response;
    }

    @Override
    public void close() {
        try {
            rpcClient.close();
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
