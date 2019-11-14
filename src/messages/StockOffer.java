package messages;

import java.io.Serializable;

public class StockOffer implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private final String tag = "BUY";
    private String stockId;
    private int offerValue;

    public StockOffer (String stockId, int offerValue) {
        this.stockId = stockId;
        this.offerValue = offerValue;
    }

    public String getStockId() { return this.stockId; }

    public int getOfferValue() { return this.offerValue; }

    public String getTag() { return this.tag; }

}