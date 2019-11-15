package messages;

import java.io.Serializable;

public class TransactionNotifyMessage implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public String buyerName; // Empresa que fez a proposta
    public String sellerName; // Empresa que aceitou a proposta
    public String stockOwner; // Empresa dona das ações a serem trocadas

    public Integer stockAmount; // Quantidade de stocks a serem trocadas
    public Integer transactionCost; // Preço da transação

    public TransactionNotifyMessage (String buyerName, String sellerName, String stockOwner, Integer stockAmount, Integer transactionCost) {
        this.buyerName = buyerName;
        this.sellerName = sellerName;
        this.stockOwner = stockOwner;
        this.stockAmount = stockAmount;
        this.transactionCost = transactionCost;
    }

}