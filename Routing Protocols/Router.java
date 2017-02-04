import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;

import tcdIO.Terminal;

/**
 * @author Ciarán Ingle, Cian McGrath, Leon Sinclair
 */
public class Router extends Node {
	public InetSocketAddress socketAddress;

	private RoutingTable routingTable;
	private ArrayList<InetSocketAddress> connectedRouters = new ArrayList<InetSocketAddress>();
	private ArrayList<InetSocketAddress> connectedEndNodes = new ArrayList<InetSocketAddress>();
	
	private Terminal terminal;
	
	private ArrayList<InetSocketAddress> routers;	//Stores the connected routers of a requested router
	
	//prevents a * message from being endlessly broadcasted
	private String lastRouter = "", lastEndNode = "", lastMessage = "";
	
	/**
	 * Constructor
	 * Creates a router that uses a passed InetSocketAddress.
	 */
	public Router(InetSocketAddress address) throws SocketException {
		socketAddress = address;
		socket = new DatagramSocket(socketAddress);
		
		listener.go();

		terminal = new Terminal(Kremlin.devices.getRouterName(socketAddress) + " - Router");
		terminal.println("Address:\t" + socketAddress.getHostName());
		terminal.println("Port:\t\t" + socketAddress.getPort());
		terminal.println("Router initialised.\n");
	}

	/**
	 * Called when this router receives a packet.
	 * 
	 * The type of data received by the packet is denoted by a single character stored at the beginning of the data.
	 * These characters are defined within Kremlin.java.
	 */
	public synchronized void onReceipt(DatagramPacket receivedPacket) {
		terminal.println("Packet received.");

		//if the received packet is a request for connected routers
		if (new String(receivedPacket.getData()).charAt(0) == Kremlin.CONNECTED_ROUTERS_REQUEST) {
			ConnectedRoutersRequest request = new ConnectedRoutersRequest(receivedPacket.getData());
			
			//determine whether the request is destined for this router or another
			if (request.destination.equals(socketAddress))
				sendConnectedRouters(request.source);
			else
				getNextNode(request.destination);
		}
		//else if the received packet contains a router's connected routers
		else if (new String(receivedPacket.getData()).charAt(0) == Kremlin.CONNECTED_ROUTERS_RESPONSE&& Kremlin.routingApproach == Kremlin.LINK_STATE_ROUTING) {
			ConnectedRoutersResponse response = new ConnectedRoutersResponse(receivedPacket.getData());
			
			//determine whether the packet is destined for this router or another
			if (response.destination.equals(socketAddress)) {
				routers = response.getConnectedRouters();
				this.notify();
			}
			else {
				DatagramPacket packet = new DatagramPacket(receivedPacket.getData(), receivedPacket.getLength(), getNextNode(response.destination));
				try {
					socket.send(packet);
				}
				catch (IOException e) { e.printStackTrace(); }
			}
		}
		//else if the received packet contains a routing table
		else if (new String(receivedPacket.getData()).charAt(0) == Kremlin.ROUTING_TABLE && Kremlin.routingApproach == Kremlin.DISTANCE_VECTOR_ROUTING) {		
			RoutingTable received = new RoutingTable(receivedPacket.getData());
			InetSocketAddress nextRouter = (InetSocketAddress)receivedPacket.getSocketAddress();
			
			boolean sendUpdate = false;
			
			//determine whether any new data is contained in the received routing table
			for (int i = 0; i < received.size(); i++) {				
				boolean exists = false;
				for (int j = 0; j < routingTable.size(); j++) {
					if (received.get(i).endNode.equals(routingTable.get(j).endNode)) {
						exists = true;

						if (received.get(i).hops < routingTable.get(j).hops || 
								(nextRouter.equals(routingTable.get(j).nextStop) && received.get(i).hops != routingTable.get(j).hops)) {
							routingTable.replace(j, received.get(i));
							sendUpdate = true;
						}
					}
					
				}
				
				//if there is new data, add it to the routing table and broadcast the new routing table
				if (!exists) {
					routingTable.add(received.get(i));
					sendUpdate = true;
				}
			}
			
			//transmit routing table to all connected routers if new updates have been received.
			if (sendUpdate) {
				print(routingTable, "Routing Table");
				
				String table = routingTable.toString();
				
				for (int i = 0; i < connectedRouters.size(); i++) {
					DatagramPacket packet = new DatagramPacket(table.getBytes(), table.getBytes().length, connectedRouters.get(i));
					try {
						socket.send(packet);
					}
					catch (IOException e) { e.printStackTrace(); }
				}
			}
			
		}
		// otherwise the packet should be forwarded to another router, or a client of this router
		else {
			PacketContent packetContent = new PacketContent(receivedPacket.getData());
			terminal.println("Destination Node:\t" + packetContent.endNode);
			terminal.println("Destination Router:\t" + packetContent.router);
			terminal.println("Message:\t\t" + packetContent.message + "\n");
			
			try {
				if (packetContent.router.equals("*") || packetContent.endNode.equals("*")) {
					// broadcast packet to all routers if it has not been already received
					if (packetContent.router.equals("*") && !lastRouter.equals(packetContent.router)
							&& !lastEndNode.equals(packetContent.endNode) && !lastMessage.equals(packetContent.message)) {
						for (int i = 0; i < connectedRouters.size(); i++) {
							DatagramPacket packet = new DatagramPacket(receivedPacket.getData(), receivedPacket.getData().length, connectedRouters.get(i));
							socket.send(packet);
						}
						
						// send packet to all relevant end nodes of the router
						for (int i = 0; i < connectedEndNodes.size(); i++) {
							if (packetContent.endNode.equals("*") || Kremlin.devices.getClientName(connectedEndNodes.get(i)).equals(packetContent.endNode)) {
								DatagramPacket packet = new DatagramPacket(receivedPacket.getData(), receivedPacket.getData().length, connectedEndNodes.get(i));
								socket.send(packet);
							}
						}
						
						lastRouter = packetContent.router;
						lastEndNode = packetContent.endNode;
						lastMessage = packetContent.message;
					}
					
					// broadcast packet to all end nodes connected if the packet is destined for this router
					if (packetContent.endNode.equals("*") && !packetContent.router.equals("*")) {
						if (Kremlin.devices.getInetSocketAddress(packetContent.router).equals(socketAddress)) {
							for (int i = 0; i < connectedEndNodes.size(); i++) {
								DatagramPacket packet = new DatagramPacket(receivedPacket.getData(), receivedPacket.getData().length, connectedEndNodes.get(i));
								socket.send(packet);
							}
						}
						else {
							InetSocketAddress nextNode = getNextNode(packetContent);
							DatagramPacket packet = new DatagramPacket(receivedPacket.getData(), receivedPacket.getData().length, nextNode);
							socket.send(packet);
						}
					}
				}
				else {
					InetSocketAddress nextNode = getNextNode(packetContent);
					DatagramPacket packet = new DatagramPacket(receivedPacket.getData(), receivedPacket.getData().length, nextNode);
					socket.send(packet);
				}
			}
			catch (IOException e) { e.printStackTrace(); } 
		}
	}
	
