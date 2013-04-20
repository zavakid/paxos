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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.zavakid.paxos.Accepted;
import com.zavakid.paxos.Acceptor;
import com.zavakid.paxos.Promise;
import com.zavakid.paxos.Proposer;
import com.zavakid.paxos.util.Counter;

/**
 * @author zavakid 2013-4-20 下午9:18:11
 * @since 1.0
 */
public class DefaultProposer implements Proposer {

    private Set<Acceptor> acceptors = new HashSet<Acceptor>();
    private int           majorityAcceptorNum;
    private Long          proposerId;
    private Integer       proposerNum;
    private Executor      executor;

    public DefaultProposer(Set<Acceptor> acceptors, Long proposerId, Integer proposerNum, int threads){
        this.acceptors = acceptors;
        this.majorityAcceptorNum = acceptors.size() / 2 + 1;
        this.proposerId = proposerId;
        this.proposerNum = proposerNum;
        this.executor = Executors.newFixedThreadPool(threads);
    }

    public Object propose(final Object var, final Object value) {
        final Long epoch = generateEpoch(0L, var);
        return propose(epoch, var, value);
    }

    public Object propose(final Long epoch, final Object var, final Object value) {

        List<Promise> promises = concurrentPrepare(var, epoch);
        // TODO 如果得到的 promises 数量不够一半，则需要进行下一轮 prepare

        int nakPromisesNum = 0;
        Long maxEpoch = epoch;
        int firstPromises = 0;
        int maybeHasValuePromises = 0;
        Counter<Object> values = new Counter<Object>();
        for (Promise promise : promises) {
            // acceptor 的 epoch 比此 epoch 要大
            if (promise.isNAK()) {
                nakPromisesNum++;
                maxEpoch = maxEpoch > promise.getPreEpoch() ? maxEpoch : promise.getPreEpoch();
                continue;
            }
            // acceptor 没有 epoch
            if (promise.getPreEpoch() == null) {
                firstPromises++;
                continue;
            }
            // acceptor 的 epoch 比此 epoch 要小或者相等
            if (promise.getPreEpoch() <= epoch) {
                maybeHasValuePromises++;
                values.add(promise.getValue());
                continue;
            }

            throw new IllegalStateException("unreachable code");
        }

        if (nakPromisesNum >= this.majorityAcceptorNum) {
            return nextRound(maxEpoch, var, value);
        }

        // prepare success, and we can continue phase 2
        if (firstPromises >= this.majorityAcceptorNum) {
            List<Accepted> accepteds = concurrentCommit(epoch, var, value);
            // TODO analytic the accepted
        }

        if (maybeHasValuePromises >= this.majorityAcceptorNum
            || (firstPromises + maybeHasValuePromises) >= this.majorityAcceptorNum) {
            Long newEpoch = generateEpoch(maxEpoch, var);
            Object newValue = values.getMostItem();
            if (newValue == null) {
                newValue = value;
            }
            List<Accepted> accepteds = concurrentCommit(newEpoch, var, newValue);
            // TODO analytic the accepted
        }

        throw new IllegalStateException("unreachable code");

    }

    protected List<Promise> concurrentPrepare(final Object var, final Long epoch) {
        CompletionService<Promise> completionService = new ExecutorCompletionService<Promise>(this.executor);
        for (final Acceptor acceptor : acceptors) {
            completionService.submit(new Callable<Promise>() {

                @Override
                public Promise call() throws Exception {
                    return acceptor.prepare(epoch, var);
                }
            });
        }

        List<Promise> promises = new ArrayList<Promise>(acceptors.size());
        for (int i = 0; i < acceptors.size(); i++) {
            Future<Promise> future = null;
            try {
                future = completionService.take();
                Promise promise = future.get();
                promises.add(promise);
            } catch (InterruptedException e) {
                throw new RuntimeException("propose (phase 1) was interrupted", e);
            } catch (ExecutionException e) {
                processExecutionExeception(e);
            }
        }
        return promises;
    }

    protected List<Accepted> concurrentCommit(final Long epoch, final Object var, final Object value) {
        CompletionService<Accepted> completionService = new ExecutorCompletionService<Accepted>(this.executor);
        for (final Acceptor acceptor : acceptors) {
            completionService.submit(new Callable<Accepted>() {

                @Override
                public Accepted call() throws Exception {
                    return acceptor.accept(epoch, var, value);
                }
            });
        }

        List<Accepted> accepteds = new ArrayList<Accepted>(acceptors.size());
        for (int i = 0; i < acceptors.size(); i++) {
            Future<Accepted> future = null;
            try {
                future = completionService.take();
                Accepted accepted = future.get();
                accepteds.add(accepted);
            } catch (InterruptedException e) {
                throw new RuntimeException("propose (phase 2) was interrupted", e);
            } catch (ExecutionException e) {
                processExecutionExeception(e);
            }
        }
        return accepteds;
    }

    protected Object nextRound(Long maxEpoch, Object var, Object value) {
        Long newEpoch = generateEpoch(maxEpoch, var);
        return propose(newEpoch, var, value);
    }

    protected void processExecutionExeception(ExecutionException e) {
        // no op
    }

    private Long generateEpoch(Long preEpoch, Object var) {
        return (preEpoch / this.proposerNum) * proposerNum + this.proposerId;
    }

}
