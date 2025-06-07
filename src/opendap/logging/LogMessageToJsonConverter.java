package opendap.logging;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.gson.Gson;

public class LogMessageToJsonConverter extends ClassicConverter {
    @Override
    public String convert(ILoggingEvent event) {
        Gson gson = new Gson();
        String jsonStr = gson.toJson(event.getFormattedMessage());
        if(jsonStr.startsWith("\"") && jsonStr.length()>1)
            jsonStr=jsonStr.substring(1);

        if(jsonStr.endsWith("\"") && jsonStr.length()>1 && !jsonStr.endsWith("\\\""))
            jsonStr=jsonStr.substring(0,jsonStr.length()-1);

        return jsonStr;
     }
}

