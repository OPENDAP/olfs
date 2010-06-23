package opendap.experiments;

import org.openrdf.model.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Jun 18, 2010
 * Time: 4:42:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReflectionTest {
    private Logger log;


    public ReflectionTest(){
        log = LoggerFactory.getLogger(getClass());

    }

    public static void main(String[] args){
        ReflectionTest rt = new ReflectionTest();

        rt.main2(args);

    }
    public void main2(String[] args){



        MethodSpy ms = new MethodSpy();

        ms.probe(this.getClass());


        String s  = "http://test.org/Smootchy";
        List<String> rdfList = new ArrayList<String>();
        rdfList.add(s);
        log.debug("test(): "+test(s));


        String fnName = "test";
        Method func = null;
        for (Method m : this.getClass().getDeclaredMethods()) {
            log.debug("Located function: " + getMethodString(m));

            if (m.getName().equals(fnName)){
                func = m;
                log.debug("Located function matching name "+fnName+" function: " + getMethodString(func));
            }
        }



        if(func!=null){
            Value stObj = null;
            try {
                s = (String) func.invoke(this,s);
            } catch (Exception e) {
                log.error("Unable to invoke processing function '"+func.getName()+"()' Caught "+e.getClass().getName()+", msg: "+e.getMessage());
                return;
            }
            log.debug("Value: "+s);
        }
        else{
            log.warn("No function found.");
        }
        log.debug("wdcqw");


    }

    public String test(String s){
        return "S1/"+s;
    }




    enum ClassMember {
        CONSTRUCTOR, FIELD, METHOD, CLASS, ALL
    }

    public class ClassSpy {
        public void probe(Class c) {
            System.out.format("Class:%n  %s%n%n", c.getCanonicalName());

            Package p = c.getPackage();
            System.out.format("Package:%n  %s%n%n",
                    (p != null ? p.getName() : "-- No Package --"));

            printMembers(c.getConstructors(), "Constuctors");
            printMembers(c.getFields(), "Fields");
            printMembers(c.getMethods(), "Methods");
            printClasses(c);

            // production code should handle these exceptions more gracefully
        }

        private void printMembers(Member[] mbrs, String s) {
            System.out.format("%s:%n", s);
            for (Member mbr : mbrs) {
                if (mbr instanceof MessageFormat.Field)
                    System.out.format("  %s%n", ((MessageFormat.Field) mbr).toString());
                else if (mbr instanceof Constructor)
                    System.out.format("  %s%n", ((Constructor) mbr).toGenericString());
                else if (mbr instanceof Method)
                    System.out.format("  %s%n", ((Method) mbr).toGenericString());
            }
            if (mbrs.length == 0)
                System.out.format("  -- No %s --%n", s);
            System.out.format("%n");
        }

        private void printClasses(Class<?> c) {
            System.out.format("Classes:%n");
            Class<?>[] clss = c.getClasses();
            for (Class<?> cls : clss)
                System.out.format("  %s%n", cls.getCanonicalName());
            if (clss.length == 0)
                System.out.format("  -- No member interfaces, classes, or enums --%n");
            System.out.format("%n");
        }
    }


    public class MethodSpy {
        private static final String fmt = "%24s: %s%n";

        // for the morbidly curious

        <E extends RuntimeException> void genericThrow() throws E {
        }

        public void probe(Class<?> c) {
            Method[] allMethods = c.getDeclaredMethods();
            for (Method m : allMethods) {
                System.out.format("%s%n", m.toGenericString());

                System.out.format(fmt, "ReturnType", m.getReturnType());
                System.out.format(fmt, "GenericReturnType", m.getGenericReturnType());

                Class<?>[] pType = m.getParameterTypes();
                Type[] gpType = m.getGenericParameterTypes();
                for (int i = 0; i < pType.length; i++) {
                    System.out.format(fmt, "ParameterType", pType[i]);
                    System.out.format(fmt, "GenericParameterType", gpType[i]);
                }

                Class<?>[] xType = m.getExceptionTypes();
                Type[] gxType = m.getGenericExceptionTypes();
                for (int i = 0; i < xType.length; i++) {
                    System.out.format(fmt, "ExceptionType", xType[i]);
                    System.out.format(fmt, "GenericExceptionType", gxType[i]);
                }
            }

            // production code should handle these exceptions more gracefully
        }
    }
    String getMethodString(Method m){


        String msg = "";

        msg += m.getReturnType().getName() + " ";
        msg += m.getName();

        String params = "";
        for( Class c : m.getParameterTypes()){
            if(!params.equals(""))
                params += ", ";
            params += c.getName();
        }
        msg += "("+params+")";

        String exceptions = "";
        for(Class c : m.getExceptionTypes()){
            if(!exceptions.equals(""))
                exceptions += ", ";
            exceptions += c.getName();
        }
        msg += " "+exceptions+";";


        return msg;

    }



}
