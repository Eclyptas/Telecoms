import java.io.IOException;
import java.net.*;
import tcdIO.*;

/**
 * @author Ciarán Ingle, Cian McGrath, Leon Sinclair
 */
public class Client extends Node{
	private final int ROUTER = 0;
	private final int END_NODE = 1;

	public InetSocketAddress socketAddress;
	private InetSocketAddress routerSocketAddress; // Router this device is connected to
	
	private Terminal terminal;

	public Client(InetSocketAddress client, InetSocketAddress router) throws SocketException {
		socketAddress = client;
		socket = new DatagramSocket(socketAddress);
		routerSocketAddress = router;
		
		listener.go();

		terminal = new Terminal(Kremlin.devices.getClientName(socketAddress)
				+ "(" + Kremlin.devices.getRouterName(routerSocketAddress) +  ") - Client");
		terminal.println("Address:\t" + socketAddress.getHostName());
		terminal.println("Port:\t\t" + socketAddress.getPort());
		terminal.println("Router:\t\t" + Kremlin.devices.getRouterName(routerSocketAddress));
		terminal.println("Client initialised.\n");
	}
	
	public void onReceipt(DatagramPacket packet) {
		terminal.println("\nPacket received.");
		terminal.toFront();
		
		PacketContent packetContent = new PacketContent(packet.getData());
		terminal.println(new String(packetContent.message));
	}

	public synchronized void start() {
		String destinationEndNode = "", destinationRouter = "";
		
		boolean validDestination = false;
		while (!validDestination) {
			terminal.print("Specify destination [Router:Device]: ");
			String input = terminal.readString().replace(" ", "").replaceAll("[^\\x00-\\x7F]", "");
			
			try {
				String[] destination = input.split(":");
				destinationRouter = destination[ROUTER];
				destinationEndNode = destination[END_NODE];
				
				InetSocketAddress temp1 = null, temp2 = null;
				boolean asterisk1 = false, asterisk2 = false;
				
				//check if the entered location is a valid input
				if (destinationRouter.equals("*"))
					asterisk1 = true;
				else
					temp1 = Kremlin.devices.getInetSocketAddress(destinationRouter);
				
				if (destinationEndNode.equals("*") || destinationRouter.equals("*"))
					asterisk2 = true;
				else
					temp2 = Kremlin.devices.getInetSocketAddress(destinationEndNode, destinationRouter);
				
				if (!asterisk1 && temp1 == null || !asterisk2 && temp2 == null)
					terminal.println("<" + input + "> | Invalid destination.");
				else
					validDestination = true;
			}
			catch (Exception e) {
				terminal.println("<" + input + "> | Invalid destination.");
			}
		}
		
		
		
		terminal.print("Message to send to (" + destinationRouter + ":" + destinationEndNode + "): ");
		String input = terminal.readString().replaceAll("[^\\x00-\\x7F]", "");

		PacketContent packetContent = new PacketContent(destinationRouter, destinationEndNode, input);
		try {
			socket.send(new DatagramPacket(packetContent.getBytes(), packetContent.getBytes().length, routerSocketAddress));
		}
		catch (IOException e) { e.printStackTrace(); }
		
		terminal.print("Message sent!");
	}
}