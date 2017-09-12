package opendap.wcs.v2_0.formats;

public class WcsResponseFormat {
    protected String _name;
    protected String _mimeType;
    protected String _dapSuffix;
    public WcsResponseFormat(){
        _name = null;
        _mimeType = null;
        _dapSuffix = null;
    }
    public String dapDataResponseSuffix(){ return _dapSuffix.toLowerCase();};
    public String name(){ return _name.toLowerCase(); }
    public String mimeType(){return _mimeType.toLowerCase();}
}
