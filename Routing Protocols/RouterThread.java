/**
 * @author Ciarán Ingle, Cian McGrath, Leon Sinclair
 */
public class RouterThread implements Runnable {
	private final Router router;
	
	public RouterThread(Router router) {
		this.router = router;
	}

	public void run() {
		try {
			router.start();
		}
		catch (InterruptedException e) { e.printStackTrace(); }
	}
}