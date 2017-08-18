package opendap.wcs.v2_0;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by ndp on 11/9/16.
 */
public class ScaleRequest {

    private CoverageDescription _myCoverageDescription;
    private String _outputCRS;
    private String _subsettingCRS;

    private boolean _isScaleRequest;
    /**
     *  Request Parameter
     *  kvp: ScaleFactor=x
     */
    private double _scaleFactor;

    /**
     *  Request Parameter
     *  kvp: ScaleAxes=ax1(x1),ax2(x2)
     */
    private HashMap<String,Double> _scaleAxisByFactor;

    /**
     *  Request Parameter
     *  kvp: ScaleSize=ax1(s1),ax2(s2)
     */
    private HashMap<String,Long> _scaleToSize;

    /**
     *
     * ScaleExtent (WTF does this even mean? Throw an exception!)
     *  ...& SCALEEXTENT=i(10:20),j(20:30) &...
     */
    private HashMap<String,long[]> _scaleToExtents;


    // INTERPOLATION
    //
    // &interpolation=iMethod&  specifes global interpolation method iMethod for all axes
    //
    // &InterpolationPerAxis=axis1,iMethod&  specifes global interpolation method iMethod for axis1
    //
    // &InterpolationPerAxis=axis1,iMethod&InterpolationPerAxis=axis2,foo&
    //    specifes global interpolation method iMethod for all axis1  and foo for axis2

    /**
     * Request Parameter
     * kvp:
     *   &interpolation=iMethod&  specifies global interpolation method iMethod for all axes
     */
    private String _interpolationMethod;


    /**
     * Request Parameter
     * kvp:
     *    &InterpolationPerAxis=axis1,iMethod&  specifes global interpolation method iMethod for axis1
     *
     *    &InterpolationPerAxis=axis1,iMethod&InterpolationPerAxis=axis2,foo&
     *    specifes global interpolation method iMethod for all axis1  and foo for axis2
     */
    private HashMap<String,String> _interpolationByAxis;



    private Logger _log;

    public ScaleRequest() {
        _log = LoggerFactory.getLogger(this.getClass());

        _myCoverageDescription = null;
        _isScaleRequest = false;

        _scaleFactor       = Double.NaN;
        _scaleAxisByFactor = new HashMap<>();
        _scaleToSize       = new HashMap<>();
        _scaleToExtents    = new HashMap<>();    // supported? maybe not...

        _outputCRS      = null;
        _subsettingCRS  = null;
        _interpolationMethod = null;
        _interpolationByAxis =  new HashMap<>();  // supported? maybe not...
    }

    public ScaleRequest(ScaleRequest sr) {
        this();
        _myCoverageDescription = sr._myCoverageDescription;

        _isScaleRequest = sr._isScaleRequest;
        _outputCRS      = sr._outputCRS;
        _subsettingCRS  = sr._subsettingCRS;
        _scaleFactor       = sr._scaleFactor;
        _scaleAxisByFactor = new HashMap<>();
        _scaleAxisByFactor.putAll(sr._scaleAxisByFactor);
        _scaleToSize       = new HashMap<>();
        _scaleToSize.putAll(sr._scaleToSize);
        _scaleToExtents    = new HashMap<>();    // supported? maybe not...
        _scaleToExtents.putAll(sr._scaleToExtents);
        _interpolationMethod = sr._interpolationMethod;
        _interpolationByAxis =  new HashMap<>();  // supported? maybe not...
        _interpolationByAxis.putAll(sr._interpolationByAxis);
    }


