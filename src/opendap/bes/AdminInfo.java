/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2018 OPeNDAP, Inc.
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
package opendap.bes;

import opendap.bes.dap2Responders.BesApi;
import opendap.ppt.PPTException;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import static opendap.bes.dap2Responders.BesApi.BES_SERVER_ADMINISTRATOR_KEY;

/**
 * A class to wrap the BES.ServerAdministrator object. In the BES the object is defined like this:
 * <pre>
 *      BES.ServerAdministrator=email:support@opendap.org
 *      BES.ServerAdministrator+=organization:OPeNDAP Inc.
 *      BES.ServerAdministrator+=street:165 NW Dean Knauss Dr.
 *      BES.ServerAdministrator+=city:Narragansett
 *      BES.ServerAdministrator+=region:RI
 *      BES.ServerAdministrator+=postalCode:02882
 *      BES.ServerAdministrator+=country:US
 *      BES.ServerAdministrator+=telephone:+1.401.575.4835
 *      BES.ServerAdministrator+=website:http://www.opendap.org
 * </pre>
 * Where each rValue is a key:value pair wher the key is al of the characters before the first colon ":" and the
 * value is all of the characters after the colon ":".
 * If one or more of these fields is missing or un-parsable then the default AdminInfo (see above) will be utilized.
 *
 */
public class AdminInfo {

    private Logger log;

    private String _organization;
    private String _country;
    private String _city;
    private String _street;
    private String _region;
    private String _postalCode;
    private String _telephone;
    private String _email;
    private String _website;

    public static final String EMAIL_KEY = "email";
    public static final String EMAIL_DEFAULT = "support@opendap.org";

    public static final String ORGANIZATION_KEY = "organization";
    public static final String ORGANIZATION_DEFAULT = "OPeNDAP Inc.";

    public static final String STREET_KEY = "street";
    public static final String STREET_DEFAULT = "165 NW Dean Knauss Dr.";

    public static final String CITY_KEY = "city";
    public static final String CITY_DEFAULT = "Narragansett";

    public static final String REGION_KEY = "region";
    public static final String REGION_DEFAULT = "RI";

    public static final String POSTAL_CODE_KEY = "postalcode";
    public static final String POSTAL_CODE_DEFAULT = "02882";

    public static final String COUNTRY_KEY = "country";
    public static final String COUNTRY_DEFAULT = "US";

    public static final String TELEPHONE_KEY = "telephone";
    public static final String TELEPHONE_DEFAULT = "+1.401.575.4835";

    public static final String WEBSITE_KEY = "website";
    public static final String WEBSITE_DEFAULT = "http://www.opendap.org";


    /**
     * Makes a default ServerAdmin object that holds the information for OPeNDAP Inc.
     */
    public AdminInfo(){
        log = LoggerFactory.getLogger(this.getClass());
        _organization = ORGANIZATION_DEFAULT;
        _country      = COUNTRY_DEFAULT;
        _city         = CITY_DEFAULT;
        _street       = STREET_DEFAULT;
        _region       = REGION_DEFAULT;
        _postalCode   = POSTAL_CODE_DEFAULT;
        _telephone    = TELEPHONE_DEFAULT;
        _email        = EMAIL_DEFAULT;
        _website      = WEBSITE_DEFAULT;
    }

    /**
     * Copy Constructor
     * @param adminInfo
     */
    public AdminInfo(AdminInfo adminInfo){
        this();
        _organization = adminInfo._organization;
        _country      = adminInfo._country;
        _city         = adminInfo._city;
        _street       = adminInfo._street;
        _region       = adminInfo._region;
        _postalCode   = adminInfo._postalCode;
        _telephone    = adminInfo._telephone;
        _email        = adminInfo._email;
        _website      = adminInfo._website;
    }


    public AdminInfo(Map<String,String> adminInfo){
        this();
        ingestAdminInfoMap(adminInfo);
    }


    private  void ingestAdminInfoMap(Map<String,String> adminInfo){
        String organization = adminInfo.get(ORGANIZATION_KEY);
        String country = adminInfo.get(COUNTRY_KEY);
        String city = adminInfo.get(CITY_KEY);
        String street = adminInfo.get(STREET_KEY);
        String region = adminInfo.get(REGION_KEY);
        String postalCode = adminInfo.get(POSTAL_CODE_KEY);
        String telephone = adminInfo.get(TELEPHONE_KEY);
        String email = adminInfo.get(EMAIL_KEY);
        String website = adminInfo.get(WEBSITE_KEY);

        // %TODO This is a pretty simple (and brutal) qc in that any missing value prompts all of it to be rejected. Review. Fix?
        if(organization==null || country==null || city==null || street==null || region==null
                || postalCode==null || telephone==null || email==null || website==null){
            log.error("The provided Map does not contain the required values for AdminInfo! Using defaults.");
            return; // Use the default values.
        }
        _organization = organization;
        _country      = country;
        _city         = city;
        _street       = street;
        _region       = region;
        _postalCode   = postalCode;
        _telephone    = telephone;
        _email        = email;
        _website      = website;
    }


    /**
     *
     * The output if this method depends on configuration content and NOT
     * on user submitted inputs.
     *
     *  <pre>
     *      "publisher": {
     *          "@type": "Organization",
     *          "name": "@PublisherName@",
     *          "address": {
     *              "@type": "PostalAddress",
     *              "addressCountry": "@Country@",
     *              "addressLocality": "@Street,City@",
     *              "addressRegion": "@State@",
     *              "postalCode": "@PostalCode@"
     *          },
     *          "telephone": "@PublisherPhoneNumber@",
     *          "email": "@PublisherEmail@",
     *          "sameAs": "@OrganizationLandingPageURL@"
     *      }
     *  </pre>
     *
     * @return THe JSON-LD Publisher representation of the AdminInfo object.
     **/
    public String getAsJsonLdPublisher() {
        String addressLocality = _street + ", " + _city;
        StringBuilder sb = new StringBuilder();
        sb.append("\"publisher\": {\n");
        sb.append("    \"@type\": \"Organization\",\n");
        sb.append("    \"name\": \"").append(_organization).append("\",\n");
        sb.append("    \"address\": {\n");
        sb.append("        \"@type\": \"PostalAddress\",\n");
        sb.append("        \"addressCountry\": \"").append(_country).append("\",\n");
        sb.append("        \"addressLocality\": \"").append(addressLocality).append("\",\n");
        sb.append("        \"addressRegion\": \"").append(_region).append("\",\n");
        sb.append("        \"postalCode\": \"").append(_postalCode).append("\"\n");
        sb.append("      },\n");
        sb.append("    \"telephone\": \"").append(_telephone).append("\",\n");
        sb.append("    \"email\": \"").append(_email).append("\",\n");
        sb.append("    \"sameAs\": \"").append(_website).append("\"\n");
        sb.append("}\n");
        return sb.toString();
    }

}
