import java.util.concurrent.*;
import java.util.TimerTask;
import java.util.Timer;
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

	private Map<String, Node> table;

	class Node {
		List<String> content;
		TimerTask task;
		Timer timer;

		public Node() {
			content = null;
			timer = null;
			task = null;
		}

		@Override
		public String toString() {
			if(content != null)
				return String.valueOf(content.size());
			return P2PTYPE.SUPER.toString();
		}
	}

	public P2PConnection (DatagramSocket socket, P2PTYPE nodeType)
	throws IOException {
		super(socket);
		this.enabled = true;
		this.heartbeat = false;

		this.socket = socket;
		this.response = "";
		this.nodeType = nodeType;

		if(nodeType == P2PTYPE.SUPER) {
			table = new HashMap<String, Node>();
		}

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
		System.out.println("Stablishing connection. . .");
		send(targetAddress, targetPort, "looktype");

		// 1º Validate if it is a supernode 
		receivedReply.acquire();
		if(P2PTYPE.valueOf(response.split(":")[1]) != P2PTYPE.SUPER)
			throw new UnsatisfiedLinkError("Cannot connect with a regular node!");
		send(targetAddress, targetPort, "connect");
		this.targetAddress = targetAddress;
		this.targetPort = targetPort;

		// 2º Checkin connection and activate KeepAlive
		receivedReply.acquire();
		super.setTarget(targetAddress, targetPort);

		// 3º
		// If the connection is a new regular node, import the files
		// If is a super node, reorganize the network topology
		if(table != null)
			for (Map.Entry<String, String> entry: table.entrySet()) {
			    send(targetAddress, targetPort, "include>"+entry.getValue());
				receivedReply.acquire();
			}
		else
			send(targetAddress, targetPort,
				("topology>"+
					targetAddress+":"+targetPort +">"+
					getLocalAddress()+":"+socket.getLocalPort()
				)
			);

		return true;
	}

	public void toggleLog() { heartbeat = !heartbeat; }
	public void toggleAlive() { super.toggleAlive(); }

	public String table() {
		return table.toString();
	}

	/* Sockets Access---------------------------------------------------------*/
	
	public void topology (DatagramPacket packet, String target, String dst) {
		String key = this.targetAddress+":"+this.targetPort;
		if(target.equals(key) || (this.targetAddress == null && this.targetPort == null)) {
			String[] args = dst.split(":");
			try {
				connect(
					InetAddress.getByName(args[0].substring(1, args[0].length())),
					Integer.parseInt(args[1]),
					null
				);
			} catch (Exception e) { e.printStackTrace(); }
		} else {
			send(createPacket("topology>"+target+">"+dst));
		}

		System.out.println("address:: " + this.targetAddress);
		System.out.println("port:: " + this.targetPort);
	}

	public void include (DatagramPacket packet) {
		String hash = trimPacketData(packet).split(">")[1];
		String key = packetKey(packet);

		if(!table.containsKey(key)){
			send(packet, "include:fail");
			return;
		}

		List<String> list = table.get(key).content;
		if(list == null){
			list = new LinkedList<String>();
			table.get(key).content = list;
		}

		list.add(hash);
		send(packet, "include:ok");
	}

	public void connect (DatagramPacket packet) {
		String key = packetKey(packet);

		if(!table.containsKey(key)) {
			table.put(key, new Node());
		}

		
		send(packet, "connect:ok");
	}


	/* Router ---------------------------------------------------------*/

	/*
	 *	Router defines what should happen with the received packet 
	 */
	private final String not_found = "!METHOD NOT FOUND!";
	private void router (DatagramPacket packet) {
		String data = trimPacketData(packet);
		String msg = packet.getAddress() + ":" + packet.getPort() + "> " + data;
		if (msg.length() > 70) msg = msg.substring(0,70) + "...";
		if( ( 		!data.contains("heartbeat")
				&&  !data.contains(":fail")
				&&  !data.contains(":ok")
			)
			|| heartbeat
		)
			System.out.println(msg);

		String[] vargs = data.split(">");
		String 	 method = "";

		try {
			method = vargs[0];
		} catch (ArrayIndexOutOfBoundsException aioobe) {
			method = data;
		}

		if(method.equals("not_found"))
			throw new AssertionError(data);
		else
		switch (method) {
			case "looktype":
				send(packet, "looktype:" + nodeType.toString());
				System.out.println("SENT looktype:"+nodeType.toString());
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

			case "topology":
				topology(packet, vargs[1], vargs[2]);
				break;


			case "heartbeat":
			String key = packetKey(packet);
				heart(key);
				break;


			default:
			try {
				Thread.sleep(5000);
				send(packet, (not_found + ">" + method));
			}catch(Exception e) {}

		}

	}

	/* Runnable Threads -------------------------------------------------*/

	public void heart(String key) {
		Node node = table.get(key);
		if(node == null) return;

		TimerTask task = node.task;
		Timer timer = node.timer;
	    if(timer != null) {
            task.cancel();
            timer.cancel();
            timer.purge();
        }

        task = new TimerTask() {
            @Override
            public void run() {
            	System.out.println("System: Dropping node>"+key);
                table.remove(key);
            }
        };
        
        timer = new Timer();
        timer.schedule(task, P2PNode.timeout);
        node.timer = timer;
        node.task = task;
        table.put(key, node);
    }

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
		send(packet);
	}
	private void send (DatagramPacket packet) {
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

	/* Search throught the network interface for the Nat CIDR */
	private InetAddress getLocalAddress() {
		Enumeration e = null;
		try {
			e = NetworkInterface.getNetworkInterfaces();
		} catch (Exception ignore) {}
		while(e.hasMoreElements()) {
		    NetworkInterface n = (NetworkInterface) e.nextElement();
		    Enumeration ee = n.getInetAddresses();
		    InetAddress inet = null;
		    while (ee.hasMoreElements()) {
		        inet = (InetAddress) ee.nextElement();
		        if(inet.getHostAddress().startsWith(P2PNode.NATCIDR))
		        	return inet;
		    }
		}
	    return null;
	}
}