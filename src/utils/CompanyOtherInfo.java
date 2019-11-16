package utils;

// Auxiliar data for a company
public class CompanyOtherInfo {
	public Integer currentCapital;
	public String currentMotherCompany;
	public Double stockValue;

	public CompanyOtherInfo (Integer currentCapital, String currentMotherCompany, Double stockValue) {
		this.currentCapital = currentCapital;
		this.currentMotherCompany = currentMotherCompany;
		this.stockValue = stockValue;
	}
}