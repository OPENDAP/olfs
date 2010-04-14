package opendap.wcs.v1_1_2;

import org.jdom.Element;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Feb 9, 2009
 * Time: 5:29:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class LanguageString  {

    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    private String lang;

    public LanguageString(String s, String language){
        value = s;
        lang = language;
    }



}
