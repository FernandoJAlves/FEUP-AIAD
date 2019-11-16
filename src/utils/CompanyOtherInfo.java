package utils;

import java.io.Serializable;

// Auxiliar data for a company
public class CompanyOtherInfo implements Serializable {
	
    /**
     *
     */
	private static final long serialVersionUID = 1L;
	
	public Integer currentCapital;
	public String currentMotherCompany;
	public Double stockValue;

	public CompanyOtherInfo (Integer currentCapital, String currentMotherCompany, Double stockValue) {
		this.currentCapital = currentCapital;
		this.currentMotherCompany = currentMotherCompany;
		this.stockValue = stockValue;
	}
}