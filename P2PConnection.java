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
	private boolean heartbeat;
	private boolean enabled;

	private Map<String, List<String>> table;

	public P2PConnection (DatagramSocket socket, P2PTYPE nodeType)
	throws IOException {
		super(socket);
		this.enabled = true;
		this.heartbeat = false;

		this.socket = socket;
		this.response = "";
		this.nodeType = nodeType;

		if(nodeType == P2PTYPE.SUPER)
			table = new HashMap<String, List<String>>();

		receivedReply = new Semaphore(1);
		new Thread(watchdog).start();

		try { receivedReply.acquire(); }
		catch(InterruptedException ie) {}
	}

	/* Console Access ---------------------------------------------------------*/
	
	public void killConnection() {
		enabled = false;
		socket.close();
	}

	public boolean connect (InetAddress targetAddress, Integer targetPort, Map<String,String> table)
	throws InterruptedException, IndexOutOfBoundsException, UnsatisfiedLinkError {
		send(targetAddress, targetPort, "looktype");

		// 1º Validate if it is a supernode 
		receivedReply.acquire();
		if(P2PTYPE.valueOf(response.split(":")[1]) != P2PTYPE.SUPER)
			throw new UnsatisfiedLinkError("Cannot connect with a regular node!");
		send(targetAddress, targetPort, "connect");

		// 2º Checkin connection and activate KeepAlive
		receivedReply.acquire();
		super.setTarget(targetAddress, targetPort);

		// 3º Update DHT
		for (Map.Entry<String, String> entry: table.entrySet()) {
		    send(targetAddress, targetPort, "include>"+entry.getValue());
			receivedReply.acquire();
		}

		return true;
	}

	public void toggleLog() { heartbeat = !heartbeat; }
	public void toggleAlive() { super.toggleAlive(); }

	/* Sockets Access---------------------------------------------------------*/
	
	public void include (DatagramPacket packet) {
		String hash = trimPacketData(packet).split(">")[1];
		String key = packetKey(packet);

		if(!table.containsKey(key)){
			send(packet, "include:fail");
			return;
		}

		table.get(key).add(hash);

		send(packet, "include:ok");
	}

	public void connect (DatagramPacket packet) {
		String key = packetKey(packet);

		if(!table.containsKey(key))
			table.put(key, new LinkedList<String>());
		
		send(packet, "connect:ok");
	}


	/* Router ---------------------------------------------------------*/

	/*
	 *	Router defines what should happen with the received packet 
	 */
	private final String not_found = "!METHOD NOT FOUND!";
	private void router (DatagramPacket packet) {
		String data = trimPacketData(packet);
		if( ( 		!data.contains("heartbeat")
				&&  !data.contains(":fail")
				&&  !data.contains(":ok")
			)
			|| heartbeat
		)
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
		else
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
				connect(packet);
				break;
			case "connect:ok":
				this.response = trimPacketData(packet);
				receivedReply.release();
				break;
			

			case "include":
				include(packet);
				break;
			case "include:fail":
			case "include:ok":
				this.response = trimPacketData(packet);
				receivedReply.release();
				break;

			case "heartbeat":
				break;

			default:
			try {
				Thread.sleep(5000);
				send(packet, (not_found + ">" + method));
			}catch(Exception e) {}

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

	/* Generate a map key by concatenating address and port */
	private String packetKey(DatagramPacket packet) {
		return packet.getAddress() + ":" + packet.getPort();
	}

	/* Send package to the destination */
	private void send (DatagramPacket packet, String content)
	{ send(packet.getAddress(), packet.getPort(), content); }
	private void send (InetAddress targetAddress, Integer targetPort, String content) {
		DatagramPacket packet = createPacket(targetAddress, targetPort, content);
		try { socket.send(packet); }
		catch(IOException ioe) { ioe.printStackTrace(); }
	}


	/* Cut packet data String unused */
	private String trimPacketData (DatagramPacket packet) {
		return new String(
			packet.getData(),
			0,
			packet.getLength()
		);
	}


	/* Clone the packet to be able to change the state and resend it */
	private DatagramPacket clonePacket (DatagramPacket packet) {
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
