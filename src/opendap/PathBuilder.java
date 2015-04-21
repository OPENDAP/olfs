package opendap;

/**
 * Created by ndp on 4/21/15.
 */
public class PathBuilder  {

    private StringBuilder _sb;

    public PathBuilder(){
        _sb = new StringBuilder();
    }

    public PathBuilder(String s){
        _sb = new StringBuilder(s);
    }

    public PathBuilder(CharSequence cs){
        _sb = new StringBuilder(cs);
    }

    public PathBuilder pathAppend(String s){

        while(s.startsWith("/") && s.length()>0) {
            s = s.substring(1);
        }

        if(_sb.lastIndexOf("/") == _sb.length()){
            _sb.append(s);
        }
        else {
            _sb.append("/").append(s);
        }
        return this;
    }
    public PathBuilder append(String s){
        _sb.append(s);
        return this;
    }

    @Override
    public String toString(){
        return _sb.toString();
    }




}
