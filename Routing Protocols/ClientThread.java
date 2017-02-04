/** 
 * @author Ciarán Ingle, Cian McGrath, Leon Sinclair
 */
public class ClientThread implements Runnable {
	private final Client client;
	
	public ClientThread(Client client) {
		this.client = client;
	}

	public void run() {
		client.start();
	}
}