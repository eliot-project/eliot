package fosstrakcloud;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.opennebula.client.Client;
import org.opennebula.client.ClientConfigurationException;

import soapwrapper.SoapWrapperServer;

import fosstrakcloud.FCVMType;

public class FossCloud {
	Client GOneClient;
	String GOneUser; // oneadmin
	String GOnePassword; // opennebula
	String GOneServerAddress; // 192.168.2.110
	String GOneServerPort; // 2633
	String GCfgFileName = "./resources/opennebula_connection.properties";
	boolean GMonitoring = true;
	FossCloudMonitor GFossCloudMonitor;
	SoapWrapperServer GSoapWrapperServer;
	List<FCVirtualMachine> GVMList;

	public FossCloud() {
		GVMList = new ArrayList<FCVirtualMachine>();
		LoadConfig();
	}

	public void ConnectOneServer() {
		ClientConnect(GOneUser, GOnePassword, GOneServerAddress, GOneServerPort);
	}

	public void LoadConfig() {
		if (new File(GCfgFileName).exists()) {
			Properties prop = new Properties();
			InputStream input = null;

			try {

				input = new FileInputStream(GCfgFileName);

				// Load a properties file
				prop.load(input);

				// Get the properties values and assign to variables
				GOneUser = prop.getProperty("user");
				GOnePassword = prop.getProperty("password");
				GOneServerAddress = prop.getProperty("server_address");
				GOneServerPort = prop.getProperty("server_port");

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
		Properties prop = new Properties();
		OutputStream output = null;
	 
		try {
			output = new FileOutputStream(GCfgFileName);
	 
// Set the properties value
			prop.setProperty("user", GOneUser);
			prop.setProperty("password", GOnePassword);
			prop.setProperty("server_address", GOneServerAddress);
			prop.setProperty("server_port", GOneServerPort);
	 
// Save properties to project folder
			prop.store(output, "OpenNebula connection parameters");
	 
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
	
	public FCVirtualMachine AddVirtualMachine(FCVMType AType, String ANetworkIP) {

		FCVirtualMachine fcvm =	new FCVirtualMachine(this, AType, ANetworkIP);
		
		GVMList.add(fcvm);
		
		return fcvm;
	}

	public void MonitorFossCloud() {
		System.out.println("Starting scalability manager monitor...");
		
// Start monitoring of Fosstrak Cloud to allocate or release VMs
		GFossCloudMonitor = new FossCloudMonitor(this, 10);
		GFossCloudMonitor.start();
	}
	
	public void MonitorVirtualMachines() {
		boolean AnyVMDeployed = false;
		
		System.out.println("Monitoring virtual machines...");

		for(FCVirtualMachine fcvm : GVMList){
			if (fcvm.isVMDeployed()) {
				AnyVMDeployed = true;
				break;
			}
		}
		
		if (AnyVMDeployed) {
			System.out.println("ID\tType\t\tScale\tReady\tCPU(%)\tNet TX\tNet RX\tLP(%)\tState\tLCM State");

			for(FCVirtualMachine fcvm : GVMList){
				if (fcvm.isVMDeployed()) {
					fcvm.MonitorVM(true);
				}
			}
		} else {
			System.out.println("There are no deployed VMs to monitor");
		}
	}

	public void StartVirtualMachines() {
		System.out.println("Starting virtual machines...");

		for (FCVMType vmType : FCVMType.values()) {
			if (vmType.AutoStart) {
				AddVirtualMachine(vmType, "");				
			}
		}

// Start VMs with static IP
		for(FCVirtualMachine fcvm : GVMList) {
			if (fcvm.getType().StaticIP) {
				fcvm.AllocateVM();
			}
		}

// Start VMs with dynamic IP
		for(FCVirtualMachine fcvm : GVMList) {
			if (!fcvm.getType().StaticIP) {
				fcvm.AllocateVM();
			}
		}
	}

	public void StopVirtualMachines() {
		System.out.println("Stopping virtual machines...");

		for(FCVirtualMachine fcvm : GVMList){
			fcvm.DeleteVM();
		}

		for(FCVirtualMachine fcvm : GVMList){
			if (fcvm.isVMDeployed()) {
				try {
					fcvm.getVMMonitor().join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		System.out.println("Virtual machines stopped");
	}

	public void StopMonitor() {
		if (GFossCloudMonitor != null) {
			System.out.print("Stopping scalability manager monitor...");
			GFossCloudMonitor.StopMonitor();

			try {
				GFossCloudMonitor.join();
				System.out.println("done!");
			} catch (InterruptedException e) {
				System.out.println("failed!");
				e.printStackTrace();
			}
		}
	}
	
	private void ClientConnect(String AUser, String APassword, String AServer_Address, String AServer_Port) {
		try {
			String cnnStr = "http://" + AServer_Address + ":" + AServer_Port + "/RPC2";
			System.out.print("Configuring connection to " + cnnStr + "...");
			GOneClient = new Client(AUser + ":" + APassword, cnnStr);
			System.out.println("done!");
		} catch (ClientConfigurationException e) {
			System.out.println("failed!");
			e.printStackTrace();
		}
	}

	public void	StartSoapWrapperServer() {
		GSoapWrapperServer = new SoapWrapperServer(this);
	};
	
	public void EvaluateAllocVM() {
		int qtVM;
		double sumLP;
		double avgLP;
		boolean flCanAlloc;

// Evaluate allocation of new VM for each VM Type
		for (FCVMType vmType : FCVMType.values()) {
			if (vmType.Scalable) {
				qtVM = 0;
				sumLP = 0;
				avgLP = 0;
				flCanAlloc = true;

				for(FCVirtualMachine fcvm : GVMList){
					if (fcvm.getType() == vmType) {
						if (fcvm.isVMDeployed() && fcvm.isOSReady()) {
							qtVM = qtVM + 1;
							sumLP = sumLP + fcvm.getLoadPrediction();
						} else {
							flCanAlloc = false;
							break;
						}
					}
				}

				if (flCanAlloc && (qtVM > 0)) {
					avgLP = sumLP / qtVM; 
					if (avgLP > vmType.UpperThreshold) {
						FCVirtualMachine fcvm =	AddVirtualMachine(vmType, "");

						fcvm.AllocateVM();

						if (fcvm.isVMDeployed()) {
							fcvm.MonitorVM(true);
						}
					}			
				}
			}
		}
	}
	
	public void EvaluateReleaseVM() {
		int qtVM;
		double sumLP;
		double avgLP;
		double minLP;
		boolean flCanRelease;
		FCVirtualMachine VMtoRelease;
		
// Evaluate releasing of VM for each VM Type
		for (FCVMType vmType : FCVMType.values()) {
			if (vmType.Scalable) {
				qtVM = 0;
				sumLP = 0;
				avgLP = 0;
				minLP = 100;
				flCanRelease = true;
				VMtoRelease = null;

				for (FCVirtualMachine fcvm : GVMList) {
					if (fcvm.getType() == vmType) {
						if (fcvm.isVMDeployed() && fcvm.isOSReady()) {
							qtVM = qtVM + 1;
							sumLP = sumLP + fcvm.getLoadPrediction();

							if (fcvm.getLoadPrediction() < minLP) {
								VMtoRelease = fcvm;
								minLP = fcvm.getLoadPrediction();
							}
						} else {
							flCanRelease = false;
							break;
						}
					}
				}

				if (flCanRelease && (qtVM > 1)) {
					avgLP = sumLP / qtVM; 
					if (avgLP < vmType.LowerThreshold) {
						if (VMtoRelease != null) {
							GVMList.remove(VMtoRelease);
							VMtoRelease.DeleteVM();

							try {
								VMtoRelease.getVMMonitor().join();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}			
				}
			}
		}
	}

	public void StopSoapWrapperServer() {
		if (GSoapWrapperServer != null)
			GSoapWrapperServer.StopSoapWrapperServer();
	}
	
	public FCVirtualMachine getVMTarget(FCVMType AType) {
		FCVirtualMachine firstVMType = null;
		FCVirtualMachine nextVMTarget = null;
		boolean foundLastVMTarger = false;
		
		for (FCVirtualMachine vmType : GVMList) {
			if (nextVMTarget == null) {
				if (vmType.getType() == AType) {
					if (vmType.isOSReady() && vmType.isVMDeployed()) {
						if (foundLastVMTarger) {
							nextVMTarget = vmType;
						}
					
						if (firstVMType == null) {
							firstVMType = vmType;
						}
					
						if (AType.IDLastVMTarget == vmType.getID()) {
							foundLastVMTarger = true;
						}
					}
				}
			}
		}		
		
		if (nextVMTarget == null) {
			if (firstVMType != null) {
				AType.IDLastVMTarget = firstVMType.getID();
			}

			return firstVMType;
		} else {
			AType.IDLastVMTarget = nextVMTarget.getID();
			return nextVMTarget;
		}
	
	}
	
	public Client getOneClient() {
		return GOneClient;
	}
	
	public String getOneUser() {
		return GOneUser;
	}

	public void setOneUser(String AOneUser) {
		GOneUser = AOneUser;
	}

	public String getOnePassword() {
		return GOnePassword;
	}

	public void setOnePassword(String AOnePassword) {
		GOnePassword = AOnePassword;
	}

	public String getOneServerAddress() {
		return GOneServerAddress;
	}

	public void setOneServerAddress(String AOneServerAddress) {
		GOneServerAddress = AOneServerAddress;
	}

	public String getOneServerPort() {
		return GOneServerPort;
	}

	public void setOneServerPort(String AOneServerPort) {
		GOneServerPort = AOneServerPort;
	}

	public boolean isMonitoring() {
		return GMonitoring;
	}

	public void setMonitoring(boolean AMonitoring) {
		GMonitoring = AMonitoring;
	}

}
