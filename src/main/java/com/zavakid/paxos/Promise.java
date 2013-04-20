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
 * @author zavakid 2013-4-20 下午8:33:06
 * @since 1.0
 */
public class Promise implements NAKAble {

    private Long    preEpoch;
    private Object  var;
    private Object  value;
    private boolean NAK;

    public static Promise create(Long preEpoch, Object var, Object value) {
        return create(preEpoch, var, value, false);
    }

    public static Promise create(Long preEpoch, Object var, Object value, boolean isNAK) {
        return new Promise(preEpoch, var, value, false);
    }

    public Promise(Long preEpoch, Object var, Object value, boolean isNAK){
        this.preEpoch = preEpoch;
        this.var = var;
        this.value = value;
        this.NAK = isNAK;
    }

    public Long getPreEpoch() {
        return preEpoch;
    }

    public Object getVar() {
        return var;
    }

    public Object getValue() {
        return value;
    }

    /**
     * 是否是 negative acknowledge，表示此 Promise 是否定的，Proposer 提交的 epoch 太低了
     * 
     * @return
     */
    public boolean isNAK() {
        return NAK;
    }

}
