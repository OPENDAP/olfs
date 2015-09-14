/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2013 OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
 * //
 * // This library is free software; you can redistribute it and/or
 * // modify it under the terms of the GNU Lesser General Public
 * // License as published by the Free Software Foundation; either
 * // version 2.1 of the License, or (at your option) any later version.
 * //
 * // This library is distributed in the hope that it will be useful,
 * // but WITHOUT ANY WARRANTY; without even the implied warranty of
 * // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * // Lesser General Public License for more details.
 * //
 * // You should have received a copy of the GNU Lesser General Public
 * // License along with this library; if not, write to the Free Software
 * // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */

package opendap.bes.dap4Responders;

import java.util.HashMap;

/**
 *
 *
 *
 *  Accept: text/xml, application/xml, application/xhtml+xml, text/html;q=0.9, text/plain;q=0.8, image/png, * / *;q=0.5
 **/


public class MediaType implements Comparable {


    protected String _name;

    protected String _mimeType;
    protected String _mediaSuffix;
    protected String _primaryType;
    protected String _subType;
    protected Double _quality;
    protected String _wildcard = "*";
    
    protected boolean _ptwc, _stwc;
    protected double score;




    public String getMimeType(){ return _mimeType;}
    public String getPrimaryType() { return _primaryType;}
    public String getSubType() { return _subType;}
    public double getQuality(){ return _quality;}
    // public double getScore(){ return score;}
    // public void   setScore(double s){ score=s;}

    public String getMediaSuffix(){ return _mediaSuffix;}

    // public boolean isWildcardSubtype(){ return _stwc; }
    // public boolean isWildcardType(){ return _ptwc; }




    @Override
    public int compareTo(Object o) throws ClassCastException {
        MediaType otherType = (MediaType)o;
        if(_quality >otherType._quality)
            return 1;

        if(_quality <otherType._quality)
            return -1;

        return 0;
    }

    @Override
    public  boolean equals(Object o) {
        return o instanceof MediaType && compareTo(o) == 0;
    }

    public void setName(String name){
        _name = name;
    }
    public String getName(){
        return _name;
    }


    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(_primaryType).append("/").append(_subType).append(";q=").append(_quality).
                append("  mediaSuffix: ").append(_mediaSuffix).
                append("  score: ").append(score).
                append("");

        return s.toString();
    }
    
    public MediaType(String primaryType, String subType, String suffix){
        _mimeType = primaryType + "/" + subType;
        _primaryType = primaryType;
        _subType = subType;
        _ptwc = primaryType.equals(_wildcard);
        _stwc = _subType.equals(_wildcard);
        _quality = 1.0;
        score = 0.0;
        _mediaSuffix = suffix;
    }
    
    public MediaType(String acceptMediaType){
        HashMap<String,String> params = new HashMap<>();
        String[] parts = acceptMediaType.split(";");

        this._quality =1.0;
        if(parts.length>1){
            for(int i=1; i<parts.length; i++){
                String[] param = parts[1].split("=");
                if(param.length==2){
                    params.put(param[0],param[1]);
                }
            }
            if(params.containsKey("q") && params.get("q")!=null){
                try {
                    double value = Double.parseDouble(params.get("q"));
                    if(0<value && value<=1.0)
                        this._quality = value;
                }
                catch(NumberFormatException e){
                    // Ignore and move on...
                }

            }

        }
        this._mimeType =parts[0];
        String[] types = parts[0].split("/");
        if(types.length==2){
            this._primaryType = types[0];
            this._subType = types[1];
        }
        else {
            this._primaryType = parts[0];
            this._subType = "";
        }

    }
    

}
