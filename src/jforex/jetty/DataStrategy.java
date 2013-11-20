package jforex.jetty;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dukascopy.api.Filter;
import com.dukascopy.api.IAccount;
import com.dukascopy.api.IBar;
import com.dukascopy.api.ICalendarMessage;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IEngine;
import com.dukascopy.api.IEngine.OrderCommand;
import com.dukascopy.api.IHistory;
import com.dukascopy.api.IIndicators;
import com.dukascopy.api.IMessage;
import com.dukascopy.api.IMessage.Type;
import com.dukascopy.api.INewsMessage;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.IStrategy;
import com.dukascopy.api.ITick;
import com.dukascopy.api.IWithdrawalMessage;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.LoadingOrdersListener;
import com.dukascopy.api.LoadingProgressListener;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.api.system.ClientFactory;
import com.dukascopy.api.system.IClient;
import com.google.gson.Gson;

@WebSocket(maxMessageSize = 64 * 1024)
public class DataStrategy implements IStrategy {

	 private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm") {{setTimeZone(TimeZone.getTimeZone("GMT"));}};
	
	static Logger LOGGER = LoggerFactory.getLogger(DataStrategy.class);
	IEngine engine;
	IContext context;
	IHistory history;
	IIndicators indicators;
	IAccount account = null;
	IClient client;
	
	final public List<IOrder> ordersHistory = new ArrayList<>();

	public void onStart(final IContext context) throws JFException {

		
		this.context = context;
		engine = context.getEngine();
		history = context.getHistory();
		indicators = context.getIndicators();
		try {
			client = ClientFactory.getDefaultInstance();
		} catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
			e.printStackTrace();
		}
		
		System.out.println("Started " + client +" " + df.format(System.currentTimeMillis())+" "+context.getSubscribedInstruments().size());


		final Set<Instrument> instruments= new HashSet<Instrument>(); //Arrays.asList(Instrument.values())
		
		instruments.add(Instrument.EURUSD);
		instruments.add(Instrument.GBPUSD);
		instruments.add(Instrument.EURGBP);
		instruments.add(Instrument.NZDUSD);
		instruments.add(Instrument.USDJPY);
		instruments.add(Instrument.EURJPY);
		instruments.add(Instrument.EURCHF);
		instruments.add(Instrument.USDCAD);
		instruments.add(Instrument.AUDUSD);
		instruments.add(Instrument.USDCHF);
		
		context.setSubscribedInstruments(instruments);


