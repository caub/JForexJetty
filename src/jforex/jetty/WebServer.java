package jforex.jetty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dukascopy.api.IStrategy;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.system.ClientFactory;
import com.dukascopy.api.system.IClient;
import com.dukascopy.api.system.ISystemListener;
import com.dukascopy.api.system.JFAuthenticationException;
import com.dukascopy.api.system.JFVersionException;


public class WebServer  {
	
	// test it on http://jsfiddle.net/k2st5/9
	
	static Logger LOGGER = LoggerFactory.getLogger(WebServer.class);
	
	final static String jnlpUrl = "https://www.dukascopy.com/client/demo/jclient/jforex.jnlp";
	static String userName = "DEMO2EZvbr";
	static String password = "EZvbr";
	
	static Map<Long, IStrategy> pids = new HashMap<>();
    
    public static void main(String[] args) throws JFAuthenticationException, JFVersionException, Exception  {
    	
    	if (args.length!=0){
    		String[] c = args[0].split("/");
    		userName = c[0];
    		password = c[1];
    	}
    
    	//keep a jforex client connected
		final IClient client = ClientFactory.getDefaultInstance();

		client.setSystemListener(new ISystemListener() {
            private int lightReconnects = 100;

        	@Override
        	public void onStart(long processId) {
                LOGGER.info("Strategy started: " + processId);
        	}

			@Override
			public void onStop(long processId) {
                LOGGER.info("Strategy stopped: " + processId);
                if (client.getStartedStrategies().size() == 0) {
                    System.exit(0);
                }
			}

			@Override
			public void onConnect() {
                LOGGER.info("Connected");
                lightReconnects = 3;
			}

			@Override
			public void onDisconnect() {
                LOGGER.warn("Disconnected");
                if (lightReconnects > 0) {
                    client.reconnect();
                    --lightReconnects;
                } else {
                    try {
                        //sleep for 10 seconds before attempting to reconnect
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        //ignore
                    }
                    try {
                        client.connect(jnlpUrl, userName, password);
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
			}
		});

        LOGGER.info("Connecting...");
        //connect to the server using jnlp, user name and password
        client.connect(jnlpUrl, userName, password);

        //wait for it to connect
        int i = 10; //wait max ten seconds
        while (i > 0 && !client.isConnected()) {
            Thread.sleep(1000);
            i--;
        }
        if (!client.isConnected()) {
            LOGGER.error("Failed to connect Dukascopy servers");
            System.exit(1);
        }

        //subscribe to the instruments
        Set<Instrument> instruments = new HashSet<Instrument>();
        instruments.add(Instrument.EURUSD);
        LOGGER.info("Subscribing instruments...");
        client.setSubscribedInstruments(instruments);
        
        //workaround for LoadNumberOfCandlesAction for JForex-API versions > 2.6.64
        Thread.sleep(5000);
		
		// run a default strategy, used in Ws (websockets)
		//ds = new DataStrategy();
		//PriceStrategy ps = new PriceStrategy();
		//long pid = client.startStrategy(ps);
		//pids.put(pid, ps);
		
		
		// run jetty 
		Server server = new Server(8080);
		
		ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.SESSIONS);

        //ServletHandler handler = new ServletHandler();
        server.setHandler(ctx);
        
        ctx.setContextPath("/");
        
        ResourceHandler resource_handler = new ResourceHandler();

        resource_handler.setDirectoriesListed(true);
        resource_handler.setWelcomeFiles(new String[]{ "index.html" });
        resource_handler.setResourceBase("static/");
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { ctx, resource_handler, new DefaultHandler()});

        ctx.addServlet(WsServlet.class, "/stream/*");
        //ctx.addServlet(new ServletHolder(WsServlet.class),"/echo");
        ctx.addServlet(StrategyLauncherServlet.class, "/launcher/*");
        
        server.setHandler(handlers);
        
        

        
        //server.setAttribute("org.eclipse.jetty.server.Request.maxFormContentSize", -1);
		server.start();
		server.join();
    }

}
