package ai.quantumsense.tgmonitor.ipcadapter.rpc;

public interface RpcClient {
    Object call(String method, Object... args);
    void close();
}
