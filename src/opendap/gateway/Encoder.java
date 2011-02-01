package opendap.gateway;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 1/29/11
 * Time: 8:34 AM
 * To change this template use File | Settings | File Templates.
 */
public interface Encoder {



    public String encode(String s);

    public String decode(String s);


    public void encode(InputStream is, OutputStream os) throws Exception;

    public void decode(InputStream is, OutputStream os) throws Exception;


}
