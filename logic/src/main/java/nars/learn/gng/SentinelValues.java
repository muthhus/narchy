package nars.learn.gng;

import com.gs.collections.impl.map.mutable.primitive.AbstractSentinelValues;

class SentinelValues extends AbstractSentinelValues {

    public boolean containsZeroKey;
    public boolean containsOneKey;


    public int size() {
        return (this.containsZeroKey?1:0) + (this.containsOneKey?1:0);
    }

    public int zeroValue;
    public int oneValue;

        SentinelValues() {
        }

        public boolean containsValue(int value) {
            boolean valueEqualsZeroValue = this.containsZeroKey && this.zeroValue == value;
            boolean valueEqualsOneValue = this.containsOneKey && this.oneValue == value;
            return valueEqualsZeroValue || valueEqualsOneValue;
        }

//        public AbstractMutableIntValuesMap.SentinelValues copy() {
//            AbstractMutableIntValuesMap.SentinelValues sentinelValues = new AbstractMutableIntValuesMap.SentinelValues();
//            sentinelValues.zeroValue = this.zeroValue;
//            sentinelValues.oneValue = this.oneValue;
//            sentinelValues.containsOneKey = this.containsOneKey;
//            sentinelValues.containsZeroKey = this.containsZeroKey;
//            return sentinelValues;
//        }
    }
