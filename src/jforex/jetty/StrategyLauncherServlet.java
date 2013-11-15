package jforex.jetty;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dukascopy.api.IStrategy;
import com.dukascopy.api.system.ClientFactory;
import com.dukascopy.api.system.IClient;

public class StrategyLauncherServlet extends HttpServlet {
	
	

	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //response.setContentType("application/json");
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);

        String name = request.getParameter("name");
        String stop = request.getParameter("stop");
        
        IClient client;
        if (name!=null){
			try {
				client = ClientFactory.getDefaultInstance();

    			Class<?> clazz = Class.forName("jforex.jetty."+name.substring(0, 1).toUpperCase() + name.substring(1)+"Strategy");
    			
    			if (stop !=null){
    				for (Entry<Long, IWs> e : WebServer.pids.entrySet()){
	    				if (clazz.equals(e.getValue().getClass())){
	    					client.stopStrategy(e.getKey());
	    				}
	    			}
    			}else{
    				Constructor<?> ctor = clazz.getConstructor();
        			
        			Object obj = ctor.newInstance();
        	    	
        	    	long pid = client.startStrategy((IStrategy) obj);
        	    	
        	    	WebServer.pids.put(pid, (IWs) obj);
    			}
			
			} catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException e1) {
				e1.printStackTrace();
			}
        }
        
        String reply="strats currently running:<br>";	
    	for (Entry<Long, IWs> e : WebServer.pids.entrySet()){
			reply += e.getKey()+" -> "+e.getValue().getClass().getName()+"<br>";
		}

        response.getWriter().println(reply);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("ok");
    }
}