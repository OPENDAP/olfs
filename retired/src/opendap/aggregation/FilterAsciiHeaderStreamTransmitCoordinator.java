package opendap.aggregation;

import opendap.aggregation.FilterAsciiHeaderStream;
import opendap.coreServlet.TransmitCoordinator;

public class FilterAsciiHeaderStreamTransmitCoordinator implements TransmitCoordinator {

    FilterAsciiHeaderStream d_fahs;
    FilterAsciiHeaderStreamTransmitCoordinator(FilterAsciiHeaderStream fahs){
        d_fahs = fahs;
    }
    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {

    }
}
