package utils;

import java.io.Serializable;
import agents.CompanyAgent.CompanyGlobals.*;

// Auxiliar data for a company
public class CompanyOtherInfo implements Serializable {
	
    /**
     *
     */
	private static final long serialVersionUID = 1L;
	
	public Integer currentCapital;
	public String currentMotherCompany;
	public Double stockValue;
    public CompanyPersonality personality;
	public Double currentCompanyValue; // calculated by EconomyAgent

	public CompanyOtherInfo (Integer currentCapital, String currentMotherCompany, Double stockValue, CompanyPersonality personality) {
		this.currentCapital = currentCapital;
		this.currentMotherCompany = currentMotherCompany;
		this.stockValue = stockValue;
		this.personality = personality;
	}
}