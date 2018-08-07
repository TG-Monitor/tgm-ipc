package ai.quantumsense.tgmonitor.ipc.messenger.serializer;

import ai.quantumsense.tgmonitor.ipc.messenger.Serializer;
import ai.quantumsense.tgmonitor.ipc.payload.Request;
import ai.quantumsense.tgmonitor.ipc.payload.Response;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

public class JsonSerializer implements Serializer {

    private Logger logger = LoggerFactory.getLogger(JsonSerializer.class);

    private Gson gson = new Gson();

    @Override
    public byte[] serialize(Request request) {
        String json = gson.toJson(request);
        logger.debug("Serializing request to " + json);
        return str2bytes(json);
    }

    @Override
    public byte[] serialize(Response response) {
        String json = gson.toJson(response);
        logger.debug("Serializing response to " + json);
        return str2bytes(json);
    }

    @Override
    public Request deserializeRequest(byte[] request) {
        String json = bytes2str(request);
        logger.debug("Deserializing request from " + json);
        return gson.fromJson(json, Request.class);
    }

    @Override
    public Response deserializeResponse(byte[] response) {
        String json = bytes2str(response);
        logger.debug("Deserializing response from " + json);
        return gson.fromJson(json, Response.class);
    }

    private String bytes2str(byte[] bytes) {
        String str = null;
        try {
            str = new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return str;
    }

    private byte[] str2bytes(String str) {
        byte[] bytes = null;
        try {
            bytes = str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return bytes;
    }
}
