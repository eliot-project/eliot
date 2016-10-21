package ctrlpanel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Properties;

import com.sun.net.httpserver.HttpServer;

public class CPanelServer {
	int GPort = 50001;
	HttpServer GServer;
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
				GPort = Integer.parseInt(prop.getProperty("cpanel_port"));

			} catch (IOException ex) {
				System.out.println("CPanelServer.LoadConfig() failed!");
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
	
	void StartCPanel(){
		System.out.print("Starting control panel on port " + GPort + "...");
		try {
			GServer = HttpServer.create(new InetSocketAddress(GPort), 0);
			System.out.println("done!");
			GServer.createContext("/", new CPanelHandler(GServer));
			GServer.setExecutor(null);
			GServer.start();
		} catch (IOException e) {
			System.out.println("StartCPanel failed!");
			e.printStackTrace();
		}
	}


	public static void main(String[] args) {
		CPanelServer cpanel = new CPanelServer();
		cpanel.LoadConfig();
		cpanel.StartCPanel();
	}
}