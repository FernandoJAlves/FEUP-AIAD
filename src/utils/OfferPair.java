package utils;

import jade.core.AID;
import messages.StockOffer;

// Auxiliar data for a company
public class OfferPair {
    public AID company;
    public StockOffer offer;

	public OfferPair (AID company, StockOffer offer) {
        this.company = company;
        this.offer = offer;
	}
}