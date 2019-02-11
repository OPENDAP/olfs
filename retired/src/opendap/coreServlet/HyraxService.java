package opendap.coreServlet;

import opendap.bes.dap2Responders.BesApi;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.Vector;

public class HyraxService implements DispatchHandler {

    Logger _log;
    HttpServlet _servlet;
    Vector<DispatchHandler> _get_handlers;
    Vector<DispatchHandler> _post_handlers;
    String _prefix;
    String _title;

    HyraxService(String title){
        _log = LoggerFactory.getLogger(this.getClass());
        _servlet = null;
        _prefix = null;
        _get_handlers = new Vector<>();
        _post_handlers = new Vector<>();
        _title = title;
    }

    /**
     * Since a constructor cannot be defined for an interface there needs to
     * be a way to initialize the objects state. The init() method is that way.
     * The IsoDispatchHandler that creates an instance of IsoDispatchHandler will
     * pass itself into it along with the XML element that declared the
     * IsoDispatchHandler in the configuration file (usually olfs.xml). The
     * contents of this XML Element are not restricted and may (should?)
     * contain any required information for configuration not availableInChunk by
     * interogating the IsoDispatchHandler's methods.
     *
     * @param servlet This should be the IsoDispatchHandler that creates the
     *                instance of IsoDispatchHandler that is being intialized.
     * @param config  A JDOM Element objct containing the XML Element that
     *                announced which implementation of IsoDispatchHandler to use. It may (or
     *                may not) contain additional confguration information.
     * @throws Exception When the bad things happen.
     * @see DispatchServlet
     */
    @Override
    public void init(HttpServlet servlet, Element config) throws Exception {
        init(servlet,config, new BesApi());
    }




    public void init(HttpServlet servlet, Element config, String prefix, BesApi besApi) throws Exception {

        _servlet = servlet;
        _prefix = prefix;

        loadHyraxServiceHandlers(_get_handlers,config, besApi);

        boolean enablePost = false;
        Element e = config.getChild("HttpPost");
        if(e!=null){
            String enabled = e.getAttributeValue("enabled");
            if(enabled.equalsIgnoreCase("true"))
                enablePost = true;
        }

        if(enablePost){
            opendap.bes.BesDapDispatcher bdd = new opendap.bes.BesDapDispatcher();
            bdd.init(servlet,config, (BesApi) besApi.clone());
            _post_handlers.add(bdd);
        }
    }



    @Override
    public void init(HttpServlet servlet, Element config, BesApi besApi) throws Exception {
        init(servlet,config,null, besApi);
    }

    /**
     *             <Handler className="opendap.bes.VersionDispatchHandler" />
     *
     *             <!-- Bot Blocker
     *                - This handler can be used to block access from specific IP addresses
     *                - and by a range of IP addresses using a regular expression.
     *               -->
     *             <!-- <Handler className="opendap.coreServlet.BotBlocker"> -->
     *                 <!-- <IpAddress>127.0.0.1</IpAddress> -->
     *                 <!-- This matches all IPv4 addresses, work yours out from here.... -->
     *                 <!-- <IpMatch>[012]?\d?\d\.[012]?\d?\d\.[012]?\d?\d\.[012]?\d?\d</IpMatch> -->
     *                 <!-- Any IP starting with 65.55 (MSN bots the don't respect robots.txt  -->
     *                 <!-- <IpMatch>65\.55\.[012]?\d?\d\.[012]?\d?\d</IpMatch>   -->
     *             <!-- </Handler>  -->
     *             <Handler className="opendap.ncml.NcmlDatasetDispatcher" />
     *             <Handler className="opendap.threddsHandler.StaticCatalogDispatch">
     *                 <prefix>thredds</prefix>
     *                 <useMemoryCache>true</useMemoryCache>
     *             </Handler>
     *             <Handler className="opendap.gateway.DispatchHandler">
     *                 <prefix>gateway</prefix>
     *                 <UseDAP2ResourceUrlResponse />
     *             </Handler>
     *             <Handler className="opendap.bes.BesDapDispatcher" >
     *                 <!-- AllowDirectDataSourceAccess
     *                   - If this element is present then the server will allow users to request
     *                   - the data source (file) directly. For example a user could just get the
     *                   - underlying NetCDF files located on the server without using the OPeNDAP
     *                   - request interface.
     *                   -->
     *                 <!-- AllowDirectDataSourceAccess / -->
     *                 <!--
     *                   By default, the server will provide a DAP2-style response
     *                   to requests for a dataset resource URL. Commenting out the
     *                   "UseDAP2ResourceUrlResponse" element will cause the server
     *                   to return the DAP4 DSR response when a dataset resource URL
     *                   is requested.
     *                 -->
     *                 <UseDAP2ResourceUrlResponse />
     *             </Handler>
     *             <Handler className="opendap.bes.DirectoryDispatchHandler" />
     *             <Handler className="opendap.bes.BESThreddsDispatchHandler"/>
     *             <Handler className="opendap.bes.FileDispatchHandler" />
     */
    private void loadHyraxServiceHandlers(Vector<DispatchHandler> handlers, Element config, BesApi besApi ) throws Exception {

        if(config==null)
            throw new ServletException("Bad configuration! The configuration element was NULL");

        Element botBlocker = config.getChild("BotBlocker");

        handlers.add(new opendap.bes.VersionDispatchHandler());
        if(botBlocker != null)
            handlers.add(new opendap.coreServlet.BotBlocker());
        handlers.add(new opendap.ncml.NcmlDatasetDispatcher());
        handlers.add(new opendap.threddsHandler.StaticCatalogDispatch());
        handlers.add(new opendap.gateway.DispatchHandler());
        handlers.add(new opendap.bes.BesDapDispatcher());
        handlers.add(new opendap.bes.DirectoryDispatchHandler());
        handlers.add(new opendap.bes.BESThreddsDispatchHandler());
        handlers.add(new opendap.bes.FileDispatchHandler());

        for(DispatchHandler dh:handlers){
            dh.init(_servlet,config, (BesApi)besApi.clone());
        }
    }

