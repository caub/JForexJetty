package jforex.jetty;

import org.eclipse.jetty.websocket.api.Session;

import com.dukascopy.api.JFException;

/**
 * for binding strategies with websocket
 * 
 * @author cyril
 *
 */

public interface IWs {

	public void onConnect(Session session);
	
	public void onText(Session session, String msg) throws ClassNotFoundException, IllegalAccessException, InstantiationException, JFException;
	
	public void onClose(Session session, int statusCode, String reason);

}
