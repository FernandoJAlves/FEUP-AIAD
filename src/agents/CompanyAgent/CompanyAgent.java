package agents.CompanyAgent;

import java.util.concurrent.ThreadLocalRandom;

import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.util.Logger;

import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;

import agents.CompanyAgent.CompanyGlobals.*;

public class CompanyAgent extends Agent {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private Logger myLogger = Logger.getMyLogger(getClass().getName());

	private DFAgentDescription[] companyAgents;

	private CompanyState state = CompanyState.SEARCH;

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
			case BUY:
				this.buy(msg);
			case MARKET:
				this.market();
			case DEAL:
				this.deal(msg);
			default:
				this.search();
			}
		}

		public ACLMessage listen() {
			ACLMessage msg = myAgent.receive();
			if (msg != null && 
			msg.getSender().getLocalName().equals(getLocalName())) {
				return null;
			}
			return msg;
		}

		public void updateState(ACLMessage msg) {
			if (msg != null) {

				switch (msg.getPerformative()) {
				case ACLMessage.PROPOSE:
					if (state == CompanyState.SEARCH) {
						String content = msg.getContent();
						if ((content != null) && (content.indexOf("BUY") != -1)) {
							setState(CompanyState.DEAL);
							myLogger.log(Logger.INFO, "Agent " + getLocalName() + " - Received BUY message from "
									+ msg.getSender().getLocalName());
						}
					}
				case ACLMessage.ACCEPT_PROPOSAL:
					if (state == CompanyState.BUY) {
						String content = msg.getContent();
						if ((content != null) && (content.indexOf("DEAL") != -1)) {
							myLogger.log(Logger.INFO, "Agent " + getLocalName() + " - Received DEAL message from "
									+ msg.getSender().getLocalName());
						}
					}
				case ACLMessage.REJECT_PROPOSAL:
					if (state == CompanyState.BUY) {
						String content = msg.getContent();
						setState(CompanyState.SEARCH);
						if ((content != null) && (content.indexOf("DEAL") != -1)) {
							myLogger.log(Logger.INFO, "Agent " + getLocalName() + " - Received BUSY message from "
									+ msg.getSender().getLocalName());
						}
					}
				case ACLMessage.INFORM:
					if (state == CompanyState.BUY) {
						String content = msg.getContent();
						setState(CompanyState.SEARCH);
						if ((content != null) && (content.indexOf("ACTION") != -1)) {
							myLogger.log(Logger.INFO, "Agent " + getLocalName() + " - Received ACTION message from "
									+ msg.getSender().getLocalName());
						}
					}
				default:
				}
			} else {
				setState(CompanyState.BUY);
			}
		}

		public void search() {
			int agentsMax = companyAgents.length;
			int companyIndex = ThreadLocalRandom.current().nextInt(0, agentsMax);
			ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
			msg.setContent("BUY");
			msg.addReceiver(companyAgents[companyIndex].getName());
			setState(CompanyState.BUY);
			send(msg);
		}

		public void buy(ACLMessage msg) {

		}

		public void market() {

		}

		public void deal(ACLMessage msg) {
			if (msg == null)
				return;

			ACLMessage reply = msg.createReply();
			reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
			reply.setContent("DEAL");

			ACLMessage action = msg.createReply();
			reply.setPerformative(ACLMessage.INFORM);
			reply.setContent("ACTION");

			send(reply);
			send(action);
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
			// System.out.println("NUMBER OF RESULTS: " + result.length);
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

		// Actually send message TODO: Change content, might want more stuff
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setContent(getLocalName());
		msg.addReceiver(economyID);
		send(msg);

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
		} catch (FIPAException e) {
			agents = new DFAgentDescription[0];
			e.printStackTrace();
		}
		this.companyAgents = agents;
	}
}