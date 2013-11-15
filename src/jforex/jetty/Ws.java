package jforex.jetty;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map.Entry;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import com.dukascopy.api.IStrategy;
import com.dukascopy.api.JFException;
import com.dukascopy.api.system.ClientFactory;
import com.google.gson.Gson;

@WebSocket(maxMessageSize = 64 * 1024)
public class Ws {

	//final Set<String> tokens = new HashSet<>();
	
	Session session;
	
	Gson gson = new Gson();
	IWs strategy;
	
	
	@OnWebSocketConnect
    public void onConnect(Session session){
		String path  = session.getUpgradeRequest().getRequestURI().getPath(),
			 strat = path.substring(path.lastIndexOf('/')+1);
		
		//look if we find a strat running with this name
		try {
			Class<?> clazz = Class.forName("jforex.jetty."+strat.substring(0, 1).toUpperCase() + strat.substring(1)+"Strategy");

			for (Entry<Long, IWs> e : WebServer.pids.entrySet()){
    			if (clazz.equals(e.getValue().getClass())){
    				strategy = e.getValue();
    			}
			}
			// if not started, start it now
			if (strategy ==null){
				Constructor<?> ctor = clazz.getConstructor();
				Object obj = ctor.newInstance();
		    	long pid = ClientFactory.getDefaultInstance().startStrategy((IStrategy) obj);
		    	strategy = (IWs) obj;
		    	WebServer.pids.put(pid, strategy);
			}

	    	strategy.onConnect(session);
		
		} catch (ClassNotFoundException | SecurityException | IllegalArgumentException e1) {
			e1.printStackTrace();
		} catch (IllegalStateException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NullPointerException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IllegalAccessException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InstantiationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InvocationTargetException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NoSuchMethodException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		this.session = session;
		
		System.out.println("init "+path+" "+strat);
    }

    @OnWebSocketMessage
    public void onText(String msg) throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException, JFException {
    	System.out.println("rec "+msg );
 		//session.getRemote().sendString(gson.toJson(new Message("msg", "received and processed by "+strategy)));

    	if (strategy!=null)
    		 strategy.onText(session, msg);
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("MyWebSocket.onClose()");
        
        if (strategy!=null)
        	strategy.onClose(session, statusCode, reason);
    } 

}
