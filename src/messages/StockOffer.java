package messages;

import java.io.Serializable;

public class StockOffer implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private static final String tag = "BUY";
    private String companyName;
    private int stockCount;
    private int offerValue;

    public StockOffer (String companyName, int stockCount, int offerValue) {
        this.companyName = companyName;
        this.stockCount = stockCount;
        this.offerValue = offerValue;
    }

    public String getCompanyName() { return this.companyName; }

    public int getStockCount() { return this.stockCount; }

    public int getOfferValue() { return this.offerValue; }

    public String getTag() { return tag; }

}