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
    public Integer companyCapital;

    public CompanySetupMessage (String companyName, Double companyActionValue, Integer companyStockAmount, Integer companyCapital) {
        this.companyName = companyName;
        this.companyActionValue = companyActionValue;
        this.companyStockAmount = companyStockAmount;
        this.companyCapital = companyCapital;
    }

}