package ctrlpanel;

import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

public class CPanelServer {
	static HttpServer GServer;
	static int GPort = 10001;
	
	public static void main(String[] args) {
		try {
			System.out.print("Starting control panel on port " + GPort + "...");
			GServer = HttpServer.create(new InetSocketAddress(GPort), 0);
			System.out.println("done!");
			GServer.createContext("/", new CPanelHandler(GServer));
			GServer.setExecutor(null);
			GServer.start();
		} catch (IOException e) {
			System.out.println("failed!");
			e.printStackTrace();
		}
	}

}
