package ctrlpanel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import fosstrakcloud.FCVMType;
import fosstrakcloud.FossCloud;

public class CPanelHandler implements HttpHandler {
	private FossCloud GFossCloud;
	private HttpServer GServer;
	private FCVMType vmTypeEd = FCVMType.EPCIS_QI;
	
	public CPanelHandler(HttpServer AServer) {
		GServer = AServer;
		GFossCloud = new FossCloud();
	}
	
	public void handle(HttpExchange http) throws IOException {
		try {
// Parse GET
//			Map<String, String> parms = parseQuery(http.getRequestURI().getQuery());
			
// Parse POST
			InputStreamReader isr = new InputStreamReader(http.getRequestBody(), "utf-8");
            BufferedReader br = new BufferedReader(isr);
            String query = br.readLine();
            Map<String, String> parms = parseQuery(query);

// Update open nebula parameters
            UpdateOpenNebulaConfig(parms);
			
// Save OpenNebula connection parameters			
			if (parms.get("opennebula_cfg") != null) {
				GFossCloud.SaveConfig();
			}
// Selected VM Template
			String vm_id = parms.get("vm_id"); 
			for (FCVMType vmt : FCVMType.values()) {
				if (vmt.ID.equalsIgnoreCase(vm_id)) {
					vmTypeEd = vmt;
					}
			}
			
// Control panel interface
			String uri = http.getRequestURI().toASCIIString();
			if(!(uri.equals("/") || uri.equalsIgnoreCase("/favicon.ico"))) {
				System.out.println("URI: " + uri);
			}
			String response = ControlPanelHTML();
			writeResponse(http, response);

// Process actions
			if (parms.get("btnStart") != null) {
				GFossCloud.ConnectOneServer();
				GFossCloud.StartSoapWrapperServer();
				GFossCloud.StartVirtualMachines();
				GFossCloud.MonitorFossCloud();
				GFossCloud.MonitorVirtualMachines();
			}

			if (parms.get("btnStop") != null) {
				GFossCloud.StopVirtualMachines();
				GFossCloud.StopMonitor();
				GFossCloud.StopSoapWrapperServer();

				System.out.print("Stopping control panel...");
				GServer.stop(1);
				System.out.println("done!");
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public void writeResponse(HttpExchange http, String response) throws IOException {
		http.sendResponseHeaders(200, response.length());
		OutputStream os = http.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}

	public static Map<String, String> parseQuery(String query){
		Map<String, String> result = new HashMap<String, String>();

		if (query != null) {
			for (String param : query.split("&")) {
				String pair[] = param.split("=");
				if (pair.length > 1) {
					try {
						result.put(pair[0], URLDecoder.decode(pair[1], "utf-8"));
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				} else {
					result.put(pair[0], "");
				}
			}
		}

		return result;
	}

	private void UpdateOpenNebulaConfig(Map<String, String> parms) {
// Update OpenNebula connection parameters			
		if (parms.get("opennebula_cfg") != null) {
			GFossCloud.setOneUser(parms.get("one_user"));
			GFossCloud.setOnePassword(parms.get("one_password"));
			GFossCloud.setOneServerAddress(parms.get("one_server_address"));
			GFossCloud.setOneServerPort(parms.get("one_server_port"));
		}
		
// Update VM Template configuration
		FCVMType vmType = null;
		String vm_template_id = parms.get("vm_template");
		if (vm_template_id != null) {
			for (FCVMType vmt : FCVMType.values()) {
				if (vmt.ID.equalsIgnoreCase(vm_template_id)) {
					vmType = vmt;
					break;
				}
			}
			
			if (vmType != null) {
				System.out.print("Updating VM Template \"" + vmType.Name + "\"...");

				vmType.Name = parms.get("name");
				vmType.AutoStart = Boolean.parseBoolean(parms.get("auto_start"));
				vmType.Scalable = Boolean.parseBoolean(parms.get("scalable"));
				vmType.StaticIP = Boolean.parseBoolean(parms.get("static_ip"));
				vmType.LowerThreshold = tryParseInt(parms.get("lower_threshold"));
				vmType.UpperThreshold = tryParseInt(parms.get("upper_threshold"));
				vmType.ServicePort = tryParseInt(parms.get("service_port"));
				vmType.Template = parms.get("template");

				vmType.SaveConfig();
				System.out.println("done!");
			}
		}
	}
	
	private int tryParseInt(String value) { 
		 try {  
		     return Integer.parseInt("0" + value);  
		  } catch(NumberFormatException nfe) {  
		      // Log exception.
		      return 0;
		  }  
		}

	private String ControlPanelHTML() {
		return
			"<!DOCTYPE html>" +
			"<html>" +
			"<head>" +
			"<title>Eliot Scalability Manager - Control Panel</title>" +
			"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">" +
			"</head>" +

			"<body>" +
			"<font face=\"verdana\" size=\"2\">" +
			"<h2>Eliot Scalability Manager</h2>" +

			"<fieldset style=\"border-radius: 10px; width: 1%\">" +
			"<legend><h3>Control Panel</h3></legend>" +

			"<fieldset style=\"border-radius: 10px;\">" +
			"<legend><b>Service Control</b></legend>" +
			"<form action=\"/\" method=\"post\">" +
			"<table cellpadding=\"3\">" +
				"<tr>" +
					"<td><input type=\"submit\" name=\"btnStart\" value=\"Start Scalability Manager\"></td>" +
					"<td><input type=\"submit\" name=\"btnStop\" value=\"Stop Scalability Manager\"></td>" +
					"<tr>" +
				"</table>" +
			"</form>" +
			"</fieldset>" +

			"</br>" +

			"<fieldset style=\"border-radius: 10px;\">" +
			"<legend><b>OpenNebula Connection Parameters</b></legend>" +
			"<form action=\"/\" method=\"post\">" +
			"<input type=\"hidden\" name=\"opennebula_cfg\">" +
			"<table cellpadding=\"3\">" +
				"<tr>" +
					"<td>Username<br><input type=\"text\" name=\"one_user\" value=\"" + GFossCloud.getOneUser() + "\"></td>" +
					"<td>Password<br><input type=\"text\" name=\"one_password\" value=\"" + GFossCloud.getOnePassword() + "\"></td>" +
					"<td>Address<br><input type=\"text\" name=\"one_server_address\" value=\"" + GFossCloud.getOneServerAddress() + "\"></td>" +
					"<td>Port<br><input type=\"text\" name=\"one_server_port\" value=\"" + GFossCloud.getOneServerPort() + "\"></td>" +
					"</tr>" +
				"<tr><td><input type=\"submit\" value=\"Save Configuration\"></td></tr>" +
			"</table>" +
			"</form>" +
			"</fieldset>" +

			VMControlPanelHTML() +
			
			"</fieldset>" +
			"</font>" +
			"</body>" +
			"</html>";
	}

	private String VMControlPanelHTML() {
		String html = "";
		String selectVM = "";
		String selected = "";
		String vmTemplate = vmTypeEd.Template;

		vmTemplate = vmTemplate.replaceAll("&", "&amp;");
		vmTemplate = vmTemplate.replaceAll("\"", "&quot;");
		vmTemplate = vmTemplate.replaceAll("<", "&lt;");
		vmTemplate = vmTemplate.replaceAll(">", "&gt;");
		vmTemplate = vmTemplate.replaceAll("'", "&#39;");
		vmTemplate = vmTemplate.replaceAll("/", "&#47;");
	
		selectVM = "<select name=\"vm_id\" onchange=\"this.form.submit()\" style=\"border: 0; font-weight: bold\">";
		
		for (FCVMType vmt : FCVMType.values()) {
			if (vmt.equals(vmTypeEd)) {
				selected = " selected";
			} else {
				selected = "";
			}
			
			selectVM = selectVM +
				"<option value=\"" + vmt.ID + "\"" + selected +">"+ vmt.Name + "</option>";
		}

		selectVM = selectVM + "</select>";

		html = html + "<br>" +
			"<form action=\"/\" method=\"post\">" +
			"<fieldset style=\"border-radius: 10px;\">" +
			"<legend><b>Template Parameters of " + selectVM + "</b>" +
			"</legend>" +
			"</form>" +
			"<form action=\"/\" method=\"post\">" +
			"<input type=\"hidden\" name=\"vm_template\" value=\"" + vmTypeEd.ID + "\">" +
			"<table cellpadding=\"3\">" +
				"<tr>" +
					"<td width=\"25%\">Auto Start?<br>" +
						"<input type=\"radio\" name=\"auto_start\" value=\"true\"" + htmlRadioChacked(vmTypeEd.AutoStart) + ">True&nbsp;&nbsp;" +
						"<input type=\"radio\" name=\"auto_start\" value=\"false\"" + htmlRadioChacked(!vmTypeEd.AutoStart) + ">False" +
					"</td>" +
					"<td width=\"25%\">Scalable?<br>" +
						"<input type=\"radio\" name=\"scalable\" value=\"true\"" + htmlRadioChacked(vmTypeEd.Scalable) + ">True&nbsp;&nbsp;" +
						"<input type=\"radio\" name=\"scalable\" value=\"false\"" + htmlRadioChacked(!vmTypeEd.Scalable) + ">False" +
					"</td>" +
					"<td width=\"25%\">Static IP?<br>" +
						"<input type=\"radio\" name=\"static_ip\" value=\"true\"" + htmlRadioChacked(vmTypeEd.StaticIP) + ">True&nbsp;&nbsp;" +
						"<input type=\"radio\" name=\"static_ip\" value=\"false\"" + htmlRadioChacked(!vmTypeEd.StaticIP) + ">False" +
					"</td>" +						
					"<td width=\"25%\">&nbsp;" + /*Name*/ "<br><input type=\"hidden\" name=\"name\" value=\"" + vmTypeEd.Name + "\"></td>" +
				"</tr>" +
				"<tr>" +
					"<td>Lower Threshold<br><input type=\"text\" name=\"lower_threshold\" value=\"" + vmTypeEd.LowerThreshold + "\"></td>" +
					"<td rowspan=\"4\" colspan=\"3\">Template<br><textarea name=\"template\" rows=\"11\" cols=\"73\">" + vmTemplate + "</textarea></td>" +
				"</tr>" +
					"<td>Upper Threshold<br><input type=\"text\" name=\"upper_threshold\" value=\"" + vmTypeEd.UpperThreshold + "\"></td>" +
				"</tr>" +
					"<td>Service Port<br><input type=\"text\" name=\"service_port\" value=\"" + vmTypeEd.ServicePort + "\"></td>" +
				"</tr>" +
				"</tr>" +
					"<td>Service URI<br><input type=\"text\" name=\"service_uri\" value=\"" + vmTypeEd.ServiceURI + "\"></td>" +
				"<tr>" +
					"<td><input type=\"submit\" value=\"Save Configuration\"></td>" +
				"</tr>" +
			"</table>" +
			"</form>" +
			"</fieldset>";
		
		return html;
	}

	private String htmlRadioChacked(boolean AValue) {
		if (AValue) {
			return " checked";
		} else {
			return "";
		}
	}
}
