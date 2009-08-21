package opendap.coreServlet;

import java.rmi.server.UID;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Aug 18, 2009
 * Time: 12:25:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class MimeBoundary {


    String boundary;

    public MimeBoundary(){
        boundary =    getNewMimeBoundary();

    }

    /**
     * This is a utility function that returns a new MIME boundary string suitable for use in a Multipart MIME respone.
     * <p><b>Do not confuse this method with <code>getMimeBoundary</code> </b></p>
     * @return Returns a NEW MIME Boundary string.
     */
    private static String getNewMimeBoundary(){
        //Date date = new Date();
        return "Part_"+newUidString();
    }

    /**
     * This is a utility function that returns a new MIME boundary string suitable for use in a Multipart MIME respone.
     * <p><b>Do not confuse this method with <code>getMimeBoundary</code> </b></p>
     * @return Returns a NEW MIME Boundary string.
     */
    public String getEncapsulationBoundary(){
        //Date date = new Date();
        return "--"+boundary;
    }

    /**
     * This is a utility function that returns a new MIME boundary string suitable for use in a Multipart MIME respone.
     * <p><b>Do not confuse this method with <code>getMimeBoundary</code> </b></p>
     * @return Returns a NEW MIME Boundary string.
     */
    public String getClosingBoundary(){
        //Date date = new Date();
        return "\n--"+boundary+"--\n";
    }

    /**
     * This is a utility function that returns a new MIME boundary string suitable for use in a Multipart MIME respone.
     * <p><b>Do not confuse this method with <code>getMimeBoundary</code> </b></p>
     * @return Returns a NEW MIME Boundary string.
     */
    public String getBoundary(){
        //Date date = new Date();
        return boundary;
    }


    /**
     *
     * @return Returns a new UID String
     */
    public String newContentID(){
        //return newUidString();
        return new UID().toString()+"@opendap.org";
    }

    /**
     *
     * @return Returns a new UID String
     */
    public static String newUidString(){
        UID uid = new UID();

        byte[] val = uid.toString().getBytes();

        String suid  = "";
        int v;

        for (byte aVal : val) {
            v = aVal;
            suid += Integer.toHexString(v);
        }

        return suid;
    }


}
