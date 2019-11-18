package agents.CompanyAgent;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import java.io.IOException;

import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.util.Logger;

import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;

import agents.CompanyAgent.CompanyGlobals.*;

import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import messages.*;
import utils.*;

public class CompanyAgent extends Agent {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private Logger myLogger = Logger.getMyLogger(getClass().getName());
	private AID economyID;

	// Existing companies returned by getCompanies()
	private DFAgentDescription[] companyAgents;

	// Current company capital
	private Integer companyCapital;

	// Map of the currently owned stocks (of any company)
	private HashMap<String, Integer> companyStocksMap = new HashMap<String, Integer>();

	private CompanyState state = CompanyState.WORK;
	private CompanyPersonality personality = CompanyPersonality.ROOKIE;

	private String dealAgent = "";
	private StockOffer actualOffer = null;

	// Maximum number of stocks of 1 company
	private static Integer maxStockAmmount = 10000;

	private class CompanyBehaviour extends TickerBehaviour {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private int tries = 0;

		public CompanyBehaviour(Agent a, int miliseconds) {
			super(a, miliseconds);
		}

		public void onTick() {
			// companyStatePrint(); // Used for debug, may be useful in the final version
			ACLMessage msg = this.listen();

			this.updateState(msg);

			switch (state) {
			case WORK:
				this.work();
				break;
			case SEARCH:
				this.search();
				break;
			case BUY:
				this.buy(msg);
				break;
			case NEGOTIATE:
				this.negotiate(msg);
				break;
			case DEAL:
				this.deal(msg);
				break;
			case CLOSE:
				this.close(msg);
				break;
			default:
				this.search();
				break;
			}
		}

		public ACLMessage listen() {
			MessageTemplate mt = null;
			MessageTemplate tmp = null;
			switch (state) {
			case SEARCH:
				mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
				break;
			case NEGOTIATE:
				mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
				tmp = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
				mt = MessageTemplate.or(mt, tmp);
				tmp = MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL);
				mt = MessageTemplate.or(mt, tmp);
				break;
			case DEAL:
				mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
				tmp = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
				mt = MessageTemplate.or(mt, tmp);
				break;
			case BUY:
				mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
				tmp = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
				mt = MessageTemplate.or(mt, tmp);
				break;
			case CLOSE:
				mt = MessageTemplate.MatchAll();
				break;
			case MARKET:
				mt = MessageTemplate.MatchAll();
				break;
			default:
				mt = MessageTemplate.MatchAll();
				break;

			}
			ACLMessage msg = myAgent.receive(mt);

			return msg;
		}

