import java.security.MessageDigest;
import java.util.*;
import java.net.*;
import java.io.*;

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


public class P2PNode extends P2PConnection {

	public static int timeout = 5000;

	public P2PNode (DatagramSocket socket, P2PTYPE nodeType)
	throws IOException {
		super(socket, nodeType);
	}

	public static void main(String args[]) throws IOException {
		if (args.length < 2 || args.length > 3) {
			System.out.println("Correct call: java P2PNode <Node type (super/regular)> <localport> [super node ip:port]");
			return;
		}

		InetAddress targetAddress = null;
		Integer targetPort = null;
		DatagramSocket socket = null;
		P2PTYPE nodeType = null;

		try {
			if(args.length == 3) {
				targetAddress = InetAddress.getByName(args[0].split(":")[0]);
				targetPort = Integer.parseInt(args[0].split(":")[1]);
			}
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

		new P2PNode(socket, nodeType)
			.script(targetAddress, targetPort);
	}

	private void script(InetAddress targetAddress, Integer targetPort) {
		Scanner  in = new Scanner(System.in);
		String[] input = null;
		String   comnd = "";

		while(true) {
			input = in.nextLine().split("\\s");
			comnd = input[0];

			switch (comnd) {
				case "quit":
					super.killConnection();
					break;

				case "hash":
					try { System.out.println(gerarHash(input[1])); }
					catch (Exception some) { some.printStackTrace(); }
					break;

				case "connect":
					try { super.connect(targetAddress, targetPort); }
					catch (Exception e)
						{ e.printStackTrace(); }
					catch (Error err)
						{ System.out.println(err.getClass().getSimpleName() + ": "+ err.getLocalizedMessage()); }
			}
		}

	}

	public static String hash(String in) throws Exception {
	    MessageDigest algorithm = MessageDigest.getInstance("SHA-256");
	    byte hash[] = algorithm.digest(in.getBytes("UTF-8"));

	    StringBuilder text = new StringBuilder();
	    for (byte b : hash) {
	      text.append(String.format("%02X", 0xFF & b));
	    }
	    return text.toString();
  	}

}