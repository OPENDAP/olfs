package opendap.threddsHandler;

import opendap.namespaces.THREDDS;
import org.jdom.Element;

import java.util.Vector;
import java.util.regex.Pattern;

/**
 * Created by ndp on 4/17/15.
 */
public class Filter {


    Vector<Clude> _cludes;


    public Filter(Element filter) {
        _cludes = new Vector<>();
        if (filter != null) {
            for (Object o : filter.getChildren()) {

                Element cludeElement = (Element) o;

                Clude clude = new Clude(cludeElement,cludeElement.getName().equals(THREDDS.EXCLUDE));

                _cludes.add(clude);

            }
        }

    }



    public class Clude {
        String regex;
        String wildcard;
        String atomicAttrVal;
        String collectionAttrVal;
        boolean appliesToAtomic;
        boolean appliesToCollection;
        boolean excludeMatching;

        Pattern wildCardPattern;
        Pattern regexPattern;

        public Clude(Element clude, boolean isExclude){

            appliesToAtomic = true;
            appliesToCollection = false;

            if(clude==null)
                return;

            wildcard = clude.getAttributeValue(THREDDS.WILDCARD);
            if(wildcard!=null){
                String regex = wildcard.replace("*",".*")+"$";
                wildCardPattern = Pattern.compile(regex);
            }
            else {
                wildCardPattern = Pattern.compile(".*$");

            }


            regex = clude.getAttributeValue(THREDDS.REGEXP);
            if(regex!=null){
                regexPattern = Pattern.compile(regex);
            }
            else {
                regexPattern = Pattern.compile(".*$");

            }

            atomicAttrVal  = clude.getAttributeValue(THREDDS.ATOMIC);
            if(atomicAttrVal!=null)
                appliesToAtomic = Boolean.parseBoolean(atomicAttrVal);

            collectionAttrVal = clude.getAttributeValue(THREDDS.COLLECTION);
            if(collectionAttrVal!=null)
                appliesToCollection = Boolean.parseBoolean(collectionAttrVal);

            excludeMatching = isExclude;
        }


        boolean include(String s, boolean isNode){

            if(wildCardPattern.matcher(s).matches() && regexPattern.matcher(s).matches()){

                if( (!isNode && appliesToAtomic) || (isNode && appliesToCollection)){
                    return !excludeMatching;
                }
            }
            return excludeMatching;
        }

    }




    boolean include(String name, boolean isNode){


        boolean include = true;
        for(Clude clude : _cludes){

            include &= clude.include(name,isNode);
        }
        return include;

    }

}