		public void updateState(ACLMessage msg) {

			if (msg != null) {

				switch (state) {
				case WORK:
					if (msg.getPerformative() == ACLMessage.PROPOSE) {
						StockOffer offer = getOffer(msg);
						if ((offer != null) && (offer.getTag().equals("BUY"))) {
							if (this.shouldReject(offer)) {
								this.customRejectProposals(msg, "REFUSED");
								return;
							}
							setState(CompanyState.DEAL);
							dealAgent = msg.getSender().getLocalName();
							actualOffer = offer;
							// myLogger.log(Logger.INFO, "Agent " + getLocalName() + " - Received BUY
							// message from "
							// + msg.getSender().getLocalName());
						}
					}
					break;
				case SEARCH:
					if (msg.getPerformative() == ACLMessage.PROPOSE) {
						rejectProposals(msg);
					}
					break;
				case NEGOTIATE:
					if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
						String content = msg.getContent();
						if ((content != null) && (content.indexOf("DEAL") != -1)) {
							setState(CompanyState.BUY);
							// myLogger.log(Logger.INFO, "Agent " + getLocalName() + " - Received DEAL
							// message from "
							// + msg.getSender().getLocalName());
						}
					} else if (msg.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
						String content = msg.getContent();
						shouldGoToWork();
						if ((content != null) && (content.indexOf("BUSY") != -1)) {
							// myLogger.log(Logger.INFO, "Agent " + getLocalName() + " - Received BUSY
							// message from "
							// + msg.getSender().getLocalName());
						}
					} else if (msg.getPerformative() == ACLMessage.PROPOSE) {
						rejectProposals(msg);
					}
					break;
				case DEAL:
					if (msg.getPerformative() == ACLMessage.REQUEST) {
						String content = msg.getContent();
						setState(CompanyState.CLOSE);
						if ((content != null) && (content.indexOf("ACTION") != -1)) {
							// myLogger.log(Logger.INFO, "Agent " + getLocalName() + " - Received ACTION
							// message from "
							// + msg.getSender().getLocalName());
						}
					} else if (msg.getPerformative() == ACLMessage.PROPOSE
							&& !msg.getSender().getLocalName().equals(dealAgent)) {
						rejectProposals(msg);
					}
					break;
				case BUY:
					if (msg.getPerformative() == ACLMessage.INFORM) {
						String content = msg.getContent();
						if ((content != null) && (content.indexOf("ACTION") != -1)) {
							// Decrease capital
							companyCapital -= actualOffer.getOfferValue();

							// Increase stock (create entry on hashmap if necessary)
							if (companyStocksMap.containsKey(actualOffer.getCompanyName())) {
								Integer tempStockAmount = companyStocksMap.get(actualOffer.getCompanyName());
								companyStocksMap.put(actualOffer.getCompanyName(),
										tempStockAmount + actualOffer.getStockCount());
							} else {
								companyStocksMap.put(actualOffer.getCompanyName(), actualOffer.getStockCount());
							}

							dealAgent = "";
							actualOffer = null;
							setState(CompanyState.SEARCH);
							// myLogger.log(Logger.INFO, "Agent " + getLocalName() + " - Received ACTION
							// message from "
							// + msg.getSender().getLocalName());
						}
					} else if (msg.getPerformative() == ACLMessage.PROPOSE) {
						rejectProposals(msg);
					}
					break;
				default:
					break;
				}
			}
		}

		public void work() {

			// While working, a company's capital increases between 0% and 20%
			Integer maxCapitalChange = (int) Math.round(companyCapital * 1.20);

			companyCapital = ThreadLocalRandom.current().nextInt(companyCapital, maxCapitalChange);

			// Notify Economy 
			ACLMessage notifyEconomyMsg = new ACLMessage(ACLMessage.CONFIRM);

			WorkNotifyMessage content = new WorkNotifyMessage(getLocalName(), companyCapital);
			try {
				notifyEconomyMsg.setContentObject(content);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			notifyEconomyMsg.addReceiver(economyID);
			sendCustom(notifyEconomyMsg);
			// End Notify Economy

			if (this.shouldStop()) {
				setState(CompanyState.SEARCH);
			}

			return;
		}

		public void search() {
			getCompanies();

			int agentsMax = companyAgents.length;

			if (agentsMax == 0) {
				setState(CompanyState.WORK);
				return;
			}

			int companyIndex = ThreadLocalRandom.current().nextInt(0, agentsMax);
			AID chosenCompany = companyAgents[companyIndex].getName();
			dealAgent = chosenCompany.getLocalName();

			ACLMessage msg = strategy();

			if (msg != null) {
				setState(CompanyState.NEGOTIATE);
				sendCustom(msg);
			}

		}

		public void negotiate(ACLMessage msg) {
			tries++;
			if (tries > 5){
				tries = 0;
				setState(CompanyState.WORK);
				dealAgent = "";
				actualOffer = null;
			}
		}

		public void buy(ACLMessage msg) {
			if (msg == null)
				return;

			ACLMessage reply = msg.createReply();
			reply.setPerformative(ACLMessage.REQUEST);
			reply.setContent("ACTION");
			sendCustom(reply);
		}

		public void deal(ACLMessage msg) {
			if (msg == null)
				return;

			ACLMessage reply = msg.createReply();
			reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
			reply.setContent("DEAL");
			sendCustom(reply);

		}

		public void close(ACLMessage msg) {
			if (msg == null)
				return;

			// Company accepted proposal and will now notify the 'buyer'
			// Update local state and notify Economy

			// Increase capital
			companyCapital += actualOffer.getOfferValue();

			// Decrease stockMap
			if (companyStocksMap.containsKey(actualOffer.getCompanyName())) {
				Integer tempStockAmount = companyStocksMap.get(actualOffer.getCompanyName());
				companyStocksMap.put(actualOffer.getCompanyName(), tempStockAmount - actualOffer.getStockCount());
			} else {
				System.out.println("(!) ERROR: KEY NOT FOUND: " + actualOffer.getCompanyName());
			}

			// Notify Economy
			ACLMessage notifyEconomyMsg = new ACLMessage(ACLMessage.PROPAGATE);

			TransactionNotifyMessage content = new TransactionNotifyMessage(dealAgent, getLocalName(),
					actualOffer.getCompanyName(), actualOffer.getStockCount(), actualOffer.getOfferValue());
			try {
				notifyEconomyMsg.setContentObject(content);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			notifyEconomyMsg.addReceiver(economyID);
			sendCustom(notifyEconomyMsg);
			// End Notify Economy

			ACLMessage reply = msg.createReply();
			reply.setPerformative(ACLMessage.INFORM);
			reply.setContent("ACTION");

			sendCustom(reply);
			actualOffer = null;
			dealAgent = "";
			setState(CompanyState.SEARCH);
		}

		public void rejectProposals(ACLMessage msg) {
			this.customRejectProposals(msg, "BUSY");
		}


		public void customRejectProposals(ACLMessage msg, String content) {
			ACLMessage reply = msg.createReply();
			reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
			reply.setContent(content);
			sendCustom(reply);
			msg = null;
		}

		public ACLMessage strategy() {
			switch (personality) {
			case ROOKIE:
				return this.rookieStrategy();
			case ADVANCED:
				return this.advancedStrategy();
			default:
				return this.rookieStrategy();
			}
		}

		public ACLMessage rookieStrategy() {

			// Choose a company to invest
			int companyIndex = ThreadLocalRandom.current().nextInt(0, companyAgents.length);
			AID companyToInvest = companyAgents[companyIndex].getName();

			// Query to know which companies have that company's stocks
			StockMapSingleMessage queryResult = queryEconomy(companyToInvest);

			// Pick one company to contact
			ArrayList<String> companies = new ArrayList<String>(queryResult.companyStocks.keySet());
			String companyToContact = null;

			do {
				int chosenCompany = ThreadLocalRandom.current().nextInt(0, companies.size());
				companyToContact = companies.get(chosenCompany);
			} while (companyToContact.equals(getLocalName()));

			int stockCount = queryResult.companyStocks.get(companyToContact);

			// Will now pick an offer between 0.5 (min(stockCount,maximumStockToAsk)) and min(stockCount,maximumStockToAsk), and if viable (enough capital), make the offer
			boolean viable = false;

			Integer maximumStockToAsk = (int) Math.round((0.9*companyCapital)/(queryResult.companyOtherInfo.stockValue));
			int offerStockCount;

			do {
				int maxStockToBuy = Math.min(stockCount, maximumStockToAsk);
				int minStockToBuy = (int) Math.floor(0.5 * (double) maxStockToBuy);
				offerStockCount = ThreadLocalRandom.current().nextInt(minStockToBuy, maxStockToBuy + 1);

				if (offerStockCount * queryResult.companyOtherInfo.stockValue < companyCapital) {
					viable = true;
				}

			} while (!viable);

			AID receiver = getCompanyAID(companyToContact);

			int offerValue = (int) Math.ceil(offerStockCount * queryResult.companyOtherInfo.stockValue);

			ACLMessage offer = makeOfferMessage(companyToInvest.getLocalName(), offerStockCount, offerValue, receiver);

			return offer;
		}

		public ACLMessage advancedStrategy() {
			
			// Get all companies
			StockMapAllMessage queryResult = queryEconomy();
			Integer numberOfCompanies = queryResult.companyOtherInfoMap.size();	
			// Inspect 40% of available companies		
			Integer numberOfSelectedCompanies = (int) Math.ceil(0.4 * (double) numberOfCompanies);

			ArrayList<String> lowestCompanyList = new ArrayList<String>(Collections.nCopies(numberOfSelectedCompanies, null));
			ArrayList<Double> lowestCompanyValueList = new ArrayList<Double>(Collections.nCopies(numberOfSelectedCompanies, 100000.0));

			// Find parent company
			String localParentCompany = queryResult.companyOtherInfoMap.get(getLocalName()) == null
				? null
				: queryResult.companyOtherInfoMap.get(getLocalName()).currentParentCompany;

			if (localParentCompany == null) 
				localParentCompany = ""; 

			// Pick the companies with the lowest stock value that isn't owned by this company or its parent

			for (int index = 0; index < numberOfSelectedCompanies; index++) {
				String lowestCompanyName = null;
				Double lowestCompanyValue = 100000.0; // Starting with a really high value

				for (String currentCompany : queryResult.companyOtherInfoMap.keySet()) {
					CompanyOtherInfo currentCompanyInfo = queryResult.companyOtherInfoMap.get(currentCompany);
	
					// Ignores values already added to list
					if (lowestCompanyList.contains(currentCompany)) {
						continue;
					}

					// If the currentCompany is already owned by this company or its parent, continue
					if (localParentCompany.equals(currentCompanyInfo.currentParentCompany) 
						|| (getLocalName()).equals(currentCompanyInfo.currentParentCompany)) {
						continue;
					} 
	
					// If the localCompany has all its stocks, don't invest in itself
					if (currentCompany.equals(getLocalName()) && companyStocksMap.get(getLocalName()) == maxStockAmmount) {
						continue;
					}
	
					if (currentCompanyInfo.stockValue < lowestCompanyValue) {
						lowestCompanyName = currentCompany;
						lowestCompanyValue = currentCompanyInfo.stockValue;
					}
				}

				// No more viable investments
				if (lowestCompanyName == null) {
					numberOfSelectedCompanies = index;
					break;
				}  

				lowestCompanyList.set(index, lowestCompanyName);
				lowestCompanyValueList.set(index, lowestCompanyValue);
			}

			// Avoid error in nextInt()
			if (numberOfSelectedCompanies == 0) return null;

			// Choose a company randomly
			Integer chosenIndex = ThreadLocalRandom.current().nextInt(0, numberOfSelectedCompanies);
			String chosenLowestCompany = lowestCompanyList.get(chosenIndex);
			Double chosenLowestValue = lowestCompanyValueList.get(chosenIndex);
			
			// Find the company with the highest amount of stocks of that company (that isn't this company)
			HashMap<String, Integer> companyInvestingInStockMap = queryResult.companyStocksMap.get(chosenLowestCompany);

			if (companyInvestingInStockMap == null) return null;

			// If the chosen company is itself, and all the stocks are currently with itself, return null
			if (companyInvestingInStockMap.size() == 1 && companyInvestingInStockMap.get(getLocalName()) != null) {
				return null;
			}

			String maximumStockOwner = null;
			Integer maximumStockAmount = 0;

			// Get maximum
			for (String currentCompany : companyInvestingInStockMap.keySet()) {
				Integer currentStockAmount = companyInvestingInStockMap.get(currentCompany);

				// don't contact self
				if (currentCompany.equals(getLocalName())) {
					continue;
				}

				if (currentStockAmount > maximumStockAmount) {
					maximumStockOwner = currentCompany;
					maximumStockAmount = currentStockAmount;
				}
			}

			if (maximumStockOwner == null) return null;

			// Calculate ideal amount of stocks to buy
			Integer maximumStockToAsk = (int) Math.round((0.9*companyCapital)/chosenLowestValue);
			Integer stockWanted = Math.min(maximumStockToAsk, Math.min((maxStockAmmount/2 + 1), maximumStockAmount));
			Integer offerCost = (int) Math.round(stockWanted*chosenLowestValue);

			return makeOfferMessage(chosenLowestCompany, stockWanted, offerCost, getCompanyAID(maximumStockOwner));
		}

		public StockMapAllMessage queryEconomy() {
			try {
				return (StockMapAllMessage) this.queryEconomyAux("ALL").getContentObject();
			} catch (UnreadableException e) {
				System.out.print("Failed to get query result");
				e.printStackTrace();
				return null;
			}

		}

		public StockMapSingleMessage queryEconomy(AID company) {
			try {
				return (StockMapSingleMessage) this.queryEconomyAux(company.getLocalName()).getContentObject();
			} catch (UnreadableException e) {
				System.out.print("Failed to get query result");
				e.printStackTrace();
				return null;
			}
		}

		public ACLMessage queryEconomyAux(String content) {
			ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
			request.setContent(content);
			request.addReceiver(economyID);
			sendCustom(request);
			MessageTemplate mt = MessageTemplate.MatchSender(economyID);
			MessageTemplate tmp = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			mt = MessageTemplate.and(mt, tmp);
			ACLMessage msg = null;
			while (msg == null) {
				msg = myAgent.receive(mt);
			}
			return msg;
		}

		public boolean shouldStop() {
			// If capital if low, don't stop working
			if (companyCapital < 20000) {
				return false;
			}

			switch (personality) {
				case ROOKIE:
					// 1 in 5 chance of leaving work
					return ThreadLocalRandom.current().nextInt(0,6) == 0;
				case ADVANCED:
					// 1 in 2 chance of leaving work
					return ThreadLocalRandom.current().nextInt(0,3) == 0;
				default:
					return false;
			}

			// return companyCapital >= (70000 + (ThreadLocalRandom.current().nextGaussian() * 50000));
		}

		public boolean shouldReject(StockOffer offer) {


			if (companyStocksMap.get(offer.getCompanyName()) == null){
				return true;
			}

			if (offer.getStockCount() > companyStocksMap.get(offer.getCompanyName())){
				return true;
			}

			return false;
		}


		protected void shouldGoToWork() {
			if (companyCapital < 20000) {
				setState(CompanyState.WORK);			
				return;
			}

			StockMapSingleMessage queryResult = queryEconomy(getAID());
			Double currentStockValue = queryResult.companyOtherInfo.stockValue;

			if (currentStockValue < 15.0) {
				setState(CompanyState.WORK);
			}
			
			switch (personality) {
				case ROOKIE:
					setState(CompanyState.WORK);
					break;
				case ADVANCED:
					setState(CompanyState.SEARCH);
					break;
				default:
					break;
			}
		}

	} // END of inner class CompanyBehaviour

	protected void setup() {
		this.setupPersonality();

		System.out.println("\t> Starting Company: " + getLocalName());

		// Registration with the DF
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("CompanyAgent");
		sd.setName(getName());
		sd.setOwnership("FEUP");
		dfd.setName(getAID());
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
			int ticks = ThreadLocalRandom.current().nextInt(5, 11) * 1000;
			CompanyBehaviour companyBehaviour = new CompanyBehaviour(this, ticks);
			addBehaviour(companyBehaviour);
		} catch (FIPAException e) {
			myLogger.log(Logger.SEVERE, "Agent " + getLocalName() + " - Cannot register with DF", e);
			doDelete();
		}

		// Send message to Economy to inform there is a new company

		DFAgentDescription dfdEconomy = new DFAgentDescription();
		ServiceDescription sdEconomy = new ServiceDescription();
		sdEconomy.setType("EconomyAgent");
		dfdEconomy.addServices(sdEconomy);

		AID tempEconomyID;
		// Search the DF
		try {
			DFAgentDescription[] result = DFService.search(this, dfdEconomy);
			if (result.length > 0)
				tempEconomyID = result[0].getName();
			else {
				System.out.println("ERROR - Economy not found!");
				return;
			}
		} catch (FIPAException fe) {
			fe.printStackTrace();
			return;
		}

		economyID = tempEconomyID;

		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

		// Company's starting capital is a random value between 60000 and 100000
		companyCapital = ThreadLocalRandom.current().nextInt(60000, 100001);
		System.out.printf("\t> %s Capital: %d\n", getLocalName(), companyCapital);

		// Company's initial stock map has only 1 entry, the companies' own stock, with
		// value of maxStockAmmout
		companyStocksMap.put(getLocalName(), maxStockAmmount);

		// Pick starting value for company stock value, between 10 and 50
		Double actionValue = ThreadLocalRandom.current().nextDouble(10, 51);
		CompanySetupMessage content = new CompanySetupMessage(getLocalName(), actionValue, maxStockAmmount, companyCapital, this.personality);
		try {
			msg.setContentObject(content);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		msg.addReceiver(economyID);
		sendCustom(msg);
	}

	protected void setState(CompanyState state) {
		this.state = state;
	}

	protected void getCompanies() {
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("CompanyAgent");
		dfd.addServices(sd);
		DFAgentDescription[] agents = null;
		try {
			agents = DFService.search(this, dfd);
			agents = Arrays.stream(agents).filter(agent -> !(agent.getName().getName()).equals(this.getAID().getName()))
					.toArray(DFAgentDescription[]::new);
		} catch (FIPAException e) {
			agents = new DFAgentDescription[0];
			e.printStackTrace();
		}
		this.companyAgents = agents;
	}

	protected DFAgentDescription[] getCompaniesWithSelf() {
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("CompanyAgent");
		dfd.addServices(sd);
		DFAgentDescription[] agents = null;
		try {
			agents = DFService.search(this, dfd);
		} catch (FIPAException e) {
			agents = new DFAgentDescription[0];
			e.printStackTrace();
		}
		return agents;
	}

	protected void sendCustom(ACLMessage msg) {
		send(msg);
	}

	protected ACLMessage makeOfferMessage(String companyName, int stockCount, int value, AID receiver) {
		ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);

		try {
			StockOffer so = new StockOffer(companyName, stockCount, value);
			actualOffer = so;
			msg.setContentObject(so);
		} catch (IOException e) {
			System.out.println("Failed to create stock offer message");
			e.printStackTrace();
		}
		msg.addReceiver(receiver);
		return msg;
	}

	protected StockOffer getOffer(ACLMessage msg) {
		StockOffer so = null;
		try {
			so = (StockOffer) msg.getContentObject();
		} catch (UnreadableException e) {
			System.out.println("Failed to extract stock offer message");
			e.printStackTrace();
		}
		return so;
	}

	protected AID getCompanyAID(String companyName) {
		for (int i = 0; i < companyAgents.length; i++) {
			if (companyAgents[i].getName().getLocalName().equals(companyName)) {
				return companyAgents[i].getName();
			}
		}
		return null;
	}

	protected void companyStatePrint() {
		System.out.println("======================");
		System.out.println(getLocalName() + " state: " + state);
		System.out.println(getLocalName() + " capital: " + companyCapital);
		System.out.println(getLocalName() + " stocks: ");
		for (String keyInner : companyStocksMap.keySet()) {
			System.out.println("\t" + keyInner + " - " + companyStocksMap.get(keyInner));
		}
		System.out.println("======================");
	}

	protected void setupPersonality() {
		String personalityChar = (String) this.getArguments()[0];
		switch (personalityChar) {
		case "R":
			this.personality = CompanyPersonality.ROOKIE;
			break;
		case "A":
			this.personality = CompanyPersonality.ADVANCED;
			break;
		default:
			this.personality = CompanyPersonality.ROOKIE;
			break;
		}
	}

}