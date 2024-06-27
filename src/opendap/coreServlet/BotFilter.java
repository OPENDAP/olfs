package opendap.coreServlet;

import opendap.logging.LogUtil;
import opendap.logging.ServletLogUtil;
import org.jdom.Document;
import org.jdom.Element;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;


/**
 * BotFilter
 * This implementation of the javax.servlet.Filter interface can be used to
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

    private static org.slf4j.Logger log = null;

    private boolean initialized;

    private final HashSet<String> ipAddresses;
    private final Vector<Pattern> ipMatchPatterns;
    private final Vector<Pattern> userAgentMatchPatterns;

    private final Vector<Pattern> blockedResponsePatterns;

    private final Vector<Pattern> allowedResponsePatterns;
    private boolean responseFiltering;

    public BotFilter() {
        log = org.slf4j.LoggerFactory.getLogger(BOT_FILTER_LOG_NAME);
        initialized = false;
        ipAddresses = new HashSet<>();
        ipMatchPatterns = new Vector<>();
        userAgentMatchPatterns = new Vector<>();
        allowedResponsePatterns = new Vector<>();
        blockedResponsePatterns = new Vector<>();
        responseFiltering = false;
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
                    log.warn(msg);
                }
                Document configDoc = ServletUtil.loadConfig(configFileName, filterConfig.getServletContext());
                init(configDoc.getRootElement());
            } catch (Exception se) {
                log.warn("init() - INITIALIZATION HAS BEEN POSTPONED! FAILED TO INITIALIZE BotFilter! " +
                        "Caught {} Message: {} ", se.getClass().getName(), se.getMessage());
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
    private void init(Element config){
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
                for (Object o : botFilterConfig.getChildren(IP_ADDRESS_ELEMENT_KEY)) {
                    String ipAddr = ((Element) o).getTextTrim();
                    ipAddresses.add(ipAddr);
                }
                for (Object o : botFilterConfig.getChildren(IP_MATCH_ELEMENT_KEY)) {
                    String ipMatch = ((Element) o).getTextTrim();
                    Pattern ipP = Pattern.compile(ipMatch);
                    ipMatchPatterns.add(ipP);
                }
                for (Object o : botFilterConfig.getChildren(USER_AGENT_MATCH_ELEMENT_KEY)) {
                    String userAgentMatch = ((Element) o).getTextTrim();
                    Pattern uaP = Pattern.compile(userAgentMatch);
                    userAgentMatchPatterns.add(uaP);
                }

                // Response filtering patterns
                for (Object o : botFilterConfig.getChildren(ALLOWED_RESPONSE_REGEX_ELEMENT_KEY)) {
                    String ipMatch = ((Element) o).getTextTrim();
                    Pattern ipP = Pattern.compile(ipMatch);
                    allowedResponsePatterns.add(ipP);
                    responseFiltering = true;
                }
                for (Object o : botFilterConfig.getChildren(BLOCKED_RESPONSE_REGEX_ELEMENT_KEY)) {
                    String ipMatch = ((Element) o).getTextTrim();
                    Pattern ipP = Pattern.compile(ipMatch);
                    blockedResponsePatterns.add(ipP);
                    responseFiltering = true;
                }
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

        if (clientShouldBeBlocked(request)) {
            String error403 = "/error/error403.jsp";
            ServletContext sc = request.getServletContext();
            RequestDispatcher rd = sc.getRequestDispatcher(error403);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            rd.forward(request, response);
        }
    }

    @Override
    public void destroy() {


    }


    /**
     *
     * @param request The request to be handled.
     * @return True if the IsoDispatchHandler can service the request, false
     * otherwise.
     */
    public boolean clientShouldBeBlocked(HttpServletRequest request) {
        boolean blockIt = false;
        String remoteAddr = request.getRemoteAddr();
        if(ipAddresses.contains(remoteAddr)){
            log.debug("The ip address: {} is " +
                    "on the list of blocked addresses", LogUtil.scrubEntry(remoteAddr));
            blockIt = isResponseBlocked(request);
        }
        for(Pattern p: ipMatchPatterns){
            if(p.matcher(remoteAddr).matches()){
                log.debug("The ip address: {} matches the pattern: \"{}\"", LogUtil.scrubEntry(remoteAddr),p.pattern());
                blockIt = isResponseBlocked(request);
            }
        }
        String userAgent = request.getHeader("User-Agent");
        if(userAgent != null) {
            for (Pattern p : userAgentMatchPatterns) {
                if (p.matcher(userAgent).matches()) {
                    log.debug("The User-Agent header: {} matches the pattern: \"{}\"", LogUtil.scrubEntry(userAgent), p.pattern());
                    blockIt = isResponseBlocked(request);
                }
            }
        }

        if(blockIt) {
            String msg;
            msg = "{ \"blocked\": {";
            msg += "\"time\": " + System.currentTimeMillis() + ", ";
            msg += "\"verb\": \"" + request.getMethod() + "\", ";
            msg += "\"ip\": \"" + LogUtil.scrubEntry(remoteAddr) + "\", ";
            msg += "\"path\": \"" + Scrub.urlContent(request.getRequestURI()) + "\", ";

            String query = request.getQueryString();
            if(query==null) query="";

            msg += "\"query\": \"" + Scrub.simpleQueryString(query) + "\" ";
            msg += "} } \n";
            log.info(msg);
        }

        return blockIt;
    }

    public boolean isResponseBlocked(HttpServletRequest request) {

        // If we aren't response filtering then the
        // answer is always yes, block that request.
        if(!responseFiltering)
            return true;

        String requestUrl = ReqInfo.getLocalUrl(request);
        boolean isBlocked;
        if (allowedResponsePatterns.isEmpty()) {
            isBlocked = false;
            for (Pattern blockedResponsePattern : blockedResponsePatterns) {
                log.debug("The request matches the blocked response regex pattern: \"" + blockedResponsePattern.pattern() + "\"");
                if (blockedResponsePattern.matcher(requestUrl).matches()) {
                    isBlocked = true;
                }
            }
        }
        else {
            isBlocked = true;
            for (Pattern allowedResponsePattern : allowedResponsePatterns) {
                boolean allowedResponse = allowedResponsePattern.matcher(requestUrl).matches();
                if (allowedResponse) {
                    isBlocked = false;
                    log.debug("The request matches the allowed response regex pattern: \"" + allowedResponsePattern.pattern() + "\"");
                    for (Pattern blockedResponsePattern : blockedResponsePatterns) {
                        boolean blockedResponse = blockedResponsePattern.matcher(requestUrl).matches();
                        if (blockedResponse) {
                            log.debug("The request matches the blocked response regex pattern: \"" + blockedResponsePattern.pattern() + "\"");
                            isBlocked = true;
                        }
                    }
                }
            }
        }
        return isBlocked;
    }

}
