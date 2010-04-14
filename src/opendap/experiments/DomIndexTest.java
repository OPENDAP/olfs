package opendap.experiments;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ContentFilter;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Mar 11, 2010
 * Time: 12:00:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class DomIndexTest {




    public static void main(String args[]){

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        SAXBuilder sb = new SAXBuilder();
        Document doc;
        Iterator i;
        Element e, docRoot;

        try {
            for(String s:args){
                doc = sb.build(s);
                docRoot =doc.getRootElement();
                i = docRoot.getDescendants(new ContentFilter(ContentFilter.ELEMENT));
                while(i.hasNext()){
                    e = (Element) i.next();
                    System.out.println("Element: "+e.getName()+"   index(Parent is "+e.getParentElement().getName() +"): "+e.getParentElement().indexOf(e));
                }




            }
        } catch (JDOMException er) {
            er.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException er) {
            er.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }



}