		final List<Instrument> loaded = new ArrayList<Instrument>();
		long now = System.currentTimeMillis();
		for (final Instrument instrument : instruments) {

			history.readOrdersHistory(instrument, now - 10*Period.WEEKLY.getInterval(), now, new LoadingOrdersListener() {

				@Override
				public void newOrder(Instrument instrument,IOrder o) {
					ordersHistory.add(o);
				}
			}, new LoadingProgressListener() {
				
				public void dataLoaded(long start, long end, long currentPosition, String information) {
					
				}
				
				public void loadingFinished(boolean allDataLoaded, long start, long end, long currentPosition) {
					if (allDataLoaded) {
						loaded.add(instrument);
						if (loaded.size() >= instruments.size()) {
							System.out.println("history "+ordersHistory.size());
						}
					}
				}
				public boolean stopJob() {
					return false;
				}
			});
		}
		
	}


	public void onStop() throws JFException {
	}
	
	
	public void onTick(Instrument instrument, ITick tick) throws JFException {
		broadcast("ticks", new Object[]{instrument.name(), tick});
	}

	public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
		
	}


	public void onMessage(IMessage m) throws JFException {
		IOrder o = m.getOrder();
		if (o != null) {
			
			if (m.getType() == Type.ORDER_FILL_OK) {
				LOGGER.debug("filled:"+o);
			} else if (m.getType() == Type.ORDER_CLOSE_OK) {
				LOGGER.debug("closed:"+o);
				ordersHistory.add(o);
				
			} else if (m.getType() == Type.ORDER_CHANGED_OK) {
				LOGGER.debug("changed:"+o);
			}
		
		/*}else if (m.getType() == Type.CALENDAR) {*/
			
		}else if (m.getType() == Type.WITHDRAWAL) {
			IWithdrawalMessage m2 = (IWithdrawalMessage)m;
			LOGGER.debug("___WITHDRAWAL__ {}", m2.getClientId()+" "+m2.amount()+" "+m.toString());
		}
	}

	public void onAccount(IAccount a) throws JFException {
		account = a;
	}

	
	public Object Bars(String instrument, String per, String nb) throws ParseException, JFException{
		
		Period period = parsePeriod(per);
		int n= Integer.parseInt(nb);
		Instrument inst = parseInstrument(instrument);
		return history.getBars(inst, period, OfferSide.BID, Filter.NO_FILTER, n, history.getStartTimeOfCurrentBar(inst, period) , 0);
	}
	
	public Object Ticks() throws JFException{
		Map<String, Object> m = new HashMap<>();
		for (Instrument i : context.getSubscribedInstruments()){
			ITick tick = history.getLastTick(i);
			if (tick!=null)
				m.put(i.name(), tick) ;
			else
				m.put(i.name(), tick) ;
		}    
		return m;
	}

	public Object Orders() throws JFException{
		return engine.getOrders();
	}
	
	public Object History(){
		return ordersHistory;
	}
	
	public Object Clients() throws JFException{
		return account.getClients();
	}
	
	public void Subscribe(String instr){
		final Set<Instrument> instruments= context.getSubscribedInstruments();
		instruments.add(parseInstrument(instr));
		context.setSubscribedInstruments(instruments);
	}

	public Instrument parseInstrument(String str){
		if(!str.contains(Instrument.getPairsSeparator())){
			str = str.substring(0,3)+Instrument.getPairsSeparator()+str.substring(3,6);
		}
		return Instrument.fromString(str.substring(0,7));
	}
	
	public Period parsePeriod(String str){
		switch (str) {
			case "1000":
			case "1s":
			case "ONE_SEC":
				return Period.ONE_SEC;
			case "10000":
			case "10s":
			case "TEN_SECS":
				return Period.TEN_SECS;
			case "300000":
			case "5M":
			case "FIVE_MINS":
				return Period.FIVE_MINS;
			case "900000":
			case "15M":
			case "FIFTEEN_MINS":
				return Period.FIFTEEN_MINS;
			case "1800000":
			case "30M":
			case "THIRTY_MINS":
				return Period.THIRTY_MINS;
			case "3600000":
			case "1H":
			case "ONE_HOUR":
				return Period.ONE_HOUR;
			case "86400000":
			case "1D":
			case "ONE_DAY":
			case "DAILY":
				return Period.DAILY;
			case "604800000":
			case "1w":
			case "ONE_WEEK":
			case "WEEKLY":
				return Period.WEEKLY;
			case "2592000000":
			case "1m":
			case "ONE_MONTH":
			case "MONTHLY":
				return Period.MONTHLY;
		}
		return Period.ONE_MIN;//default
	}
	

	
	//----------- Websocket code ------
	
	Gson gson = new Gson();
	
	ConcurrentLinkedQueue<Session> sessions = new ConcurrentLinkedQueue<Session>();

	public void broadcast(String type, Object data) {
		String msg = gson.toJson(new Message(type, data));

        for (Session session : sessions){
        	try {
        		RemoteEndpoint ep = session.getRemote();
        		ep.sendStringByFuture(msg);
        		
        	} catch (Exception e) {
    			e.printStackTrace();
    		}

        }
	}
	private void send(Session session, Object data){
		String msg = gson.toJson(new Message("msg", data));
        try {
			session.getRemote().sendString(msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	Session session;
	
	@OnWebSocketConnect
    public void onConnect(Session session){

		sessions.add(session);
		
		this.session = session;
    }

    @OnWebSocketMessage
    public void onText(String msg) throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException, JFException {
    	System.out.println("rec "+msg );
 		//session.getRemote().sendString(gson.toJson(new Message("msg", "received and processed by "+strategy)));

    	String[] d  = msg.split("/");
		switch(d[0]){
				
			case "ticks": 			send(session, Ticks()); break;
			case "subscribe": 		Subscribe(d[1]); break;
			
			case "close": 			client.startStrategy(new CloseStrategy(session, d[2])); break;
			case "partialclose": 	client.startStrategy(new PartialCloseStrategy(session, d[2], d[3])); break;
			case "sl": 				client.startStrategy(new SetSLStrategy(session, d[2], d[3])); break;
			case "tp": 				client.startStrategy(new SetTPStrategy(session, d[2], d[3])); break;
			case "open":			client.startStrategy(new OpenStrategy(session, d[2], d[3], d[4])); break;
			case "news": 			client.startStrategy(new NewsStrategy(session)); break;
			
			case "help":  default: send(session, new String[]{"ticks", "subscribe", "close", "open", "sl", "tp", "..."}); break;
			
		}
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("MyWebSocket.onClose()");
        
        sessions.remove(session);
    } 

	
	
	
	
	// ----  commands strategies -----
	
	String randId=UUID.randomUUID().toString().replaceAll("-", "");
	int count=0;
    
    public class CloseStrategy implements IStrategy {
    	String id;
    	Session session;
    	public CloseStrategy(Session session, String id){
    		this.session = session;
    		this.id = id;
    	}
    	public void onStart(IContext context) throws JFException {
    		IEngine engine = context.getEngine();
    		IOrder order = engine.getOrderById(id);
    		if (order!=null){
    			order.close();
    		}
    		context.stop();
    	}
    	public void onTick(Instrument instrument, ITick tick) throws JFException {
    	}
    	public void onBar(Instrument instrument, Period period, IBar askBar,IBar bidBar) throws JFException {
    	}
    	public void onMessage(IMessage message) throws JFException {
    	}
    	public void onAccount(IAccount account) throws JFException {
    	}
    	public void onStop() throws JFException {
    	}
    }
    
    public class PartialCloseStrategy implements IStrategy {
    	String id, amount;
    	Session session;
    	public PartialCloseStrategy(Session session, String id, String amount){
    		this.session = session;
    		this.id = id;
    		this.amount = amount;
    	}
    	public void onStart(IContext context) throws JFException {
    		IEngine engine = context.getEngine();
    		IOrder order = engine.getOrderById(id);
    		if (order!=null){
    			double am = Double.parseDouble(amount);
    			order.close(am);
    		}
    		context.stop();
    	}
    	public void onTick(Instrument instrument, ITick tick) throws JFException {
    	}
    	public void onBar(Instrument instrument, Period period, IBar askBar,IBar bidBar) throws JFException {
    	}
    	public void onMessage(IMessage message) throws JFException {
    	}
    	public void onAccount(IAccount account) throws JFException {
    	}
    	public void onStop() throws JFException {
    	}
    }
    
    public class OpenStrategy implements IStrategy {
    	String instr, type, amount;
    	Session session;
    	public OpenStrategy(Session session, String instr, String type, String amount){
    		this.session = session;
    		this.instr = instr;
    		this.type = type;
    		this.amount = amount;
    	}
    	public void onStart(IContext context) throws JFException {
    		IEngine engine = context.getEngine();
    		Instrument i = parseInstrument(instr);
    		OrderCommand cmd = type.equals("BUY")?OrderCommand.BUY:OrderCommand.SELL;
    		double am = Double.parseDouble(amount);
    		IOrder order = engine.submitOrder(randId+count++, i, cmd, am);
    		send(session, order);
    		context.stop();
    	}
    	public void onTick(Instrument instrument, ITick tick) throws JFException {
    	}
    	public void onBar(Instrument instrument, Period period, IBar askBar,IBar bidBar) throws JFException {
    	}
    	public void onMessage(IMessage message) throws JFException {
    	}
    	public void onAccount(IAccount account) throws JFException {
    	}
    	public void onStop() throws JFException {
    	}
    }
    
    public class SetSLStrategy implements IStrategy {
    	String id, value;
    	Session session;
    	public SetSLStrategy(Session session, String id, String value){
    		this.session = session;
    		this.id = id;
    		this.value = value;
    	}
    	public void onStart(IContext context) throws JFException {
    		IEngine engine = context.getEngine();
    		IOrder order = engine.getOrderById(id);
    		if (order!=null){
    			double val = Double.parseDouble(value);
    			order.setStopLossPrice(val);
    		}
    		context.stop();
    	}
    	public void onTick(Instrument instrument, ITick tick) throws JFException {
    	}
    	public void onBar(Instrument instrument, Period period, IBar askBar,IBar bidBar) throws JFException {
    	}
    	public void onMessage(IMessage message) throws JFException {
    	}
    	public void onAccount(IAccount account) throws JFException {
    	}
    	public void onStop() throws JFException {
    	}
    }
    
    public class SetTPStrategy implements IStrategy {
    	String id, value;
    	Session session;
    	public SetTPStrategy(Session session, String id, String value){
    		this.session = session;
    		this.id = id;
    		this.value = value;
    	}
    	public void onStart(IContext context) throws JFException {
    		IEngine engine = context.getEngine();
    		IOrder order = engine.getOrderById(id);
    		if (order!=null){
    			double val = Double.parseDouble(value);
    			order.setTakeProfitPrice(val);
    		}
    		context.stop();
    	}
    	public void onTick(Instrument instrument, ITick tick) throws JFException {
    	}
    	public void onBar(Instrument instrument, Period period, IBar askBar,IBar bidBar) throws JFException {
    	}
    	public void onMessage(IMessage message) throws JFException {
    	}
    	public void onAccount(IAccount account) throws JFException {
    	}
    	public void onStop() throws JFException {
    	}
    }

    public class NewsStrategy implements IStrategy {
    	Session session;
    	
        public NewsStrategy(Session session) {
			this.session = session;
		}

		public void onStart(IContext context) throws JFException {
        }

        public void onTick(Instrument instrument, ITick tick) throws JFException {
        }

        public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        }

        public void onMessage(IMessage message) throws JFException {
            if(!(message.getType().equals(IMessage.Type.CALENDAR) || message.getType().equals(IMessage.Type.NEWS))){
                return;
            }

            INewsMessage news = ((INewsMessage) message);
            
            send(session, news);
            
            print("---------------------------");
            //log some news specific info
            if (message.getType().equals(IMessage.Type.NEWS)) {
                print(String.format("[News message] %s at %s", news.getHeader(), df.format(news.getCreationTime())));
            }

            //log some calendar specific info
            if (message.getType().equals(IMessage.Type.CALENDAR)) {
                ICalendarMessage cal = ((ICalendarMessage) message);
                List<ICalendarMessage.Detail> calDetails = cal.getDetails();
                String detailStr = "[Calendar details]";
                for (ICalendarMessage.Detail d : calDetails){
                    detailStr += String.format("\n    Id:%s, Description:%s, Expected:%s, Actual:%s, Delta:%s, Previous:%s ",
                            d.getId(), d.getDescription(), d.getExpected(), d.getActual(), d.getDelta(), d.getPrevious());
                }
                print(String.format("[Calendar message] %s, %s, %s, %s, %s, %s, %s ",
                        cal.getContent(), cal.getCountry(), cal.getCompanyURL(),
                        cal.getEventCode(), cal.getOrganisation(), cal.getPeriod(), df.format(cal.getEventDate())));
                if (calDetails.size() > 0){
                    print(detailStr);
                }
            }

            //log common meta info
            print(String.format("[meta info] Stock Indicies: %s. Regions: %s. Market sectors: %s. Currencies: %s",
                    news.getStockIndicies(), news.getGeoRegions(),
                    news.getMarketSectors(), news.getCurrencies()));
        }

        public void onAccount(IAccount account) throws JFException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void onStop() throws JFException {
            //To change body of implemented methods use File | Settings | File Templates.
        }
        private void print(Object o){
            System.out.println(o);
        }
    }

}
