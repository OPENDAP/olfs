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


    @Override
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
