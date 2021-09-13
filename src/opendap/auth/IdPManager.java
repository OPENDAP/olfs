package opendap.auth;

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class IdPManager {

    private static Logger log;
    private static ConcurrentHashMap<String, IdProvider> ipdInstances;
    static {
        ipdInstances = new ConcurrentHashMap<>();
        log = LoggerFactory.getLogger(IdPManager.class);
    }

    private static IdProvider defaultProvider = null;

    private static String serviceContext = null;

    public static void setServiceContext(String sc){ serviceContext = sc;}

    public static Collection<IdProvider> getProviders(){ return ipdInstances.values(); }

    public static IdProvider getProvider(String auth_context){
        return ipdInstances.get(auth_context);
    }



    public static void addProvider(IdProvider ipd) throws ConfigurationException {
        if(ipd.isDefault()){
            if(defaultProvider !=null){
                StringBuilder msg = new StringBuilder("addProvider() - ");
                msg.append("The IdP \"").append(ipd.getAuthContext()).append("\" ");
                msg.append("(").append(ipd.getDescription()).append(") ");
                msg.append("Self identifies as THE default IdP, yet there is already a default IdP registered");
                msg.append(" authContext: \"").append(defaultProvider.getAuthContext()).append("\" ");
                msg.append("(").append(defaultProvider.getDescription()).append(") THIS IS UNACCEPTABLE.");
                log.error(msg.toString());
                throw new ConfigurationException(msg.toString());
            }
            defaultProvider = ipd;
        }
        ipdInstances.put(ipd.getAuthContext(),ipd);
    }

    public static void addProvider(Element config) throws ConfigurationException {
        String msg;
        if(config==null) {
            msg = "Configuration MAY NOT be null!.";
            log.error("idpFactory() -  {}",msg);
            throw new ConfigurationException(msg);
        }
        String idpClassName = config.getAttributeValue("class");

        if(idpClassName==null) {
            msg = "IdProvider definition must contain a \"class\" attribute whose value is the class name of the IdProvider implementation to be created.";
            log.error("idpFactory() - {}",msg);
            throw new ConfigurationException(msg);
        }
        try {
            log.debug("idpFactory(): Building Identity Provider: " + idpClassName);
            Class<?> classDefinition = Class.forName(idpClassName);
            IdProvider idp = (IdProvider) classDefinition.getDeclaredConstructor().newInstance();
            idp.init(config, serviceContext);
            addProvider(idp);
        } catch (Exception e) {
            msg = "Unable to manufacture an instance of "+idpClassName+"  Caught an " + e.getClass().getName() + " exception.  msg:" + e.getMessage();
            log.error("idpFactory() - {}"+msg);
            throw new ConfigurationException(msg, e);

        }
    }

    public static boolean hasDefaultProvider(){
        return defaultProvider != null;
    }

    public static IdProvider getDefaultProvider() {
        return defaultProvider;
    }
}
