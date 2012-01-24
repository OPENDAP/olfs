/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
// Author: Nathan David Potter  <ndp@opendap.org>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////
package opendap.namespaces;

import org.jdom.Namespace;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Feb 19, 2009
 * Time: 11:01:23 AM
 * To change this template use File | Settings | File Templates.
 */
public class BES {

    public static final String BES_NAMESPACE_STRING = "http://xml.opendap.org/ns/bes/1.0#";
    public static final Namespace BES_NS =  Namespace.getNamespace("bes", BES_NAMESPACE_STRING);
    
    public static final String BES_ADMIN_NAMESPACE_STRING = "http://xml.opendap.org/ns/bes/admin/1.0#";
    public static final Namespace BES_ADMIN_NS =  Namespace.getNamespace("bai", BES_ADMIN_NAMESPACE_STRING);

}
