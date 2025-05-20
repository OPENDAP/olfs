package opendap.coreServlet;

import opendap.logging.LogUtil;
import org.apache.commons.text.StringEscapeUtils;
import org.jdom.Document;
import org.jdom.Element;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;


/**
 * BotFilter
 * This implementation of the jakarta.servlet.Filter interface can be used to
 * block access from specific IP addresses, by a range of IP addresses using
 * a regular expression, or by the value of the requests User-Agent header using
 * a regular expression.
 * Ip addresses are specifed as one or more elements:
 *   <IpAddress>127.0.0.1</IpAddress>
 * Ip match regex are specified as zero or more IpMatch elements
 *   <IpMatch>65\.55\.[012]?\d?\d\.[012]?\d?\d</IpMatch>
 * and the user agent can be blocked using a UserAgentMatch element:
 * 	 <UserAgentMatch>^.*facebookexternalhit.*$</UserAgentMatch>
 */
public class BotFilter implements Filter {

    private class ReturnValue {
        boolean blockResponse;
        String cause;
    }

    private static final java.util.concurrent.locks.Lock initLock;
    static {
        initLock = new ReentrantLock();
    }


    private FilterConfig filterConfig;
    private static final String CONFIG_PARAMETER_KEY = "config";
    private static final String DEFAULT_CONFIG_FILENAME = "olfs.xml";
    private static final String BOT_FILTER_ELEMENT_KEY = "BotFilter";
    private static final String BOT_BLOCKER_ELEMENT_KEY = "BotBlocker";
    private static final String IP_ADDRESS_ELEMENT_KEY = "IpAddress";
    private static final String IP_MATCH_ELEMENT_KEY = "IpMatch";
    private static final String USER_AGENT_MATCH_ELEMENT_KEY = "UserAgentMatch";
    private static final String ALLOWED_RESPONSE_REGEX_ELEMENT_KEY = "AllowedResponseRegex";
    private static final String BLOCKED_RESPONSE_REGEX_ELEMENT_KEY = "BlockedResponseRegex";
    private static final String BOT_FILTER_LOG_NAME = "BotFilterLog";
    private static final String BLOCK_IMAGES_AND_CSS_ELEMENT_NAME = "BlockImagesAndCss";
    private static final String imagesAndCssRegex = "\\/docs\\/(images|css)\\/.*$";

    private static org.slf4j.Logger log = null;

    private boolean initialized;
    private boolean filterBlockedResponses;
    private final HashSet<String> ipAddresses;
    private final Vector<Pattern> ipMatchPatterns;
    private final Vector<Pattern> userAgentMatchPatterns;
    private final Vector<Pattern> blockedResponsePatterns;
    private final Vector<Pattern> allowedResponsePatterns;

    public BotFilter() {
        log = org.slf4j.LoggerFactory.getLogger(BOT_FILTER_LOG_NAME);
        initialized = false;
        ipAddresses = new HashSet<>();
        ipMatchPatterns = new Vector<>();
        userAgentMatchPatterns = new Vector<>();
        allowedResponsePatterns = new Vector<>();
        blockedResponsePatterns = new Vector<>();
        filterBlockedResponses = false;
    }

