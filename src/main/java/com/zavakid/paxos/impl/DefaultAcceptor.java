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
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.zavakid.paxos.Accepted;
import com.zavakid.paxos.Acceptor;
import com.zavakid.paxos.Promise;
import com.zavakid.paxos.util.MDCs;

/**
 * @author zavakid 2013-4-20 下午8:29:14
 * @since 1.0
 */
public class DefaultAcceptor implements Acceptor {

    private static final Logger               LOG            = LoggerFactory.getLogger(DefaultAcceptor.class);

    private static final AtomicInteger        SEQUENCE       = new AtomicInteger();
    private static final String               NAME_PREFIX    = "acceptor_";

    private final String                      name;
    private ConcurrentHashMap<Object, Object> values         = new ConcurrentHashMap<Object, Object>();
    private ConcurrentHashMap<Object, Long>   lastestEpoches = new ConcurrentHashMap<Object, Long>();

    public DefaultAcceptor(){
        this(NAME_PREFIX + SEQUENCE.getAndIncrement());
    }

    public DefaultAcceptor(String name){
        this.name = name;
    }

    @Override
    public synchronized Promise prepare(Long epoch, Object var) {
        MDC.put(MDCs.MDC_NAME, this.name);
        LOG.info("receive prepare, epoch [{}], var [{}]", epoch, var);
        Long preEpoch = lastestEpoches.get(var);
        if (preEpoch == null) {
            LOG.info("ACK prepare for var [{}],epoch [{}],  no preEpoch", var, epoch);
            lastestEpoches.put(var, epoch);
            return Promise.create(null, var, null);
        }

        Object oldValue = values.get(var);
        if (preEpoch > epoch) {
            LOG.info("NAK prepare fro var [{}], preEpoch [{}] is greater than epoch [{}]", var, preEpoch, epoch);
            return Promise.create(preEpoch, var, oldValue, true);
        }

        LOG.info("ACK accept for var [{}], epoch [{}], preEpoch [{}]", var, epoch, preEpoch);
        lastestEpoches.put(var, epoch);
        return Promise.create(preEpoch, var, oldValue);
    }

    @Override
    public synchronized Accepted accept(Long epoch, Object var, Object value) {
        MDC.put(MDCs.MDC_NAME, this.name);
        LOG.info("receive accept, epoch [{}], var [{}], value [{}]", epoch, var, value);
        Long preEpoch = lastestEpoches.get(var);
        if (preEpoch == null) {
            String msg = "I(the Acceptor) need an epoch before I can accept";
            LOG.error(msg);
            throw new IllegalStateException(msg);
        }

        Object oldValue = values.get(var);
        if (preEpoch > epoch) {
            LOG.info("NAK accept for var [{}], preEpoch is greater! current : (epoch [{}],value [{}]), pre (repEpoch [{}], preValue [{}] )",
                var,
                epoch,
                value,
                preEpoch,
                oldValue);
            return Accepted.create(preEpoch, var, oldValue, true);
        }

        LOG.info("ACK accept for var [{}], current : (epoch [{}],value [{}]), pre (repEpoch [{}], preValue [{}] )",
            var,
            epoch,
            value,
            preEpoch,
            oldValue);
        values.put(var, value);
        return Accepted.create(epoch, var, value);

    }
}
