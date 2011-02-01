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

    private Method _doResponse;

    private HttpResponder(){}

    public HttpResponder(String sysPath, String regexPattern){//}, Method doResponse){
        _pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
        _systemPath = sysPath;
    }

    public Pattern getPattern(){ return _pattern;}

    public boolean matches(String s){
       return _pattern.matcher(s).matches();

    }

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
