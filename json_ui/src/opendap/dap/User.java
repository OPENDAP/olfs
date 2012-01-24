package opendap.dap;

import opendap.bes.BES;
import opendap.bes.BESManager;
import opendap.coreServlet.ReqInfo;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 6/8/11
 * Time: 12:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class User {


    String userName;
    String dataSource;


    public User(HttpServletRequest request){

        userName = request.getRemoteUser();
        String relativeUrl = ReqInfo.getLocalUrl(request);
        dataSource = ReqInfo.getBesDataSourceID(relativeUrl);

    }


    public int getMaxResponseSize(){


        if(userName==null) {
            return BESManager.getBES(dataSource).getMaxResponseSize();
        }

        return 0;
    }
}
