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
package com.zavakid.paxos.impl;

import java.util.concurrent.ConcurrentHashMap;

import com.zavakid.paxos.Accepted;
import com.zavakid.paxos.Acceptor;
import com.zavakid.paxos.Promise;

/**
 * @author zavakid 2013-4-20 下午8:29:14
 * @since 1.0
 */
public class DefaultAcceptor implements Acceptor {

    private ConcurrentHashMap<Object, Object> values         = new ConcurrentHashMap<Object, Object>();
    private ConcurrentHashMap<Object, Long>   lastestEpoches = new ConcurrentHashMap<Object, Long>();

    @Override
    public Promise prepare(Long epoch, Object var) {
        Long preEpoch = lastestEpoches.get(var);

        if (preEpoch == null) {
            lastestEpoches.put(var, epoch);
            return Promise.create(null, var, null);
        }

        Object oldValue = values.get(var);
        if (preEpoch > epoch) {
            // return a NAK to proposer
            return Promise.create(preEpoch, var, oldValue, true);
        }

        lastestEpoches.put(var, epoch);
        return Promise.create(preEpoch, var, oldValue);
    }

    @Override
    public Accepted accept(Long epoch, Object var, Object value) {
        Long preEpoch = lastestEpoches.get(var);
        if (preEpoch == null) {
            throw new IllegalStateException("I(the Acceptor) need an epoch before I can accept");
        }

        Object oldValue = values.get(var);
        if (preEpoch > epoch) {
            return Accepted.create(preEpoch, var, oldValue, true);
        }

        values.put(var, value);
        return Accepted.create(epoch, var, value);

    }
}
