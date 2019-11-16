package messages;

import java.util.HashMap;
import java.io.Serializable;
import utils.*;

public class StockMapAllMessage implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

	private HashMap<String, HashMap<String, Integer>> companyStocksMap = new HashMap<String, HashMap<String, Integer>>(); 
	private HashMap<String, CompanyOtherInfo> companyOtherInfoMap = new HashMap<String, CompanyOtherInfo>(); 

    public StockMapAllMessage (HashMap<String, HashMap<String, Integer>> companyStocksMap, HashMap<String, CompanyOtherInfo> companyOtherInfoMap) {
        this.companyStocksMap = companyStocksMap;
        this.companyOtherInfoMap = companyOtherInfoMap;
    }

}