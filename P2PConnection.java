import java.util.*;
import java.net.*;
import java.io.*;

public class P2PConnection extends KeepAlive {
	
	protected volatile DatagramSocket socket;
	protected final P2PTYPE nodeType;

	protected InetAddress targetAddress;
	protected Integer targetPort;

	protected Thread watchdogThread;
	protected boolean enabled;

	public P2PConnection(DatagramSocket socket, P2PTYPE nodeType)
	throws IOException {
		super(socket);
		enabled = true;

		this.nodeType = nodeType;
		watchdogThread = new Thread(watchdog);
	}

	public void connect(InetAddress targetAddress, Integer targetPort) {
		send(targetAddress, targetPort, "lookup");

		// super.setTarget(targetAddress, targetPort);
		// super.start();
	}

	public void killConnection() {
		enabled = false;
	}

	private Runnable watchdog = new Runnable() {
		public void run() {
			byte[] data = new byte[1024];
			DatagramPacket received =
				new DatagramPacket(data, data.length);
			while(enabled) {
				if(!socket.isConnected())
					break;

				try {
					socket.receive(received);
					router(clonePacket(received));
				} catch (IOException e) {
					if(socket.isConnected())
						socket.close();
				}
				
				Arrays.fill(data, (byte) 0);
			}
			if(socket.isConnected())
				socket.close();
		}
	};


	private void router(DatagramPacket packet) {
		String data = trimPacketData(packet);
		System.out.println("Received> " + data);
		String method = "";

		try {
			method = data.split(">")[0];
		} catch (ArrayIndexOutOfBoundsException aioobe) {
			method = data;
		}

		switch (method) {
			case "lookup":
				send(packet, nodeType.toString());
				break;
			default:
		}

	}


	/* Auxiliar methods -------------------------------------------------*/

	/* Send package to the destination */
	private void send (DatagramPacket packet, String content)
	{ send(packet.getAddress(), packet.getPort(), content); }
	private void send (InetAddress targetAddress, Integer targetPort, String content) {
		DatagramPacket packet = createPacket(targetAddress, targetPort, content);
		try { socket.send(packet); }
		catch(IOException ioe) { ioe.printStackTrace(); }
	}

	/* Cut packet data String unused */
	private String trimPacketData(DatagramPacket packet) {
		return new String(
			packet.getData(),
			0,
			packet.getLength()
		);
	}

	/* Create a packet to be sent to the applicant */
	private DatagramPacket createPacket (String data)
	{ return createPacket( data.getBytes()); }
	private DatagramPacket createPacket (byte[] data) {
		return new
			DatagramPacket(
				data,
				data.length,
				this.targetAddress,
				this.targetPort
			);
	}
	private DatagramPacket createPacket (DatagramPacket from, String data)
	{ return createPacket( from, data.getBytes()); }
	private DatagramPacket createPacket (DatagramPacket from, byte[] data) {
		return new
			DatagramPacket(
				data,
				data.length,
				from.getAddress(),
				from.getPort()
			);
	}
	private DatagramPacket createPacket (InetAddress targetAddress, Integer targetPort, String data)
	{ return createPacket( targetAddress, targetPort, data.getBytes()); }
	private DatagramPacket createPacket (InetAddress targetAddress, Integer targetPort, byte[] data) {
		return new
			DatagramPacket(
				data,
				data.length,
				targetAddress,
				targetPort
			);
	}


	/* Clone the packet to be able to change the state and resend it */
	private DatagramPacket clonePacket(DatagramPacket packet) {
		return
			new DatagramPacket (
				packet.getData(),
				packet.getLength(),
				packet.getAddress(),
				packet.getPort()
			);
	}
}
