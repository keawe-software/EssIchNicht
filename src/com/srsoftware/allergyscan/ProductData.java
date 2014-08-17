package com.srsoftware.allergyscan;

final class ProductData {
    private String name;
    private Barcode barcode;

    public ProductData(Barcode barcode, String name) {
        this.barcode=barcode;
        this.name = name;
    }

    public String name() {
        return name;
    }
    
    public Barcode barcode(){
    	return barcode;
    }

    public String setName(String name) {
        String old = this.name;
        this.name = name;
        return old;
    }
    
    @Override
    public String toString() {
    	return barcode+" / "+name;
    }
}