    /**
     * For the Filter interface.
     * @param filterConfig
     * @throws ServletException
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        initLock.lock();
        try {
            if(initialized) {
                return;
            }
            this.filterConfig = filterConfig;
            try {
                String configFileName = filterConfig.getInitParameter(CONFIG_PARAMETER_KEY);
                if (configFileName == null) {
                    configFileName = DEFAULT_CONFIG_FILENAME;
                    String msg = "init() - The web.xml configuration for " + getClass().getName() +
                            " does not contain an init-parameter named \"" + CONFIG_PARAMETER_KEY + "\" " +
                            "Using the DEFAULT name: " + configFileName;
                    jsonWarn(msg);
                }
                Document configDoc = ServletUtil.loadConfig(configFileName, filterConfig.getServletContext());
                String contextPath = filterConfig.getServletContext().getContextPath();
                init(configDoc.getRootElement(), contextPath);
            } catch (Exception se) {
                jsonWarn("init() - INITIALIZATION HAS BEEN POSTPONED! " +
                        "FAILED TO INITIALIZE BotFilter! " +
                        "Caught " + se.getClass().getName() +
                        " Message: " + se.getMessage());
            }
        }
        finally {
            initLock.unlock();
        }
    }

    /**
     * Reads the configuration state (if any) from the XML Element.
     * @param config
     */
    private void init(Element config,String contextPath ){
        initLock.lock();
        try {
            if(initialized) {
                return;
            }
            Element botFilterConfig = config.getChild(BOT_FILTER_ELEMENT_KEY);
            if (botFilterConfig == null) {
                botFilterConfig = config.getChild(BOT_BLOCKER_ELEMENT_KEY);
            }

            if (botFilterConfig != null) {
                // Client Blocking
                //
                // Ingest the blocked ip addresses.
                for (Object o : botFilterConfig.getChildren(IP_ADDRESS_ELEMENT_KEY)) {
                    String ipAddr = ((Element) o).getTextTrim();
                    ipAddresses.add(ipAddr);
                }
                // Ingest the blocked ip address match regex expressions.
                processConfigMatchElements(botFilterConfig,IP_MATCH_ELEMENT_KEY,ipMatchPatterns);
                // Ingest the blocked user-agent match regex expressions.
                processConfigMatchElements(botFilterConfig,USER_AGENT_MATCH_ELEMENT_KEY,userAgentMatchPatterns);

                // Response filtering patterns
                //
                // By default, we allow block clients to get images and CSS so
                // that if the client is a browser the error pages will render.
                // But if the element <BlockImagesAndCss /> is present in the
                // config then we don't
                Element blockImagesAndCss = botFilterConfig.getChild(BLOCK_IMAGES_AND_CSS_ELEMENT_NAME);
                if(blockImagesAndCss == null) {
                    // We didn't get told to block images and css, so we allow it.
                    while(contextPath.startsWith("/")){
                        contextPath = contextPath.substring(1);
                    }
                    String regex = "^"+ (contextPath.isEmpty()?"":"\\/"+contextPath) + imagesAndCssRegex;
                    Pattern imageAndCssPattern = Pattern.compile(regex);
                    allowedResponsePatterns.add(imageAndCssPattern);
                }
                // Process the allowed response regex elements
                processConfigMatchElements(botFilterConfig,ALLOWED_RESPONSE_REGEX_ELEMENT_KEY,allowedResponsePatterns);
                // Process the blocked response regex elements
                processConfigMatchElements(botFilterConfig,BLOCKED_RESPONSE_REGEX_ELEMENT_KEY,blockedResponsePatterns);
                // If we ended up with patterns then we know we have to filter responses.
                filterBlockedResponses = !blockedResponsePatterns.isEmpty() || !allowedResponsePatterns.isEmpty();
            }
            initialized = true;
        }
        finally {
            initLock.unlock();
        }
    }


    /**
     * For the Filter interface.
     * @param servletRequest
     * @param servletResponse
     * @param filterChain
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest)servletRequest;
        HttpServletResponse response = (HttpServletResponse)servletResponse;

        ReturnValue rv = requestShouldBeBlocked(request);
        if (rv.blockResponse) {

            // Logs this blocked request to the BotFilterLog
            log.info(makeBlockedRequestMessageJson(request, rv.cause));

            // Forward client to the 403 error response page.
            String error403 = "/error/error403.jsp";
            ServletContext sc = request.getServletContext();
            RequestDispatcher rd = sc.getRequestDispatcher(error403);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            rd.forward(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    @Override
    public void destroy() {


    }

    /**
     * This helper function is used by init(Element) to process the regex match
     * expressions in the configuration.
     *
     * @param config The coinfiuguration Element to use.
     * @param matchElementName The name of the element(s) in the configuration
     *                         to process.
     * @param matchPatterns The Vector to which the new Patterns will be added.
     */
    void processConfigMatchElements(Element config, String matchElementName, Vector<Pattern> matchPatterns){
        for (Object o : config.getChildren(matchElementName)) {
            String userAgentMatch = ((Element) o).getTextTrim();
            Pattern uaP = Pattern.compile(userAgentMatch);
            matchPatterns.add(uaP);
        }
    }

    /**
     * Determines if the incoming request should be blocked.
     * @param request The request to be handled.
     * @return Returns a ReturnValue which values set to indicated request block
     * status and the cause, if blocked.
     */
    private ReturnValue requestShouldBeBlocked(HttpServletRequest request) {
        ReturnValue rv = new ReturnValue();
        rv.blockResponse = false;
        rv.cause = "Not Blocked";
        String remoteAddr = request.getRemoteAddr();
        if(ipAddresses.contains(remoteAddr)){
            jsonDebug("The ip address: " + LogUtil.scrubEntry(remoteAddr) +
                    " is on the list of blocked addresses.");
            rv.blockResponse = isResponseBlocked(request);
            rv.cause = "IpAddress";
        }
        for(Pattern p: ipMatchPatterns){
            if(p.matcher(remoteAddr).matches()){
                jsonDebug("The ip address: " + LogUtil.scrubEntry(remoteAddr) +
                        " matches the pattern: \""+p.pattern()+"\"");
                rv.blockResponse = isResponseBlocked(request);
                rv.cause = "IpMatch";
            }
        }
        String userAgent = request.getHeader("User-Agent");
        if(userAgent != null) {
            for (Pattern p : userAgentMatchPatterns) {
                if (p.matcher(userAgent).matches()) {
                    jsonDebug("The User-Agent header: " +
                            LogUtil.scrubEntry(userAgent) +
                            " matches the pattern: \""+ p.pattern() + "\"");
                    rv.blockResponse = isResponseBlocked(request);
                    rv.cause = "UserAgentMatch";
                }
            }
        }
        return rv;
    }

