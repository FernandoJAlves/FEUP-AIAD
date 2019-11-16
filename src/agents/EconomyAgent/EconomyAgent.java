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

// Auxiliar data for a company
class CompanyOtherInfo {
	public Integer currentCapital;
	public String currentMotherCompany;
	public Double stockValue;

	public CompanyOtherInfo (Integer currentCapital, String currentMotherCompany, Double stockValue) {
		this.currentCapital = currentCapital;
		this.currentMotherCompany = currentMotherCompany;
		this.stockValue = stockValue;
	}
}

public class EconomyAgent extends Agent {

	private static int PRINT_INTERVAL = 2000;

	private static boolean PRINT_ECONOMY = true;

	private static final long serialVersionUID = 1L;
	private Logger myLogger = Logger.getMyLogger(getClass().getName());

	private HashMap<String, Double> companyValues = new HashMap<String, Double>(); // individual company stock values
	private HashMap<String, HashMap<String, Integer>> companyStocksMap = new HashMap<String, HashMap<String, Integer>>(); // stocks location
	private HashMap<String, CompanyOtherInfo> companyOtherInfoMap = new HashMap<String, CompanyOtherInfo>(); // company capital and mother-company map

	private class EconomyBehaviour extends TickerBehaviour {

		private static final long serialVersionUID = 1L;

		public EconomyBehaviour(Agent a, int ticks) {
			super(a, ticks);
		}

		public void onTick() {

			if (PRINT_ECONOMY) {
				System.out.println("\n///////////////////////////////////////////////////////////////\n");
				System.out.println("============ COMPANY STOCK VALUES ============");
			}

			for (String key : companyOtherInfoMap.keySet()) {
				CompanyOtherInfo currentCompanyInfo = companyOtherInfoMap.get(key);
				// Pick next value from a normal distribution with mean=currentValue and std-deviation=1
				// Min action value is 0.01 (to avoid negative values)
				currentCompanyInfo.stockValue = Math.max(0.01, (ThreadLocalRandom.current().nextGaussian() * 1 + currentCompanyInfo.stockValue));

				companyOtherInfoMap.put(key, currentCompanyInfo);

				if (PRINT_ECONOMY) {
					System.out.println("NAME: " + key + " | Value: " + currentCompanyInfo.stockValue);
				}

			}

			if (PRINT_ECONOMY) {
				System.out.println("\n============ COMPANY STOCK MAPS ============");
			}

			for (String keyOuter : companyStocksMap.keySet()) {
				HashMap<String, Integer> currentCompanyStocks = companyStocksMap.get(keyOuter);

				if (PRINT_ECONOMY) {
					System.out.println(keyOuter + " stocks are in companies: ");
					for (String keyInner : currentCompanyStocks.keySet()) {
						System.out.println("\t" + keyInner + " - " + currentCompanyStocks.get(keyInner));
					}
				}
			}

		}

	} // END of inner class EconomyBehaviour

	private class ListeningBehaviour extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;

		public void action() {
			ACLMessage msg = receive();
			// TODO: Handle other messages
			if (msg != null) {
				switch (msg.getPerformative()) {
				case ACLMessage.INFORM: {
					// New company created, add it to the respective maps
					CompanySetupMessage content;
					try {
						content = (CompanySetupMessage) msg.getContentObject();
					} catch (UnreadableException e) {
						e.printStackTrace();
						return;
					}

					System.out.println("\t> Inserting <" + content.companyName + "," + content.companyActionValue + ">");

					// Build current company stock map (shows where its stocks are located)
					HashMap<String, Integer> currentCompanyStocks = new HashMap<String, Integer>();
					currentCompanyStocks.put(content.companyName, content.companyStockAmount);
					companyStocksMap.put(content.companyName, currentCompanyStocks);

					// Add entry in companyOtherInfoMap
					CompanyOtherInfo currentCompanyInfo = new CompanyOtherInfo(0, "", content.companyActionValue); // TODO: Replace 0 for starting capital value (assume a company always starts with no mother-company?)
					companyOtherInfoMap.put(content.companyName, currentCompanyInfo);

					// TODO: Add a reply? Probably not
					break;
				}
				case ACLMessage.PROPAGATE: {
					// A transaction has occured, record it and update the local state
					TransactionNotifyMessage content;
					try {
						content = (TransactionNotifyMessage) msg.getContentObject();
					} catch (UnreadableException e) {
						e.printStackTrace();
						return;
					}

					// Relevant print
					System.out.println("\n(!!!) TRANSACTION: " + content.sellerName + " SOLD STOCKS OF " + content.stockOwner + " TO " + content.buyerName + " (!!!)"); 

					// Atualizar Mapa Acções
					updateStockMapAfterTransaction(content);

					// TODO: Atualizar o mapa de capital de cada empresa
					// TODO: Atualizar o mapa de empresa mãe de cada empresa (e mandar a notificação à stockOwner caso a empresa mãe mude)

					break;
				}
				// TODO: Add case for message coming from working companies
				default:
					System.out.println("(!) ERROR - UNKNOWN MESSAGE RECEIVED => " + msg.getPerformative() + " | "
							+ msg.getContent());
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
			EconomyBehaviour economyBehaviour = new EconomyBehaviour(this, PRINT_INTERVAL);
			ListeningBehaviour listeningBehaviour = new ListeningBehaviour();

			addBehaviour(economyBehaviour);
			addBehaviour(listeningBehaviour);
		} catch (FIPAException e) {
			myLogger.log(Logger.SEVERE, "Agent " + getLocalName() + " - Cannot register with DF", e);
			doDelete();
		}

		createTestCompanies();
	}

	protected void updateStockMapAfterTransaction (TransactionNotifyMessage content) {
		HashMap<String, Integer> stockOwnerMap = companyStocksMap.get(content.stockOwner);

		// Increment Buyer Stocks
		if (stockOwnerMap.containsKey(content.buyerName)) {
			Integer tempStockAmount = stockOwnerMap.get(content.buyerName);
			stockOwnerMap.put(content.buyerName, tempStockAmount + content.stockAmount);
		} else {
			stockOwnerMap.put(content.buyerName, content.stockAmount);
		}

		// Decrement Seller Stocks
		Integer tempStockAmount = stockOwnerMap.get(content.sellerName);
		stockOwnerMap.put(content.sellerName, tempStockAmount - content.stockAmount);

		companyStocksMap.put(content.stockOwner, stockOwnerMap);
	}

	protected void createTestCompanies() {

	}
}