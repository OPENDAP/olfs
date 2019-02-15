/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2013 OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
 * //
 * // This library is free software; you can redistribute it and/or
 * // modify it under the terms of the GNU Lesser General Public
 * // License as published by the Free Software Foundation; either
 * // version 2.1 of the License, or (at your option) any later version.
 * //
 * // This library is distributed in the hope that it will be useful,
 * // but WITHOUT ANY WARRANTY; without even the implied warranty of
 * // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * // Lesser General Public License for more details.
 * //
 * // You should have received a copy of the GNU Lesser General Public
 * // License along with this library; if not, write to the Free Software
 * // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */
package opendap.wcs.v1_1_2;

import org.jdom.Attribute;
import org.jdom.Element;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Feb 9, 2009
 * Time: 8:32:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class XLink {

    public static final String TYPE = "type";
    public static final String HREF = "href";
    public static final String ROLE = "role";
    public static final String ARCROLE = "arcrole";
    public static final String TITLE = "title";
    public static final String SHOW = "show";
    public static final String ACTUATE = "actuate";
    public static final String LABEL = "label";
    public static final String FROM = "from";
    public static final String TO = "to";
    private Vector<Attribute> attributes = new Vector<>();


    private  static final HashSet<String> showValues = new HashSet<>();
    static {
        showValues.add("new");
        showValues.add("replace");
        showValues.add("embed");
        showValues.add("other");
        showValues.add("none");
    }

    private static final HashSet<String> actuateValues = new HashSet<>();
    static {
        showValues.add("onLoad");
        showValues.add("onRequest");
        showValues.add("other");
        showValues.add("none");
    }

    public enum Type {SIMPLE , EXTENDED, LOCATOR, ARC, RESOURCE, TITLE, EMPTY}


    public XLink(Element e) throws WcsException {

        Iterator i = e.getAttributes().iterator();


        Attribute a;
        while(i.hasNext()){
            a = (Attribute)i.next();

            if(a.getNamespace().equals(WCS.XLINK_NS)){
                attributes.add(a);
            }
        }
    }

    public XLink(Type type,
                 String href,
                 String role,
                 String arcrole,
                 String title,
                 String show,
                 String actuate,
                 String label,
                 String from,
                 String to)
            throws WcsException {

        URI hrefURI = null;
        URI roleURI = null;
        URI arcroleURI = null;

        if(href!=null){
            try {
                hrefURI = new URI(href);
            } catch (URISyntaxException e) {
                throw new WcsException("Invalid URI syntax for xlink:href'",
                        WcsException.INVALID_PARAMETER_VALUE,"xlink:href");
            }
        }

        if(role!=null){
            try {
                roleURI = new URI(role);
            } catch (URISyntaxException e) {
                throw new WcsException("Invalid URI syntax for xlink:role'",
                        WcsException.INVALID_PARAMETER_VALUE,"xlink:role");
            }

        }
        if(arcrole!=null){
            try {
                arcroleURI = new URI(arcrole);
            } catch (URISyntaxException e) {
                throw new WcsException("Invalid URI syntax for xlink:arcrole'",
                        WcsException.INVALID_PARAMETER_VALUE,"xlink:arcrole");
            }
        }
        new XLink(type,hrefURI,roleURI,arcroleURI,title,show,actuate,label,from,to);


    }

    public XLink(Type type,
                 URI href,
                 URI role,
                 URI arcrole,
                 String title,
                 String show,
                 String actuate,
                 String label,
                 String from,
                 String to)
            throws WcsException {

        switch(type){
            case SIMPLE:

                if(label!=null ||  from!=null || to!=null)
                    throw new WcsException("xlink attributes not compatible with xlink:type of 'simpleLink'",
                            WcsException.INVALID_PARAMETER_VALUE,"xlink:type");

                attributes.add(new Attribute(TYPE,"simpleLink",WCS.XLINK_NS));

                if(href!=null)
                    attributes.add(new Attribute(HREF,href.toASCIIString(),WCS.XLINK_NS));

                if(role!=null)
                    attributes.add(new Attribute(ROLE,role.toASCIIString(),WCS.XLINK_NS));

                if(arcrole!=null)
                    attributes.add(new Attribute(ARCROLE,arcrole.toASCIIString(),WCS.XLINK_NS));

                if(title!=null)
                    attributes.add(new Attribute(TITLE,title,WCS.XLINK_NS));

                if(show!=null){
                    if(!showValues.contains(show))
                        throw new WcsException("The xlink:show attribute may not have a value of '"+show+"'",
                                WcsException.INVALID_PARAMETER_VALUE,"xlink:show");
                    attributes.add(new Attribute(SHOW,show,WCS.XLINK_NS));
                }

                if(actuate!=null){
                    if(!actuateValues.contains(actuate))
                        throw new WcsException("The xlink:actuate attribute may not have a value of '"+actuate+"'",
                                WcsException.INVALID_PARAMETER_VALUE,"xlink:actuate");
                    attributes.add(new Attribute(ACTUATE,actuate,WCS.XLINK_NS));
                }

                break;

            case EXTENDED:
                if(href!=null || arcrole!=null ||  show!=null || actuate!=null || label!=null ||  from!=null || to!=null)
                    throw new WcsException("xlink attributes not compatible with xlink:type of 'extendedLink'",
                            WcsException.INVALID_PARAMETER_VALUE,"xlink:extendedLink");

                attributes.add(new Attribute(TYPE,"extendedLink",WCS.XLINK_NS));

                if(role!=null)
                    attributes.add(new Attribute(ROLE,role.toASCIIString(),WCS.XLINK_NS));

                if(title!=null)
                    attributes.add(new Attribute(TITLE,title,WCS.XLINK_NS));
                break;

            case LOCATOR:

                if(href==null || arcrole!=null ||  show!=null || actuate!=null ||  from!=null || to!=null)
                    throw new WcsException("xlink attributes not compatible with xlink:type of 'locatorLink'",
                            WcsException.INVALID_PARAMETER_VALUE,"xlink:locatorLink");

                attributes.add(new Attribute(TYPE,"locatorLink",WCS.XLINK_NS));
                attributes.add(new Attribute(HREF,href.toASCIIString(),WCS.XLINK_NS));

                if(role!=null)
                    attributes.add(new Attribute(ROLE,role.toASCIIString(),WCS.XLINK_NS));

                if(title!=null)
                    attributes.add(new Attribute(TITLE,title,WCS.XLINK_NS));

                if(label!=null)
                    attributes.add(new Attribute(LABEL,label,WCS.XLINK_NS));
                break;

            case ARC:
                if(href!=null || role!=null ||  to!=null)
                    throw new WcsException("xlink attributes not compatible with xlink:type of 'arcLink'",
                            WcsException.INVALID_PARAMETER_VALUE,"xlink:arcLink");

                attributes.add(new Attribute(TYPE,"arcLink",WCS.XLINK_NS));

                if(arcrole !=null)
                    attributes.add(new Attribute(ARCROLE,arcrole.toASCIIString(),WCS.XLINK_NS));

                if(title!=null)
                    attributes.add(new Attribute(TITLE,title,WCS.XLINK_NS));



                if(show!=null){
                    if(!showValues.contains(show))
                    throw new WcsException("The xlink:show attribute may not have a value of '"+show+"'",
                            WcsException.INVALID_PARAMETER_VALUE,"xlink:show");
                    attributes.add(new Attribute(SHOW,show,WCS.XLINK_NS));
                }

                if(actuate!=null){
                    if(!actuateValues.contains(actuate))
                    throw new WcsException("The xlink:actuate attribute may not have a value of '"+actuate+"'",
                            WcsException.INVALID_PARAMETER_VALUE,"xlink:actuate");
                    attributes.add(new Attribute(ACTUATE,actuate,WCS.XLINK_NS));
                }

                if(from!=null)
                    attributes.add(new Attribute(FROM,from,WCS.XLINK_NS));

                if(label!=null)
                    attributes.add(new Attribute(LABEL,label,WCS.XLINK_NS));
                break;

            case RESOURCE:
                if(href!=null || arcrole!=null || show!=null || actuate!=null || from!=null || to!=null)
                    throw new WcsException("xlink attributes not compatible with xlink:type of 'resourceLink'",
                            WcsException.INVALID_PARAMETER_VALUE,"xlink:resourceLink");


                attributes.add(new Attribute(TYPE,"resourceLink",WCS.XLINK_NS));

                if(role!=null)
                    attributes.add(new Attribute(ROLE,role.toASCIIString(),WCS.XLINK_NS));

                if(title!=null)
                    attributes.add(new Attribute(TITLE,title,WCS.XLINK_NS));

                if(label!=null)
                    attributes.add(new Attribute(LABEL,label,WCS.XLINK_NS));

                break;

            case TITLE:
                if(href!=null || role!=null || arcrole!=null || show!=null || actuate!=null || label!=null ||  from!=null || to!=null)
                    throw new WcsException("xlink attributes not compatible with xlink:type of 'title'",
                            WcsException.INVALID_PARAMETER_VALUE,"xlink:title");
                attributes.add(new Attribute(TYPE,"title",WCS.XLINK_NS));
                break;

            case EMPTY:
                if(href!=null || role!=null || arcrole!=null || title!=null || show!=null || actuate!=null || label!=null ||  from!=null || to!=null)
                    throw new WcsException("xlink attributes not compatible with xlink:type of 'empty'",
                            WcsException.INVALID_PARAMETER_VALUE,"xlink:empty");
                attributes.add(new Attribute(TYPE,"none",WCS.XLINK_NS));
                break;

            default:
                throw new WcsException("Unkown value for xlink:type",
                        WcsException.INVALID_PARAMETER_VALUE,"xlink:type");

        }

    }



    public Vector<Attribute> getAttributes() {
        return attributes;

    }





}
