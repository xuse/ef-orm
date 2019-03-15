/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.accelerator.cglib.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Joey
 */
public final class CollectionsX {
    private CollectionsX() { }

    public static <K,V> Map<K,List<V>> bucket(Collection<V> c, Transformer<V,K> t) {
        Map<K,List<V>> buckets = new HashMap<>();
        for (Iterator<V> it = c.iterator(); it.hasNext();) {
            V value = it.next();
            K key = t.transform(value);
            List<V> bucket = (List<V>)buckets.get(key);
            if (bucket == null) {
                buckets.put(key, bucket = new LinkedList<>());
            }
            bucket.add(value);
        }
        return buckets;
    }

    public static <K,V> void reverse(Map<K,V> source, Map<V,K> target) {
        for (Iterator<K> it = source.keySet().iterator(); it.hasNext();) {
            K key = it.next();
            target.put(source.get(key), key);
        }
    }

    public static <E> Collection<E> filter(Collection<E> c, Predicate p) {
        Iterator<E> it = c.iterator();
        while (it.hasNext()) {
            if (!p.evaluate(it.next())) {
                it.remove();
            }
        }
        return c;
    }

    public static <E,T> List<T> transform(Collection<E> c, Transformer<E,T> t) {
        List<T> result = new ArrayList<>(c.size());
        for (Iterator<E> it = c.iterator(); it.hasNext();) {
            result.add(t.transform(it.next()));
        }
        return result;
    }

    public static <V> Map<V,Integer> getIndexMap(List<V> list) {
        Map<V,Integer> indexes = new HashMap<>();
        int index = 0;
        for (Iterator<V> it = list.iterator(); it.hasNext();) {
            indexes.put(it.next(), Integer.valueOf(index++));
        }
        return indexes;
    }
}    
    
