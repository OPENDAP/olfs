package opendap.http.mediaTypes;

import opendap.bes.dap4Responders.MediaType;

/**
 * Created by ndp on 1/27/15.
 */
public class Html  extends MediaType {

    public Html(){
        this("html");
    }

    public Html(String typeMatchString){
        super("text","html", typeMatchString);
    }

}
