package opendap.aws.glacier;

import opendap.bes.BadConfigurationException;

/**
 * Created by ndp on 8/27/16.
 */
public class GlacierStateException extends BadConfigurationException {
    // jhrg Java complains that this is missing for a thing that is
    // serializable: static final long serialVersionUID;

    public GlacierStateException(String msg) {
        super(msg);
    }

}
