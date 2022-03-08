package opendap.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import opendap.coreServlet.Scrub;
import org.slf4j.LoggerFactory;

public class LogUtil {

    /**
     * https://affinity-it-security.com/how-to-prevent-log-injection/
     * @param s String to prep for log.
     * @return String ready for log.
     */
    public static String scrubEntry(String s){
        char[] disallowedChars = {'\r','\n', 0x08, '<', '>', '&', '\"', '\''} ;
        // Grind out a char by char replacement.
        for(char badChar: disallowedChars){
            s = s.replace(badChar,'_');
        }
        return s;
    }

    public static String getLogLevel(String loggerName){

        StringBuilder sb = new StringBuilder();


        if(loggerName != null){
            Logger namedLog = (Logger) LoggerFactory.getLogger(loggerName);

            Level level = namedLog.getLevel();

            String levelStr = "off";
            if(level!=null)
                levelStr = level.toString().toLowerCase();

            sb.append(levelStr);
        }

        return sb.toString();

    }

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
