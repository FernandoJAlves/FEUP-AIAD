package agents.EconomyAgent;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.text.DecimalFormat;
import java.io.IOException;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.util.Logger;

import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;
import jade.lang.acl.UnreadableException;

import java.util.Date;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;

import messages.*;
import utils.*;

public class EconomyAgent extends Agent {

	private static int PRINT_INTERVAL = 3000;

	private static boolean PRINT_ECONOMY = true;

	private static final long serialVersionUID = 1L;
	private Logger myLogger = Logger.getMyLogger(getClass().getName());

	private HashMap<String, HashMap<String, Integer>> companyStocksMap = new HashMap<String, HashMap<String, Integer>>(); // stocks location
	private HashMap<String, CompanyOtherInfo> companyOtherInfoMap = new HashMap<String, CompanyOtherInfo>(); // company capital and mother-company map
	private HashMap<String, Double> rankingMap = new HashMap<String, Double>();

	private ArrayList<TransactionNotifyMessage> transactionLog = new ArrayList<TransactionNotifyMessage>();

	private static DecimalFormat formatter = new DecimalFormat("#.000");

	// Maximum number of stocks of 1 company
	private static Integer maxStockAmmount = 10000;

	private class EconomyBehaviour extends TickerBehaviour {

		private static final long serialVersionUID = 1L;

		public EconomyBehaviour(Agent a, int ticks) {
			super(a, ticks);
		}

		public void onTick() {

			calculateCurrentParents();

			printRanking();
			printCompanies();
			printStockMaps();

		}

	} // END of inner class EconomyBehaviour

	private class ListeningBehaviour extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;

		public void action() {
			ACLMessage msg = receive();
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
					CompanyOtherInfo currentCompanyInfo = new CompanyOtherInfo(content.companyCapital, null, content.companyActionValue, content.personality);
					companyOtherInfoMap.put(content.companyName, currentCompanyInfo);

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
					System.out.println("\n >>>>>>>>>>>>>>>>>>>>>>>>>>>>> (!!!) TRANSACTION: " + content.sellerName + " SOLD " + content.stockAmount + " STOCKS OF " + content.stockOwner + " TO " + content.buyerName + " FOR " + content.transactionCost + "$ (!!!)"); 

					// Adicionar transação ao log -> TODO: Maybe create a transactioninfo object and push that
					transactionLog.add(content);

					if (content.acceptance) {
						// Atualizar Mapa Acções
						updateStockMapAfterTransaction(content);

						// Atualizar Mapa de OtherInfo
						updateOtherInfoMapAfterTransaction(content);
					}

					break;
				}
				case ACLMessage.REQUEST: {
					// Query to get a company's (or all companies') stock map
					String content = msg.getContent();

					ACLMessage reply = msg.createReply();
					reply.setPerformative(ACLMessage.INFORM);

					if (content.equals("ALL")) {

						StockMapAllMessage replyContent = new StockMapAllMessage(companyStocksMap, companyOtherInfoMap);
						try {
							reply.setContentObject(replyContent);
						} catch (IOException e) {
							e.printStackTrace();
							return;
						}
					}
					else {
						
						HashMap<String, Integer> currentCompanyStocks = companyStocksMap.get(content);
						CompanyOtherInfo currentCompanyOtherInfo = companyOtherInfoMap.get(content);

						StockMapSingleMessage replyContent = new StockMapSingleMessage(currentCompanyStocks, currentCompanyOtherInfo);
						try {
							reply.setContentObject(replyContent);
						} catch (IOException e) {
							e.printStackTrace();
							return;
						}
					}

					send(reply);

					break;
				}
				case ACLMessage.CONFIRM: {
					// Informing the Economy of the change of capital from agents in WORK state
					WorkNotifyMessage content;
					try {
						content = (WorkNotifyMessage) msg.getContentObject();
					} catch (UnreadableException e) {
						e.printStackTrace();
						return;
					}

					CompanyOtherInfo currentCompanyInfo = companyOtherInfoMap.get(content.workingCompany);
					currentCompanyInfo.currentCapital = content.newCapitalValue;
					companyOtherInfoMap.put(content.workingCompany, currentCompanyInfo);

					break;
				}
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

		Runtime.getRuntime().addShutdownHook(new Thread(EconomyAgent::generateCSV));
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

	protected void updateOtherInfoMapAfterTransaction (TransactionNotifyMessage content) {
		// Decrement buyer capital
		CompanyOtherInfo buyerInfo = companyOtherInfoMap.get(content.buyerName);
		buyerInfo.currentCapital = buyerInfo.currentCapital - content.transactionCost;
		companyOtherInfoMap.put(content.buyerName, buyerInfo);

		// Increment seller capital
		CompanyOtherInfo sellerInfo = companyOtherInfoMap.get(content.sellerName);
		sellerInfo.currentCapital = sellerInfo.currentCapital + content.transactionCost;
		companyOtherInfoMap.put(content.sellerName, sellerInfo);
	}

