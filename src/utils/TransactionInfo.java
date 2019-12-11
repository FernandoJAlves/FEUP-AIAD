package utils;

import java.io.Serializable;
import agents.CompanyAgent.CompanyGlobals.*;

// Auxiliar data for a transaction to store in transaction map
public class TransactionInfo implements Serializable {
	
    /**
     *
     */
	private static final long serialVersionUID = 1L;
	
    public Boolean acceptance;
    public Double transactionCost;
    public Integer stockAmmount;
    public Double currentStockPrice;
    public Double paidStockPrice;
    public Double stockValueProportion;
    public String personality;
    public Integer lifeTime;
    public String buyerName;
    public String sellerName;
    public String stockOwnerName;

    
	public TransactionInfo (Boolean acceptance, Double transactionCost, Integer stockAmmount, Double currentStockPrice, Double paidStockPrice, Double stockValueProportion, String personality, Integer lifeTime, String buyerName, String sellerName, String stockOwnerName) {
        this.acceptance = acceptance;
        this.transactionCost = transactionCost;
        this.stockAmmount = stockAmmount;
        this.currentStockPrice = currentStockPrice;
        this.paidStockPrice = paidStockPrice;
        this.stockValueProportion = stockValueProportion;
        this.personality = personality;
        this.lifeTime = lifeTime;
        this.buyerName = buyerName;
        this.sellerName = sellerName;
        this.stockOwnerName = stockOwnerName;
	}
}