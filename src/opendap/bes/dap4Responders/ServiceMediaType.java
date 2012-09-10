package opendap.bes.dap4Responders;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 9/4/12
 * Time: 3:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServiceMediaType extends MediaType {

    public ServiceMediaType(String pType, String sType, String suffix){
        mimeType = pType + "/" + sType;
        primaryType = pType;
        subType = sType;
        twc = pType.equals(wildcard);
        stwc = subType.equals(wildcard);
        quality = 1.0;
        score = 0.0;
        mediaSuffix = suffix;
    }




}
