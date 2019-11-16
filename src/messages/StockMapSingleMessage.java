package messages;

import java.util.HashMap;
import java.io.Serializable;
import utils.*;

public class StockMapSingleMessage implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

	public HashMap<String, Integer> companyStocks = new HashMap<String, Integer>(); 
	public CompanyOtherInfo companyOtherInfo; 

    public StockMapSingleMessage (HashMap<String, Integer> companyStocks, CompanyOtherInfo companyOtherInfo) {
        this.companyStocks = companyStocks;
        this.companyOtherInfo = companyOtherInfo;
    }

}