package opendap.wcs.v2_0.formats;

import opendap.coreServlet.MimeTypes;

public class Dap2DataFormat extends WcsResponseFormat {
    public Dap2DataFormat(){
        super();
        _name = "dap2";
        _dapSuffix = "dods";
        _mimeType = MimeTypes.getMimeType(_dapSuffix);
    }
}
