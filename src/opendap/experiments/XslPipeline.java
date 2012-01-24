package opendap.experiments;

import net.sf.saxon.s9api.*;
import opendap.bes.Version;
import opendap.xml.Transformer;

import javax.xml.transform.stream.StreamSource;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 11/9/11
 * Time: 3:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class XslPipeline {


    public static void main(String[] args){


        try {
            StreamSource sourceDocument = new StreamSource("path/to/source/document");

            Serializer stdout = new Serializer();
            stdout.setOutputProperty(Serializer.Property.METHOD, "xml");
            stdout.setOutputProperty(Serializer.Property.INDENT, "yes");
            stdout.setOutputStream(System.out);


            StreamSource xlst_1 = new StreamSource("path/to/transform/filename1");
            Processor proc = new Processor(false);
            XsltCompiler comp = proc.newXsltCompiler();
            XsltExecutable exp = comp.compile(xlst_1);
            XsltTransformer transform_1 = exp.load();

            StreamSource xlst_2 = new StreamSource("path/to/transform/filename2");
            comp = proc.newXsltCompiler();
            exp = comp.compile(xlst_2);
            XsltTransformer transform_2 = exp.load();


            transform_1.setSource(sourceDocument);
            transform_1.setDestination(transform_2);
            transform_2.setDestination(stdout);

            transform_1.transform();


        } catch (SaxonApiException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }




    }
}
