package messages;

import java.io.Serializable;
import agents.CompanyAgent.CompanyGlobals.*;

public class CompanySetupMessage implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    public String companyName;
    public Double companyActionValue;
    public Integer companyStockAmount;
    public Integer companyCapital;
    public CompanyPersonality personality;

    public CompanySetupMessage (String companyName, Double companyActionValue, Integer companyStockAmount, Integer companyCapital, CompanyPersonality personality) {
        this.companyName = companyName;
        this.companyActionValue = companyActionValue;
        this.companyStockAmount = companyStockAmount;
        this.companyCapital = companyCapital;
        this.personality = personality;
    }

}