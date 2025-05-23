/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2022 OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
 * //
 * // This library is free software; you can redistribute it and/or
 * // modify it under the terms of the GNU Lesser General Public
 * // License as published by the Free Software Foundation; either
 * // version 2.1 of the License, or (at your option) any later version.
 * //
 * // This library is distributed in the hope that it will be useful,
 * // but WITHOUT ANY WARRANTY; without even the implied warranty of
 * // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * // Lesser General Public License for more details.
 * //
 * // You should have received a copy of the GNU Lesser General Public
 * // License along with this library; if not, write to the Free Software
 * // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */
package opendap.coreServlet;

import opendap.bes.BesApi;
import opendap.http.error.Forbidden;
import opendap.logging.LogUtil;
import org.jdom.Element;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashSet;
import java.util.Vector;
import java.util.regex.Pattern;

/**
 * User: ndp
 * Date: Oct 6, 2008
 * Time: 1:28:05 PM
 */
public class BotBlocker implements DispatchHandler {


    private static org.slf4j.Logger log = null;

    private boolean initialized;

    private final HashSet<String> ipAddresses;
    private final Vector<Pattern> ipMatchPatterns;
    private final Vector<Pattern> userAgentMatchPatterns;

    private final Vector<Pattern> blockedResponsePatterns;

    private final Vector<Pattern> allowedResponsePatterns;
    private boolean responseFiltering;
    
    BotBlocker() {
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
     * Since a constructor cannot be defined for an interface there needs to
     * be a way to initialize the objects state. The init() method is that way.
     * The IsoDispatchHandler that creates an instance of IsoDispatchHandler will
     * pass itself into it along with the XML element that declared the
     * IsoDispatchHandler in the configuration file (usually olfs.xml). The
     * contents of this XML Element are not restricted and may (should?)
     * contain any required information for configuration not availableInChunk by
     * interogating the IsoDispatchHandler methods.
     *
     * @param servlet This should be the IsoDispatchHandler that creates the
     *                instance of DispatychHandler that is being intialized.
     * @param config  A JDOM Element objct containing the XML Element that
     *                announced which implementation of IsoDispatchHandler to use. It may (or
     *                may not) contain additional confguration information.
     * @throws Exception When the bad things happen.
     * @see DispatchServlet
     */
    @Override
    public void init(HttpServlet servlet, Element config) throws Exception{
        init(servlet,config,null);
    }

    @Override
    public void init(HttpServlet servlet, Element config, BesApi ignored) throws Exception
    {
        if(initialized) return;
        configure(config);
        log.info("Initialized.");
        initialized = true;
    }

    private void configure(Element config){
        Element botBlockerConfig = config.getChild("BotBlocker");
        if(botBlockerConfig!=null) {
            for (Object o : botBlockerConfig.getChildren("IpAddress")) {
                String ipAddr = ((Element) o).getTextTrim();
                ipAddresses.add(ipAddr);
            }
            for (Object o : botBlockerConfig.getChildren("IpMatch")) {
                String ipMatch = ((Element) o).getTextTrim();
                Pattern ipP = Pattern.compile(ipMatch);
                ipMatchPatterns.add(ipP);
            }
            for (Object o : botBlockerConfig.getChildren("UserAgentMatch")) {
                String userAgentMatch = ((Element) o).getTextTrim();
                Pattern uaP = Pattern.compile(userAgentMatch);
                userAgentMatchPatterns.add(uaP);
            }

            // Response filtering patterns
            for (Object o : botBlockerConfig.getChildren("allowedResponseRegex")) {
                String ipMatch = ((Element) o).getTextTrim();
                Pattern ipP = Pattern.compile(ipMatch);
                allowedResponsePatterns.add(ipP);
                responseFiltering = true;
            }
            for (Object o : botBlockerConfig.getChildren("blockedResponseRegex")) {
                String ipMatch = ((Element) o).getTextTrim();
                Pattern ipP = Pattern.compile(ipMatch);
                blockedResponsePatterns.add(ipP);
                responseFiltering = true;
            }
        }
    }





    /**
     *
     * @param request The request to be handled.
     * @return True if the IsoDispatchHandler can service the request, false
     * otherwise.
     * @throws Exception When the bad things happen.
     */
    public boolean requestCanBeHandled(HttpServletRequest request)
            throws Exception {
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


    /**
     *
     * @param request The request to be handled.
     * @param response The response object into which the response information
     * will be placed.
     * @throws Exception When the bad things happen.
     */
    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response)
            throws Exception {

        String msg = "Denied access to "+request.getRemoteAddr()+" because it is " +
                "either on the list, or matches a blocking pattern.";
        log.info("handleRequest() - {}", LogUtil.scrubEntry(msg));

        throw new Forbidden(msg);
    }



    /**
     *
     *
     * @param req The request for which we need to get a last modified date.
     * @return The last modified date of the URI referenced in th request.
     * @see javax.servlet.http.HttpServlet
     */
    public long getLastModified(HttpServletRequest req) {
        return new Date().getTime();
    }


    /**
     * Called when the servlet is shutdown. Here is where to clean up open
     * connections etc.
     */
    public void destroy() {

    }


}
