import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

import javax.swing.JOptionPane;


/*
 * 
        /\      /\
      <~  ~>  <~  ~>
       |/\|    |/\|
        /\      /\
       #  \    /`-\
      / ###\  /`--_\
    ,#     ##.`---__`.
   /  ####    \--__ ~ \
   #      #####__  ~~~~|
   \#####     /  ~~~~~/
    `____#####_~~---_'
 */

/**
 * @author Ciarán Ingle, Cian McGrath, Leon Sinclair
 */
public class Kremlin {
	// Routing approaches
	public static final int LINK_STATE_ROUTING = 0; 		// Dijkstra's algorithm
	public static final int DISTANCE_VECTOR_ROUTING = 1; 	// Bellman-Ford algorithm
	
	//Packet type identifiers
	public static final char MESSAGE = 'm';
	public static final char ROUTING_TABLE = 't';
	public static final char CONNECTED_ROUTERS_RESPONSE = 'r';
	public static final char CONNECTED_ROUTERS_REQUEST = 'q';

	public static final String DEFAULT_DST_NODE = "localhost";
	private static final int DEFAULT_PORT = 50000;
	private static int nextFreePort = DEFAULT_PORT;

	private static ArrayList<Router> routers = new ArrayList<Router>();
	private static ArrayList<Client> clients = new ArrayList<Client>();
	
	public static BidirectionalMap devices = new BidirectionalMap();	//Used to convert an InetSocketAddress to a device name	
	public static WeightMap weightMap = new WeightMap();	//Stores the weights between routers for link-state routing
	
	public static int routingApproach;	//Link-state or Distance-vector

	/**
	 * Creates a connection between two routers.
	 * The weight between the two routers is the delay in ms between them.
	 */
	public static void link(Router router1, Router router2, int weight) {
		router1.link(router2);
		router2.link(router1);
		weightMap.add(router1.socketAddress, router2.socketAddress, weight);
	}

	/**
	 * Creates a new router and adds it to the ArrayList of routers.
	 * Its InetSocketAddress and name are stored.
	 */
	private static Router newRouter(String name) throws IOException {
		InetSocketAddress socketAddress = new InetSocketAddress(DEFAULT_DST_NODE, nextFreePort++);
		Kremlin.devices.put(name, socketAddress);
		
		Router router = new Router(socketAddress);
		routers.add(router);

		return router;
	}
	
	/**
	 * Creates a new client with its associated router and adds it to the ArrayList of clients.
	 * Its InetSocketAddress and name are stored.
	 */
	private static Client newClient(String name, Router router) throws SocketException {
		InetSocketAddress socketAddress = new InetSocketAddress(DEFAULT_DST_NODE, nextFreePort++);
		Kremlin.devices.put(name, devices.getRouterName(router.socketAddress), socketAddress);
		
		Client client = new Client(socketAddress, router.socketAddress);
		router.link(client);
		clients.add(client);
		
		return client;
	}

	/**
	 * Main function to create all clients and routers. Creates connections
	 * between all linked nodes.
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		String[] options = { "Link State Routing", "Distance Vector Routing" };
		routingApproach = JOptionPane.showOptionDialog(null, "Choose path finding method:", null,
				JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		
		// Create routers
		Router a = newRouter("A");
		Router b = newRouter("B");
		Router c = newRouter("C");
		Router d = newRouter("D");
		Router e = newRouter("E");
		Router f = newRouter("F");
		Router g = newRouter("G");
		Router h = newRouter("H");
		
		// Link connected routers
		link(a, b, 2);
		link(a, g, 6);
		link(b, c, 7);
		link(b, e, 2);
		link(c, d, 3);
		link(c, f, 3);
		link(d, h, 2);
		link(e, f, 2);
		link(e, g, 1);
		link(f, h, 2);
		link(g, h, 4);

		// Create clients with their associated routers
		newClient("1", a);
		newClient("2", a);
		newClient("1", b);
		newClient("1", c);
		newClient("2", c);
		newClient("1", g);
		newClient("2", g);
		newClient("3", g);
		newClient("4", g);
		
		// Print devices and routers connected to each router
		for (int i = 0; i < routers.size(); i++)
			routers.get(i).printConnectedDevices();

		// Establish routing tables for all routers (threaded)
		for (int i = 0; i < routers.size(); i++) {
			Runnable r = new RouterThread(routers.get(i));
			new Thread(r).start();
		}
		
		// Start clients (threaded) - allow clients to send & receive data
		for (int i = 0; i < clients.size(); i++) {
			Runnable r = new ClientThread(clients.get(i));
			new Thread(r).start();
		}
	}
}
