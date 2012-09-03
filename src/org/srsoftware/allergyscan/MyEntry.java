package org.srsoftware.allergyscan;
import java.util.Map;

final class MyEntry implements Map.Entry<Integer, String> {
    private final Integer key;
    private String value;

    public MyEntry(Integer key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public Integer getKey() {
        return key;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String setValue(String value) {
        String old = this.value;
        this.value = value;
        return old;
    }
    
    @Override
    public String toString() {
    	return key+" / "+value;
    }
}
