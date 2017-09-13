package opendap.wcs.v2_0.formats;

import opendap.coreServlet.MimeTypes;

public class GeotiffFormat extends WcsResponseFormat {
    public GeotiffFormat(){
        super();
        _name = "geotiff";
        _dapSuffix = "tiff";
        _mimeType = MimeTypes.getMimeType(_dapSuffix);
    }
}
