package opendap.bes;

import org.jdom.Element;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 5/27/11
 * Time: 11:39 AM
 * To change this template use File | Settings | File Templates.
 */
public class BesConfigurationModule {

    private String name;

    private Element _config;


    BesConfigurationModule(Element config){
        _config = (Element) config.clone();

        name =_config.getAttributeValue("module");

        if(name==null){
            name = "Unnamed";
        }


    }


    public String getName(){
        return name;

    }

    public String getShortName(){

        String sName = name;

        if(sName.endsWith(".conf")){
            sName = sName.substring(0,sName.lastIndexOf(".conf"));
        }

        return sName;

    }

    public void setName(String s){
        name = s;
    }


    public String getConfig(){
        return _config.getText();
    }

    public void setConfig(String config){
        _config.setText(config);
    }


    public Element getConfigElement(){
        return (Element) _config.clone();
    }


}
