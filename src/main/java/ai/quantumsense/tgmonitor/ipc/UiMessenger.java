package ai.quantumsense.tgmonitor.ipc;

import ai.quantumsense.tgmonitor.ipc.payload.Request;
import ai.quantumsense.tgmonitor.ipc.payload.Response;
import ai.quantumsense.tgmonitor.logincodeprompt.LoginCodePrompt;

public interface UiMessenger {
    Response request(Request request);
    Response loginRequest(Request request, LoginCodePrompt loginCodePrompt);
    void close();
}
