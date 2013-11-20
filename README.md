JForexJetty
===========

JForex on the web with Jetty


run WebServer, with a valid JForex account username and password ( [create demo account](http://www.dukascopy.com/swiss/french/forex/demo_fx_account/) )

then go to http://localhost:8080/static/

In this demo page, the ticks informations are pushed for several instruments

from your console type:

    ws.send("ticks") // send last ticks of your subscribed instruments  
    ws.send("subscribe EURJPY") // subscribe to a new instrument  
    ws.send("close 24646272") // close a given order  
    //and several others, see DataStrategy

The DataStrategy is using a websocket, (the strategies are picked by url, ex: "ws://localhost:8080/stream/data", or "ws://localhost:8080/stream/foo" will attempt to start DataStrategy, FooStrategy respectively...), and so is launched automatically at the first websocket connection.

You can manage at any time all strategies from http://localhost:8080/cmd/?name=data&stop
    http://localhost:8080/cmd/?name=data //start strategy DataStrategy  
    http://localhost:8080/cmd/?name=data&stop // stop it