    /**
     * @param request The request to be handled.
     * @return True if the IsoDispatchHandler can service the request, false
     * otherwise.
     * @throws Exception When the bad things happen.
     */
    @Override
    public boolean requestCanBeHandled(HttpServletRequest request) throws Exception {
        String relativeUrl = ReqInfo.getLocalUrl(request);
        Vector<DispatchHandler> dispatchHandlers = _get_handlers;
        if(request.getMethod().equalsIgnoreCase("post")){
            dispatchHandlers = _post_handlers;
        }

        if(_prefix!=null && relativeUrl.startsWith(_prefix)){
            DispatchHandler dh = getDispatchHandler(request, dispatchHandlers);
            if (dh != null) {
                return true;
            }
            return false;
        }
        DispatchHandler dh = getDispatchHandler(request, dispatchHandlers);
        if (dh != null) {
            return true;
        }

        return false;
    }



    /**
     * Returns the first handler in the vector of DispatchHandlers that claims
     * be able to handle the incoming request.
     *
     * @param request The request we are looking to handle
     * @param dhvec   A Vector of DispatchHandlers that will be asked if they can
     *                handle the request.
     * @return The IsoDispatchHandler that can handle the request, null if no
     *         handler claims the request.
     * @throws Exception For bad behaviour.
     */
    private DispatchHandler getDispatchHandler(HttpServletRequest request, Vector<DispatchHandler> dhvec) throws Exception {
        for (DispatchHandler dh : dhvec) {
            _log.debug("Checking handler: " + dh.getClass().getName());
            if (dh.requestCanBeHandled(request)) {
                return dh;
            }
        }
        return null;
    }


    /**
     * @param request  The request to be handled.
     * @param response The response object into which the response information
     *                 will be placed.
     * @throws Exception When the bad things happen.
     */
    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        Vector<DispatchHandler> dispatchHandlers = _get_handlers;
        if(request.getMethod().equalsIgnoreCase("post")){
            dispatchHandlers = _post_handlers;
        }
        DispatchHandler dh = getDispatchHandler(request, dispatchHandlers);
        if (dh != null) {
            _log.debug("Request being handled by: " + dh.getClass().getName());
            dh.handleRequest(request, response);
        } else {
            //send404(request,response);
            throw  new OPeNDAPException(HttpServletResponse.SC_NOT_FOUND, "Failed to locate resource: "+relativeUrl);
        }

    }

    /**
     * @param req The request for which we need to get a last modified date.
     * @return The last modified date of the URI referenced in th request.
     * @see HttpServlet
     */
    @Override
    public long getLastModified(HttpServletRequest req) {
        return new Date().getTime();
    }

    /**
     * Called when the servlet is shutdown. Here is where to clean up open
     * connections etc.
     */
    @Override
    public void destroy() {

        for(DispatchHandler dh: _get_handlers)
            dh.destroy();
        _get_handlers.clear();

        for(DispatchHandler dh: _post_handlers)
            dh.destroy();
        _post_handlers.clear();

    }
}
