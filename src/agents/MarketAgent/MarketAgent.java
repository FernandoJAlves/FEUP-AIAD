package agents.MarketAgent;

import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.util.Logger;

import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;

public class MarketAgent extends Agent {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private Logger myLogger = Logger.getMyLogger(getClass().getName());

	private class MarketBehaviour extends Behaviour {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public MarketBehaviour(Agent a) {
			super(a);
		}

		public void action() {

		}

		@Override
		public boolean done() {
			return false;
		}
	} // END of inner class MarketBehaviour


	protected void setup() {
		// Registration with the DF 
		DFAgentDescription agentDescription = new DFAgentDescription();
		ServiceDescription serviceDescription = new ServiceDescription();   

		agentDescription.setName(getAID()); // required
		agentDescription.addServices(serviceDescription); // required

		serviceDescription.setType("MarketAgent"); // required
		serviceDescription.setName(getName()); // required
		serviceDescription.setOwnership("FEUP");

		try {
			myLogger.log(Logger.INFO, "Registering " + getLocalName());
			DFService.register(this, agentDescription);
			MarketBehaviour marketBehaviour = new MarketBehaviour(this);
			addBehaviour(marketBehaviour);
		} catch (FIPAException e) {
			myLogger.log(Logger.SEVERE, "Agent "+getLocalName()+" - Cannot register with DF", e);
			doDelete();
		}
	}
}