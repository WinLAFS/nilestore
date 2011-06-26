/**
 * This file is part of the Nilestore project.
 * 
 * Copyright (C) (2011) Nile University (NU)
 *
 * Nilestore is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package eg.nileu.cis.nilestore.simulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.PatternLayout;
import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.p2p.fd.ping.PingFailureDetectorConfiguration;
import se.sics.kompics.timer.Timer;
import eg.nileu.cis.nilestore.channelfilters.MessageDestinationFilter;
import eg.nileu.cis.nilestore.channelfilters.WebRequestDestinationFilter;
import eg.nileu.cis.nilestore.introducer.IntroducerConfiguration;
import eg.nileu.cis.nilestore.monitor.NilestoreMonitorConfiguration;
import eg.nileu.cis.nilestore.peer.ClientConfiguration;
import eg.nileu.cis.nilestore.peer.NsPeer;
import eg.nileu.cis.nilestore.peer.NsPeerInit;
import eg.nileu.cis.nilestore.simulator.port.NsExperiment;
import eg.nileu.cis.nilestore.utils.EncodingParam;
import eg.nileu.cis.nilestore.utils.FileUtils;
import eg.nileu.cis.nilestore.webserver.port.OperationRequest;
import eg.nileu.cis.nilestore.webserver.port.ServletRequest;
import eg.nileu.cis.nilestore.webserver.port.Web;
import eg.nileu.cis.nilestore.webserver.port.WebRequest;
import eg.nileu.cis.nilestore.webserver.port.WebResponse;

// TODO: Auto-generated Javadoc
/**
 * The Class NsSimulator.
 * 
 * @author Mahmoud Ismail <mahmoudahmedismail@gmail.com>
 */
public class NsSimulator extends ComponentDefinition {

	/** The experiment port. */
	Negative<NsExperiment> experiment = provides(NsExperiment.class);

	/** The web port. */
	Negative<Web> web = provides(Web.class);

	/** The network port. */
	Positive<Network> network = requires(Network.class);

	/** The timer port. */
	Positive<Timer> timer = requires(Timer.class);

	/** The peer0 address. */
	private Address peer0Address;

	/** The introducer configuration. */
	private IntroducerConfiguration introducerConfiguration;

	/** The monitor configuration. */
	private NilestoreMonitorConfiguration monitorConfiguration;

	/** The home dir. */
	private String homeDir;

	/** The webport. */
	private int webport;

	/** The nodes map. */
	private final HashMap<String, Component> nodes;

	/** The first node id. */
	private int nodeId = 1;

	/** The logs map. */
	private final HashMap<String, List<String>> logs;

	/** The logger. */
	Logger logger = LoggerFactory.getLogger(NsSimulator.class);

	// TODO: edit the interface by adding for each node in the nodepane radioset
	// [on off]
	/**
	 * Instantiates a new ns simulator.
	 */
	public NsSimulator() {
		nodes = new HashMap<String, Component>();
		logs = new HashMap<String, List<String>>();

		ComponentAppender appender = new ComponentAppender(this);
		appender.setLayout(new PatternLayout(
				"%d{[HH:mm:ss,SSS]} %-5p {%c{1}} %m%n"));
		org.apache.log4j.Logger.getRootLogger().addAppender(appender);

		subscribe(handleInit, control);
		subscribe(handleWebRequest, web);
	}

	/** The handle init. */
	Handler<NsSimulatorInit> handleInit = new Handler<NsSimulatorInit>() {
		@Override
		public void handle(NsSimulatorInit init) {

			peer0Address = init.getPeer0Address();
			introducerConfiguration = init.getIntroducerConfiguration();
			monitorConfiguration = init.getMonitorConfiguration();
			homeDir = init.getHomeDir();
			webport = init.getWebport();
			FileUtils.mkdirsifnotExists(homeDir);
			logger.info("Initiated");
		}
	};

	/** The handle web request. */
	Handler<WebRequest> handleWebRequest = new Handler<WebRequest>() {
		@SuppressWarnings("unchecked")
		@Override
		public void handle(WebRequest event) {

			ServletRequest request = event.getRequest();
			if (request instanceof OperationRequest) {
				OperationRequest req = (OperationRequest) request;
				String op = req.getOperation();
				logger.info(" Got " + op);
				if (op.equals("run")) {
					int nc = Integer.valueOf(req.getParameter("nc"));
					int sc = Integer.valueOf(req.getParameter("sc"));
					int k = Integer.valueOf(req.getParameter("k"));
					int n = Integer.valueOf(req.getParameter("n"));

					for (int i = 0; i < nc; i++) {
						boolean storageEnabled = true;
						if (i >= sc) {
							storageEnabled = false;
						}
						createAndStartNewNode(storageEnabled, k, n);
					}
					trigger(new WebResponse(event, "nodes created"), web);
				} else if (op.equals("getlog")) {

					int dest = Integer.valueOf(req.getParameter("mdest"));
					String node = dest == -1 ? "introducer"
							: dest == 0 ? "monitor" : "node" + dest;
					List<String> nlogs = new ArrayList<String>();
					synchronized (logs) {
						if (logs.containsKey(node)) {
							nlogs.addAll(logs.get(node));
							logs.get(node).clear();
						}
					}

					JSONArray arr = new JSONArray();
					arr.addAll(nlogs);

					trigger(new WebResponse(event, arr.toJSONString()), web);
				}
			}
		}
	};

	/**
	 * Creates the and start new node.
	 * 
	 * @param storageEnabled
	 *            the storage enabled
	 * @param k
	 *            the k
	 * @param n
	 *            the n
	 * @return the component
	 */
	private final Component createAndStartNewNode(boolean storageEnabled,
			int k, int n) {
		Address nodeAddress = getNodeAddress();

		Component node = create(NsPeer.class);

		connect(node.required(Timer.class), timer);
		connect(node.required(Network.class), network,
				new MessageDestinationFilter(nodeAddress));
		connect(node.provided(Web.class), web, new WebRequestDestinationFilter(
				nodeId));

		// connect(node.required(NetworkControl.class), networkcontrol);

		PingFailureDetectorConfiguration pingConfiguration = new PingFailureDetectorConfiguration(
				1000, 5000, 1000, 0, Transport.TCP);

		String nickname = "node" + nodeId;
		ClientConfiguration clientConfiguration = new ClientConfiguration(
				nickname, storageEnabled, FileUtils.JoinPath(homeDir,
						String.valueOf(nodeId)), new EncodingParam(k, n));

		synchronized (nodes) {
			nodes.put(nickname, node);
		}

		trigger(new NsPeerInit(nodeAddress, webport, clientConfiguration, null,
				introducerConfiguration, monitorConfiguration,
				pingConfiguration), node.getControl());
		trigger(new Start(), node.getControl());
		nodeId++;
		return node;
	}

	/**
	 * Gets the node address.
	 * 
	 * @return the node address
	 */
	private Address getNodeAddress() {
		return new Address(peer0Address.getIp(), peer0Address.getPort(), nodeId);
	}

	/**
	 * Adds the logging event.
	 * 
	 * @param loggerName
	 *            the logger name
	 * @param logMessage
	 *            the log message
	 */
	public void addLoggingEvent(String loggerName, String logMessage) {

		synchronized (logs) {
			if (!logs.containsKey(loggerName)) {
				logs.put(loggerName, new ArrayList<String>());
			}

			logs.get(loggerName).add(logMessage);
		}
	}
}
