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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.zavakid.paxos.Accepted;
import com.zavakid.paxos.Acceptor;
import com.zavakid.paxos.Promise;
import com.zavakid.paxos.Proposer;
import com.zavakid.paxos.util.Asserts;
import com.zavakid.paxos.util.Counter;
import com.zavakid.paxos.util.MDCs;

/**
 * @author zavakid 2013-4-20 下午9:18:11
 * @since 1.0
 */
public class DefaultProposer implements Proposer {

    private static final Logger LOG         = LoggerFactory.getLogger(DefaultProposer.class);
    private static final String NAME_PREFIX = "proposer_";
    private AtomicLong          EPOCH_SEQ   = new AtomicLong();
    private AtomicLong          LAST_EPOCH  = new AtomicLong(-1L);

    private String              name;
    private Set<Acceptor>       acceptors   = new HashSet<Acceptor>();
    private int                 majorityAcceptorNum;
    private Long                proposerId;
    private Integer             proposerNum;
    private ThreadPoolExecutor  executor;

    public DefaultProposer(Set<Acceptor> acceptors, long proposerId, int proposerNum, int threads){
        this.name = NAME_PREFIX + proposerId;
        this.acceptors = acceptors;
        this.majorityAcceptorNum = acceptors.size() / 2 + 1;
        this.proposerId = proposerId;
        this.proposerNum = proposerNum;
        this.executor = new ThreadPoolExecutor(threads,
            threads,
            0L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(threads),
            new CallerRunsPolicy());
    }

    public Object propose(final Object var, final Object value) {
        final Long epoch = generateEpoch(0L, var);
        return proposeWithEpoch(epoch, var, value);
    }

    public Object proposeWithEpoch(final Long epoch, final Object var, final Object value) {
        MDC.put(MDCs.MDC_NAME, this.name);
        LOG.info("start to send prepare (epoch[{}], var [{}], value [{}])", epoch, var, value);
        List<Promise> promises = concurrentPrepare(var, epoch);
        // 有可能是网络出现中断引起的
        if (promises.size() < this.majorityAcceptorNum) {
            LOG.info("receive promises num [{}] , less then majorityAcceptorNum [{}], try to propose again",
                promises.size(),
                this.majorityAcceptorNum);
            proposeWithEpoch(epoch, var, value);
        }

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

            Asserts.unreachable();
        }

        LOG.info("prepare (epoch [{}], var [{}], value [{}]) status  : nakPromisesNum [{}], maxEpoch [{}], firstPromises [{}], maybeHasValuePromises [{}], this.majorityAcceptorNum [{}]",
            epoch,
            var,
            value,
            nakPromisesNum,
            maxEpoch,
            firstPromises,
            maybeHasValuePromises,
            this.majorityAcceptorNum);
        if (nakPromisesNum >= this.majorityAcceptorNum) {
            LOG.info("prepare fail for var [{}], value [{}], nakPromisesNum [{}] is greater then majorityAcceptorNum [{}], the max preEpoch is [{}]",
                var,
                value,
                nakPromisesNum,
                majorityAcceptorNum,
                maxEpoch);
            return nextRound(maxEpoch, var, value);
        }

        // prepare success, and we can continue phase 2
        if (firstPromises >= this.majorityAcceptorNum) {
            LOG.info("prepare success for var [{}], value [{}], firstPromises [{}] is greater then majorityAcceptorNum [{}], try accept with epoch [{}]",
                var,
                value,
                firstPromises,
                this.majorityAcceptorNum,
                epoch);
            return tryAccept(epoch, var, value);
        }

        if (maybeHasValuePromises >= this.majorityAcceptorNum
            || (firstPromises + maybeHasValuePromises) >= this.majorityAcceptorNum) {
            Object newValue = values.getMostItem();
            if (newValue == null) {
                newValue = value;
            }

            LOG.info("prepare success for var [{}], epoch [{}], we can try accept with new value [{}]",
                var,
                epoch,
                newValue);
            return tryAccept(epoch, var, newValue);
        }

        return nextRound(maxEpoch, var, value);

    }

    public Object tryAccept(final Long epoch, final Object var, final Object value) {
        List<Accepted> accepteds = concurrentCommit(epoch, var, value);
        // 网络中断，重新发起一次
        if (accepteds.size() < this.majorityAcceptorNum) {
            return proposeWithEpoch(epoch, var, value);
        }

        int nakAcceptedNum = 0;
        int successAcceptedNum = 0;
        Long maxEpochWhenAccepted = epoch;
        Counter<Object> oldAcceptedValues = new Counter<Object>();
        for (Accepted accepted : accepteds) {
            // 说明别的 Proposer 预约了更新的 epoch
            if (accepted.isNAK()) {
                nakAcceptedNum++;
                maxEpochWhenAccepted = maxEpochWhenAccepted > accepted.getEpoch() ? maxEpochWhenAccepted : accepted.getEpoch();
                oldAcceptedValues.add(accepted.getValue());
                continue;
            }

            // 成功占领
            if (!accepted.isNAK()) {
                successAcceptedNum++;
                oldAcceptedValues.add(accepted.getValue());
                continue;
            }
            Asserts.unreachable();
        }

        LOG.info("accept (epoch [{}], var [{}], value [{}]) status : nakAcceptedNum [{}], successAcceptedNum [{}], maxEpochWhenAccepted [{}], majorityAcceptorNum [{}]",
            epoch,
            var,
            value,
            nakAcceptedNum,
            successAcceptedNum,
            maxEpochWhenAccepted,
            this.majorityAcceptorNum);

        if (nakAcceptedNum >= this.majorityAcceptorNum) {
            LOG.info("NAK accept, epoch [{}], var [{}], value[{}], the maxEpochWhenAccepted is [{}]",
                epoch,
                var,
                value,
                maxEpochWhenAccepted);
            return nextRound(maxEpochWhenAccepted, var, value);
        }

        if (successAcceptedNum >= this.majorityAcceptorNum) {
            LOG.info("ACK accept, epoch [{}], var [{}], value[{}]", epoch, var, value);
            return oldAcceptedValues.getMostItem();
        }

        // 跑到这里，有可能是有偶数个 acceptor，并且 nakAcceptedNum == successAcceptedNum
        // 所以认为没有超过一半 acceptor 同意，因此需要进行下一轮
        return nextRound(maxEpochWhenAccepted, var, value);
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
        return proposeWithEpoch(newEpoch, var, value);
    }

    protected void processExecutionExeception(ExecutionException e) {
        // TODO
        e.printStackTrace();
    }

    /**
     * preEpoch 可能是从 EPOCH_SEQ.getAndIncrement(); 获得<br/>
     * 也可能是从上次 prepare/acceptor 中获得的最大 epoch <br/>
     */
    synchronized protected Long generateEpoch(Long preEpoch, Object var) {
        long lastEpoch = this.EPOCH_SEQ.get();
        if (preEpoch > lastEpoch) {
            long epoch = (preEpoch / this.proposerNum + 1) * this.proposerNum + this.proposerId;
            this.EPOCH_SEQ.set(epoch);
            return epoch;
        } else {
            long epoch = ((lastEpoch / this.proposerNum + 1) * this.proposerNum) + this.proposerId;
            this.EPOCH_SEQ.set(epoch);
            return epoch;
        }
    }

    @Override
    public void stop() {
        this.executor.shutdown();
        try {
            this.executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // TODO
            e.printStackTrace();
        }
    }
}
