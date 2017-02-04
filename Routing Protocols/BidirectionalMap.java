import java.net.*;
import java.util.HashMap;

/**
 * @author Ciarán Ingle, Cian McGrath, Leon Sinclair
 *
 * @param <a> type 1
 * @param <b> type 2
 */
public class BidirectionalMap {
	private HashMap<String, InetSocketAddress> ab = new HashMap<String, InetSocketAddress>();
	private HashMap<InetSocketAddress, String> ba = new HashMap<InetSocketAddress, String>();
	
	public void put(String router, InetSocketAddress address) {
		ab.put("0:" + router, address);
		ba.put(address, "0:" + router);
	}
	
	public void put(String client, String router, InetSocketAddress address) {
		ab.put(client.length() + ":" + client + router, address);
		ba.put(address, client.length() + ":" + client + router);
	}
	
	public InetSocketAddress getInetSocketAddress(String client, String router) {
		return ab.get(client.length() + ":" + client + router);
	}
	
	public InetSocketAddress getInetSocketAddress(String router) {
		return ab.get("0:" + router);
	}
	
	public String getClientName(InetSocketAddress address) {
		String string = ba.get(address);
		
		int clientLength = Integer.parseInt(string.substring(0, string.indexOf(':')));
		String client = string.substring(string.indexOf(':')+1, string.indexOf(':')+1 + clientLength);
		
		return client;
	}
	
	public String getRouterName(InetSocketAddress address) {
		String string = ba.get(address);
		
		int clientLength = Integer.parseInt(string.substring(0, string.indexOf(':')));
		String router = string.substring(string.indexOf(':')+1 + clientLength, string.length());
	
		return router;
	}
	
	public int size() { return ab.size(); }
}