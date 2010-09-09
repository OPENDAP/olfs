package opendap.webstart;

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Sep 5, 2010
 * Time: 5:06:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApplicationRegistry {

    private static Logger log = LoggerFactory.getLogger(ApplicationRegistry.class);

    private static Vector<JwsHandler> jwsHandlers = null;



    public static void init(String resourcesDir, Element webStartConfig) throws ServletException {
        jwsHandlers = buildJwsHandlers(resourcesDir,webStartConfig);

    }

    /**
     * Navigates the config document to instantiate an ordered list of
     * JwsHandler Handlers. Then all of the handlers are initialized by
     * calling their init() methods and passing into them the XML Element
     * that defined them from the config document.
     *
     * @return A VEector of JwsHandlers that have been intialized and are ready to use.
     * @throws javax.servlet.ServletException When things go poorly
     */
    private static Vector<JwsHandler> buildJwsHandlers(String resourcesDir, Element webStartConfig) throws ServletException {

        String msg;

        Vector<JwsHandler> jwsHandlers = new Vector<JwsHandler>();

        log.debug("Building JwsHandlers");


        for (Object o : webStartConfig.getChildren("JwsHandler")) {
            Element handlerElement = (Element) ((Element) o).clone();
            String className = handlerElement.getAttribute("className").getValue();
            JwsHandler dh;
            try {

                log.debug("Building Handler: " + className);
                Class classDefinition = Class.forName(className);
                dh = (JwsHandler) classDefinition.newInstance();

            } catch (ClassNotFoundException e) {
                msg = "Cannot find class: " + className;
                log.error(msg);
                throw new ServletException(msg, e);
            } catch (InstantiationException e) {
                msg = "Cannot instantiate class: " + className;
                log.error(msg);
                throw new ServletException(msg, e);
            } catch (IllegalAccessException e) {
                msg = "Cannot access class: " + className;
                log.error(msg);
                throw new ServletException(msg, e);
            } catch (ClassCastException e) {
                msg = "Cannot cast class: " + className + " to opendap.webstart.JwsHandler";
                log.error(msg);
                throw new ServletException(msg, e);
            }

            log.debug("Initializing Handler: " + className);
            dh.init(handlerElement,resourcesDir);

            jwsHandlers.add(dh);
        }

        log.debug("JwsHandlers have been built.");
        return jwsHandlers;

    }



}
