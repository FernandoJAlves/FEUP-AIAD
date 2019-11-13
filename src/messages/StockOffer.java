package messages;

import java.io.Serializable;

public class StockOffer implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private final String tag = "BUY";
    private String stockId;
    private long offerValue;

    public StockOffer (String stockId, long offerValue) {
        this.stockId = stockId;
        this.offerValue = offerValue;
    }

    public String getStockId() { return this.stockId; }

    public long getOfferValue() { return this.offerValue; }

    public String getTag() { return this.tag; }

}