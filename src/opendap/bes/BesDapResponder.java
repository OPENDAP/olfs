package opendap.bes;

import opendap.bes.dapResponders.BesApi;
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

    public BesDapResponder(String sysPath, String pathPrefix, String reqSuffixRegex, BesApi besApi) {
        super(sysPath, pathPrefix, reqSuffixRegex);

        log = LoggerFactory.getLogger(DapResponder.class);

        _besApi = besApi;

    }


    public BesApi getBesApi(){
        return _besApi;
    }

    public void setBesApi(BesApi besApi){
        _besApi = besApi;
    }



}
