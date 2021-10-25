import java.security.MessageDigest;

/* 	Funções de hash
 *	Código ecoado do trecho:
 * 	Secure Hash Algorithm 2 (SHA-2)
 * 	Atualizado 23 May 2020 - 22:50, Rafael Guimarães Sakurai
 * 	http://www.universidadejava.com.br/outros/criptografia-funcao-hash/
 */
public class Hash {

	private static Hash instance;
	private static MessageDigest algorithm;

	private Hash () {
		try {
			algorithm = MessageDigest.getInstance("SHA-256");
		} catch (Exception ignore) {}
	}

	public static String function(String in) throws Exception {
		if(instance == null)
			instance = new Hash();

	    byte hash[] = algorithm.digest(in.getBytes("UTF-8"));

	    StringBuilder text = new StringBuilder();
	    for (byte b : hash) {
	      text.append(String.format("%02X", 0xFF & b));
	    }
	    return text.toString();
  	}

}