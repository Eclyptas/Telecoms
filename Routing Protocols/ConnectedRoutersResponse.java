import java.net.*;
import java.util.ArrayList;

/**
 * @author Ciarán Ingle, Cian McGrath, Leon Sinclair
 */
public class ConnectedRoutersResponse {
	public InetSocketAddress destination;
	ArrayList<InetSocketAddress> connectedRouters = new ArrayList<InetSocketAddress>();
	
	public ConnectedRoutersResponse(InetSocketAddress destination, ArrayList<InetSocketAddress> connectedRouters) {
		this.destination = destination;
		this.connectedRouters = connectedRouters;
	}
	
	public ConnectedRoutersResponse(byte[] data) {
		String response = new String(data);
		response = response.substring(1, response.lastIndexOf(' ')+1);
		
		int start = 0, end = response.indexOf('/');
		int length = Integer.parseInt(response.substring(start, end++));
		
		start = end; end += length;
		destination = PacketContent.toInetSocketAddress(response.substring(start, end));
		
		response = response.substring(end, response.length());
		
		while (response.length() > 0) {
			String temp = response.substring(0, response.indexOf(' '));
			connectedRouters.add(PacketContent.toInetSocketAddress(temp));
			response = response.substring(response.indexOf(' ')+1, response.length());
		}
	}
	
	public String toString() {
		String string = "" + Kremlin.CONNECTED_ROUTERS_RESPONSE + destination.toString().length() + "/" + destination.toString();
		
		for (int i = 0; i < connectedRouters.size(); i++)
			string += connectedRouters.get(i).toString() + " ";
		
		return string;
	}
	
	public ArrayList<InetSocketAddress> getConnectedRouters() {
		return connectedRouters;
	}
}
