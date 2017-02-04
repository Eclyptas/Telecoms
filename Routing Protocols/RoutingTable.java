import java.util.ArrayList;
import java.net.*;

/**
 * @author Ciarán Ingle, Cian McGrath, Leon Sinclair
 */
public class RoutingTable {	
	public static class Entry {
		//link-state routing
		public InetSocketAddress source; 		// socket address of sender
		public InetSocketAddress destination; 	// socket address of receiver
		public long weight; 					// time to ping (ms)
		
		//distance-vector routing
		public InetSocketAddress endNode;		// socket address of end node
		public InetSocketAddress nextStop;		// socket address of next stop
		public int hops;						// hops to end node from current router

		/**
		 * Constructor.
		 * 
		 * Routing table entries contain different fields for link-state and distance-vector routing.
		 */
		public Entry(InetSocketAddress source, InetSocketAddress destination, long number) {			
			if (Kremlin.routingApproach == Kremlin.LINK_STATE_ROUTING) {
				this.source = source;
				this.destination = destination;
				this.weight = number;
			}
			else if (Kremlin.routingApproach == Kremlin.DISTANCE_VECTOR_ROUTING) {
				this.endNode = source;
				this.nextStop = destination;
				this.hops = (int)number;
			}
		}
		
		public String toString() {
			if (Kremlin.routingApproach == Kremlin.LINK_STATE_ROUTING)
				return Kremlin.devices.getRouterName(source) + " >>> " + Kremlin.devices.getRouterName(destination) + "\t: " + weight + "ms";
			else if (Kremlin.routingApproach == Kremlin.DISTANCE_VECTOR_ROUTING)
				return Kremlin.devices.getClientName(endNode) + "\t" + Kremlin.devices.getRouterName(nextStop) + "\t" + hops + ((hops == 1)? " hop" : " hops");
			else
				return "-1";
		}
	}
	
	public ArrayList<Entry> routingTable = new ArrayList<Entry>();
	
	/** 
	 * Constructor to create a routing table from the toString.getBytes() representation of a routing table.
	 */
	public RoutingTable(byte[] bytes) {
		String string = new String(bytes);
		string = string.substring(1, string.lastIndexOf('/')+1);
		
		int start, end;
		
		int socketAddressLength;
		InetSocketAddress endNode, nextStop;
		int hops;
		
		while (string.length() > 0) {
			end = string.indexOf('/');
			socketAddressLength = Integer.parseInt(string.substring(0, end));
			start = end+1;
			end = start + socketAddressLength;
			endNode = PacketContent.toInetSocketAddress(string.substring(start, end));
			string = string.substring(end, string.length());
			
			end = string.indexOf('/');
			socketAddressLength = Integer.parseInt(string.substring(0, end));
			start = end+1;
			end = start + socketAddressLength;
			nextStop = PacketContent.toInetSocketAddress(string.substring(start, end));
			string = string.substring(end, string.length());
			
			end = string.indexOf('/');
			hops = Integer.parseInt(string.substring(0, end));
			string = string.substring(end+1, string.length());
			
			routingTable.add(new RoutingTable.Entry(endNode, nextStop, hops+1));
		}
	}
	
	/**
	 * Constructor to create an empty routing table.
	 */
	public RoutingTable() {}

	public void remove(Entry entry) {
		for (int i = 0; i < routingTable.size(); i++)
			if (routingTable.get(i).equals(entry))
				routingTable.remove(i);
	}
	
	/**
	 * Removes an entry from the routing table at position i.
	 */
	public void remove(int i) {
		routingTable.remove(i);
	}
	
	/**
	 * Adds an entry to the end of the routing table.
	 * 
	 * If the same entry is added with a lower weight, the entry is updated,
	 * else it is ignored. (link-state routing)
	 */
	public void add(Entry entry) {		
		if (Kremlin.routingApproach == Kremlin.LINK_STATE_ROUTING) {
			boolean present = false;
			int index = -1;
			
			for (int i = 0; i < routingTable.size(); i++) {
				if (entry.destination.equals(routingTable.get(i).destination)) {
					index = i;
					present = true;
				}
			}
			
			if (present) {
				if (entry.weight <= routingTable.get(index).weight) {
					routingTable.remove(index);
					routingTable.add(entry);
				}
			}
			else
				routingTable.add(entry);
		}
		else if (Kremlin.routingApproach == Kremlin.DISTANCE_VECTOR_ROUTING) {
			routingTable.add(entry);
		}
	}
	
	/**
	 * Replaces an entry in the routing table at an index with a new entry.
	 */
	public void replace(int index, Entry entry) {
		remove(index);
		add(entry);
	}

	/**
	 * Returns the entry at index.
	 */
	public Entry get(int index) {
		return routingTable.get(index);
	}

	/**
	 * Returns whether a path between router1 and router2 exists in the routing table.
	 */
	public boolean contains(InetSocketAddress router1, InetSocketAddress router2) {
		for (int i = 0; i < routingTable.size(); i++) {
			if ((routingTable.get(i).source.equals(router1) && routingTable.get(i).destination.equals(router2))
					|| (routingTable.get(i).source.equals(router2) && routingTable.get(i).destination.equals(router1))) {
				return true;
			}
		}

		return false;
	}
	
	/**
	 * Returns whether a path ending at router exists in the routing table.
	 */
	public boolean containsDestination(InetSocketAddress router) {
		for (int i = 0; i < routingTable.size(); i++) {
			if (routingTable.get(i).destination.equals(router))
				return true;
		}

		return false;
	}

	/**
	 * Returns the size of the routing table.
	 */
	public int size() {
		return routingTable.size();
	}

	/**
	 * Returns the entry with the lowest weight in the routing table.
	 */
	public Entry getFastestPath() {
		Entry fastest = routingTable.get(0);

		for (int i = 0; i < routingTable.size(); i++)
			if (routingTable.get(i).weight < fastest.weight)
				fastest = routingTable.get(i);

		return fastest;
	}
	
	/**
	 * Creates a string representation of the routing table in order to transmit it to another router.
	 * 		Packet = [routerAddress.length|/|routerAddress|endNodeAddress.length|/|endNodeAddress|hops|/]
	 * 		String inetSocketAddress = [hostname/address:port]
	 */
	public String toString() {
		String string = "" + Kremlin.ROUTING_TABLE;	//identifies data as a routing table
		
		if (Kremlin.routingApproach == Kremlin.DISTANCE_VECTOR_ROUTING) {
			for (int i = 0; i < size(); i++)				
				string += get(i).endNode.toString().length() + "/" + get(i).endNode.toString() + get(i).nextStop.toString().length() + "/" + get(i).nextStop.toString() + get(i).hops + "/";
		}
		
		return string;
	}
}
