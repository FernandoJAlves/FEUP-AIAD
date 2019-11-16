package agents.CompanyAgent;

import java.util.HashMap;
import java.util.Arrays;
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

	private CompanyState state = CompanyState.SEARCH;

	private String dealAgent = "";
	private StockOffer actualOffer = null;

	// Maximum number of stocks of 1 company
	private static Integer maxStockAmmount = 10000;

	private class CompanyBehaviour extends TickerBehaviour {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

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
			case MARKET:
				this.market();
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
			// TODO: Check if these prints can be removed or if Juan needs them
			// myLogger.log(Logger.INFO, "Agent " + getLocalName() + " old state: " +
			// state);
			// System.out.println("Agent " + getLocalName() + " old state: " + state);

			if (msg != null) {

				switch (state) {
				case WORK:
					if (msg.getPerformative() == ACLMessage.PROPOSE) {
						StockOffer offer = getOffer(msg);
						if ((offer != null) && (offer.getTag().equals("BUY"))) {
							// TODO: Think about the offer
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
						setState(CompanyState.WORK);
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
								companyStocksMap.put(actualOffer.getCompanyName(), tempStockAmount + actualOffer.getStockCount());
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
			// TODO: Check if these prints can be removed or if Juan needs them
			// System.out.println("Agent " + getLocalName() + " new state: " + state);
			// myLogger.log(Logger.INFO, "Agent " + getLocalName() + " new state: " +
			// state);
		}

		public void work() {

			// While working, a company's capital increases between 0% and 3%
			Integer maxCapitalChange = (int) Math.round(companyCapital * 1.03);

			companyCapital = ThreadLocalRandom.current().nextInt(companyCapital, maxCapitalChange);
			// System.out.println("WORKING: " + companyCapital); // TODO: Remove this print, only for debug

			// Notify Economy - TODO: Function this?
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


			// TODO: Decide if the state should change to SEARCH (depends on the personality?)
			if (companyCapital >= 10000000) {
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

			ACLMessage msg;

			// TODO: Remove this if/else, only here to prevent errors (simulates a valid
			// buy)
			if (getLocalName().equals("company2")) {
				msg = makeOfferMessage("company1", 3000, 3000, chosenCompany);
			} else {
				msg = makeOfferMessage("company2", 3000, 3000, chosenCompany);
			}

			setState(CompanyState.NEGOTIATE);
			sendCustom(msg);
		}

		public void negotiate(ACLMessage msg) {

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
				// TODO: after we fix message, this print and if/else can probably be deleted
			}


			// Notify Economy - TODO: Function this? (used somewhere else I believe)
			ACLMessage notifyEconomyMsg = new ACLMessage(ACLMessage.PROPAGATE);

			TransactionNotifyMessage content = new TransactionNotifyMessage(dealAgent, getLocalName(), actualOffer.getCompanyName(), actualOffer.getStockCount(), actualOffer.getOfferValue());
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

		public void market() {

		}

		public void rejectProposals(ACLMessage msg) {
			ACLMessage reply = msg.createReply();
			reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
			reply.setContent("BUSY");
			sendCustom(reply);
			msg = null;
		}

		public void queryEconomy() {
			this.queryEconomyAux("ALL");
		}

		public void queryEconomy(AID company) {
			queryEconomyAux(company.getName());

		}

		public void queryEconomyAux(String content){
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
		}
	} // END of inner class CompanyBehaviour

	protected void setup() {

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

		// Company's starting capital is a random value between 30000 and 60000
		companyCapital = ThreadLocalRandom.current().nextInt(30000, 60001);
		System.out.printf("\t> %s Capital: %d\n", getLocalName(), companyCapital);

		// Company's initial stock map has only 1 entry, the companies' own stock, with
		// value of maxStockAmmout
		companyStocksMap.put(getLocalName(), maxStockAmmount);

		// Pick starting value for company stock value, between 10 and 50
		Double actionValue = ThreadLocalRandom.current().nextDouble(10, 51);
		CompanySetupMessage content = new CompanySetupMessage(getLocalName(), actionValue, maxStockAmmount,
				companyCapital);
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

	protected void sendCustom(ACLMessage msg) {
		// System.out.println(" -> " + getLocalName() + " is Sending " +
		// ACLMessage.getPerformative(msg.getPerformative())); TODO: remove this print?
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
}