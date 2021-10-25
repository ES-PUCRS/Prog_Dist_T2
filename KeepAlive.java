import java.io.*;
import java.net.*;
import java.util.*;

public class KeepAlive extends Thread {
	protected volatile DatagramSocket socket;
	protected DatagramPacket packet;
	protected boolean LOG;
	protected byte[] data;

	public KeepAlive(DatagramSocket socket)
	throws IOException {
		this.socket = socket;
		LOG = false;
		data = null;
	}

	public void toggleLog() { LOG = !LOG; }

	public void setTarget(InetAddress targetAddress, Integer targetPort) {
		data 	= ("heartbeat>" + socket.getLocalPort()).getBytes();
		packet 	= new DatagramPacket(
			data,
			data.length,
			targetAddress,
			targetPort
		);
	}

	public void run() {
		while (true) {
			try {
				socket.send(packet);
			} catch (IOException e) {
				if(socket.isConnected())
					socket.close();
			}
			
			try {
				Thread.sleep(P2PNode.timeout);
			} catch(InterruptedException ignore) {}

			if(LOG)
				System.out.println("\npulse!");
		}
	}
}
