package de.srsoftware.allergyscan;

final class ProductData {
    private String name;
    private Long barcode;

    public ProductData(Long barcode, String name) {
        this.barcode=barcode;
        this.name = name;
    }

    public String name() {
        return name;
    }
    
    public Long barcode(){
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
