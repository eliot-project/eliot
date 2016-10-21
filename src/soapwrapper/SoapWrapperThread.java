package soapwrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import com.sun.net.httpserver.HttpExchange;

import fosstrakcloud.FCVMType;
import fosstrakcloud.FCVirtualMachine;
import fosstrakcloud.FossCloud;

public class SoapWrapperThread extends Thread {
	FossCloud GFossCloud;
	HttpExchange GhttpEx;

	public SoapWrapperThread(HttpExchange AhttpEx, FossCloud AFossCloud) {
//		super("ThreadWrapper");
		GhttpEx = AhttpEx;
		GFossCloud = AFossCloud;
	}

	public void run() {
		try {
			FCVirtualMachine vmTarget = GFossCloud.getVMTarget(FCVMType.EPCIS_QI);
			
			if (vmTarget != null) {
				System.out.println(
					"Dispatching \"" + vmTarget.getType().Name + "\" request " +
					"to VM ID \"" + vmTarget.getID() + "\" IP " + vmTarget.getNetworkIP());
			
//				String req_uri = GhttpEx.getRequestURI().toASCIIString();
				String dest_IP = vmTarget.getNetworkIP();
//				String dest_IP = "192.168.2.151";
				String req_body = IOUtils.toString(GhttpEx.getRequestBody(), "UTF-8");
				String dest_uri =
					"http://" +
					dest_IP + ":" +
					FCVMType.EPCIS_QI.ServicePort +
					FCVMType.EPCIS_QI.ServiceURI;

//				String dest_uri = "http://localhost:8082";
				String response = MyHttpPost(dest_uri, req_body);
				GhttpEx.sendResponseHeaders(200, response.length());
				OutputStream os = GhttpEx.getResponseBody();
				os.write(response.getBytes());
				os.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String MyHttpPost(String url, String body)
			throws ClientProtocolException, IOException {
		HttpClient client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost(url);

		HttpEntity entity = new ByteArrayEntity(body.getBytes("UTF-8"));
		post.setEntity(entity);

		HttpResponse response = client.execute(post);

		BufferedReader rd =
				new BufferedReader(
					new InputStreamReader(
						response.getEntity().getContent()));

		String line = "";
		StringBuffer result = new StringBuffer();

		while ((line = rd.readLine()) != null) {
			result.append(line);
		}

		return result.toString();
	}
}
