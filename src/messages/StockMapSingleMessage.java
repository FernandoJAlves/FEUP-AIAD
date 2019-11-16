package messages;

import java.util.HashMap;
import java.io.Serializable;
import utils.*;

public class StockMapSingleMessage implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

	private HashMap<String, Integer> companyStocks = new HashMap<String, Integer>(); 
	private CompanyOtherInfo companyOtherInfo; 

    public StockMapSingleMessage (HashMap<String, Integer> companyStocks, CompanyOtherInfo companyOtherInfo) {
        this.companyStocks = companyStocks;
        this.companyOtherInfo = companyOtherInfo;
    }

}