package opendap.coreServlet;

import java.io.FileOutputStream;

public class FileOutputStreamTransmitCoordinator implements TransmitCoordinator {

    FileOutputStream d_fos;
    public FileOutputStreamTransmitCoordinator(FileOutputStream fos){
        d_fos = fos;
    }
    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {

    }
}
