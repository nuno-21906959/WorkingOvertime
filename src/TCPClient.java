import java.io.*;
import java.net.*;
import java.util.*;

public class TCPClient {

	private static final int port = 7142;
	private static BufferedReader input;
	private static PrintStream output;
	private static ArrayList<String> termos = new ArrayList<>() {{
		add("over");
		add("user valido");
		add("mensagem valida");
		add("comando valido");
		add("fechar");
		add("entrou");
	}};
	private static String menu = "[MENU CLIENTE]\n" +
			"0 \t–> Menu Inicial\n" +
			"1 \t–> Listar utilizadores online\n" +
			"2 \t–> Enviar mensagem a um utilizador\n" +
			"3 \t–> Enviar mensagem a todos os utilizadores\n" +
			"4 \t–> lista branca de utilizadores\n" +
			"5 \t–> lista negra de utilizadores\n" +
			"99 \t–> Sair";


	private static void out (String mensagem){
		if (!termos.contains(mensagem)){
			System.out.println(mensagem);
		}
	}



	public static void main(String args[]) throws Exception {

		if (args.length !=1){
			System.err.println ("Usage: java TCPClient <host>");
			System.exit(1);
		}

		String host = args[0];
		String messageOut, messageIn = null;
		Socket client = null;
		try {client = new Socket(host,port);
		} catch (ConnectException e){
			System.out.print("O Servidor está offline...");
			return;
		}
		input = new BufferedReader(
				new InputStreamReader(client.getInputStream()));
		output = new PrintStream(
				client.getOutputStream(),true);
		boolean feito = false;



		while(true) {
			Scanner scan = new Scanner (System.in);

			if (!feito){
				do {	/* espera a verificacao das listas branca e negra */
					messageIn = input.readLine();
					out(messageIn);
					if (messageIn.equals("fechar")){
						input.close();
						output.close();
						client.close();
						return;
					} else if (messageIn.equals("entrou")){
						out(menu);
						break;
					}
				}while (!messageIn.equals("fechar") || !messageIn.equals("entrou"));
				feito = true;
			}

			do {
				System.out.print(">");
				messageOut = scan.nextLine();
				try{
					output.println(messageOut);
					messageIn = input.readLine();
					out(messageIn);
				} catch (SocketException e){
					if (messageOut.equals("99")){
						System.out.print("O Servidor já se encontra offline..\nA fechar..");
						input.close();
						output.close();
						return;
					}
					int timer = 10;
					System.out.print("A tentar reestabelecer ligação ao servidor");
					while (timer > 0) {
						timer--;
						Thread.sleep(1000);
						if (timer % 3 == 0){
							System.out.print(".");
						}
						if (timer == 0){
							try {
								client = new Socket(host,port);
							} catch (ConnectException ce){
								System.out.println("\nImpossível estabelecer conexão!\nA fechar..");
								input.close();
								output.close();
								return;
							}
							System.out.println("\nConexão restabelecida!");
							input.close();
							output.close();
							input = new BufferedReader(
									new InputStreamReader(client.getInputStream()));
							output = new PrintStream(
									client.getOutputStream(),true);
							input.readLine();
							input.readLine();

						}
					}
				}
			} while (!messageIn.equals("comando valido"));


			String opcao = messageOut;
			switch(opcao) {
				case "99":
					do {
						messageIn = input.readLine();
						out(messageIn);
						if (messageIn.equals("fechar")){
							input.close();
							output.close();
							client.close();

						}
					} while (!messageIn.equals("fechar"));
					return;
				case "0":
					out(menu);
					break;
				case "1":
				case "4":
				case "5":
					do {
						messageIn = input.readLine();
						out(messageIn);
					}
					while(!messageIn.equals("over"));
					break;
				case "2":
					do{
						do {	/* ciclo ate enviar um user valido */
							messageIn = input.readLine();
							out(messageIn);
							if (!termos.contains(messageIn)) {
								System.out.print(">");
								messageOut = scan.nextLine();
								output.println(messageOut);
							}
						}while(!termos.contains(messageIn));

						do {	/* ciclo ate enviar uma mensagem */
							messageIn = input.readLine();
							out(messageIn);
							if (!termos.contains(messageIn)){
								System.out.print(">");
								messageOut = scan.nextLine();
								output.println(messageOut);
							}
						}while(!termos.contains(messageIn));

						/* print das mensagens de confirmacao */
						do {
							messageIn = input.readLine();
							out(messageIn);
						}while(!termos.contains(messageIn));
					}while(!messageIn.equals("over"));
					break;
				case "3":
					do{
						do {	/* ciclo ate enviar uma mensagem */
							messageIn = input.readLine();
							out(messageIn);
							if (!termos.contains(messageIn)){
								System.out.print(">");
								messageOut = scan.nextLine();
								output.println(messageOut);
							}
						}while(!termos.contains(messageIn));

						/* print das mensagens de confirmacao */
						do{
							messageIn = input.readLine();
							out(messageIn);
						}while(!messageIn.equals("over"));
					}while(!messageIn.equals("over"));
					break;
				default:
					break;
			}
		}
	}
}