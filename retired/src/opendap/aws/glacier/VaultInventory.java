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

package opendap.aws.glacier;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 10/16/13
 * Time: 1:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class VaultInventory {

    private   String _vaultARN;
    private String _inventoryDate;
    private GlacierArchive[] _archiveList;



    @JsonProperty("VaultARN")
    public String getARN(){
        return _vaultARN;
    }
    public void setARN(String vaultARN){
        _vaultARN = vaultARN;
    }



    @JsonProperty("InventoryDate")
    public String getInventoryDate(){
        return _inventoryDate;
    }
    public void setInventoryDate(String date){
        _inventoryDate = date;
    }

    @JsonProperty("ArchiveList")
    public GlacierArchive[] getArchiveList(){
        return _archiveList;
    }
    public void setArchiveList(GlacierArchive[] archiveList){
        _archiveList = archiveList;
    }


    public static void main(String[] args)  {


        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationConfig.Feature.INDENT_OUTPUT);


            VaultInventory vaultInventory = mapper.readValue(new File("vault-inventory.json"), VaultInventory.class);

            System.out.println(mapper.writeValueAsString(vaultInventory));



        }
        catch (Exception e){
            e.printStackTrace();
        }


    }



}

