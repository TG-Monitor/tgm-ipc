package ai.quantumsense.tgmonitor.ipc;

import ai.quantumsense.tgmonitor.ipc.messenger.Serializer;
import ai.quantumsense.tgmonitor.ipc.messenger.serializer.JsonSerializer;
import ai.quantumsense.tgmonitor.ipc.payload.Request;
import ai.quantumsense.tgmonitor.ipc.requests.RequestName;
import ai.quantumsense.tgmonitor.ipc.payload.Response;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

public class SerializerTest {

    Serializer serializer = new JsonSerializer();

    @Test
    public void testNoArgRequest() {
        testRequest(new Request(RequestName.LOGOUT));
    }

    @Test
    public void testStringRequest() {
        testRequest(new Request(RequestName.ADD_PEER, "foobar"));
    }

    @Test
    public void testSetRequest() {
        testRequest(new Request(RequestName.ADD_PEERS, new HashSet<>(Arrays.asList("foobar1", "foobar2", "foobar3"))));
    }

    @Test
    public void testVoidResponse() {
        testResponse(new Response());
    }

    @Test
    public void testBoolResponse() {
        testResponse(new Response(true));
    }

    @Test
    public void testStringResponse() {
        testResponse(new Response("foobar"));
    }

    @Test
    public void testSetResponse() {
        testResponse(new Response(new HashSet<>(Arrays.asList("foobar1", "foobar2", "foobar3"))));
    }

    private void testRequest(Request original) {
        byte[] serialized = serializer.serialize(original);
        printSerialized(serialized);
        Request deserialized = serializer.deserializeRequest(serialized);
        Assert.assertEquals(original, deserialized);
    }

    private void testResponse(Response original) {
        byte[] serialized = serializer.serialize(original);
        printSerialized(serialized);
        Response deserialized = serializer.deserializeResponse(serialized);
        Assert.assertEquals(original, deserialized);
    }

    private void printSerialized(byte[] bytes) {
        System.out.println(new String(bytes));
    }
}
