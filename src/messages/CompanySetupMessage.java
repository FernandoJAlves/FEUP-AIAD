package messages;

import java.io.Serializable;

public class CompanySetupMessage implements Serializable {

    public String companyName;
    public Double companyActionValue;

    public CompanySetupMessage (String companyName, Double companyActionValue) {
        this.companyName = companyName;
        this.companyActionValue = companyActionValue;
    }

}