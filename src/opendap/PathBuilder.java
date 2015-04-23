package opendap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ndp on 4/21/15.
 */
public class PathBuilder  {

    private Logger _log;

    private StringBuilder _sb;

    public PathBuilder(){

        _log = LoggerFactory.getLogger(this.getClass());

        _sb = new StringBuilder();
    }

    public PathBuilder(String s){
        _sb = new StringBuilder(s);
    }

    public PathBuilder(CharSequence cs){
        _sb = new StringBuilder(cs);
    }

    public PathBuilder pathAppend(String s){

        if(s==null || s.length()==0)
            return this;


        while (s.startsWith("/") && s.length() > 0) {
            s = s.substring(1);
        }

        //_log.debug("pathAppend: _sb: '{}' s: '{}'",_sb.toString(),s);
        //_log.debug("pathAppend: _sb.lastIndexOf(\"/\"): '{}' _sb.length(): '{}'",_sb.lastIndexOf("/"),_sb.length());


        if (_sb.length()==0 || (_sb.lastIndexOf("/") == _sb.length()-1)) {
            _sb.append(s);
        } else {
            _sb.append("/").append(s);
        }

        _log.info("pathAppend: result _sb: ",_sb.toString());

        return this;
    }


    public static String pathConcat(String path1, String path2){

        String result;
        if(path1==null || path1.length()==0) {
            result = path2;

        }
        else if(path2==null || path2.length()==0){
            result = path1;
        }
        else {
            while (path2.startsWith("/") && path2.length() > 0) {
                path2 = path2.substring(1);
            }

            if (path1.lastIndexOf("/") == path1.length()) {
                result = path1 + path2;
            } else {
                result = path1 + "/" + path2;
            }

        }
        return result;




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