	/**
	 * Consults routing table to find the next destination for the current packet
	 */
	private InetSocketAddress getNextNode(PacketContent packetContent) {		
		if (Kremlin.routingApproach == Kremlin.LINK_STATE_ROUTING) {
			if (Kremlin.devices.getInetSocketAddress(packetContent.router).equals(socketAddress)) {
				return Kremlin.devices.getInetSocketAddress(packetContent.endNode, packetContent.router);
			}
			else {
				RoutingTable.Entry entry = null;
				
				for (int i = 0; i < routingTable.size() && entry == null; i++) {
					if (Kremlin.devices.getInetSocketAddress(packetContent.router).equals(routingTable.get(i).destination))
						entry = routingTable.get(i);
				}
				
				while (!entry.source.equals(socketAddress)) {
					for (int i = 0; i < routingTable.size(); i++) {
						if (entry.source.equals(routingTable.get(i).destination))
							entry = routingTable.get(i);
					}
				}
	
				return entry.destination;
			}
		}
		else if (Kremlin.routingApproach == Kremlin.DISTANCE_VECTOR_ROUTING) {
			for (int i = 0; i < routingTable.size(); i++) {
				if (routingTable.get(i).endNode.equals(Kremlin.devices.getInetSocketAddress(packetContent.endNode, packetContent.router)))
					return (routingTable.get(i).hops == 1)? routingTable.get(i).endNode : routingTable.get(i).nextStop;
			}
		}
		
		return null;
	}
	
	/**
	 * Consults routing table to find the next node to go to in order to reach the destination.
	 */
	private InetSocketAddress getNextNode(InetSocketAddress destination) {
		if (Kremlin.routingApproach == Kremlin.LINK_STATE_ROUTING) {
			RoutingTable.Entry entry = null;
			
			for (int i = 0; i < routingTable.size() && entry == null; i++) {
				if (destination.equals(routingTable.get(i).destination))
					entry = routingTable.get(i);
			}
			
			while (!entry.source.equals(socketAddress)) {
				for (int i = 0; i < routingTable.size(); i++) {
					if (entry.source.equals(routingTable.get(i).destination))
						entry = routingTable.get(i);
				}
			}

			return entry.destination;
		}
		
		return null;
	}
	
