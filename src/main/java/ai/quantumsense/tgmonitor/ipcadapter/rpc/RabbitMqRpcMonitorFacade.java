package ai.quantumsense.tgmonitor.ipcadapter.rpc;

import ai.quantumsense.tgmonitor.monitorfacade.MonitorFacade;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class RabbitMqRpcMonitorFacade implements MonitorFacade {

    RpcClient rpcClient;

    public RabbitMqRpcMonitorFacade(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    @Override
    public void login(String phoneNumber) {
        rpcClient.call("login", phoneNumber);
    }

    @Override
    public void logout() {
        rpcClient.call("logout");
    }

    @Override
    public boolean isLoggedIn() {
        return (boolean) rpcClient.call("isLoggedIn");
    }

    @Override
    public void start() {
        rpcClient.call("start");
    }

    @Override
    public void stop() {
        rpcClient.call("stop");
    }

    @Override
    public boolean isRunning() {
        return (boolean) rpcClient.call("isRunning");
    }

    @Override
    public String getPhoneNumber() {
        return (String) rpcClient.call("getPhoneNumber");
    }

    @Override
    public Set<String> getPeers() {
        ArrayList<String> l = (ArrayList<String>) rpcClient.call("getPeers");
        return new HashSet<>(l);
    }

    @Override
    public void setPeers(Set<String> set) {
        rpcClient.call("setPeers", set);
    }

    @Override
    public void addPeer(String peer) {
        rpcClient.call("addPeer", peer);
    }

    @Override
    public void addPeers(Set<String> peers) {
        rpcClient.call("addPeers", peers);
    }

    @Override
    public void removePeer(String peer) {
        rpcClient.call("removePeer", peer);
    }

    @Override
    public void removePeers(Set<String> peers) {
        rpcClient.call("removePeers", peers);
    }

    @Override
    public Set<String> getPatterns() {
        return null;
    }

    @Override
    public void setPatterns(Set<String> set) {

    }

    @Override
    public void addPattern(String s) {

    }

    @Override
    public void addPatterns(Set<String> set) {

    }

    @Override
    public void removePattern(String s) {

    }

    @Override
    public void removePatterns(Set<String> set) {

    }

    @Override
    public Set<String> getEmails() {
        return null;
    }

    @Override
    public void setEmails(Set<String> set) {

    }

    @Override
    public void addEmail(String s) {

    }

    @Override
    public void addEmails(Set<String> set) {

    }

    @Override
    public void removeEmail(String s) {

    }

    @Override
    public void removeEmails(Set<String> set) {

    }

    @Override
    public void registerLoginCodePrompt(LoginCodePromptFacade loginCodePromptFacade) {
        rpcClient.call("registerLoginCodePrompt", loginCodePromptFacade);
    }
}
