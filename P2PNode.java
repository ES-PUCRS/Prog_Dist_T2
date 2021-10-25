import java.util.*;
import java.net.*;
import java.io.*;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	private Map<String, String> table; 

	public P2PNode (DatagramSocket socket, P2PTYPE nodeType)
	throws IOException {
		super(socket, nodeType);
		table = new HashMap<String,String>();
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
		String[] vargs = null;
		String   input = "";
		String   comnd = "";

		while(!comnd.equals("quit")) {
			input = in.nextLine();
			vargs = input.split("\\s");
			comnd = vargs[0];

			if(nodeType == P2PTYPE.SUPER)
				superConsole(input, vargs, comnd);
			else
				regularConsole(input, vargs, comnd);
		}
	}

	private void superConsole(String input, String[] vargs, String comnd) {
		switch (comnd) {
			case "quit":
				super.killConnection();
				break;

			case "table":
				try { System.out.println(table); }
				catch (Exception some) { some.printStackTrace(); }
				break;

			case "connect":
				try { super.connect(targetAddress, targetPort); }
				catch (Exception e)
					{ e.printStackTrace(); }
				catch (Error err)
					{ System.out.println(err.getClass().getSimpleName() + ": "+ err.getLocalizedMessage()); }
				break;


			default:
				System.out.println("Command not found.");
		}
	}

	private void regularConsole(String input, String[] vargs, String comnd) {
		switch (comnd) {
			case "quit":
				super.killConnection();
				break;

			case "add":
				String file = input.replaceFirst(comnd+"\\s", "");
				try { table.put(file, Hash.function(file)); }
				catch (Exception some) { some.printStackTrace(); }
				break;

			case "table":
				try { System.out.println(table.keySet()); }
				catch (Exception some) { some.printStackTrace(); }
				break;


			case "connect":
				try { super.connect(targetAddress, targetPort); }
				catch (Exception e)
					{ e.printStackTrace(); }
				catch (Error err)
					{ System.out.println(err.getClass().getSimpleName() + ": "+ err.getLocalizedMessage()); }
				break;


			default:
				System.out.println("Command not found.");
		}
	}

	/*
	 *   Regex due to decompile the message to be sent
	 */
	public static String sanitize(String sentence) {
		System.out.println(sentence);
		Matcher matcher
			= Pattern
				.compile("\".*\"")
				.matcher(sentence);

		if(matcher.find())
			return matcher.group(0);
		return null;
	}
}