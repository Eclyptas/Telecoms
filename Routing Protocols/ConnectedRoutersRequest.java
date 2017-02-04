import java.net.*;

/**
 * @author Ciarán Ingle, Cian McGrath, Leon Sinclair
 */
public class ConnectedRoutersRequest {
	public InetSocketAddress source, destination;
	
	public ConnectedRoutersRequest(InetSocketAddress source, InetSocketAddress destination) {
		this.source = source;
		this.destination = destination;
	}
	
	public ConnectedRoutersRequest(byte[] data) {
		String request = new String(data);
		request = request.substring(1, request.length());	//remove identifying char at front
		
		int start = 0, end = request.indexOf('/');
		int length = Integer.parseInt(request.substring(start, end++));
		
		start = end; end += length;
		source = PacketContent.toInetSocketAddress(request.substring(start, end));
		
		start = end; end += request.substring(start, request.length()).indexOf('/');
		length = Integer.parseInt(request.substring(start, end++));
		
		start = end; end += length;
		destination = PacketContent.toInetSocketAddress(request.substring(start, end));
	}
	
	/**
	 *  [q|sourceLength|/|source|destinationLength|/|destination]
	 */
	public String toString() {
		return "" + Kremlin.CONNECTED_ROUTERS_REQUEST + source.toString().length() + "/" + source.toString() + destination.toString().length() + "/" + destination.toString();
	}
}