	/**
	 * Sends this router's connected routers to the destination specified.
	 */
	public void sendConnectedRouters(InetSocketAddress destination) {		
		String response = new ConnectedRoutersResponse(destination, connectedRouters).toString();

		try {
			socket.send(new DatagramPacket(response.getBytes(), response.getBytes().length, destination));
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	/**
	 * Requests the connected routers from a specified router.
	 */
	private synchronized void getConnectedRouters(InetSocketAddress socketAddress) throws InterruptedException {
		if (socketAddress.equals(this.socketAddress)) {
			routers = connectedRouters;
		}
		else {
			routers = new ArrayList<InetSocketAddress>();
			
			String connectedRouterRequest = new ConnectedRoutersRequest(this.socketAddress, socketAddress).toString();
			try {
				socket.send(new DatagramPacket(connectedRouterRequest.getBytes(), connectedRouterRequest.getBytes().length, socketAddress));
			}
			catch (IOException e) { e.printStackTrace(); }
			
			this.wait();	//wait until routers are received
		}
	}

	/**
	 * Determines time taken to acknowledge a packet sent between two routers.
	 */
	public long ping(InetSocketAddress source, InetSocketAddress destination) {
		return Kremlin.weightMap.getWeight(source, destination);
	}

	/**
	 * Connects this router to the passed router. These 2 routers can now directly communicate.
	 */
	public void link(Router router) {
		if (!connectedRouters.contains(router.socketAddress) && !router.equals(this))
			connectedRouters.add(router.socketAddress);
	}

	/**
	 * Connects an end-node to this router. These 2 devices can now directly communicate.
	 */
	public void link(Client client) {
		if (!connectedEndNodes.contains(client.socketAddress))
			connectedEndNodes.add(client.socketAddress);
	}
	
	/**
	 * Establish routing table, then start receiving and forwarding data.
	 */
	public synchronized void start() throws InterruptedException {
		createRoutingTable(Kremlin.routingApproach);
		
		terminal.println("Listening for packets...");
	}

	/**
	 * Determines whether link-state or distance-vector routing should be used based on
	 * what the user chose in Kremlin.java
	 */
	public void createRoutingTable(int routingApproach) throws InterruptedException {
		if (routingApproach == Kremlin.LINK_STATE_ROUTING) 
			linkStateRouting(); 
		else if (routingApproach == Kremlin.DISTANCE_VECTOR_ROUTING)
			distanceVectorRouting();
	}
	
	/**
	 * Uses Dijkstra's shortest path algorithm to create a routing table for the
	 * router to determine the quickest route to a node.
	 */
	public void linkStateRouting() throws InterruptedException {
		routingTable = new RoutingTable();
		RoutingTable tentativeNodes = new RoutingTable();

		terminal.println("Establishing routing table via link state routing...");

		tentativeNodes.add(new RoutingTable.Entry(socketAddress, socketAddress, 0));
		while (tentativeNodes.size() > 0) {			
			RoutingTable.Entry fastestPath = tentativeNodes.getFastestPath();
			routingTable.add(fastestPath);
			tentativeNodes.remove(fastestPath);
			
			getConnectedRouters(fastestPath.destination);	//sets 'routers' to the routers connected to the fastest path's destination			
			
			for (int i = 0; i < routers.size(); i++) {
				if (!routingTable.containsDestination(routers.get(i)))
					tentativeNodes.add(new RoutingTable.Entry(fastestPath.destination, routers.get(i),
							Kremlin.weightMap.getWeight(fastestPath.destination, routers.get(i)) + fastestPath.weight));
			}
		}
		
		print(routingTable, "Routing Table");
		terminal.println("Established routing table.\n");
	}

	/**
	 * Uses the Bellman-Ford algorithm to create a routing table for the router
	 * to determine the shortest path between nodes.
	 */
	private void distanceVectorRouting() {
		terminal.println("Establishing routing table via distance vector routing...");

		initialiseRoutingTable();	//add end-nodes to routing table
		
		if (routingTable.size() > 0) {
			String table = routingTable.toString();
			for (int i = 0; i < connectedRouters.size(); i++) {
				DatagramPacket packet = new DatagramPacket(table.getBytes(), table.getBytes().length, connectedRouters.get(i));
				try {
					socket.send(packet);
				}
				catch (IOException e) { e.printStackTrace(); }
			}
		}

		terminal.println("Established routing table.\n");
	}
	
	/**
	 * Adds all connected end-nodes to the routing table. Necessary for distance-vector routing.
	 */
	public void initialiseRoutingTable() {
		routingTable = new RoutingTable();
		
		for (int i = 0; i < connectedEndNodes.size(); i++)
			routingTable.add(new RoutingTable.Entry(connectedEndNodes.get(i), socketAddress, 1));
	}
	
	private void print(RoutingTable table, String name) {
		terminal.println("\n" + name + ":");
		for (int j = 0; j < table.size(); j++)
			terminal.println(table.get(j).toString());
		terminal.println();
	}

	/**
	 * Prints the connected routers and end-nodes to the router's terminal.
	 */
	public void printConnectedDevices() {
		terminal.println("Connected Routers: ");
		for (int i = 0; i < connectedRouters.size(); i++)
			terminal.println("\t" + Kremlin.devices.getRouterName(connectedRouters.get(i)));

		terminal.println("Connected End-Nodes: ");
		for (int i = 0; i < connectedEndNodes.size(); i++)
			terminal.println("\t" + Kremlin.devices.getClientName(connectedEndNodes.get(i)));

		terminal.println();
	}
}