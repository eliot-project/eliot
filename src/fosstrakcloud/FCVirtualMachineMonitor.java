package fosstrakcloud;

public class FCVirtualMachineMonitor extends Thread {
	int GInterval;
	boolean GRunning = true;
	FCVirtualMachine Gfcvm;
	
	public FCVirtualMachineMonitor(FCVirtualMachine Afcvm, int AInterval) {
		Gfcvm = Afcvm;
		GInterval = AInterval;
	}
	
	public void StopMonitor() {
		GRunning = false;
    }
	  
	  public void run() {
		while (GRunning) {
			Gfcvm.UpdateInfo();

			try {
				Thread.sleep((long) GInterval * 1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

}
