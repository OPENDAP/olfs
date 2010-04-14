package opendap.wcs.v1_1_2;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Feb 9, 2009
 * Time: 5:35:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class CodeString {

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    private String value;

    public String getCodeSpace() {
        return codeSpace;
    }

    public void setCodeSpace(String codeSpace) {
        this.codeSpace = codeSpace;
    }

    private String codeSpace;

    public CodeString(String s, String codeSpace){
        value = s;
        this.codeSpace = codeSpace;
    }

    
}
