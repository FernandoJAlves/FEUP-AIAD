package messages;

import java.io.Serializable;

public class CompanySetupMessage implements Serializable {

    public String companyName;
    public Double companyActionValue;
    public Integer companyStockAmount;

    public CompanySetupMessage (String companyName, Double companyActionValue, Integer companyStockAmount) {
        this.companyName = companyName;
        this.companyActionValue = companyActionValue;
        this.companyStockAmount = companyStockAmount;
    }

}