    /**
     * Builds the json message for the BotFilter log file.
     * @param request The request thatis being blocked.
     * @param cause The cause of the blockage.
     * @return The json message for the BotFilter log
     */
    private String makeBlockedRequestMessageJson(HttpServletRequest request, String cause){
        String json_msg;
        json_msg = "{ \"blocked\": {";
        json_msg += "\"time\": " + System.currentTimeMillis() + ", ";
        json_msg += "\"verb\": \"" + request.getMethod() + "\", ";
        json_msg += "\"ip\": \"" + LogUtil.scrubEntry(request.getRemoteAddr()) + "\", ";

        String userAgent = Scrub.simpleString(request.getHeader("User-Agent"));
        if(userAgent == null) { userAgent = ""; }
        json_msg += "\"user_agent\": \"" + StringEscapeUtils.escapeJson(userAgent) + "\", ";

        json_msg += "\"path\": \"" + StringEscapeUtils.escapeJson(Scrub.urlContent(request.getRequestURI())) + "\", ";

        String query = request.getQueryString();
        if(query==null) { query=""; }
        json_msg += "\"query\": \"" + StringEscapeUtils.escapeJson(Scrub.simpleQueryString(query)) + "\", ";

        json_msg += "\"cause\": \"" + cause + "\" ";
        json_msg += "} } \n";
        return json_msg;
    }


    /**
     * Used to determine if a response should be blocked. The assumption here
     * is that the client is blocked (by IpAddress, IpMatch, or UserAgentMatch)
     * and this method decides if the response should or should not be
     * transmitted. Essentially, this is a blocked response filter. This is
     * useful for things like allowing images and css (which by
     * default will not be blocked) that are used by browsers to render the
     * return error pages etc.
     * @param request The blocked request
     * @return True if the response should not be transmitted, false otherwise.
     */
    private boolean isResponseBlocked(HttpServletRequest request) {

        // If we aren't response filtering then the
        // answer is always yes, block that request.
        if(!filterBlockedResponses)
            return true;

        String requestURI = request.getRequestURI();
        if (allowedResponsePatterns.isEmpty()) {
            return matchMe(requestURI,blockedResponsePatterns);
        }
        else {
            for (Pattern allowedResponsePattern : allowedResponsePatterns) {
                boolean allowedResponse = allowedResponsePattern.matcher(requestURI).matches();
                if (allowedResponse) {
                    return matchMe(requestURI,blockedResponsePatterns);
                }
            }
        }
        return true;
    }

    /**
     * Helper method to evaluate a Vector of regex match patterns against
     * a candidateString
     * @param candidateString The string to evaluate.
     * @param matchPatterns The Vector of potential match patterns
     * @return True if any pattern in matchPatterns matches the candidateString
     */
    private boolean matchMe(String candidateString, Vector<Pattern> matchPatterns){
        for (Pattern pattern : matchPatterns) {
            boolean matched = pattern.matcher(candidateString).matches();
            if (matched) {
                jsonDebug("The candidate string " + candidateString +
                        " matches the blocked response regex pattern: \""
                        + pattern.pattern() + "\"");
                return true;
            }
        }
        return false;
    }

    private void jsonDebug(String msg){
        if(log.isDebugEnabled()) {
            String json_msg;
            json_msg = "{ \"debug\": {";
            json_msg += "\"time\": " + System.currentTimeMillis() + ", ";
            json_msg += "\"message\": \"" + StringEscapeUtils.escapeJson(msg) + "\" ";
            json_msg += "} }\n";
            log.debug(json_msg);
        }
    }

    private void jsonWarn(String msg){
        if(log.isWarnEnabled()) {
            String json_msg;
            json_msg = "{ \"warning\": {";
            json_msg += "\"time\": " + System.currentTimeMillis() + ", ";
            json_msg += "\"message\": \"" + StringEscapeUtils.escapeJson(msg) + "\" ";
            json_msg += "} }\n";
            log.warn(json_msg);
        }
    }

}
