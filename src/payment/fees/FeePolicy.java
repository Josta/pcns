package payment.fees;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import core.Component;
import core.event.Event;
import graph.Channel;

public abstract class FeePolicy extends Component {
	private static final float FEE_UPDATE_INTERVAL = 10;
	private List<Consumer<Channel>> handlers;
	
	public void prepare() {
		handlers = new LinkedList<>();
		graph().channels().forEach(ch -> updateChannelFee(ch));
		//graph().onChannelUpdate(ch -> updateChannelFee(ch));
		new UpdateFees().now(sim);
	}
	
	public void onFeeUpdate(Consumer<Channel> handler) {
		handlers.add(handler);
	}
	
	public abstract boolean updateChannelFee(Channel ch);
	
	private class UpdateFees extends Event {
		@Override
		public void run() {
			graph().channels().forEach(ch -> {
				if (updateChannelFee(ch))
					handlers.forEach(h -> h.accept(ch));
			});
			new UpdateFees().after(FEE_UPDATE_INTERVAL, sim);
		}
	}

}
