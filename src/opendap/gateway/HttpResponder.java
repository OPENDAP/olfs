package opendap.gateway;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 */
public abstract class HttpResponder {

    private Pattern _pattern;
    protected String _systemPath;
    private String pathPrefix;


    private HttpResponder(){}

    public HttpResponder(String sysPath, String pathPrefix, String regexPattern){//}, Method doResponse){
        super();
        _pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
        _systemPath = sysPath;
        this.pathPrefix = pathPrefix;
    }

    public Pattern getPattern(){ return _pattern;}
    public void setPattern(String regexPattern){ _pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);}

    public boolean matches(String s){
       return _pattern.matcher(s).matches();

    }


    public void setPathPrefix(String prefix){ pathPrefix = prefix ;}
    public String getPathPrefix() { return pathPrefix; }


    public abstract void respondToHttpRequest(HttpServletRequest request, HttpServletResponse response) throws Exception;



    public String readFileAsString(String pathname) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        Scanner scanner = new Scanner(new File(pathname));

        try {
            while (scanner.hasNextLine()) {
                stringBuilder.append(scanner.nextLine() + "\n");
            }
        } finally {
            scanner.close();
        }
        return stringBuilder.toString();
    }




}
