package opendap.coreServlet;

import opendap.logging.LogUtil;
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

public class BotFilter implements Filter {

    private static final java.util.concurrent.locks.Lock initLock;
    static {
        initLock = new ReentrantLock();
    }


    private FilterConfig filterConfig;
    private static final String CONFIG_PARAMETER_KEY = "config";
    private static final String DEFAULT_CONFIG_FILENAME = "olfs.xml";
    private static final String BOT_FILTER_ELEMENT_KEY = "BotBlocker";
    private static final String IP_ADDRESS_ELEMENT_KEY = "IpAddress";
    private static final String IP_MATCH_ELEMENT_KEY = "IpMatch";
    private static final String USER_AGENT_MATCH_ELEMENT_KEY = "UserAgentMatch";
    private static final String ALLOWED_RESPONSE_REGEX_ELEMENT_KEY = "allowedResponseRegex";
    private static final String BLOCKED_RESPONSE_REGEX_ELEMENT_KEY = "blockedResponseRegex";

    private static org.slf4j.Logger log = null;

    private boolean initialized;

    private final HashSet<String> ipAddresses;
    private final Vector<Pattern> ipMatchPatterns;
    private final Vector<Pattern> userAgentMatchPatterns;

    private final Vector<Pattern> blockedResponsePatterns;

    private final Vector<Pattern> allowedResponsePatterns;
    private boolean responseFiltering;

    public BotFilter() {
        log = org.slf4j.LoggerFactory.getLogger(getClass());
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
                log.warn("init() - INITIALIZATION HAS BEEN POSTPONED! FAILED TO INITIALIZE BotBlocker! " +
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
            Element botBlockerConfig = config.getChild(BOT_FILTER_ELEMENT_KEY);
            if (botBlockerConfig != null) {
                for (Object o : botBlockerConfig.getChildren(IP_ADDRESS_ELEMENT_KEY)) {
                    String ipAddr = ((Element) o).getTextTrim();
                    ipAddresses.add(ipAddr);
                }
                for (Object o : botBlockerConfig.getChildren(IP_MATCH_ELEMENT_KEY)) {
                    String ipMatch = ((Element) o).getTextTrim();
                    Pattern ipP = Pattern.compile(ipMatch);
                    ipMatchPatterns.add(ipP);
                }
                for (Object o : botBlockerConfig.getChildren(USER_AGENT_MATCH_ELEMENT_KEY)) {
                    String userAgentMatch = ((Element) o).getTextTrim();
                    Pattern uaP = Pattern.compile(userAgentMatch);
                    userAgentMatchPatterns.add(uaP);
                }

                // Response filtering patterns
                for (Object o : botBlockerConfig.getChildren(ALLOWED_RESPONSE_REGEX_ELEMENT_KEY)) {
                    String ipMatch = ((Element) o).getTextTrim();
                    Pattern ipP = Pattern.compile(ipMatch);
                    allowedResponsePatterns.add(ipP);
                    responseFiltering = true;
                }
                for (Object o : botBlockerConfig.getChildren(BLOCKED_RESPONSE_REGEX_ELEMENT_KEY)) {
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

        if(clientShouldBeBlocked(request)) {
            String error403 = "/error/error403.jsp";
            ServletContext sc = request.getServletContext();
            RequestDispatcher rd  = sc.getRequestDispatcher(error403);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            rd.forward(request,response);
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
            log.info("The ip address: {} is " +
                    "on the list of blocked addresses", LogUtil.scrubEntry(remoteAddr));
            blockIt = isResponseBlocked(request);
        }
        for(Pattern p: ipMatchPatterns){
            if(p.matcher(remoteAddr).matches()){
                log.info("The ip address: {} matches the pattern: \"{}\"", LogUtil.scrubEntry(remoteAddr),p.pattern());
                blockIt = isResponseBlocked(request);
            }
        }
        String userAgent = request.getHeader("User-Agent");
        if(userAgent != null) {
            for (Pattern p : userAgentMatchPatterns) {
                if (p.matcher(userAgent).matches()) {
                    log.info("The User-Agent header: {} matches the pattern: \"{}\"", LogUtil.scrubEntry(userAgent), p.pattern());
                    blockIt = isResponseBlocked(request);
                }
            }
        }

        if(blockIt) {
            log.warn("Blocking ip address: {}", LogUtil.scrubEntry(remoteAddr));
        }

        return blockIt;
    }

    public boolean isResponseBlocked(HttpServletRequest request) {

        // If we aren't response filtering then the
        // answer is always yes, block that request.
        if(!responseFiltering)
            return true;

        String requestUrl = ReqInfo.getRequestUrlPath(request);
        boolean isBlocked;
        if (allowedResponsePatterns.isEmpty()) {
            isBlocked = false;
            for (Pattern blockedResponsePattern : blockedResponsePatterns) {
                log.info("The request matches the blocked response regex pattern: \"" + blockedResponsePattern.pattern() + "\"");
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
                    log.info("The request matches the allowed response regex pattern: \"" + allowedResponsePattern.pattern() + "\"");
                    for (Pattern blockedResponsePattern : blockedResponsePatterns) {
                        boolean blockedResponse = blockedResponsePattern.matcher(requestUrl).matches();
                        if (blockedResponse) {
                            log.info("The request matches the blocked response regex pattern: \"" + blockedResponsePattern.pattern() + "\"");
                            isBlocked = true;
                        }
                    }
                }
            }
        }
        return isBlocked;
    }

}
