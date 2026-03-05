/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2017 OPeNDAP, Inc.
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

package opendap.wcs.v2_0.http;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A Map implementation that makes String keys case insensitive.
 *
 * User: ndp
 * Date: 11/7/12
 * Time: 9:20 AM
 */
public class KeyCaseInsensitiveMap<K,V> implements Map<K, V> {

    HashMap<K, V> map;

    public KeyCaseInsensitiveMap(){
        super();
         map = new HashMap<K, V>();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {

        if(key!=null && key instanceof String){
            String keyString = (String) key;
            return map.containsKey(keyString.toLowerCase());
        }
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {

        if(key!=null && key instanceof String){
            String keyString = (String) key;
            return map.get(keyString.toLowerCase());
        }
        return map.get(key);
    }

    @Override
    public V put(K key, V value) {

        if(key!=null && key instanceof String ){
            String keyString = (String) key;
            keyString = keyString.toLowerCase();

            // Why a warning for this next bit? Unchecked cast 'java.lang.String' to 'K'
            return map.put((K) keyString, value);
        }

        return map.put(key,value);
    }

    @Override
    public V remove(Object key) {

        if(key!=null && key instanceof String){
            String keyString = (String) key;
            return map.remove(keyString.toLowerCase());
        }
        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {

        map.putAll(m);

    }

    @Override
    public void clear() {
        map.clear();

    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }
}
