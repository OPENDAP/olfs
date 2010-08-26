package opendap.semantics.IRISail;

import org.openrdf.model.ValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Aug 26, 2010
 * Time: 3:29:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class IntrospectionUtil {
    
    private static Logger log = LoggerFactory.getLogger(IntrospectionUtil.class);

    
    public static Method getMethodForFunction(String className,
                                              String methodName) {

        Method method;


        try {
            Class methodContext = Class.forName(className);
            log.debug("getMethodForFunction() - Located java class: "
                    + className);

            try {
                method = methodContext.getMethod(methodName, List.class, ValueFactory.class);

                if (Modifier.isStatic(method.getModifiers())) {
                    log.debug("getMethodForFunction() - Located static java method: "
                                    + getProcessingMethodDescription(method));
                    return method;
                }

                /*
                 * for(Constructor c : methodContext.getConstructors()){
                 * if(c.getGenericParameterTypes().length==0){
                 * log.debug("getMethodForFunction() - Located java class
                 * '"+className+"' with a no element " + "constructor and the
                 * method "+getProcessingMethodDescription(method)); return
                 * method; } }
                 */

            } catch (NoSuchMethodException e) {
                log.error("getMethodForFunction() - The class '" + className
                        + "' does not contain a method called '" + methodName
                        + "'");
            }

        } catch (ClassNotFoundException e) {
            log.error("getMethodForFunction() - Unable to locate java class: "
                    + className);
        }

        log.error("getMethodForFunction() - Unable to locate the requested java class/method combination. "
                        + "class: '"
                        + className
                        + "'   method: '"
                        + methodName
                        + "'");
        return null;

    }

    public static Method getMethodForFunction(Object classInstance,
            String methodName) {

        Method method;

        Class methodContext = classInstance.getClass();
        String className = methodContext.getName();


        try {
            method = methodContext.getMethod(methodName, List.class,
                    ValueFactory.class);
            log.debug("getMethodForFunction() - Located the java method: "
                    + getProcessingMethodDescription(method)
                    + " in an instance of the class '" + className + "'");
            return method;

        } catch (NoSuchMethodException e) {
            log.error("getMethodForFunction() - The class '" + className
                            + "' does not contain a method called '"
                            + methodName + "'");
        }

        log.error("getMethodForFunction() - Unable to locate the requested java class/method combination. "
                        + "class: '"
                        + className
                        + "'   method: '"
                        + methodName
                        + "'");
        return null;

    }

    public static String getProcessingMethodDescription(Method m) {

        String msg = "";

        msg += m.getReturnType().getName() + " ";
        msg += m.getName();

        String params = "";
        for (Class c : m.getParameterTypes()) {
            if (!params.equals(""))
                params += ", ";
            params += c.getName();
        }
        msg += "(" + params + ")";

        String exceptions = "";
        for (Class c : m.getExceptionTypes()) {
            if (!exceptions.equals(""))
                exceptions += ", ";
            exceptions += c.getName();
        }
        msg += " " + exceptions + ";";

        return msg;

    }
}
