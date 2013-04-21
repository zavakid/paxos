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
package com.zavakid.paxos;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;

import com.zavakid.paxos.util.Asserts;

/**
 * @author zavakid 2013-4-21 上午11:15:40
 * @since 1.0
 */
public class ChooseMasterTest {

    private static final int   DEFAULT_THREAD = 10;
    private Quorum             quorum         = null;
    private ThreadPoolExecutor executor;

    @Test
    public void test_seq_1Proposer_1Acceptor() {
        quorum = QuorumFactory.create(1, 1);
        Proposer proposer = quorum.getProposers().get(0);

        Object master1 = proposer.propose("master", "node_1");
        Object master2 = proposer.propose("master", "node_2");
        assertEquals(master1, master2);
    }

    @Test
    public void test_seq_1Proposer_3Acceptor() {
        quorum = QuorumFactory.create(3, 1);
        Proposer proposer = quorum.getProposers().get(0);

        Object master1 = proposer.propose("master", "node_1");
        Object master2 = proposer.propose("master", "node_2");

        assertEquals(master1, master2);
    }

    @Test
    public void test_seq_2Proposer_3Acceptor() {
        quorum = QuorumFactory.create(3, 1);
        Proposer proposer1 = quorum.getProposers().get(0);
        Proposer proposer2 = quorum.getProposers().get(0);

        Object master1 = proposer1.propose("master", "node_1");
        Object master2 = proposer1.propose("master", "node_2");
        Object master3 = proposer2.propose("master", "node_3");
        Object master4 = proposer2.propose("master", "node_4");

        Asserts.equals(master1, master2, master3, master4);
    }

    @Test
    public void test_concurrent_1Proposer_1Acceptor() throws InterruptedException, ExecutionException {
        quorum = QuorumFactory.create(1, 1);
        CompletionService executor = createCompletionService();

        Proposer proposer1 = quorum.getProposers().get(0);
        executor.submit(new ProposorCall(proposer1, "master", "node_1"));
        executor.submit(new ProposorCall(proposer1, "master", "node_2"));
        executor.submit(new ProposorCall(proposer1, "master", "node_3"));

        ArrayList<Object> results = new ArrayList<Object>();
        for (int i = 0; i < 3; i++) {
            Future<Object> future = executor.take();
            Object result = future.get();
            results.add(result);
        }

        for (Object o : results) {
            System.out.println(o);
        }
        Asserts.equals(results.toArray());
    }

    public CompletionService createCompletionService() {
        this.executor = new ThreadPoolExecutor(DEFAULT_THREAD,
            DEFAULT_THREAD,
            0,
            TimeUnit.MICROSECONDS,
            new ArrayBlockingQueue<Runnable>(DEFAULT_THREAD),
            new CallerRunsPolicy());

        return new ExecutorCompletionService<Object>(this.executor);
    }

    // ======== after =======
    @After
    public void after() {
        if (quorum != null) {
            quorum.stop();
        }

        if (this.executor != null) {
            this.executor.shutdown();
            try {
                this.executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class ProposorCall implements Callable<Object> {

        private Proposer proposer;
        private Object   var;
        private Object   value;

        public ProposorCall(Proposer proposer, Object var, Object value){
            this.proposer = proposer;
            this.var = var;
            this.value = value;
        }

        @Override
        public Object call() throws Exception {
            Object actual = this.proposer.propose(var, value);
            return actual;
        }

    }
}
