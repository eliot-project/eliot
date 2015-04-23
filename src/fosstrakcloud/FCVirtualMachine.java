package fosstrakcloud;

import org.opennebula.client.OneResponse;
import org.opennebula.client.vm.VirtualMachine;

public class FCVirtualMachine {
// internal fields	
	private FossCloud GFossCloud;
	private String VMTemplate;
	private boolean VMDeployed = false;
	private boolean Ready = false;
	private String LastPoll = "";
	private FCVirtualMachineMonitor VMMonitor;
	private int[] CPUSerie = {0, 0, 0, 0, 0, 0, 0};
	
// read-only fields - getter method
	private VirtualMachine VMInstance;
	private int ID;
	private FCVMType Type;
	private int CPUUsage = 0;
	private int NetworkUsageTX = 0;
	private int NetworkUsageRX = 0;
	private String State = "";
	private String LCMState = "";
	private double LoadPrediction = 0;
	
// read-write fields - create and getter method
	private String NetworkIP;

// public-methods	
	public FCVirtualMachine(FossCloud AFossCloud, FCVMType AType, String ANetworkIP) {

// Call superclass creator
		super();

// Initialize fields
		GFossCloud = AFossCloud;
		Type = AType;
		NetworkIP = ANetworkIP;
		if (NetworkIP.equalsIgnoreCase("")) {
			NetworkIP = AType.IP;
		}
		
// Initialize VM Template
		System.out.println("Defining template type \"" + AType.Name + "\"");
		VMTemplate = String.format(Type.VMTemplate,	NetworkIP);
	}
	
	public void ShowInfo() {
		String info =
			String.format("%d\t", ID) +
			String.format("%-15s\t", Type.Name) +
			String.format("%b\t", Type.Scalable) +
			String.format("%b\t", Ready) +
			String.format("%2d\t", CPUUsage) +
			String.format("%d\t", NetworkUsageTX) +
			String.format("%d\t", NetworkUsageRX) +
			String.format("%.2f\t", LoadPrediction) +
			String.format("%s\t", State) +
			String.format("%s", LCMState);

			System.out.println(info);
	}
	
	public void UpdateInfo() {
		OneResponse oneResp = VMInstance.info();
			
		if (oneResp.isError()) {
			System.out.println("Error retrieving info from \"" + Type.Name + "\": " + oneResp.getErrorMessage());
		} else {
			if (!LastPoll.equalsIgnoreCase(VMInstance.xpath("LAST_POLL"))) {
				LastPoll = VMInstance.xpath("LAST_POLL");
				CPUUsage = Integer.valueOf(VMInstance.xpath("CPU"));
				if (CPUUsage > 100) CPUUsage = 100;
				NetworkUsageTX = Integer.valueOf(VMInstance.xpath("NET_TX"));
				NetworkUsageRX = Integer.valueOf(VMInstance.xpath("NET_RX"));
				State = VMInstance.stateStr();
				LCMState = VMInstance.lcmStateStr();
			
// Update CPU Usage Serie
				for(int i = 6; i > 0; i--) {
					CPUSerie[i] = CPUSerie[i - 1]; 
				}
				CPUSerie[0] = CPUUsage;
			
// Update Load Prediction
				double PrevLP = LoadPrediction;
				LoadPrediction = 0;
				for(int i = 0; i < 7; i++) {
					LoadPrediction = LoadPrediction + (CPUSerie[i] / Math.pow(2, (i + 1)));
				}

// Check if OS is ready to use
				if (!Ready) {
					if (LoadPrediction < PrevLP)
						if (NetworkUsageTX > 0) {
							Ready = true;

// Reset load prediction, CPU Usage and CPU serie							
							CPUSerie[0] = CPUUsage;
							LoadPrediction = CPUUsage / 2;
							for(int i = 1; i < 7; i++) {
								CPUSerie[i] = 0;
							}
						}
				}
// Show info
				ShowInfo();
				
			}
		}
	}

	public void AllocateVM() {
		try
		{
			System.out.print("Trying to allocate the virtual machine \"" + Type.Name + "\"...");
			
// Allocate the VM
			OneResponse rc = VirtualMachine.allocate(GFossCloud.getOneClient(), VMTemplate, true);

		    if (rc.isError())
		    {
		        System.out.println("failed!");
		        throw new Exception(rc.getErrorMessage());
		    }

		    ID = Integer.parseInt(rc.getMessage());
		    System.out.println("ok, ID " + ID + ".");

// Instantiate the VM
		    VMInstance = new VirtualMachine(ID, GFossCloud.getOneClient());

// Deploy the VM
		    System.out.print("Trying to deploy the new VM \"" + Type.Name + "\"...");
		    rc = VMInstance.deploy(0);

		    if (rc.isError())
		    {
		        System.out.println("failed!");
		        throw new Exception(rc.getErrorMessage());
		    }
		    else
		    {
		    	VMDeployed = true;
		    	System.out.println("success!");
		    	
// Retrieve new VM IP
				OneResponse oneResp = VMInstance.info();
				
				if (!oneResp.isError()) {
					NetworkIP = VMInstance.xpath("TEMPLATE/CONTEXT/ETH0_IP");
				    System.out.println("New VM \"" + Type.Name + "\" has IP " + NetworkIP);
				}
		    }
		}
		catch (Exception e)
		{
		    System.out.println(e.getMessage());
		}
		
// Create monitoring thread
		VMMonitor = new FCVirtualMachineMonitor(this, 10);
}
	
	public void DeleteVM() {
		try
		{
			if (VMDeployed) {
				System.out.println("Trying to finalize (delete) the VM \"" + Type.Name + "\" ID \"" + ID + "\" ...");
				VMDeployed = false;
				MonitorVM(false);
				OneResponse rc = VMInstance.delete(false);

				rc = VMInstance.info();
				if(rc.isError())
					throw new Exception(rc.getErrorMessage());

				System.out.println(
						"The VM \"" + Type.Name + "\" ID " + ID + "\" has state \"" + VMInstance.stateStr() +
						"\" and lcmState \"" + VMInstance.lcmStateStr() + "\"");
			} else {
				System.out.println("VM \"" + Type.Name + "\" not deployed, nothing to do");
			}
	    
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
		}	
	}
	
	public void MonitorVM(boolean AActive) {
		if (AActive) {
			VMMonitor.start();
		} else {
			VMMonitor.StopMonitor();
		}
	}
	
	public FCVirtualMachineMonitor getVMMonitor() {
		return VMMonitor;
	}
	
	public VirtualMachine getVMInstance() {
		return VMInstance;
	}

	public int getID() {
		return ID;
	}

	public FCVMType getType() {
		return Type;
	}

	public boolean isOSReady() {
		return Ready;
	}
	
	public boolean isVMDeployed() {
		return VMDeployed;
	}
	
	public int getCPUUsage() {
		return CPUUsage;
	}

	public int getNetworkUsageTX() {
		return NetworkUsageTX;
	}

	public int getNetworkUsageRX() {
		return NetworkUsageRX;
	}

	public String getState() {
		return State;
	}

	public String getLCMState() {
		return LCMState;
	}

	public double getLoadPrediction() {
		return LoadPrediction;
	}

	public String getNetworkIP() {
		return NetworkIP;
	}

}
