package opendap.threddsHandler;

import opendap.namespaces.THREDDS;
import org.jdom.Element;

import java.util.Vector;
import java.util.regex.Pattern;

/**
 * Created by ndp on 4/17/15.
 */
public class Filter {


    String includeAllRegexString = ".*$";

    Vector<Clude> _cludes;


    public Filter(Element filter) {
        _cludes = new Vector<>();
        if (filter != null) {
            for (Object o : filter.getChildren()) {

                Element cludeElement = (Element) o;

                Clude clude = new Clude(cludeElement);

                _cludes.add(clude);

            }
        }
        else {
            _cludes.add(new Clude());
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


        /**
         * Default includes everything!
         */
        public Clude() {
            appliesToAtomic = true;
            appliesToCollection = false;
            excludeMatching = false;
            regexPattern = Pattern.compile(includeAllRegexString);
            wildCardPattern = Pattern.compile(includeAllRegexString);

        }

        public Clude(Element clude){
            this();

            if(clude==null)
                return;

            excludeMatching = clude.getName().equals(THREDDS.EXCLUDE);


            wildcard = clude.getAttributeValue(THREDDS.WILDCARD);
            if(wildcard!=null){
                String regex = wildcard.replace(".","\\.");
                regex = regex.replace("*",".*")+"$";
                wildCardPattern = Pattern.compile(regex);
            }


            regex = clude.getAttributeValue(THREDDS.REGEXP);
            if(regex!=null){
                regexPattern = Pattern.compile(regex);
            }

            atomicAttrVal  = clude.getAttributeValue(THREDDS.ATOMIC);
            if(atomicAttrVal!=null)
                appliesToAtomic = Boolean.parseBoolean(atomicAttrVal);

            collectionAttrVal = clude.getAttributeValue(THREDDS.COLLECTION);
            if(collectionAttrVal!=null)
                appliesToCollection = Boolean.parseBoolean(collectionAttrVal);

        }


        boolean include(String s, boolean isNode){


            if( (!isNode && appliesToAtomic) || (isNode && appliesToCollection)){

                if(wildCardPattern.matcher(s).matches() && regexPattern.matcher(s).matches()) {
                    return !excludeMatching;
                }
                else {
                    return excludeMatching;
                }
            }

            return true;


        }

    }




    boolean include(String name, boolean isNode){


        boolean include = false;
        for(Clude clude : _cludes){

            if(clude.include(name,isNode))
                include = true;
        }
        return include;

    }

}
