package opendap.wcs.v1_1_2;

import org.jdom.Element;

import java.util.Vector;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Feb 9, 2009
 * Time: 5:34:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class Keywords {

    private Vector<LanguageString> keywords;
    private Vector<CodeString>     types;

    public Keywords(){
        keywords = null;
        types= null;

    }

    public Keywords(LanguageString[] keywords, CodeString[] types){
        this.keywords = new Vector<LanguageString>();
        for(LanguageString keyword: keywords)
            this.keywords.add(keyword);

        this.types = new Vector<CodeString>();
        for(CodeString type: types)
            this.types.add(type);

    }


    public Keywords(Vector<LanguageString> keywords, Vector<CodeString> types){
        this.keywords = keywords;
        this.types = types;
    }


    public void addKeyword(LanguageString keyword){
        keywords.add(keyword);
    }


    public LanguageString[] getKeywordsArray(){

        LanguageString[] array = new  LanguageString[keywords.size()];

        return keywords.toArray(array);

    }

    public Iterator<LanguageString> getKeywordsIterator(){
        return keywords.iterator();
    }


    public void addType(CodeString type){
        types.add(type);
    }

    public CodeString[] getTypesArray(){

        CodeString[] array = new  CodeString[types.size()];

        return types.toArray(array);

    }

    public Iterator<CodeString> getTypesIterator(){
        return types.iterator();
    }



    public Element getElement(){
        Element e;
        Element keywordsElement = new Element("Keyword",WCS.OWCS_NS);

        for(LanguageString keyword: keywords){
            e = new Element("Keyword",WCS.OWS_NS);
            e.setText(keyword.getValue());
            e.setAttribute("xml:lang",keyword.getLang());
            keywordsElement.addContent(e);
        }

        for(CodeString type: types){
            e = new Element("Type",WCS.OWS_NS);
            e.setText(type.getValue());
            e.setAttribute("codeSpace",type.getValue());
            keywordsElement.addContent(e);
        }
        return keywordsElement;

    }



}
