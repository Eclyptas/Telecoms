import java.net.*;

/**
 * @author Ciarán Ingle, Cian McGrath, Leon Sinclair
 * 
 * Packet = [0|routerAddress.length|/|routerAddress|endNodeAddress.length|/|endNodeAddress|message]
 * String inetSocketAddress = [hostname/address:port]
 */

public class PacketContent {
	String router;
	String endNode;
	String message;
	
	String fullPacket;
	
	public PacketContent(String router, String endNode, String message) {
		this.message = message;
		this.router = router;
		this.endNode = endNode;
		
		fullPacket = "" + Kremlin.MESSAGE + router.length() + "/" + router + endNode.length() + "/" + endNode + message;
	}
	
	/**
	 * Constructor for the getBytes() representation of a PacketContent
	 * @param packet
	 */
	public PacketContent(byte[] packet) {
		fullPacket = new String(packet);
		fullPacket = fullPacket.substring(1, fullPacket.length());	//remove identifying char at front
		
		int start = 0, end = fullPacket.indexOf('/');
		int length = Integer.parseInt(fullPacket.substring(start, end++));
		
		start = end; end += length;
		router = fullPacket.substring(start, end);
		
		start = end; end += fullPacket.substring(start, fullPacket.length()).indexOf('/');
		length = Integer.parseInt(fullPacket.substring(start, end++));
		
		start = end; end += length;
		endNode = fullPacket.substring(start, end);
		
		start = end; end = fullPacket.length();
		message = fullPacket.substring(start, end);
	}
	
	public static InetSocketAddress toInetSocketAddress(String string) {
		String hostname = string.substring(0, string.indexOf('/'));
		int port = Integer.parseInt(string.substring(string.indexOf(':')+1, string.length()));
		
		return new InetSocketAddress(hostname, port);
	}
	
	public String toString() { return fullPacket; }
	
	public byte[] getBytes() { return fullPacket.getBytes();}
}