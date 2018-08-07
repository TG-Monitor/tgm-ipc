package ai.quantumsense.tgmonitor.ipc.payload;

import java.util.Set;

public class Response {
    private Boolean bool;
    private String string;
    private Set<String> set;

    public Response() {
    }

    public Response(boolean bool) {
        this.bool = bool;
    }

    public Response(String string) {
        this.string = string;
    }

    public Response(Set<String> set) {
        this.set = set;
    }

    public boolean isVoidResponse() {
        return string == null && set == null && bool == null;
    }

    public boolean isBooleanResponse() {
        return bool != null;
    }

    public boolean isStringResponse() {
        return string != null;
    }

    public boolean isSetResponse() {
        return set != null;
    }

    public Boolean getBool() {
        return bool;
    }

    public String getString() {
        return string;
    }

    public Set<String> getSet() {
        return set;
    }

    @Override
    public boolean equals(Object response) {
        Response other = (Response) response;
        return hasSameStringValue(this, other) && hasSameSetValue(this, other) && hasSameBoolValue(this, other);
    }
    private boolean hasSameBoolValue(Response res1, Response res2) {
        return (res1.getBool() == null && res2.getBool() == null) || res1.getBool().equals(res2.getBool());
    }
    private boolean hasSameStringValue(Response res1, Response res2) {
        return (res1.getString() == null && res2.getString() == null) || res1.getString().equals(res2.getString());
    }
    private boolean hasSameSetValue(Response res1, Response res2) {
        return (res1.getSet() == null && res2.getSet() == null) || res1.getSet().equals(res2.getSet());
    }

    @Override
    public String toString() {
        String s = null;
        if (isVoidResponse()) s = "<void>";
        else if (isBooleanResponse()) s = bool + " (boolean)";
        else if (isStringResponse()) s = string;
        else if (isSetResponse()) s = set.toString();
        return s;
    }
}

