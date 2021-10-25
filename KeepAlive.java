import java.io.*;
import java.net.*;
import java.util.*;

public class KeepAlive extends Thread {
	protected volatile DatagramSocket socket;
	protected DatagramPacket packet;
	protected boolean alive;
	protected boolean LOG;
	protected byte[] data;

	public KeepAlive(DatagramSocket socket)
	throws IOException {
		this.socket = socket;

		alive = true;
		data = null;
	}

	public void toggleAlive() { alive = !alive; }

	public void setTarget(InetAddress targetAddress, Integer targetPort) {
		data 	= ("heartbeat").getBytes();
		packet 	= new DatagramPacket(
			data,
			data.length,
			targetAddress,
			targetPort
		);
		
		if(!this.isAlive())
			this.start();
	}

	public void run() {
		while (alive) {
			try {
				socket.send(packet);
			} catch (SocketException se) {
				alive = false;
			} catch (Exception e) { e.printStackTrace(); }
			
			try {
				Thread.sleep(P2PNode.timeout-2000);
			} catch(InterruptedException ignore) { ignore.printStackTrace(); }
		}
	}
}
