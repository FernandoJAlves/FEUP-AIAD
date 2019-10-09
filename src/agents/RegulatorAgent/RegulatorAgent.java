package agents.RegulatorAgent;

import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.util.Logger;

import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;

public class RegulatorAgent extends Agent {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private Logger myLogger = Logger.getMyLogger(getClass().getName());

    static private int MAX_COMPANIES = 1;
    static private int actualCompanyIndex = 0;

    private boolean canSend = true;
    private DFAgentDescription[] companyAgents;

    private class RegulatorBehaviour extends CyclicBehaviour {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public RegulatorBehaviour(Agent a) {
            super(a);
        }

        public void action() {

            if (canSend) {

                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent("your turn");
                msg.addReceiver(companyAgents[actualCompanyIndex].getName());
                send(msg);
                canSend = false;
                
            }

            ACLMessage reply = myAgent.receive();

            if (reply != null) {

                if (reply.getPerformative() == ACLMessage.INFORM) {
                    String content = reply.getContent();
                    if ((content != null) && (content.indexOf("gotcha") != -1)) {
                        myLogger.log(Logger.INFO, "Regulator " + getLocalName() + " - Received gotcha message from "
                                + reply.getSender().getLocalName());
                        
                        canSend = true;
                    } else {
                        myLogger.log(Logger.INFO, "Agent " + getLocalName() + " - Unexpected request [" + content
                                + "] received from " + reply.getSender().getLocalName());
                    }

                } else {
                    myLogger.log(Logger.INFO,
                            "Agent " + getLocalName() + " - Unexpected message ["
                                    + ACLMessage.getPerformative(reply.getPerformative()) + "] received from "
                                    + reply.getSender().getLocalName());
                }
            } else {
                block();
                return;
            }

            actualCompanyIndex = (actualCompanyIndex + 1) % MAX_COMPANIES;

        }
    } // END of inner class RegulatorBehavioir

    protected void setup() {
        // Registration with the DF
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("RegulatorAgent");
        sd.setName(getName());
        sd.setOwnership("FEUP");
        dfd.setName(getAID());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            RegulatorBehaviour companyBehaviour = new RegulatorBehaviour(this);
            addBehaviour(companyBehaviour);
        } catch (FIPAException e) {
            myLogger.log(Logger.SEVERE, "Agent " + getLocalName() + " - Cannot register with DF", e);
            doDelete();
        }
        this.getCompanies();
    }

    protected void getCompanies() {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("CompanyAgent");
        dfd.addServices(sd);
        DFAgentDescription[] agents = null;
        try {
            agents = DFService.search(this, dfd);
        }
        catch (FIPAException e) {
            e.printStackTrace();
        }
        this.companyAgents = agents;
        MAX_COMPANIES = agents.length;
    }
}