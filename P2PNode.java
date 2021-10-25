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

	public static final String NATCIDR = "192.168.100.";
	public static final int timeout = 5000;

	private Map<String, String> table; 

	public P2PNode (DatagramSocket socket, P2PTYPE nodeType)
	throws IOException {
		super(socket, nodeType);

		if(nodeType == P2PTYPE.REGULAR)
			table = new HashMap<String,String>();
	}

	public static void main(String args[]) throws IOException {
		if (args.length < 2 || args.length > 3) {
			System.out.println("Usage: java P2PNode <Node type (super/regular)> <localport> [super node ip:port]");
			return;
		}
		System.out.println(Arrays.toString(args));
		
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
			.script(targetAddress, targetPort, nodeType);
	}

	private void script(InetAddress targetAddress, Integer targetPort, P2PTYPE nodeType) {
		Scanner  in = new Scanner(System.in);
		String[] vargs = null;
		String   input = "";
		String   comnd = "";

		if(nodeType == P2PTYPE.REGULAR){
			readFile("dataset");
			System.out.println("[");
			for (String key: table.keySet()) {
			    System.out.println("\t" + key);
			}
			System.out.println("]");
		}

		while(!comnd.equals("quit")) {
			input = in.nextLine();
			vargs = input.split("\\s");
			comnd = vargs[0];

			if(nodeType == P2PTYPE.SUPER)
				superConsole(targetAddress, targetPort, input, vargs, comnd);
			else
				regularConsole(targetAddress, targetPort, input, vargs, comnd);
		}
	}

	/* Console Interface -------------------------------------------------*/

	private void superConsole(InetAddress targetAddress, Integer targetPort, String input, String[] vargs, String comnd) {
		switch (comnd) {
			case "quit":
				super.killConnection();
				break;

			case "table":
				System.out.println(super.table());
				break;

			case "toggleBeatsLog":
				super.toggleLog();
				break;

			case "toggleBeats":
				super.toggleAlive();
				break;

			case "connect":
				try { super.connect(targetAddress, targetPort, table); }
				catch (Exception e)
					{ e.printStackTrace(); }
				catch (Error err)
					{ System.out.println(err.getClass().getSimpleName() + ": "+ err.getLocalizedMessage()); }
				break;


			default:
				System.out.println("Command not found.");
		}
	}

	private void regularConsole(InetAddress targetAddress, Integer targetPort, String input, String[] vargs, String comnd) {
		switch (comnd) {
			case "quit":
				super.killConnection();
				break;

			case "add":
				String file = input.replaceFirst(comnd+"\\s", "");
				table.put(file, Hash.function(file));
				System.out.println(table.get(file));
				break;

			case "import":
				readFile(input.replaceFirst(comnd+"\\s", ""));
				break;

			case "toggleBeats":
				super.toggleAlive();
				break;


			case "table":
				System.out.println("[");
				for (String key: table.keySet()) {
				    System.out.println("\t" + key);
				}
				System.out.println("]");
				break;


			case "connect":
				try { super.connect(targetAddress, targetPort, table); }
				catch (Exception e)
					{ e.printStackTrace(); }
				catch (Error err)
					{ System.out.println(err.getClass().getSimpleName() + ": "+ err.getLocalizedMessage()); }
				break;


			default:
				System.out.println("Command not found.");
		}
	}

	/* File Manager -------------------------------------------------*/

	public void readFile(String filename) {
		try {
			File file = new File(filename);
			Scanner reader = new Scanner(file);
			while (reader.hasNextLine()) {
				String data = reader.nextLine();
				table.put(data, Hash.function(data));
			}
			reader.close();
		} catch (FileNotFoundException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}
}