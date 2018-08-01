package ai.quantumsense.tgmonitor.ipc.messenger.serializer;

import ai.quantumsense.tgmonitor.ipc.payload.Request;
import ai.quantumsense.tgmonitor.ipc.payload.Response;
import ai.quantumsense.tgmonitor.ipc.messenger.Serializer;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;

public class JsonSerializer implements Serializer {

    private Gson gson = new Gson();

    @Override
    public byte[] serialize(Request request) {
        return str2bytes(gson.toJson(request));
    }

    @Override
    public byte[] serialize(Response response) {
        return str2bytes(gson.toJson(response));
    }

    @Override
    public Request deserializeRequest(byte[] request) {
        return gson.fromJson(bytes2str(request), Request.class);
    }

    @Override
    public Response deserializeResponse(byte[] response) {
        return gson.fromJson(bytes2str(response), Response.class);
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
