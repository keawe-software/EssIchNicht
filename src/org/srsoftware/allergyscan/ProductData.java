package org.srsoftware.allergyscan;

final class ProductData {
    private final Integer pid;
    private String name;
    private String code;

    public ProductData(Integer pid,String code, String name) {
        this.pid = pid;
        this.code=code;
        this.name = name;
    }

    public Integer pid() {
        return pid;
    }

    public String name() {
        return name;
    }
    
    public String code(){
    	return code;
    }

    public String setName(String name) {
        String old = this.name;
        this.name = name;
        return old;
    }
    
    @Override
    public String toString() {
    	return pid+" / "+code+" / "+name;
    }
}
