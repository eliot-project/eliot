package fosstrakcloud;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Properties;

public enum FCVMType {
	DATABASE("database"),
	ALE("ale"),
	CAPTURE_APP("capture_app"),
	EPCIS_CI("epcis_capture_interface"),
	EPCIS_QI("epcis_query_interface");
		
	public String ID;
	public String Name;
	public String VMTemplate;
	public boolean Scalable;
	public boolean AutoStart;
	public int LowerThreshold;
	public int UpperThreshold;
	public int ServicePort;
	public String ServiceURI;
	public float CPU;
	public int Memory;
	public String Disk_Image;
	public String Image_Uname;
	public String Network_Uname;
	public String Network;
	public String IP;	

	private FCVMType(String AID) {
		ID = AID;
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
				LowerThreshold = Integer.parseInt(prop.getProperty("lower_threshold"));
				UpperThreshold = Integer.parseInt(prop.getProperty("upper_threshold"));
				ServicePort = Integer.parseInt(prop.getProperty("service_port"));
				ServiceURI = prop.getProperty("service_uri");
				CPU = Float.parseFloat(prop.getProperty("cpu"));
				Memory = Integer.parseInt(prop.getProperty("memory"));
				Disk_Image = prop.getProperty("disk_image");
				Image_Uname = prop.getProperty("image_uname");
				Network_Uname = prop.getProperty("network_uname");
				Network = prop.getProperty("network");
				IP = prop.getProperty("ip");

				VMTemplate =
						String.format(
							Locale.ENGLISH,
							"NAME=\"%s\"\n" +
							"CONTEXT=[SSH_PUBLIC_KEY=\"$USER[SSH_PUBLIC_KEY]\",NETWORK=\"YES\"]\n" +
							"MEMORY=\"%d\"\n"+
							"DISK=[IMAGE=\"%s\",IMAGE_UNAME=\"%s\"]\n" +
							"FEATURES=[ACPI=\"no\"]\n" +
							"DESCRIPTION=\"Fosstrak in a cloud\"\n" +
							"GRAPHICS=[TYPE=\"VNC\",LISTEN=\"0.0.0.0\"]\n" +
							"NIC=[NETWORK_UNAME=\"%s\",NETWORK=\"%s\",IP=\"%%s\"]\n" +
							"CPU=\"%.1f\"",
							Name, Memory, Disk_Image, Image_Uname, Network_Uname, Network, CPU);

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
	 
// Set the properties value
			prop.setProperty("name", Name);
			prop.setProperty("auto_start", Boolean.toString(AutoStart));
			prop.setProperty("scalable", Boolean.toString(Scalable));
			prop.setProperty("lower_threshold", Integer.toString(LowerThreshold));
			prop.setProperty("upper_threshold", Integer.toString(UpperThreshold));
			prop.setProperty("service_port", Integer.toString(ServicePort));
			prop.setProperty("service_uri", ServiceURI);
			prop.setProperty("cpu", String.format(Locale.ENGLISH, "%.1f", CPU));
			prop.setProperty("memory", Integer.toString(Memory));
			prop.setProperty("disk_image", Disk_Image);
			prop.setProperty("image_uname", Image_Uname);
			prop.setProperty("network_uname", Network_Uname);
			prop.setProperty("network", Network);
			prop.setProperty("ip", IP);
	 
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
