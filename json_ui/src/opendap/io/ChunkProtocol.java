package opendap.io;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 10/10/11
 * Time: 11:09 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ChunkProtocol {

    public String serverProtocolUndefined();
    public String clientTestingConnection();
    public String serverConnectionOk();
    public String clientCompleteDataTransmission();
    public String clientExitingNow();


}
