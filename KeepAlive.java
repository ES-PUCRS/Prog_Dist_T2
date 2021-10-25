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
		
		this.start();
	}

	public void run() {
		while (alive) {
			try {
				socket.send(packet);
			} catch (SocketException se) {
				alive = false;
			} catch (IOException ioe) {}
			
			try {
				Thread.sleep(P2PNode.timeout);
			} catch(InterruptedException ignore) {}
		}
	}
}
