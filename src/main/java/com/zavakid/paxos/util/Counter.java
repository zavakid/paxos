/*
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.zavakid.paxos.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author zavakid 2013-4-20 下午10:02:47
 * @since 1.0
 */
public class Counter<T> {

    private Map<T, Long> store = new HashMap<T, Long>();

    public synchronized void add(T t) {
        Long value = store.get(t);
        if (value == null) {
            store.put(t, 1L);
            return;
        }
        store.put(t, ++value);
    }

    public Long get(T t) {
        return store.get(t);
    }

    public Set<T> items() {
        return Collections.unmodifiableSet(store.keySet());
    }

    public Collection<Long> values() {
        return Collections.unmodifiableCollection(store.values());
    }

    public T getMostItem() {

        Long maxKey = -1L;
        T mostItem = null;
        for (Entry<T, Long> entry : this.store.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            if (entry.getValue() > maxKey) {
                maxKey = entry.getValue();
                mostItem = entry.getKey();
            }
        }
        return mostItem;
    }
}
