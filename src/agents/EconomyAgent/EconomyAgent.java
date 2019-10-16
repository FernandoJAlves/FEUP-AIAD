package agents.EconomyAgent;

import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.util.Logger;
import java.util.concurrent.ThreadLocalRandom;

import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;

public class EconomyAgent extends Agent {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private Logger myLogger = Logger.getMyLogger(getClass().getName());

	private double[] companyValues;

	private class EconomyBehaviour extends TickerBehaviour {

		private static final long serialVersionUID = 1L;

		public EconomyBehaviour(Agent a, int ticks) {
			super(a, ticks);
		}

		public void onTick() {
			System.out.println("======================");
			for (int i = 0; i < companyValues.length; i++) {
				companyValues[i] = (ThreadLocalRandom.current().nextInt(0, 3000))/1000.0;
				System.out.println("Value of Company " + (i+1) + ": " + companyValues[i]);
			}
		}

	} // END of inner class EconomyBehaviour


	protected void setup() {
		// Registration with the DF 
		DFAgentDescription agentDescription = new DFAgentDescription();
		ServiceDescription serviceDescription = new ServiceDescription();   

		agentDescription.setName(getAID()); // required
		agentDescription.addServices(serviceDescription); // required

		serviceDescription.setType("EconomyAgent"); // required
		serviceDescription.setName(getName()); // required
		serviceDescription.setOwnership("FEUP");

		try {
			myLogger.log(Logger.INFO, "Registering " + getLocalName());
			DFService.register(this, agentDescription);
			EconomyBehaviour economyBehaviour = new EconomyBehaviour(this, 2000);
			addBehaviour(economyBehaviour);
		} catch (FIPAException e) {
			myLogger.log(Logger.SEVERE, "Agent "+getLocalName()+" - Cannot register with DF", e);
			doDelete();
		}

		createTestCompanies();
	}

	protected void createTestCompanies() {

		this.companyValues = new double[3];
		this.companyValues[0] = 1.1;
		this.companyValues[1] = 2.2;
		this.companyValues[2] = 3.3;


	}
}