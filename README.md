JForexJetty
===========

JForex on the web with Jetty


run WebServer, with a valid JForex account username and password ( [create demo account](http://www.dukascopy.com/swiss/french/forex/demo_fx_account/) )

then go to http://localhost:8080/static/

the ticks informations are pushed for several instruments

from your console type:

ws.send("ticks") // send last ticks of your subscribed instruments
ws.subscribe("EURJPY") // subscribe to a new instrument
ws.send("close 24646272") // close a given order
//and several others, see DataStrategy

The DataStrategy is used by the websocket, and so is launched automatically at the first websocket connection, but you can manage all strategies from http://localhost:8080/cmd/?name=data&stop

http://localhost:8080/cmd/?name=data //start strategy DataStrategy
http://localhost:8080/cmd/?name=data&stop // stop it