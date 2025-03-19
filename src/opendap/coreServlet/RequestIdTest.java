package opendap.coreServlet;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.UUID;

public class RequestIdTest {

    @Test
    public void testDefaultConstructor() {
        RequestId reqId = new RequestId();
        // Verify that the id is built from the current thread's name and id.
        String expectedId = Thread.currentThread().getName() + "_" + Thread.currentThread().getId();
        assertEquals("Default constructor should set id based on thread name and id",
                expectedId, reqId.id());
        assertNotNull("UUID should not be null", reqId.uuid());

        // Verify that logId() is composed of id and uuid separated by a dash.
        String logId = reqId.logId();
        assertTrue("logId should start with the expected id prefix",
                logId.startsWith(expectedId + "-"));
        // Extract the UUID part and check for equality.
        String uuidPart = logId.substring(expectedId.length() + 1);
        assertEquals("UUID part of logId should match the generated UUID",
                reqId.uuid().toString(), uuidPart);
    }

    @Test
    public void testConstructorWithString() {
        String customId = "custom-request";
        RequestId reqId = new RequestId(customId);
        // Verify that the id is set correctly.
        assertEquals("The id should match the custom id provided",
                customId, reqId.id());
        assertNotNull("UUID should not be null", reqId.uuid());

        // Verify that logId() is composed of custom id and uuid.
        String logId = reqId.logId();
        assertTrue("logId should start with the custom id prefix",
                logId.startsWith(customId + "-"));
        String uuidPart = logId.substring(customId.length() + 1);
        assertEquals("UUID part of logId should match the generated UUID",
                reqId.uuid().toString(), uuidPart);
    }

    @Test
    public void testLogIdFormat() {
        String customId = "logTest";
        RequestId reqId = new RequestId(customId);
        String logId = reqId.logId();
        String expectedPrefix = customId + "-";
        assertTrue("logId should start with '" + expectedPrefix + "'", logId.startsWith(expectedPrefix));

        // Verify that the UUID part of the logId is valid by attempting to parse it.
        String uuidPart = logId.substring(expectedPrefix.length());
        UUID parsedUuid = UUID.fromString(uuidPart);
        assertEquals("Parsed UUID should match the object's UUID",
                reqId.uuid(), parsedUuid);
    }

    @Test
    public void testToStringFormat() {
        String customId = "toStringTest";
        RequestId reqId = new RequestId(customId);
        // Expected format: "request_id": { "id": "toStringTest", "uuid": "<uuid>"}
        String expected = "\"request_id\": { \"id\": \"" + customId +
                "\", \"uuid\": \"" + reqId.uuid().toString() + "\"}";
        assertEquals("toString() should return the expected formatted string",
                expected, reqId.toString());
    }

    @Test
    public void testUniqueUUIDsForSameId() {
        String customId = "duplicateTest";
        RequestId reqId1 = new RequestId(customId);
        RequestId reqId2 = new RequestId(customId);
        // Verify that the ids are the same, as provided.
        assertEquals("Both objects should have the same id", customId, reqId1.id());
        assertEquals("Both objects should have the same id", customId, reqId2.id());
        // Verify that the generated UUIDs are different.
        assertNotEquals("UUIDs should be unique for each RequestId instance",
                reqId1.uuid(), reqId2.uuid());
        // Consequently, logId() values must also differ.
        assertNotEquals("logId values should be unique", reqId1.logId(), reqId2.logId());
    }
}