	protected void calculateCurrentParents () {
		
		HashMap<String, String> companyParentMap = new HashMap<String, String>(); // temporary parent-company map

		for (String currentCompany : companyStocksMap.keySet()) {
			HashMap<String, Integer> currentCompanyStockMap = companyStocksMap.get(currentCompany);

			for (String currentStockOwner : currentCompanyStockMap.keySet()) {
				// if a company (different from the currentCompany) has more than 5000 stocks, add entry to companyParentMap
				if ( currentCompanyStockMap.get(currentStockOwner) > (maxStockAmmount/2) && !(currentCompany.equals(currentStockOwner)) ) {
					companyParentMap.put(currentCompany, currentStockOwner);
					break;
				}
			}
		}

		// Calcular "precedências"
		boolean valuesChanged = false;

		do {
			valuesChanged = false;
			
			for (String currentCompany : companyParentMap.keySet()) {
				String currentCompanyParent = companyParentMap.get(currentCompany);
	
				// Se o pai atual já tiver um "super-pai", então esse "super-pai" torna-se pai para company atual
				String potentialNewParent = companyParentMap.get(currentCompanyParent);
				if (potentialNewParent != null) {
					valuesChanged = true;
					companyParentMap.put(currentCompany, potentialNewParent);
				}
			}
		} while (valuesChanged);

		// Reset the parents and total company value
		rankingMap.clear();
		for (String currentCompany : companyOtherInfoMap.keySet()) {
			CompanyOtherInfo currentCompanyInfo = companyOtherInfoMap.get(currentCompany);
			
			currentCompanyInfo.currentCompanyValue = null;

			String companyParent = companyParentMap.get(currentCompany);
			if (companyParent == null) {
				currentCompanyInfo.currentParentCompany = null;
				// Add to rankingMap, because this company does not have a parent company
				double tempCapital = currentCompanyInfo.currentCapital;
				rankingMap.put(currentCompany, tempCapital);
			} else {
				currentCompanyInfo.currentParentCompany = companyParent;
			}
			
			companyOtherInfoMap.put(currentCompany, currentCompanyInfo);
		}

		// Add value to the parent company
		for (String currentCompany : companyOtherInfoMap.keySet()) {
			CompanyOtherInfo currentCompanyInfo = companyOtherInfoMap.get(currentCompany);

			// has a parent, so add value to him
			String companyParent = currentCompanyInfo.currentParentCompany;
			if (companyParent != null) {
				double currentValue = rankingMap.get(companyParent);
				currentValue += currentCompanyInfo.currentCapital;
				rankingMap.put(companyParent, currentValue);
			}
		}

	}

	protected void printRanking () {
		if (PRINT_ECONOMY) {
			System.out.println("\n///////////////////////////////////////////////////////////////\n");
			System.out.println("============ COMPANY RANKING ============");
		}

		HashMap<String,Double> tmp = new HashMap<>(this.rankingMap);

		int place = 0;

		while (!tmp.isEmpty()) {
			place++;
			Entry<String, Double> maxEntry = Collections.max(tmp.entrySet(), new Comparator<Entry<String, Double>>() {
				public int compare(Entry<String, Double> e1, Entry<String, Double> e2) {
					return e1.getValue()
						.compareTo(e2.getValue());
				}
			});
			if (PRINT_ECONOMY) {
				System.out.println(place + "º Place: " + maxEntry.getKey() + " | TotalValue: " + maxEntry.getValue());
			}
			tmp.remove(maxEntry.getKey());
		}
	}

	protected void printCompanies () {
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
				System.out.println("NAME: " + key +  " | StockValue: " + formatter.format(currentCompanyInfo.stockValue) + " | Capital: " + currentCompanyInfo.currentCapital + "$ | Type: " + currentCompanyInfo.personality + " | Parent: " + currentCompanyInfo.currentParentCompany);
			}
		}
	}

	protected void printStockMaps () {
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

	// add array of TransactionInfo to generateCSV's arguments
	protected static void generateCSV(){
		String filename = "data.csv";
		File file = new File(filename);
		boolean fileExists = file.exists();

		try (FileWriter fw = new FileWriter(filename, true);
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter out = new PrintWriter(bw))
		{
			// add variables header to CSV if file doesn't exist
			if(!fileExists)
				out.println("V1;V2;V3");

			// loop through TransactionInfo to output variable data 
			out.println("1;2;3");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}