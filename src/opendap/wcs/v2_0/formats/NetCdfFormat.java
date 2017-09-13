package opendap.wcs.v2_0.formats;

import opendap.coreServlet.MimeTypes;

public class NetCdfFormat extends WcsResponseFormat {
    public NetCdfFormat(){
        super();
        _name = "netcdf";
        _dapSuffix = "nc";
        _mimeType = MimeTypes.getMimeType(_dapSuffix);
    }
};
