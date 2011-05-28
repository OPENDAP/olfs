package opendap.hai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 5/28/11
 * Time: 11:09 AM
 * To change this template use File | Settings | File Templates.
 */
public class Util {


    public static HashMap<String, String> processQuery(HttpServletRequest request){

        Logger log = LoggerFactory.getLogger("opendap.bes.BesControlApi");
        HashMap<String, String> kvp = new HashMap<String, String>();

        StringBuilder sb = new StringBuilder();
        Map<String,String[]> params = request.getParameterMap();
        for(String name: params.keySet()){
            sb.append(name).append(" = ");
            String[] values = params.get(name);
            if(values.length>1){
                log.warn("Multiple values found for besctl parameter '{}'. Will use the last one found.", name);
            }
            for(String value: values){
                sb.append("'").append(value).append("' ");
                kvp.put(name,value);
            }
            sb.append("\n");
        }

        log.debug("Parameters:\n{}",sb);



        return kvp;


    }


}
