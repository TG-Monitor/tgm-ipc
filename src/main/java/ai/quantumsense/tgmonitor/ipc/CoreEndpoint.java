package ai.quantumsense.tgmonitor.ipc;

import ai.quantumsense.tgmonitor.corefacade.CoreFacade;
import ai.quantumsense.tgmonitor.ipc.payload.Request;
import ai.quantumsense.tgmonitor.ipc.payload.Response;
import ai.quantumsense.tgmonitor.logincodeprompt.LoginCodePrompt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static ai.quantumsense.tgmonitor.ipc.requests.RequestName.*;

public class CoreEndpoint {

    private Logger logger = LoggerFactory.getLogger(CoreEndpoint.class);

    public CoreEndpoint(CoreMessenger messenger, CoreFacade coreFacade) {
        logger.debug("Creating Core Endpoint");
        messenger.startRequestListener(request -> {
            switch (request.getName()) {

                case LOGIN:
                    String phoneNumber = request.getStringArg();
//                    coreFacade.login(phoneNumber, new LoginCodePrompt() {
//                        @Override
//                        public String promptLoginCode() {
//                            Response response = messenger.loginCodeRequest(new Request(PROMPT_LOGIN_CODE));
//                            return response.getString();
//                        }
//                    });
//                    return new Response();
                    LoginCodePrompt loginCodePrompt = new LoginCodePrompt() {
                        @Override
                        public String promptLoginCode() {
                            Response response = messenger.loginCodeRequest(new Request(PROMPT_LOGIN_CODE));
                            return response.getString();
                        }
                    };
                    String loginCode = loginCodePrompt.promptLoginCode();
                    return new Response();
                case LOGOUT:
                    coreFacade.logout();
                    return new Response();
                case IS_LOGGED_IN:
                    return new Response(coreFacade.isLoggedIn());
                case GET_PHONE_NUMBER:
                    return new Response(coreFacade.getPhoneNumber());

                case START:
                    coreFacade.start();
                    return new Response();
                case STOP:
                    coreFacade.stop();
                    return new Response();
                case IS_RUNNING:
                    return new Response(coreFacade.isRunning());

                case GET_PEERS:
                    return new Response(coreFacade.getPeers());
                case SET_PEERS:
                    coreFacade.setPeers(request.getSetArg());
                    return new Response();
                case ADD_PEER:
                    coreFacade.addPeer(request.getStringArg());
                    return new Response();
                case ADD_PEERS:
                    coreFacade.addPeers(request.getSetArg());
                    return new Response();
                case REMOVE_PEER:
                    coreFacade.removePeer(request.getStringArg());
                    return new Response();
                case REMOVE_PEERS:
                    coreFacade.removePeers(request.getSetArg());
                    return new Response();

                case GET_PATTERNS:
                    return new Response(coreFacade.getPatterns());
                case SET_PATTERNS:
                    coreFacade.setPatterns(request.getSetArg());
                    return new Response();
                case ADD_PATTERN:
                    coreFacade.addPattern(request.getStringArg());
                    return new Response();
                case ADD_PATTERNS:
                    coreFacade.addPatterns(request.getSetArg());
                    return new Response();
                case REMOVE_PATTERN:
                    coreFacade.removePattern(request.getStringArg());
                    return new Response();
                case REMOVE_PATTERNS:
                    coreFacade.removePatterns(request.getSetArg());
                    return new Response();

                case GET_EMAILS:
                    return new Response(coreFacade.getEmails());
                case SET_EMAILS:
                    coreFacade.setEmails(request.getSetArg());
                    return new Response();
                case ADD_EMAIL:
                    coreFacade.addEmail(request.getStringArg());
                    return new Response();
                case ADD_EMAILS:
                    coreFacade.addEmails(request.getSetArg());
                    return new Response();
                case REMOVE_EMAIL:
                    coreFacade.removeEmail(request.getStringArg());
                    return new Response();
                case REMOVE_EMAILS:
                    coreFacade.removeEmails(request.getSetArg());
                    return new Response();
                default:
                    return null;
            }
        });
    }
}
