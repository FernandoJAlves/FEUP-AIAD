package messages;

import java.io.Serializable;

public class CompanySetupMessage implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    public String companyName;
    public Double companyActionValue;
    public Integer companyStockAmount;

    public CompanySetupMessage (String companyName, Double companyActionValue, Integer companyStockAmount) {
        this.companyName = companyName;
        this.companyActionValue = companyActionValue;
        this.companyStockAmount = companyStockAmount;
    }

}