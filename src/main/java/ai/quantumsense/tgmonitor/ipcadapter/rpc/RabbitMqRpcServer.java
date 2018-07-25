package ai.quantumsense.tgmonitor.ipcadapter.rpc;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.tools.jsonrpc.JsonRpcServer;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitMqRpcServer {

    public RabbitMqRpcServer(String queueName, Class rpcInterface, Object rpcObject) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(queueName, false, false, true, null);
            JsonRpcServer rpcServer = new JsonRpcServer(channel, queueName, rpcInterface, rpcObject);
            rpcServer.mainloop();
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}
