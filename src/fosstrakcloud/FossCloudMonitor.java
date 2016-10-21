package fosstrakcloud;

public class FossCloudMonitor extends Thread {
	int GInterval;
	boolean GRunning = true;
	FossCloud GFossCloud;
	
	public FossCloudMonitor(FossCloud AFossCloud, int AInterval) {
		GFossCloud = AFossCloud;
		GInterval = AInterval;
	}
	
	public void StopMonitor() {
		GRunning = false;
    }
	  
	  public void run() {
		while (GRunning) {
			GFossCloud.EvaluateAllocVM();
			GFossCloud.EvaluateReleaseVM();

			try {
				Thread.sleep((long) GInterval * 1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			
		}
	}
}
