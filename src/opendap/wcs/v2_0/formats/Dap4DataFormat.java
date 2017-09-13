package opendap.wcs.v2_0.formats;

import opendap.coreServlet.MimeTypes;

public class Dap4DataFormat extends WcsResponseFormat {
    public Dap4DataFormat() {
        super();
        _name = "dap4";
        _dapSuffix = "dap";
        _mimeType = MimeTypes.getMimeType(_dapSuffix);
    }
}
