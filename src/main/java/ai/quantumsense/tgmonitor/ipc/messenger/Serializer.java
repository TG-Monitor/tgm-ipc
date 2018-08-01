package ai.quantumsense.tgmonitor.ipc.messenger;

import ai.quantumsense.tgmonitor.ipc.payload.Request;
import ai.quantumsense.tgmonitor.ipc.payload.Response;

public interface Serializer {
    byte[] serialize(Request request);
    byte[] serialize(Response response);
    Request deserializeRequest(byte[] request);
    Response deserializeResponse(byte[] response);
}