    public ScaleRequest(Map<String,String[]> kvp, CoverageDescription cd)
            throws WcsException, InterruptedException {
        this();

        _myCoverageDescription = cd;


        String s[];

        // Did they submit a global scale factor?
        s = kvp.get("scalefactor".toLowerCase());
        if(s!=null){
            try {
                _scaleFactor = Double.parseDouble(s[0]);
            }
            catch (NumberFormatException e){
                throw new WcsException("The value of the SCALEFACTOR parameter failed to parse as a floating " +
                        "point value. Msg: "+e.getMessage(),
                        WcsException.INVALID_PARAMETER_VALUE,
                        "scal:ScaleByFactor");
            }
            _isScaleRequest = true;
        }


        // Did the submit per-axis scale factors?
        s = kvp.get("scaleaxes".toLowerCase());
        if(s!=null){
            if(_isScaleRequest){
                throw new WcsException("Only a single Scaling operation may be requested.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "SCALING");
            }
            ingestKvpForScaleAxesParameter(s[0]);
            _isScaleRequest = true;

        }

        // Did they submit per-axis scale sizes?
        s = kvp.get("scalesize".toLowerCase());
        if(s!=null){
            if(_isScaleRequest){
                throw new WcsException("Only a single Scaling operation may be requested.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "SCALING");
            }
            ingestKvpForScaleSizeParameter(s[0]);
            _isScaleRequest = true;
        }


        // Did they submit a scale to extent parameter?
        s = kvp.get("scaleextent".toLowerCase());
        if(s!=null){
            if(_isScaleRequest){
                throw new WcsException("Only a single Scaling operation may be requested.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "SCALING");
            }
            _isScaleRequest = true;

            // too bad because that's something we never understood...
            throw new WcsException("The SCALEEXTENT operation is not implemented.",
                    WcsException.OPERATION_NOT_SUPPORTED,
                    "scal:ScaleToExtent");
        }




        // Did they specify an output CRS?
        s = kvp.get("outputCRS".toLowerCase());
        if(s!=null){
            _outputCRS = s[0];
        }


        // Did they specify a CRS for their subset coordinates?
        s = kvp.get("subsettingCRS".toLowerCase());
        if(s!=null){
            _subsettingCRS = s[0];
            throw new WcsException("The 'SUBSETTINGCRS' feature is not yet supported.",
                    WcsException.OPERATION_NOT_SUPPORTED,
                    "SUBSETTINGCRS");

        }



        // Did they submit an interpolation method parameter?
        s = kvp.get("interpolation".toLowerCase());
        if(s!=null){
            _interpolationMethod = s[0];
        }

        // Did they submit one or more  per axis interpolation method parameters?
        s = kvp.get("interpolationperaxis".toLowerCase());
        if(s!=null){

            for(String interpolationPerAxisString: s){
                String parts[] = interpolationPerAxisString.split(",");

                if(parts.length < 2){
                    throw new WcsException("The INTERPOLATIONPERAXIS parameter '"+interpolationPerAxisString+
                            "' does not have both an axis and an interpolation method.",
                            WcsException.INVALID_PARAMETER_VALUE,
                            "int:InterpolationPerAxis");
                }

                if(parts.length > 2){
                    throw new WcsException("The INTERPOLATIONPERAXIS parameter '"+interpolationPerAxisString+
                            "' has too many componets - it should have a single dimension name and a " +
                            "single interpolation method name.",
                            WcsException.INVALID_PARAMETER_VALUE,
                            "int:InterpolationPerAxis");
                }
                _interpolationByAxis.put(parts[0],parts[1]);
            }

            throw new WcsException("The 'INTERPOLATIONPERAXIS' feature is not yet supported.",
                    WcsException.OPERATION_NOT_SUPPORTED,
                    "int:InterpolationPerAxis");

        }

    }

    private void ingestKvpForScaleAxesParameter(String kvpScaleAxesString) throws WcsException {

        String axisScaleStrings[] = kvpScaleAxesString.split(",");

        for(String axisScaleString: axisScaleStrings){

            int leftParen = axisScaleString.indexOf("(");
            int rghtParen = axisScaleString.indexOf(")");

            if(leftParen<0 || rghtParen<0 || leftParen > rghtParen){
                throw new WcsException("Invalid subset expression. The 'SCALEAXES' expression '"+kvpScaleAxesString+"' lacks " +
                        "correctly organized parenthetical content.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "scal:ScaleAxesByFactor");
            }

            String dimensionName = axisScaleString.substring(0,leftParen);

            if(dimensionName.length()==0){
                throw new WcsException("Each subclause of the  'SCALEAXES' parameter must begin with a dimension name.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "scal:ScaleAxesByFactor");
            }

            String scaleFactorString = axisScaleString.substring(leftParen+1,rghtParen);

            if(scaleFactorString.length()==0){
                throw new WcsException("Each subclause of the  'SCALEAXES' parameter must contain a scale factor value.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "scal:ScaleAxesByFactor");
            }

            double scaleFactor;
            try {
                scaleFactor = Double.parseDouble(scaleFactorString);
            }
            catch (NumberFormatException e){
                throw new WcsException("The scale factor string for dimension"+dimensionName+
                        " failed to parse as a floating point value.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "scal:ScaleAxesByFactor");
            }

            _scaleAxisByFactor.put(dimensionName, scaleFactor);
        }
    }


    private void ingestKvpForScaleSizeParameter(String kvpScaleSizeString) throws WcsException {


        String axisScaleStrings[] = kvpScaleSizeString.split(",");

        for(String axisScaleString: axisScaleStrings){

            int leftParen = axisScaleString.indexOf("(");
            int rghtParen = axisScaleString.indexOf(")");

            if(leftParen<0 || rghtParen<0 || leftParen > rghtParen){
                throw new WcsException("Invalid subset expression. The 'SCALESIZE' expression '"+kvpScaleSizeString+"' lacks " +
                        "correctly organized parenthetical content.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "scal:ScaleAxesByFactor");
            }

            String dimensionName = axisScaleString.substring(0,leftParen);

            if(dimensionName.length()==0){
                throw new WcsException("Each subclause of the  'SCALEAXES' parameter must begin with a dimension name.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "scal:ScaleAxesByFactor");
            }

            String scaleSizeString = axisScaleString.substring(leftParen+1,rghtParen);

            if(scaleSizeString.length()==0){
                throw new WcsException("Each subclause of the  'SCALEAXES' parameter must contain a scale factor value.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "scal:ScaleAxesByFactor");
            }

            long scaleFactor;
            try {
                scaleFactor = Long.parseLong(scaleSizeString);
            }
            catch (NumberFormatException e){
                throw new WcsException("The scale size string for dimension"+dimensionName+
                        " failed to parse as an integer value.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "scal:ScaleAxesByFactor");
            }

            _scaleToSize.put(dimensionName, scaleFactor);
        }
    }

    /*
    public String getOutputCRS(){
        return _outputCRS;
    }

    public String getSubsettingCRS(){
        return _subsettingCRS;
    }


    public double getScaleFactor(){
        return _scaleFactor;
    }



    public HashMap<String, Double> getScaleAxesByFactor(){
        HashMap<String,Double> map = new HashMap<>();
        map.putAll(_scaleAxisByFactor);
        return map;
    }



    public HashMap<String, Long> getScaleToSize(){
        HashMap<String,Long> map = new HashMap<>();
        map.putAll(_scaleToSize);
        return map;
    }



    public String getInterpolationMethod(){
        return _interpolationMethod;
    }

    public HashMap<String, String> getInterpolationByAxis(){
        HashMap<String,String> map = new HashMap<>();
        map.putAll(_interpolationByAxis);
        return map;
    }

    private long getTargetSize(String dimName, long dimSize) throws WcsException {

        long newDimSize = dimSize;

        if(!Double.isNaN(_scaleFactor)){
            newDimSize = (long)(dimSize * _scaleFactor);
        }
        else if(!_scaleAxisByFactor.isEmpty()){
            Double scaler = _scaleAxisByFactor.get(dimName);
            if(scaler!=null){
                newDimSize = (long)(dimSize*scaler);
            }
        }
        else if(!_scaleToSize.isEmpty()){
            Long targetSize = _scaleToSize.get(dimName);
            if(targetSize!=null){
                newDimSize = targetSize;
            }
        }
        else if(!_scaleToExtents.isEmpty()){
            // too bad because that's something we never understood...
            throw new WcsException("The SCALEEXTENT operation is not implemented.",
                    WcsException.OPERATION_NOT_SUPPORTED,
                    "scal:ScaleToExtent");

        }

        return newDimSize;


    }

*/

    //  SSF: scale_grid(Grid, Y size, X size, CRS, Interpolation method)

    public String getScaleExpression(String gridSubset) throws WcsException {

        if(!_isScaleRequest){
            return gridSubset;
        }

        StringBuilder scaleMe = new StringBuilder();


        LinkedHashMap<String,DomainCoordinate> domCoords = _myCoverageDescription.getDomainCoordinates();

        Iterator<DomainCoordinate> i  = domCoords.values().iterator();
        DomainCoordinate yCoordinate=null, xCoordinate=null;
        if(i.hasNext()){
            xCoordinate = i.next(); // initialize
        }
        while(i.hasNext()){
            yCoordinate = xCoordinate;   // next to last, should be latitude.
            xCoordinate = i.next();      // last should be longitude.
        }
        if(xCoordinate==null || yCoordinate==null){
            throw new WcsException("Not enough domain cooordinates in this coverage!",WcsException.NO_APPLICABLE_CODE);
        }


        long y_size = yCoordinate.getSize();
        _log.debug("getScaleExpression() - Y-COORDINATE: {}[{}]",yCoordinate.getName(),y_size);

        long x_size = xCoordinate.getSize();
        _log.debug("getScaleExpression() - X-COORDINATE: {}[{}]",xCoordinate.getName(),x_size);



        /**
         * We know that there can be only ONE scaling parameter and that we
         * enforced this in the constructor. So, we examine things...
         */
        if(!Double.isNaN(_scaleFactor)){
            // Looks like a single ScaleFactor is the winner!
            x_size = (long)(xCoordinate.getSize() * _scaleFactor);
            y_size = (long)(yCoordinate.getSize() * _scaleFactor);
        }
        else if(!_scaleAxisByFactor.isEmpty()){
            // Looks like ScaleAxis is the winner!
            Double scaleFactor = _scaleAxisByFactor.get(xCoordinate.getName());
            if(scaleFactor!=null){
                x_size = (long)(xCoordinate.getSize() * scaleFactor);
            }
            scaleFactor = _scaleAxisByFactor.get(yCoordinate.getName());
            if(scaleFactor!=null){
                y_size = (long)(yCoordinate.getSize() * scaleFactor);
            }
        }
        else if(!_scaleToSize.isEmpty()){
            // Looks like a single ScaleSize is the winner!
            Long size = _scaleToSize.get(xCoordinate.getName());
            if(size!=null){
                x_size = size;
            }
            size = _scaleToSize.get(yCoordinate.getName());
            if(size!=null){
                y_size = size;
            }
        }
        else if(!_scaleToExtents.isEmpty()){
            // Looks like ScaleExtent is the winner!
            throw new WcsException("The SCALEEXTENT operation is not available.",
                    WcsException.OPERATION_NOT_SUPPORTED,"SCALEEXTENT");
        }

        _log.debug("getScaleExpression() - SCALED Y-COORDINATE {}[{}]",yCoordinate.getDapID(),y_size);
        _log.debug("getScaleExpression() - SCALED X-COORDINATE: {}[{}]",xCoordinate.getDapID(),x_size);

        scaleMe.append("scale_grid(");
        scaleMe.append(gridSubset);
        scaleMe.append(",").append(y_size);
        scaleMe.append(",").append(x_size);
        if (_outputCRS != null)
            scaleMe.append(",").append(_outputCRS);

        if (_interpolationMethod != null)
            scaleMe.append(",").append(_interpolationMethod);
        scaleMe.append(")");

        _log.debug("getScaleExpression() - scale_expression: {}",scaleMe.toString());
        _log.debug("getScaleExpression() - END");

        return scaleMe.toString();

    }




}
