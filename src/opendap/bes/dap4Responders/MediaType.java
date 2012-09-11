/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
 * //
 * //
 * // Copyright (c) 2012 OPeNDAP, Inc.
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
 * // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */
package opendap.bes.dap4Responders;

/**
 *
 *
 *
 *  Accept: text/xml, application/xml, application/xhtml+xml, text/html;q=0.9, text/plain;q=0.8, image/png, * / *;q=0.5
 **/


public class MediaType implements Comparable {

    protected String mimeType;
    protected String mediaSuffix;
    protected String primaryType;
    protected String subType;
    protected Double quality;
    protected String wildcard = "*";
    protected boolean twc, stwc;
    protected double score;




    public String getMimeType(){ return mimeType;}
    public String getPrimaryType() { return primaryType;}
    public String getSubType() { return subType;}
    public double getQuality(){ return quality;}
    public double getScore(){ return score;}
    public void   setScore(double s){ score=s;}

    public String getMediaSuffix(){ return mediaSuffix;}

    public boolean isWildcardSubtype(){ return stwc; }
    public boolean isWildcardType(){ return twc; }


    public int compareTo(Object o) throws ClassCastException {
        MediaType otherType = (MediaType)o;
        if(quality>otherType.quality)
            return 1;

        if(quality<otherType.quality)
            return -1;

        return 0;
    }


    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(primaryType).append("/").append(subType).append(";q=").append(quality).
                append("  mediaSuffix: ").append(mediaSuffix).
                append("  score: ").append(score).
                append("");

        return s.toString();
    }

}
