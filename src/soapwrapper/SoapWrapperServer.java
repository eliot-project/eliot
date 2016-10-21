package soapwrapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Properties;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import fosstrakcloud.FossCloud;

public class SoapWrapperServer {
	FossCloud GFossCloud;
	int GPort = 50002;
	int GBacklogs = 4096;
	String contextpath = "";
	HttpServer GSoapWrapperServer;

	String GCfgFileName = "./resources/eliot.properties";
	
	void LoadConfig() {
		if (new File(GCfgFileName).exists()) {
			Properties prop = new Properties();
			InputStream input = null;

			try {

				input = new FileInputStream(GCfgFileName);

				// Load a properties file
				prop.load(input);

				// Get the properties values and assign to variables
				GPort = Integer.parseInt(prop.getProperty("soap_wrapper_port"));

			} catch (IOException ex) {
				System.out.println("SoapWrapperServer.LoadConfig() failed!");
				ex.printStackTrace();
			} finally {
				if (input != null) {
					try {
						input.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public SoapWrapperServer(FossCloud AFossCloud) {
		GFossCloud = AFossCloud;		
		System.out.print("Starting SOAP Wrapper Server on port " + GPort + "...");
		try {
			GSoapWrapperServer = HttpServer.create(new InetSocketAddress(GPort), GBacklogs);
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
