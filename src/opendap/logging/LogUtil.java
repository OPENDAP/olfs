package opendap.logging;

import ch.qos.logback.classic.Level;
import opendap.coreServlet.Scrub;
import org.slf4j.LoggerFactory;

public class LogUtil {

    public enum logLevels {all, error, warn, info, debug, off}

    public static String setLogLevel(String loggerName, String level){

        StringBuilder sb = new StringBuilder();

        if(loggerName != null){
            ch.qos.logback.classic.Logger namedLog = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(loggerName);

            switch(logLevels.valueOf(level)){

                case all:
                    namedLog.setLevel(Level.ALL);
                    sb.append(loggerName).append(" logging level set to: ").append(logLevels.all.toString());
                    break;

                case error:
                    namedLog.setLevel(Level.ERROR);
                    sb.append(loggerName).append(" logging level set to: ").append(logLevels.error.toString());
                    break;

                case warn:
                    namedLog.setLevel(Level.WARN);
                    sb.append(loggerName).append(" logging level set to: ").append(logLevels.warn.toString());
                    break;

                case info:
                    namedLog.setLevel(Level.INFO);
                    sb.append(loggerName).append(" logging level set to: ").append(logLevels.info.toString());
                    break;

                case debug:
                    namedLog.setLevel(Level.DEBUG);
                    sb.append(loggerName).append(" logging level set to: ").append(logLevels.debug.toString());
                    break;

                case off:
                    namedLog.setLevel(Level.OFF);
                    sb.append(loggerName).append(" logging level set to: ").append(logLevels.off.toString());
                    break;

                default:
                    sb.append(loggerName).append(" ERROR! The logging level ")
                            .append(Scrub.simpleString(level))
                            .append(" is unrecognized. Nothing has been done.");
                    break;


            }
        }

        return sb.toString();

    }

}
