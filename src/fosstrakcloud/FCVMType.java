package fosstrakcloud;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import org.apache.commons.codec.binary.Base64;

public enum FCVMType {
	DATABASE("database"),
	ALE("ale"),
	CAPTURE_APP("capture_app"),
	EPCIS_CI("epcis_capture_interface"),
	EPCIS_QI("epcis_query_interface");
		
	public String ID;
	public int IDLastVMTarget;
	public String Name;
	public String Template;
	public boolean Scalable;
	public boolean AutoStart;
	public boolean StaticIP;
	public int LowerThreshold;
	public int UpperThreshold;
	public int ServicePort;
	public String ServiceURI;
	public String IP;	

	private int tryParseInt(String value) { 
		 try {  
		     return Integer.parseInt("0" + value);  
		  } catch(NumberFormatException nfe) {  
		      // Log exception.
		      return 0;
		  }  
		}
	
	private FCVMType(String AID) {
		ID = AID;
		IDLastVMTarget = 0;
		
		LoadConfig();
	}
	
	public void LoadConfig() {
		String cfgFileName = "./resources/" + ID + ".properties";
		if (new File(cfgFileName).exists()) {
			Properties prop = new Properties();
			InputStream input = null;

			try {
				input = new FileInputStream(cfgFileName);

// Load a properties file
				prop.load(input);

// Get the properties values and assign to variables
				Name = prop.getProperty("name");
				AutoStart = Boolean.parseBoolean(prop.getProperty("auto_start"));
				Scalable = Boolean.parseBoolean(prop.getProperty("scalable"));
				StaticIP = Boolean.parseBoolean(prop.getProperty("static_ip"));
				LowerThreshold = tryParseInt(prop.getProperty("lower_threshold"));
				UpperThreshold = tryParseInt(prop.getProperty("upper_threshold"));
				ServicePort = tryParseInt(prop.getProperty("service_port"));
				ServiceURI = prop.getProperty("service_uri");
				byte[] encTemplate = prop.getProperty("template").getBytes();
				Template = new String(Base64.decodeBase64(encTemplate));
				
			} catch (IOException ex) {
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

	public void SaveConfig() {
		String cfgFileName = "./resources/" + ID + ".properties";
		Properties prop = new Properties();
		OutputStream output = null;
	 
		try {
			output = new FileOutputStream(cfgFileName);
			byte[] encTemplate = Base64.encodeBase64(Template.getBytes());
	 
// Set the properties value
			prop.setProperty("name", Name);
			prop.setProperty("auto_start", Boolean.toString(AutoStart));
			prop.setProperty("scalable", Boolean.toString(Scalable));
			prop.setProperty("static_ip", Boolean.toString(StaticIP));
			prop.setProperty("lower_threshold", Integer.toString(LowerThreshold));
			prop.setProperty("upper_threshold", Integer.toString(UpperThreshold));
			prop.setProperty("service_port", Integer.toString(ServicePort));
			prop.setProperty("service_uri", ServiceURI);
			prop.setProperty("template", new String(encTemplate));
	 
// Save properties to project folder
			prop.store(output, Name + " template parameters");
	 
		} catch (IOException io) {
			io.printStackTrace();
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
