package opendap.bes.caching;

import opendap.namespaces.BES;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class is used to wrap whatever object is being cached along with data used to
 * operate in the cache. Most significantly this class implements the Comparable interface such that
 * the "natural" ordering of instances will be based on the last time each instance was accessed by the server.
 * This is not an autonomous operation and is tightly coupled with code in "BesNodeCache.getNode()" to
 * ensure that the ordering remains correct.
 */
class NodeTransaction implements Comparable  {


    private static final AtomicLong counter = new AtomicLong(0);

    private Logger log;

    private Document request;
    private Object response;
    private long lastUpdateTime;
    private long lastAccessedTime;
    private String key;
    private long serialNumber;


    public NodeTransaction(String key, Document request, Object response){
        log = LoggerFactory.getLogger(getClass());
        this.key = key;
        this.request = (Document)request.clone();


        // Dump the timeout context from the request.
        List list = this.request.getRootElement().getChildren("setContext", BES.BES_NS);
        List<Element> dropList = new ArrayList<>();
        for(Object o : list){
            Element setContextElement = (Element) o;
            String contextName=setContextElement.getAttributeValue("name");
            if(contextName.equals("bes_timeout")){
                dropList.add(setContextElement);
            }
        }
        for(Element dropMe: dropList){
            dropMe.detach();
        }

        this.response = response;
        lastUpdateTime = System.nanoTime();
        lastAccessedTime = lastUpdateTime;
        serialNumber = counter.getAndIncrement();
    }

    public String getKey(){
        return key;
    }

    public long getLastAccessedTime(){
        return lastAccessedTime;
    }

    public void updateAccessedTime() {
        lastAccessedTime = System.nanoTime();
    }



    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public Object getResponse(){
        return response;
    }
    public void setResponse(Object response) {
        this.response = response;
    }


    public Document getRequest(){
        return (Document) request.clone();
    }

    /**
     * The evaluation is based on the last accessed time (firstly) and the serial number of the
     * CatalogTransaction (secondly). If the last accessed times of two objects are the same
     * (unlikely but possible) then the serial numbers are used to determine the hierarchy/ranking/relation
     * @param o object (CatalogTransaction) to be compared
     * @return
     */
    @Override
    public int compareTo(Object o) {
        if (!(o instanceof NodeTransaction))
            throw new ClassCastException("An instance of a NodeTransaction object was expected.");
        NodeTransaction that = (NodeTransaction) o;
        if(this==that)
            return 0;

        if(this.lastAccessedTime == that.lastAccessedTime){
            log.warn("compareTo() - Required object serial numbers to differentiate " +
                    "instances. this: {} that: {}",this.serialNumber, that.serialNumber);
            return (int) (this.serialNumber - that.serialNumber);
        }


        // Why return like this? Because the return value is an integer and the computation produces a long
        // incorrect conversion (over/under flow) could change sign of result.
        return (this.lastAccessedTime - that.lastAccessedTime)>0?1:-1;


    }
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof NodeTransaction)) return false;

        NodeTransaction that = (NodeTransaction)object;

        return ( (this.lastAccessedTime == that.lastAccessedTime) &&
                (this.request == that.request)  &&
                (this.response == that.response)
        );

    }

    @Override
    public int hashCode() {
        int result = 73;
        result += lastAccessedTime + (request ==null?0: request.hashCode()) + (response ==null?0: response.hashCode());
        return result;
    }

}
