package messages;

import java.io.Serializable;

public class WorkNotifyMessage implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public String workingCompany; // Empresa que trabalhou
    public Integer newCapitalValue; // Novo valor de capital

    public WorkNotifyMessage (String workingCompany, Integer newCapitalValue) {
        this.workingCompany = workingCompany;
        this.newCapitalValue = newCapitalValue;
    }

}