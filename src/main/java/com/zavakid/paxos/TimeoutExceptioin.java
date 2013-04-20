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
 * @author zavakid 2013-4-20 下午8:30:01
 * @since 1.0
 */
public class TimeoutExceptioin extends RuntimeException {

    private static final long serialVersionUID = -6739844097616845965L;

    public TimeoutExceptioin(){
        super();
    }

    /**
     * @param message
     * @param cause
     */
    public TimeoutExceptioin(String message, Throwable cause){
        super(message, cause);
    }

    /**
     * @param message
     */
    public TimeoutExceptioin(String message){
        super(message);
    }

    /**
     * @param cause
     */
    public TimeoutExceptioin(Throwable cause){
        super(cause);
    }

}
