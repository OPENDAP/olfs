package opendap.bes;

import opendap.bes.dapResponders.BesApi;
import opendap.coreServlet.ResourceInfo;
import opendap.coreServlet.ReqInfo;
import opendap.dap.DapResponder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 11/28/11
 * Time: 3:17 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class BesDapResponder extends DapResponder {



    private Logger log;

    private BesApi _besApi;


    public BesDapResponder(String sysPath, String requestSuffixRegex, BesApi besApi) {
        this(sysPath,null,requestSuffixRegex, besApi);
    }

    public BesDapResponder(String sysPath, String pathPrefix, String requestSuffix, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffix);

        log = LoggerFactory.getLogger(DapResponder.class);

        _besApi = besApi;

    }



    @Override
    public long getLastModified(HttpServletRequest request) throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = getBesApi().getBesDataSourceID(relativeUrl, false);

        ResourceInfo ri = getResourceInfo(dataSource);
        return ri.lastModified();

    }




    public ResourceInfo getResourceInfo(String resourceName) throws Exception {
        return new BESResource(resourceName, getBesApi());
    }




/**
     *
     * @param relativeUrl
     * @return
     */
    @Override
    public boolean matches(String relativeUrl) {

        return matches(relativeUrl,true);

    }


    /**
     *
     * @param relativeUrl
     * @param checkWithBes
     * @return
     *
     */
    public boolean matches(String relativeUrl, boolean checkWithBes) {

        String besDataSourceId = getBesApi().getBesDataSourceID(relativeUrl, getRequestSuffixMatchPattern(), checkWithBes);

        if(besDataSourceId!=null)
            return true;


        return false;

    }

    //public abstract boolean needsBesToMatch();
    //public abstract boolean needsBesToRespond();


    public BesApi getBesApi(){
        return _besApi;
    }

    public void setBesApi(BesApi besApi){
        _besApi = besApi;
    }



}
