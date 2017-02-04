import java.net.*;
import java.util.ArrayList;

/**
 * @author Ciarán Ingle, Cian McGrath, Leon Sinclair
 */
public class WeightMap {
	ArrayList<RoutingTable.Entry> weights = new ArrayList<RoutingTable.Entry>();
	
	public void add(InetSocketAddress router1, InetSocketAddress router2, long weight) {
		weights.add(new RoutingTable.Entry(router1, router2, weight));
	}
	
	public long getWeight(InetSocketAddress router1, InetSocketAddress router2) {
		for (int i = 0; i < weights.size(); i++) {
			if (weights.get(i).source.equals(router1) && weights.get(i).destination.equals(router2))
				return weights.get(i).weight;
			else if (weights.get(i).source.equals(router2) && weights.get(i).destination.equals(router1))
				return weights.get(i).weight;
		}
		
		return -1;
	}
}
