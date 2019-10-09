package agents.CompanyAgent;

import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.util.Logger;

import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;

public class CompanyAgent extends Agent {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private Logger myLogger = Logger.getMyLogger(getClass().getName());

	private class CompanyBehaviour extends Behaviour {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public CompanyBehaviour(Agent a) {
			super(a);
		}

		public void action() {
			ACLMessage  msg = myAgent.receive();
			
			if(msg != null){
				ACLMessage reply = msg.createReply();

				if(msg.getPerformative()== ACLMessage.INFORM){
					String content = msg.getContent();
					if ((content != null) && (content.indexOf("your turn") != -1)){
						myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Received turn change message from "+msg.getSender().getLocalName());
						reply.setPerformative(ACLMessage.INFORM);
						reply.setContent("gotcha");
					}
					else{
						myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Unexpected request ["+content+"] received from "+msg.getSender().getLocalName());
						reply.setPerformative(ACLMessage.REFUSE);
						reply.setContent("( UnexpectedContent ("+content+"))");
					}

				}
				else {
					myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Unexpected message ["+ACLMessage.getPerformative(msg.getPerformative())+"] received from "+msg.getSender().getLocalName());
					reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
					reply.setContent("( (Unexpected-act "+ACLMessage.getPerformative(msg.getPerformative())+") )");   
				}
				send(reply);
			}
			else {
				block();
			}
		}

		@Override
		public boolean done() {
			return false;
		}
	} // END of inner class CompanyBehaviour


	protected void setup() {
		// Registration with the DF 
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();   
		sd.setType("CompanyAgent"); 
		sd.setName(getName());
		sd.setOwnership("FEUP");
		dfd.setName(getAID());
		dfd.addServices(sd);
		try {
			DFService.register(this,dfd);
			CompanyBehaviour companyBehaviour = new  CompanyBehaviour(this);
			addBehaviour(companyBehaviour);
		} catch (FIPAException e) {
			myLogger.log(Logger.SEVERE, "Agent "+getLocalName()+" - Cannot register with DF", e);
			doDelete();
		}
	}
}