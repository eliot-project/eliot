package soapwrapper;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import fosstrakcloud.FossCloud;

public class SoapWrapperServer {
	FossCloud GFossCloud;
	int port = 8081;
	int backlogs = 4096;
	String contextpath = "";
	HttpServer GSoapWrapperServer;
	
	public SoapWrapperServer(FossCloud AFossCloud) {
		GFossCloud = AFossCloud;		
		System.out.print("Starting soap wrapper server on port " + port + "...");
		try {
			GSoapWrapperServer = HttpServer.create(new InetSocketAddress(port), backlogs);
			System.out.println("done!");
			GSoapWrapperServer.createContext("/" + contextpath, new ThreadHandler());
			GSoapWrapperServer.setExecutor(null);
			GSoapWrapperServer.start();
		} catch (IOException e) {
			System.out.println("failed!");
			e.printStackTrace();
		}
	}

	class ThreadHandler implements HttpHandler {
		public void handle(HttpExchange http) throws IOException {
			new SoapWrapperThread(http, GFossCloud).start();
		}
	}

	public void StopSoapWrapperServer() {
		System.out.print("Stopping soap wrapper server...");
		
		if (GSoapWrapperServer != null) {
			GSoapWrapperServer.stop(1);
		}
		
		System.out.println("done!");
	}
}
