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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.zavakid.paxos.impl.DefaultAcceptor;
import com.zavakid.paxos.impl.DefaultProposer;

/**
 * @author zavakid 2013-4-21 上午11:02:16
 * @since 1.0
 */
public abstract class QuorumFactory {

    private static final int DEFAULT_THREAD = 5;

    public static Quorum create(int acceptorNum, int proposerNum) {
        Set<Acceptor> acceptors = new HashSet<Acceptor>(acceptorNum);
        for (int i = 0; i < acceptorNum; i++) {
            acceptors.add(new DefaultAcceptor());
        }

        List<Proposer> proposers = new ArrayList<Proposer>(proposerNum);
        for (int i = 0; i < proposerNum; i++) {
            proposers.add(new DefaultProposer(acceptors, i, proposerNum, DEFAULT_THREAD));
        }

        return Quorum.create(acceptors, proposers);
    }

}
