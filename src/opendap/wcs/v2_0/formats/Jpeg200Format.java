package opendap.wcs.v2_0.formats;

import opendap.coreServlet.MimeTypes;

public class Jpeg200Format extends WcsResponseFormat {
    public Jpeg200Format(){
        super();
        _name = "jpeg2000";
        _dapSuffix = "jp2";
        _mimeType = MimeTypes.getMimeType(_dapSuffix);

    }
}
