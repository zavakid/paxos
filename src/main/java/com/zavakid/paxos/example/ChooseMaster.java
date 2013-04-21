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
package com.zavakid.paxos.example;

import java.util.HashSet;
import java.util.Set;

import com.zavakid.paxos.Acceptor;
import com.zavakid.paxos.Proposer;
import com.zavakid.paxos.impl.DefaultAcceptor;
import com.zavakid.paxos.impl.DefaultProposer;

/**
 * @author zavakid 2013-4-21 上午10:00:16
 * @since 1.0
 */
public class ChooseMaster {

    public static void main(String[] args) {
        Set<Acceptor> acceptors = new HashSet<Acceptor>();
        acceptors.add(new DefaultAcceptor());
        acceptors.add(new DefaultAcceptor());
        acceptors.add(new DefaultAcceptor());

        Proposer proposer1 = new DefaultProposer(acceptors, 1, acceptors.size(), 5);
        Proposer proposer2 = new DefaultProposer(acceptors, 2, acceptors.size(), 5);
        Proposer proposer3 = new DefaultProposer(acceptors, 3, acceptors.size(), 5);

        Object value2 = proposer2.propose("master", "node_2");
        System.out.println(value2);

        Object value = proposer1.propose("master", "node_1");
        System.out.println(value);

        Object value3 = proposer3.propose("master", "node_3");
        System.out.println(value3);

        proposer1.stop();
        proposer2.stop();
        proposer3.stop();
    }
}
