package opendap.logging;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.gson.Gson;

public class LogMessageToJsonConverter extends ClassicConverter {

    /**
     *
     * @param event The logging event in question.
     * @return The event's message string encoded as a JSON string. NOTE: The returned string value is always enclosed
     * in double quote '"' characters.
     */
    @Override
    public String convert(ILoggingEvent event) {
        Gson gson = new Gson();
        String jsonStr = gson.toJson(event.getFormattedMessage(), String.class);
        return jsonStr;
     }
}

