package agents.EconomyAgent;

import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.util.Logger;

import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;
import jade.lang.acl.UnreadableException;

import messages.*;

public class EconomyAgent extends Agent {

	private static final long serialVersionUID = 1L;
	private Logger myLogger = Logger.getMyLogger(getClass().getName());

	private HashMap<String, Double> companyValues = new HashMap<String, Double>();
	private HashMap<String, HashMap<String, Integer>> companyStocksMap = new HashMap<String, HashMap<String, Integer>>();

	private class EconomyBehaviour extends TickerBehaviour {

		private static final long serialVersionUID = 1L;

		public EconomyBehaviour(Agent a, int ticks) {
			super(a, ticks);
		}

		public void onTick() {

			System.out.println("\n///////////////////////////////////////////////////////////////\n");
			System.out.println("============ COMPANY STOCK VALUES ============");
			for (String key : companyValues.keySet()) {
				Double currentValue = companyValues.get(key);
				// Pick next value from a normal distribution with mean=currentValue and std-deviation=1
				// Min action value is 0.01 (to avoid negative values)
				Double newValue = Math.max(0.01, (ThreadLocalRandom.current().nextGaussian() * 1 + currentValue));

				companyValues.put(key, newValue);
				System.out.println("KEY: " + key + " | Value: " + newValue);
			}

			System.out.println("\n============ COMPANY STOCK MAPS ============");
			for (String keyOuter : companyStocksMap.keySet()) {
				HashMap<String, Integer> currentCompanyStocks = companyStocksMap.get(keyOuter);

				System.out.println("Company: " + keyOuter + " stocks are in companies: ");
				for (String keyInner : currentCompanyStocks.keySet()) {
					System.out.println("\t" + keyInner + " - " + currentCompanyStocks.get(keyInner));
				}
			}
		}

	} // END of inner class EconomyBehaviour

	private class ListeningBehaviour extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;

		public void action () {
			ACLMessage msg = receive();
			// TODO: Handle other messages
			if (msg != null) {
				switch (msg.getPerformative()) {
					case ACLMessage.INFORM:
						CompanySetupMessage content;
						try {
							content = (CompanySetupMessage)msg.getContentObject();
						} catch (UnreadableException e) {
							e.printStackTrace();
							return;
						}

						System.out.println("\t> Inserting <" + content.companyName + "," + content.companyActionValue + ">");
						companyValues.put(content.companyName, content.companyActionValue);

						// Build current company stock map (shows where its stocks are located)
						HashMap<String, Integer> currentCompanyStocks = new HashMap<String, Integer>();
						currentCompanyStocks.put(content.companyName, content.companyStockAmount);
						companyStocksMap.put(content.companyName, currentCompanyStocks);

						// TODO: Add a reply?
						break;
				
					default:
						System.out.println("(!) ERROR - UNKNOWN MESSAGE RECEIVED => " + msg.getPerformative() + " | " + msg.getContent());
						break;
				}
				
				

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