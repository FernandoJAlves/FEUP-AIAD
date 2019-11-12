package agents.CompanyAgent;

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

import messages.*;

public class CompanyAgent extends Agent {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private Logger myLogger = Logger.getMyLogger(getClass().getName());

	private DFAgentDescription[] companyAgents;

	private CompanyState state = CompanyState.SEARCH;

	private static Integer stockAmmount = 10000;

	private class CompanyBehaviour extends TickerBehaviour {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public CompanyBehaviour(Agent a, int miliseconds) {
			super(a, miliseconds);
		}

		public void onTick() {
			ACLMessage msg = this.listen();

			this.updateState(msg);

			switch (state) {
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
				mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
				tmp = MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL);
				mt = MessageTemplate.or(mt, tmp);
				break;
			case DEAL:
				mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
				tmp = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
				mt = MessageTemplate.or(mt, tmp);
				break;
			case BUY:
				mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
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
			if (msg != null && msg.getSender().getLocalName().equals(getLocalName())) {
				return null;
			}
			return msg;
		}

		public void updateState(ACLMessage msg) {
			// myLogger.log(Logger.INFO, "Agent " + getLocalName() + " old state: " + state);
			System.out.println("Agent " + getLocalName() + " old state: " + state);

			if (msg != null) {

				switch (msg.getPerformative()) {
				case ACLMessage.PROPOSE:
					if (state == CompanyState.SEARCH) {
						String content = msg.getContent();
						if ((content != null) && (content.indexOf("BUY") != -1)) {
							setState(CompanyState.DEAL);
							// myLogger.log(Logger.INFO, "Agent " + getLocalName() + " - Received BUY message from "
							// 		+ msg.getSender().getLocalName());
						}
					}
					break;
				case ACLMessage.ACCEPT_PROPOSAL:
					if (state == CompanyState.NEGOTIATE) {
						String content = msg.getContent();
						if ((content != null) && (content.indexOf("DEAL") != -1)) {
							setState(CompanyState.BUY);
							// myLogger.log(Logger.INFO, "Agent " + getLocalName() + " - Received DEAL message from "
							// 		+ msg.getSender().getLocalName());
						}
					}
					break;
				case ACLMessage.REJECT_PROPOSAL:
					if (state == CompanyState.NEGOTIATE) {
						String content = msg.getContent();
						setState(CompanyState.SEARCH);
						if ((content != null) && (content.indexOf("BUSY") != -1)) {
							// myLogger.log(Logger.INFO, "Agent " + getLocalName() + " - Received BUSY message from "
							// 		+ msg.getSender().getLocalName());
						}
					}
					break;
				case ACLMessage.REQUEST:
					if (state == CompanyState.DEAL) {
						String content = msg.getContent();
						setState(CompanyState.CLOSE);
						if ((content != null) && (content.indexOf("ACTION") != -1)) {
							// myLogger.log(Logger.INFO, "Agent " + getLocalName() + " - Received ACTION message from "
							// 		+ msg.getSender().getLocalName());
						}
					}
					break;
				case ACLMessage.INFORM:
					if (state == CompanyState.BUY) {
						String content = msg.getContent();
						setState(CompanyState.SEARCH);
						if ((content != null) && (content.indexOf("ACTION") != -1)) {
							// myLogger.log(Logger.INFO, "Agent " + getLocalName() + " - Received ACTION message from "
							// 		+ msg.getSender().getLocalName());
						}
					}
					break;
				default:
					break;
				}
			}
			System.out.println("Agent " + getLocalName() + " new state: " + state);
			//myLogger.log(Logger.INFO, "Agent " + getLocalName() + " new state: " + state);
		}

		public void search() {
			int agentsMax = companyAgents.length;

			if (agentsMax == 0) {
				return;
			}

			int companyIndex = ThreadLocalRandom.current().nextInt(0, agentsMax);
			ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
			msg.setContent("BUY");
			msg.addReceiver(companyAgents[companyIndex].getName());
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

			if (msg.getPerformative() == ACLMessage.PROPOSE) {
				ACLMessage reply = msg.createReply();
				reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
				reply.setContent("BUSY");
				sendCustom(reply);
				return;
			}

			ACLMessage reply = msg.createReply();
			reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
			reply.setContent("DEAL");
			sendCustom(reply);

		}

		public void close(ACLMessage msg) {
			if (msg == null)
				return;

			ACLMessage reply = msg.createReply();
			reply.setPerformative(ACLMessage.INFORM);
			reply.setContent("ACTION");

			sendCustom(reply);
			setState(CompanyState.SEARCH);
		}

		public void market() {

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

		this.getCompanies();

		// Send message to Economy to inform there is a new company

		DFAgentDescription dfdEconomy = new DFAgentDescription();
		ServiceDescription sdEconomy = new ServiceDescription();
		sdEconomy.setType("EconomyAgent");
		dfdEconomy.addServices(sdEconomy);

		AID economyID;

		// Search the DF
		try {
			DFAgentDescription[] result = DFService.search(this, dfdEconomy);
			if (result.length > 0)
				economyID = result[0].getName();
			else {
				System.out.println("ERROR - Economy not found!");
				return;
			}
		} catch (FIPAException fe) {
			fe.printStackTrace();
			return;
		}

		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

		// Pick starting value for company stock value, between 10 and 50
		Double actionValue = ThreadLocalRandom.current().nextDouble(10, 51);
		CompanySetupMessage content = new CompanySetupMessage(getLocalName(), actionValue, stockAmmount);
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
			agents = Arrays.stream(agents).filter(agent -> agent.getName() != this.getAID()).toArray(DFAgentDescription[]::new);
		} catch (FIPAException e) {
			agents = new DFAgentDescription[0];
			e.printStackTrace();
		}
		this.companyAgents = agents;
	}

	protected void sendCustom (ACLMessage msg) {
		System.out.println(" -> " + getLocalName() + " is Sending " + msg.getPerformative(msg.getPerformative()));
		send(msg);
	}
}