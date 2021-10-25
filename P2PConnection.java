import java.util.concurrent.*;
import java.util.*;
import java.net.*;
import java.io.*;

public class P2PConnection extends KeepAlive {
	
	private volatile DatagramSocket socket;
	private final P2PTYPE nodeType;
	private String response;

	private InetAddress targetAddress;
	private Integer targetPort;

	private Semaphore receivedReply;
	private boolean enabled;

	public P2PConnection(DatagramSocket socket, P2PTYPE nodeType)
	throws IOException {
		super(socket);
		enabled = true;

		this.socket = socket;
		this.response = "";
		this.nodeType = nodeType;

		receivedReply = new Semaphore(1);
		new Thread(watchdog).start();

		try { receivedReply.acquire(); }
		catch(InterruptedException ie) {}
	}

	public void killConnection() {
		enabled = false;
		socket.close();
	}



	public boolean connect(InetAddress targetAddress, Integer targetPort)
	throws InterruptedException, IndexOutOfBoundsException, UnsatisfiedLinkError {
		send(targetAddress, targetPort, "looktype");

		// Wait for the response from target node 
		receivedReply.acquire();
		if(P2PTYPE.valueOf(response.split(":")[1]) != P2PTYPE.SUPER)
			throw new UnsatisfiedLinkError("Cannot connect with a regular node!");


		send(targetAddress, targetPort, "connect>");
		// super.setTarget(targetAddress, targetPort);
		// super.start();
		return true;
	}



	/*
	 *	Router defines what should happen with the received packet 
	 */
	private final String not_found = "!METHOD NOT FOUND!";
	private void router(DatagramPacket packet) {
		String data = trimPacketData(packet);
		System.out.println(
			packet.getAddress() + ":" + packet.getPort() + "> " + data
		);
		String method = "";

		try {
			method = data.split(">")[0];
		} catch (ArrayIndexOutOfBoundsException aioobe) {
			method = data;
		}

		if(method.equals("not_found"))
			throw new AssertionError(data);
		switch (method) {
			case "looktype":
				send(packet, "looktype:" + nodeType.toString());
				break;

			case "looktype:REGULAR":
			case "looktype:SUPER":
				this.response = trimPacketData(packet);
				receivedReply.release();
				break;

			case "connect":
				//
				break;

			default:
				send(packet, (not_found + ">" + method));

		}

	}

	/* Runnable Threads -------------------------------------------------*/

	private Runnable watchdog = new Runnable() {
		public void run() {
			byte[] data = new byte[1024];
			DatagramPacket received =
				new DatagramPacket(data, data.length);
			while(enabled) {
				try {
					socket.receive(received);
					router(clonePacket(received));
				} catch (IOException ioe) {}
				
				Arrays.fill(data, (byte) 0);
			}

			if(socket != null)
				socket.close();
		}
	};

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


	/* Create a packet to be sent to the applicant ++Method overload */
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

}
