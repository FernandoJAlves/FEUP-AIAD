package agents.EconomyAgent;

import java.util.HashMap;
import java.util.Random; 

import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.util.Logger;

import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;

public class EconomyAgent extends Agent {

	private static final long serialVersionUID = 1L;
	private Logger myLogger = Logger.getMyLogger(getClass().getName());

	private HashMap<String, Double> companyValues = new HashMap<String, Double>();

	private class EconomyBehaviour extends TickerBehaviour {

		private static final long serialVersionUID = 1L;

		public EconomyBehaviour(Agent a, int ticks) {
			super(a, ticks);
		}

		public void onTick() {
			// TODO: Maybe create this as a property of the class, to avoid re-initializing rand every loop
			Random rand = new Random(); 
			System.out.println("============ PRINTING ============");
			for (String key : companyValues.keySet()) {
				companyValues.put(key, rand.nextDouble() * 30);
				System.out.println("KEY: " + key + " | Value: " + companyValues.get(key));
			}
		}

	} // END of inner class EconomyBehaviour

	private class ListeningBehaviour extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;

		public void action () {
			ACLMessage msg = receive();
			// TODO: Handle other messages
			if (msg != null) {
				// TODO: Right now every company starts with 30.0 in their stock value, change that to be dinamic (maybe come from the msg)
				System.out.println("\t> Inserting <" + msg.getContent() + "," + 30.0 + ">");
				companyValues.put(msg.getContent(), 30.0);
				// TODO: Add a reply?

			} else {
				block();
			}
		}



	} // END of inner class ListeningBehaviour



	protected void setup() {

		System.out.println("\t> Starting Economy: " + getLocalName());

		// Registration with the DF 
		DFAgentDescription agentDescription = new DFAgentDescription();
		ServiceDescription serviceDescription = new ServiceDescription();   

		serviceDescription.setType("EconomyAgent"); // required
		serviceDescription.setName(getName()); // required
		serviceDescription.setOwnership("FEUP");

		agentDescription.setName(getAID()); // required
		agentDescription.addServices(serviceDescription); // required

		try {
			myLogger.log(Logger.INFO, "Registering " + getLocalName());
			DFService.register(this, agentDescription);
			EconomyBehaviour economyBehaviour = new EconomyBehaviour(this, 2000);
			ListeningBehaviour listeningBehaviour = new ListeningBehaviour();

			addBehaviour(economyBehaviour);
			addBehaviour(listeningBehaviour);
		} catch (FIPAException e) {
			myLogger.log(Logger.SEVERE, "Agent "+getLocalName()+" - Cannot register with DF", e);
			doDelete();
		}

		createTestCompanies();
	}

	protected void createTestCompanies() {

	}
}