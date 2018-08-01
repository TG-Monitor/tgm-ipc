package ai.quantumsense.tgmonitor.ipc;

import ai.quantumsense.tgmonitor.corefacade.CoreFacade;
import ai.quantumsense.tgmonitor.ipc.payload.Request;
import ai.quantumsense.tgmonitor.ipc.payload.Response;
import ai.quantumsense.tgmonitor.logincodeprompt.LoginCodePrompt;

import java.util.Set;

import static ai.quantumsense.tgmonitor.ipc.requests.RequestName.*;

public class UiEndpoint implements CoreFacade {

    private UiMessenger messenger;

    public UiEndpoint(UiMessenger messenger) {
        this.messenger = messenger;
    }

    @Override
    public void login(String phoneNumber, LoginCodePrompt loginCodePrompt) {
        messenger.loginRequest(new Request(LOGIN), loginCodePrompt);
    }

    @Override
    public void logout() {
        messenger.request(new Request(LOGOUT));
    }

    @Override
    public boolean isLoggedIn() {
        Response response = messenger.request(new Request(IS_LOGGED_IN));
        return response.getBool();
    }

    @Override
    public void start() {
        messenger.request(new Request(START));
    }

    @Override
    public void stop() {
        messenger.request(new Request(STOP));
    }

    @Override
    public boolean isRunning() {
        Response response = messenger.request(new Request(IS_RUNNING));
        return response.getBool();
    }

    @Override
    public String getPhoneNumber() {
        Response response = messenger.request(new Request(GET_PHONE_NUMBER));
        return response.getString();
    }

    @Override
    public Set<String> getPeers() {
        Response response = messenger.request(new Request(GET_PEERS));
        return response.getSet();
    }

    @Override
    public void setPeers(Set<String> peers) {
        messenger.request(new Request(SET_PEERS, peers));
    }

    @Override
    public void addPeer(String peer) {
        messenger.request(new Request(ADD_PEER, peer));
    }

    @Override
    public void addPeers(Set<String> peers) {
        messenger.request(new Request(ADD_PEERS, peers));
    }

    @Override
    public void removePeer(String peer) {
        messenger.request(new Request(REMOVE_PEER, peer));
    }

    @Override
    public void removePeers(Set<String> peers) {
        messenger.request(new Request(REMOVE_PEERS, peers));
    }

    @Override
    public Set<String> getPatterns() {
        Response response = messenger.request(new Request(GET_PATTERNS));
        return response.getSet();
    }

    @Override
    public void setPatterns(Set<String> patterns) {
        messenger.request(new Request(SET_PATTERNS, patterns));
    }

    @Override
    public void addPattern(String pattern) {
        messenger.request(new Request(ADD_PATTERN, pattern));
    }

    @Override
    public void addPatterns(Set<String> patterns) {
        messenger.request(new Request(ADD_PATTERNS, patterns));
    }

    @Override
    public void removePattern(String pattern) {
        messenger.request(new Request(REMOVE_PATTERN, pattern));
    }

    @Override
    public void removePatterns(Set<String> patterns) {
        messenger.request(new Request(REMOVE_PATTERNS, patterns));
    }

    @Override
    public Set<String> getEmails() {
        Response response = messenger.request(new Request(GET_EMAILS));
        return response.getSet();
    }

    @Override
    public void setEmails(Set<String> emails) {
        messenger.request(new Request(SET_EMAILS, emails));
    }

    @Override
    public void addEmail(String email) {
        messenger.request(new Request(ADD_EMAIL, email));
    }

    @Override
    public void addEmails(Set<String> emails) {
        messenger.request(new Request(ADD_EMAILS, emails));
    }

    @Override
    public void removeEmail(String email) {
        messenger.request(new Request(REMOVE_EMAIL, email));
    }

    @Override
    public void removeEmails(Set<String> emails) {
        messenger.request(new Request(REMOVE_EMAILS, emails));
    }
}
