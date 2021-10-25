import java.util.*;
import java.net.*;
import java.io.*;


public class P2PNode extends P2PConnection {

	public static int timeout = 5000;

	public P2PNode (InetAddress targetAddress, Integer targetPort, DatagramSocket socket, P2PTYPE nodeType)
	throws IOException {
		super(socket, nodeType);
	}

	public static void main(String args[]) throws IOException {
		if (args.length != 3) {
			System.out.println("Correct call: java P2PNode <super node ip:port> <Node type (super/regular)> <localport>");
			return;
		}

		InetAddress targetAddress = null;
		Integer targetPort = null;
		DatagramSocket socket = null;
		P2PTYPE nodeType = null;

		try {

			targetAddress = InetAddress.getByName(args[0].split(":")[0]);
			targetPort = Integer.parseInt(args[0].split(":")[1]);
			socket = new DatagramSocket(Integer.parseInt(args[2]));
			nodeType = P2PTYPE.valueOf(args[1].toUpperCase());

		} catch (ArrayIndexOutOfBoundsException aioobe) {
			System.out.println("Please, provide a valid host target (IP:PORT)");
			return;
		} catch (NumberFormatException nfe) {
			System.out.println("Please, provide a valid number for the port");
			return;
		} catch (IllegalArgumentException iae) {
			System.out.println("Please, provide a valid node type (super / regular)");
			return;
		} catch (UnknownHostException uhe) {
			System.out.println("Please, provide a valid target node IP");
			return;
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		P2PNode node = new P2PNode(targetAddress, targetPort, socket, nodeType);
		node.script();
	}

	private void script() {

	}

}

enum P2PTYPE  {
    REGULAR("REGULAR"), SUPER("SUPER");

    private String type;
    P2PTYPE(String type) {
        this.type = type;
    }

    public String toString() {
        return this.type;
    }
}