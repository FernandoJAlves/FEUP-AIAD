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
    private double offerStockValue;
    private double realStockValue;

    public StockOffer (String companyName, int stockCount, int offerValue, double offerStockValue, double realStockValue) {
        this.companyName = companyName;
        this.stockCount = stockCount;
        this.offerValue = offerValue;
        this.offerStockValue = offerStockValue;
        this.realStockValue = realStockValue;
    }

    public String getCompanyName() { return this.companyName; }

    public int getStockCount() { return this.stockCount; }

    public int getOfferValue() { return this.offerValue; }

    public String getTag() { return tag; }

    public double getOfferStockValue() { return this.offerStockValue; }

    public double getRealStockValue() { return this.realStockValue; }
}