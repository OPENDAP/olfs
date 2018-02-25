package opendap.auth;

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class IdPManager {

    private static Logger _log;
    private static ConcurrentHashMap<String, IdProvider> _ipdInstances ;
    static {
        _ipdInstances = new ConcurrentHashMap<>();
        _log = LoggerFactory.getLogger(IdPManager.class);
    }

    private static IdProvider _defaultProvider = null;

    private static String _serviceContext = null;

    public static void setServiceContext(String sc){ _serviceContext = sc;}

    public static Collection<IdProvider> getProviders(){ return _ipdInstances.values(); }



    public static void addProvider(IdProvider ipd) throws ConfigurationException {
        if(ipd.isDefault()){
            if(_defaultProvider!=null){
                StringBuilder msg = new StringBuilder("addProvider() - ");
                msg.append("The IdP \"").append(ipd.getAuthContext()).append("\" ");
                msg.append("(").append(ipd.getDescription()).append(") ");
                msg.append("Self identifies as THE default IdP, yet there is already a default IdP registered");
                msg.append(" authContext: \"").append(_defaultProvider.getAuthContext()).append("\" ");
                msg.append("(").append(_defaultProvider.getDescription()).append(") THIS IS UNACCEPTABLE.");
                _log.error(msg.toString());
                throw new ConfigurationException(msg.toString());
            }
            _defaultProvider = ipd;
        }
        _ipdInstances.put(ipd.getAuthContext(),ipd);
    }

    public static void addProvider(Element config) throws ConfigurationException {
        String msg;
        if(config==null) {
            msg = "Configuration MAY NOT be null!.";
            _log.error("idpFactory() -  {}",msg);
            throw new ConfigurationException(msg);
        }
        String idpClassName = config.getAttributeValue("class");

        if(idpClassName==null) {
            msg = "IdProvider definition must contain a \"class\" attribute whose value is the class name of the IdProvider implementation to be created.";
            _log.error("idpFactory() - {}",msg);
            throw new ConfigurationException(msg);
        }
        try {
            _log.debug("idpFactory(): Building Identity Provider: " + idpClassName);
            Class classDefinition = Class.forName(idpClassName);
            IdProvider idp = (IdProvider) classDefinition.newInstance();
            idp.init(config,_serviceContext );
            addProvider(idp);
        } catch (Exception e) {
            msg = "Unable to manufacture an instance of "+idpClassName+"  Caught an " + e.getClass().getName() + " exception.  msg:" + e.getMessage();
            _log.error("idpFactory() - {}"+msg);
            throw new ConfigurationException(msg, e);

        }
    }

    public static boolean hasDefaultProvider(){
        return _defaultProvider != null;
    }

    public static IdProvider getDefaultProvider() {
        return _defaultProvider;
    }
}
