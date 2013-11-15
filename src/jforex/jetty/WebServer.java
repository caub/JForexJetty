package jforex.jetty;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dukascopy.api.IStrategy;
import com.dukascopy.api.system.ClientFactory;
import com.dukascopy.api.system.IClient;
import com.dukascopy.api.system.ISystemListener;
import com.dukascopy.api.system.JFAuthenticationException;
import com.dukascopy.api.system.JFVersionException;


public class WebServer  {
	
	// test it on http://jsfiddle.net/k2st5/7
	
	static Logger LOGGER = LoggerFactory.getLogger(WebServer.class);
	
	final static String jnlpUrl = "https://www.dukascopy.com/client/demo/jclient/jforex.jnlp";
	final static String user = "DEMO2EZvbr";
	final static String password = "EZvbr";
	
	static Map<Long, IWs> pids = new HashMap<>();
    
    public static void main(String[] args) throws JFAuthenticationException, JFVersionException, Exception  {
    
    	//keep a jforex client connected
		final IClient client = ClientFactory.getDefaultInstance();

		client.setSystemListener(new ISystemListener() {
			private long t = 5000;

			public void onStart(long processId) {
				LOGGER.info("pid: " + processId);
			}

			public void onStop(long processId) {
				LOGGER.info("cl stopped: " + processId);
			}

			public void onConnect() {
				LOGGER.info("Connected");
				t = 10000;
			}

			public void onDisconnect() {
				LOGGER.warn("Disconnected");
				/*t *= 1.2;
				if (t > 1800000)
					t = 1800000;*/
				try {
					Thread.sleep(t);
					client.connect(jnlpUrl, user, password);
				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
				}
			}
		});

		client.connect(jnlpUrl, user, password);
		int i = 10; // wait max ten seconds
		while (i > 0 && !client.isConnected()) {
			Thread.sleep(1000);
			i--;
		}
		if (!client.isConnected()) {
			LOGGER.error("Failed to connect Dukascopy servers");
			System.exit(1);
		}
		
		// run a default strategy, used in Ws (websockets)
		//ds = new DataStrategy();
		//PriceStrategy ps = new PriceStrategy();
		//long pid = client.startStrategy(ps);
		//pids.put(pid, ps);
		
		
		// run jetty 
		Server server = new Server(8080);

        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);

        handler.addServletWithMapping(StaticServlet.class, "/static/*");

        handler.addServletWithMapping(WsServlet.class, "/stream/*");
        
        handler.addServletWithMapping(StrategyLauncherServlet.class, "/launcher/*");
        
        server.setAttribute("org.eclipse.jetty.server.Request.maxFormContentSize", -1);
		server.start();
		server.join();
    }

}
