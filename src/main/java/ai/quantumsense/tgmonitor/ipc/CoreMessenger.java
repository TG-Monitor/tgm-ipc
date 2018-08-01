package ai.quantumsense.tgmonitor.ipc;

import ai.quantumsense.tgmonitor.ipc.payload.Request;
import ai.quantumsense.tgmonitor.ipc.payload.Response;

public interface CoreMessenger {
    void startRequestListener(OnRequestReceivedCallback callback);
    Response loginCodeRequest(Request request);
    void close();

    interface OnRequestReceivedCallback {
        Response onRequestReceived(Request request);
    }
}
