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

/**
 * @author zavakid 2013-4-20 下午8:27:51
 * @since 1.0
 */
public class PaxosException extends RuntimeException {

    private static final long serialVersionUID = -3012258423468509432L;

    public PaxosException(){
        super();
    }

    /**
     * @param message
     * @param cause
     */
    public PaxosException(String message, Throwable cause){
        super(message, cause);
    }

    /**
     * @param message
     */
    public PaxosException(String message){
        super(message);
    }

    /**
     * @param cause
     */
    public PaxosException(Throwable cause){
        super(cause);
    }

}
