package ai.quantumsense.tgmonitor.ipc.payload;

import ai.quantumsense.tgmonitor.ipc.requests.RequestName;

import java.util.Set;

public class Request {

    private RequestName name;
    private String stringArg;
    private Set<String> setArg;

    public Request(RequestName name) {
        this.name = name;
    }

    public Request(RequestName name, String stringArg) {
        this.name = name;
        this.stringArg = stringArg;
    }

    public Request(RequestName name, Set<String> setArg) {
        this.name = name;
        this.setArg = setArg;
    }

    public RequestName getName() {
        return name;
    }

    public String getStringArg() {
        return stringArg;
    }

    public Set<String> getSetArg() {
        return setArg;
    }

    public boolean isNoArgRequest() {
        return stringArg == null && setArg == null;
    }

    public boolean isStringRequest() {
        return stringArg != null;
    }

    public boolean isSetRequest() {
        return setArg != null;
    }

    @Override
    public boolean equals(Object request) {
        Request other = (Request) request;
        return hasSameName(this, other) && hasSameStringArg(this, other) && hasSameSetArg(this, other);
    }
    private boolean hasSameName(Request req1, Request req2) {
        return req1.getName() == req2.getName();
    }
    private boolean hasSameStringArg(Request req1, Request req2) {
        return (req1.getStringArg() == null && req2.getStringArg() == null) || req1.getStringArg().equals(req2.getStringArg());
    }
    private boolean hasSameSetArg(Request req1, Request req2) {
        return (req1.getSetArg() == null && req2.getSetArg() == null) || req1.getSetArg().equals(req2.getSetArg());
    }

    @Override
    public String toString() {
        String s = "<" + name;
        if (isStringRequest()) s += " " + stringArg;
        else if (isSetRequest()) s += " " + setArg;
        return s + ">";
    }

}
