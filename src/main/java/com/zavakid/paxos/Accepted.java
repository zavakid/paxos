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
 * @author zavakid 2013-4-20 下午9:11:23
 * @since 1.0
 */
public class Accepted implements NAKAble {

    private Object  var;
    private Object  value;
    private Long    epoch;
    private boolean NAK;

    public static Accepted create(Long epoch, Object var, Object value) {
        return create(epoch, var, value, false);
    }

    public static Accepted create(Long epoch, Object var, Object value, boolean isNAK) {
        return new Accepted(epoch, var, value, isNAK);
    }

    public Accepted(Long epoch, Object var, Object value, boolean isNAK){
        this.epoch = epoch;
        this.var = var;
        this.value = value;
        this.NAK = isNAK;
    }

    public Object getVar() {
        return var;
    }

    public Object getValue() {
        return value;
    }

    public Long getEpoch() {
        return epoch;
    }

    /**
     * 表示此 Accepted 是否定的，表示：Proposer 提交给 accept 太晚了，导致之间允许的 epoch 其他的 Proposer
     * 覆盖了
     */
    @Override
    public boolean isNAK() {
        return NAK;
    }

}
