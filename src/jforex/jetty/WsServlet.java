package jforex.jetty;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map.Entry;

import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import com.dukascopy.api.IStrategy;
import com.dukascopy.api.system.ClientFactory;

public class WsServlet extends WebSocketServlet {
	
    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(60000);
        
        //factory.register(Ws.class);
        
        factory.setCreator(new MyAdvancedCreator());
    }
}

class MyAdvancedCreator implements WebSocketCreator {
	
    @Override
    public Object createWebSocket(UpgradeRequest req, UpgradeResponse resp) {  
    	System.out.println(".. "+req.getRequestURI().getPath());
    	String path = req.getRequestURI().getPath(),
    		   name = path.substring(path.lastIndexOf('/')+1); 

		try {

			Class<?> clazz = Class.forName("jforex.jetty."+name.substring(0, 1).toUpperCase() + name.substring(1)+"Strategy");
			
			Object strat = null;
			for (Entry<Long, IStrategy> e : WebServer.pids.entrySet()){
    			if (clazz.equals(e.getValue().getClass())){
    				strat = e.getValue();
    			}
			}
			// if not started, start it now
			if (strat ==null){
				Constructor<?> ctor = clazz.getConstructor();
				strat = ctor.newInstance();
		    	long pid = ClientFactory.getDefaultInstance().startStrategy((IStrategy) strat);
		    	WebServer.pids.put(pid, (IStrategy) strat);
			}
			return strat;
		
		} catch (ClassNotFoundException | SecurityException | IllegalArgumentException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e1) {
			e1.printStackTrace();
		}
		
        return null;
    }
}
