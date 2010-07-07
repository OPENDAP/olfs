package opendap.semantics.IRISail;

import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Jul 7, 2010
 * Time: 10:43:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class RepositoryFunctions {




    /***************************************************************************
     * function join to concatenate strings
     *
     * @param RDFList
     * @param createValue
     * @return
     */
    public static Value join(List<String> RDFList, ValueFactory createValue) {
        int i = 0;
        boolean joinStrIsURL = false;
        String targetObj = "";
        if (RDFList.get(1).startsWith("http://")) {
            joinStrIsURL = true;
        }
        for (i = 1; i < RDFList.size() - 1; i++) {
            targetObj += RDFList.get(i) + RDFList.get(0); // rdfList.get(0) +
            // separator
            // log.debug("Component("+i+")= " + RDFList.get(i));
        }

        targetObj += RDFList.get(i); // last component no separator

        Value stObjStr;
        if (joinStrIsURL) {
            stObjStr = createValue.createURI(targetObj);
        } else {
            stObjStr = createValue.createLiteral(targetObj);
        }

        return stObjStr;
    }



    
}
