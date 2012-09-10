package opendap.bes.dap4Responders;

import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 9/4/12
 * Time: 3:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class ClientMediaType extends MediaType {

    public ClientMediaType(String mediaType){
        HashMap<String,String> params = new HashMap<String,String>();
        String[] parts = mediaType.split(";");

        this.quality=1.0;
        if(parts.length>1){
            for(int i=1; i<parts.length; i++){
                String[] param = parts[1].split("=");
                if(param.length==2){
                    params.put(param[0],param[1]);
                }
            }
            if(params.containsKey("q") && params.get("q")!=null){
                try {
                    double value = Double.parseDouble(params.get("q"));
                    if(0<value && value<=1.0)
                        this.quality = value;
                }
                catch(NumberFormatException e){
                    // Ignore and move on...
                }

            }

        }
        this.mimeType=parts[0];
        String[] types = parts[0].split("/");
        if(types.length==2){
            this.primaryType = types[0];
            this.subType = types[1];
        }
        else {
            this.primaryType = parts[0];
            this.subType = "";
        }

    }

}
