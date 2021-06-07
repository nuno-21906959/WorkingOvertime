import java.lang.reflect.Array;
import java.net.*;
import java.text.*;
import java.util.*;
import java.io.*;

public class Server {
	
	private static final int TCPport = 7142;
	private static final int UDPport = 9031;
	private static DatagramSocket datagramSocket;
	private static DatagramPacket inPacket, outPacket;
	private static byte[] buffer;
	private static InetAddress clientIP;
	private static ArrayList<InetAddress> listaOnline = new ArrayList<>();
	private static String menu = "[MENU CLIENTE]\n" +
			"0 \t–> Menu Inicial\n" +
			"1 \t–> Listar utilizadores online\n" +
			"2 \t–> Enviar mensagem a um utilizador\n" +
			"3 \t–> Enviar mensagem a todos os utilizadores\n" +
			"4 \t–> lista branca de utilizadores\n" +
			"5 \t–> lista negra de utilizadores\n" +
			"99 \t–> Sair";
	static String PATH_LISTABRANCA = "res/listabranca.txt";
	static String PATH_LISTANEGRA = "res/listanegra.txt";

	static List<String> listaBranca;
	static List<String> listaNegra;


	private static void out(String mensagem){
		escreveLog(mensagem);
		System.out.println(mensagem);
	}

	public static void escreveLog(String mensagem) {
		try {
			long millis = System.currentTimeMillis();
			java.util.Date data = new java.util.Date(millis);

			FileWriter fw = new FileWriter("log.txt", true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write("["+data+"] "+ mensagem);
			bw.newLine();
			bw.close();
		} catch (IOException e) {
			out("Erro a escrever no log.");
			e.printStackTrace();
		}
	}

	public static String lerFicheiro(String path) throws IOException {
		String s = "";
		File f = new File(path);

		if (f.exists()){
			BufferedReader buffRead = new BufferedReader(new FileReader(path));
			s = buffRead.readLine();
			buffRead.close();
		} else {
			s = "Ficheiro ("+ f.getName() +") não existe";
		}

		if (s == null){
			s = "Ficheiro ("+ f.getName() +") vazio";
		}

		return s;
	}

	public static List<String> carregarListas(String path) throws IOException {
		String s = lerFicheiro(path);
		List<String> lista = null;
		if (! (s == null) ){
			String[] l = s.split(";");
			lista = Arrays.asList(l);
		}
		return lista;
	}

	private static String listaIPParaString( ArrayList<InetAddress> lista, String opcao) {
		String s = "";
		int count = 0;

		switch (opcao){
			case "1":
				s += ("Users online ("+listaOnline.size()+"):\n");
		}

		for (InetAddress elemento: lista){
			s += (count)+ " – " +(elemento.toString().replace("/","")).toString() + "\n";
		}

		return s;
	}

	private static String listaParaString(List<String> lista, String opcao) {
		String s = "";
		int count = 0;

		switch (opcao){
			case "4":
				s += ("Lista branca:\n");
				break;
			case "5":
				s+= ("Lista negra:\n");
				break;
		}

		for (String elemento: lista){
			s += elemento + "\n";
		}

		return s;
	}

	private static InetAddress obterIP(String user, ArrayList<String> listaOnline ) throws UnknownHostException {
				return InetAddress.getByName(listaOnline.get(Integer.parseInt(user)));
	}

	private static void enviaMensagem(String user, String mensagem, DatagramSocket datagramSocket, DatagramPacket outPacket, ArrayList<InetAddress> listaOnline, boolean todos) throws UnknownHostException {

		String m = clientIP + " - "+ mensagem;

		if (!todos){
			InetAddress inetadress = listaOnline.get(Integer.parseInt(user));
			outPacket =	new DatagramPacket(m.getBytes(),
					m.length(), inetadress ,4445);

			//tenta enviar por UDP
			try {
				datagramSocket.send(outPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			for (InetAddress i : listaOnline){
				outPacket =	new DatagramPacket(m.getBytes(),
						m.length(), i ,4445);

				try {
					datagramSocket.send(outPacket);
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}

	}

	public static boolean validaConexao(Socket socket, List<String> listaNegra, List<String> listaBranca, ArrayList<InetAddress> listaUtilizadores) {
		if (listaBranca.contains("lista vazia") && listaNegra.contains(clientIP.toString().replace("/",""))){
			out("O cliente [" + clientIP.toString().replace("/","") + "] tentou conectar-se, mas está na lista negra...");
		} else if (listaBranca.contains(clientIP.toString().replace("/","")) && listaNegra.contains(clientIP.toString().replace("/",""))) {
			out("O cliente [" + clientIP.toString().replace("/","") + "] tentou conectar-se, mas está na lista negra...");
		} else if ( !listaNegra.contains(clientIP.toString().replace("/","")) && !listaBranca.contains("lista vazia") && !listaBranca.contains(clientIP.toString().replace("/",""))){
			out("O cliente [" + clientIP.toString().replace("/","") + "] tentou conectar-se, mas nao está na lista branca...");
		} else {
			out("O cliente [" + clientIP.toString().replace("/","") + "] conectou-se com sucesso!");
			return true;
		}
		return false;
	}


	public static void main(String args[]) throws Exception {
		ServerSocket server = new ServerSocket(TCPport);
		InetAddress myIPaddress = InetAddress.getLocalHost();
		Socket client = null;
		datagramSocket = new DatagramSocket(UDPport);
		boolean feito = false;

		out("TCP> " + myIPaddress.toString() + ":" + TCPport);
		out("UDP> " + myIPaddress.toString() + ":" + UDPport);


		while (true){
			client = server.accept();
			clientIP = client.getInetAddress();

			PrintStream output = new PrintStream(client.getOutputStream(),true);

			/* carregar listaBranca */
			listaBranca = carregarListas(PATH_LISTABRANCA);
			/* carregar listaNegra */
			listaNegra = carregarListas(PATH_LISTANEGRA);


			if (validaConexao(client,listaNegra,listaBranca,listaOnline)) {
				output.println("Conexão aceite!");
				output.println("entrou");
			} else {
				output.println("Conexão negada!");
				output.println("fechar");
			}


			// adiciona o novo cliente à lista de online
			if (!listaOnline.contains(clientIP)){
				listaOnline.add(clientIP);
			}

			Thread t = new Thread(new EchoClientThread(client));                             
			t.start();			
		}		
	}	

	public static class EchoClientThread implements Runnable{

		private Socket s;
		public EchoClientThread(Socket socket) {
			this.s = socket;
		}
		public void run() {

			String f = "";
			String threadName = Thread.currentThread().getName();
			String clientIP = s.getInetAddress().toString();
			//out("Nova conexão com [" + clientIP.toString().replace("/","")+"]");

			try {				
				BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
				PrintStream output = new PrintStream(s.getOutputStream(),true);
				String messageIn = null, messageOut = null;
				boolean sai = false;

				while ((messageIn = input.readLine()) !=null) {				
					out(clientIP.toString().replace("/","")+": "+threadName+": "+messageIn);


					switch(messageIn){
						case "99":
							output.println("comando valido");
							output.println("Vai ser desconectado!\nA sair...");
							output.println("fechar");
							out("O cliente "+ clientIP.toString().replace("/","")+" desconectou-se...");
							break;
						case "0":
							output.println("comando valido");
							out("Enviado menu para > "+ clientIP.toString().replace("/",""));
							break;
						case "1":
							output.println("comando valido");
							f = listaIPParaString(listaOnline,messageIn);
							output.print(f);
							output.println("over");
							out("Enviada a lista de online para > "+ clientIP.toString().replace("/",""));
							break;
						case "2":
							output.println("comando valido");
							sai = false;
							output.print("User:\n");
							String mensagem;
							String user;

							do {	/* ciclo ate o cliente enviar um user valido */
								user = input.readLine();
								boolean check = Integer.parseInt(user) >= listaOnline.size() || Integer.parseInt(user) < 0;
								if ( check ){
									output.println("Indique um User válido");
								} else {
									output.println("user valido");
									sai = true;
								}
							}while ( !sai );

							sai = false;
							do {	/* ciclo ate o cliente enviar um mensagem (só ENTER n dá) */
								output.print("Mensagem: \n");
								 mensagem = input.readLine();
								 boolean check = mensagem.length() > 0;
								 if ( check ){
								 	output.println("mensagem valida");
								 } else {
								 	sai = true;
								 }
							} while (sai);
							enviaMensagem(user, mensagem, datagramSocket, outPacket, listaOnline,false);

							output.println("Ok, mensagem enviada ao user "+user+".");
							output.println("over");
							out("O User > "+ clientIP.toString().replace("/","")+" enviou a mensagem \""+mensagem+"\" para o User ["
									+ listaOnline.get(Integer.parseInt(user)).toString().replace("/","")+"]");
							break;
						case "3":
							output.println("comando valido");
							sai = false;
							do {	/* ciclo ate o cliente enviar um mensagem (só ENTER n dá) */
								output.print("Mensagem: \n");
								mensagem = input.readLine();
								boolean check = mensagem.length() > 0;
								if ( check ){
									output.println("mensagem valida");
								} else {
									sai = true;
								}
							} while (sai);
							enviaMensagem("", mensagem, datagramSocket, outPacket, listaOnline,true);

							output.println("Ok, mensagem enviada a todos os users.");
							output.println("over");
							out("O User ["+ clientIP.toString().replace("/","")+"] enviou a mensagem \""+mensagem+"\" para todos os Users");
							break;
						case "4":
							listaBranca = carregarListas(PATH_LISTABRANCA);
							output.println("comando valido");
							f = listaParaString(listaBranca,messageIn);
							output.print(f);
							output.println("over");
							out("Enviada a lista branca para ["+ clientIP.toString().replace("/","")+"]");
							break;
						case "5":
							listaNegra = carregarListas(PATH_LISTANEGRA);
							output.println("comando valido");
							f = listaParaString(listaNegra,messageIn);
							output.print(f);
							output.println("over");
							out("Enviada a lista negra para ["+ clientIP.toString().replace("/","")+"]");
							break;
						default :
							output.println("Comando inválido.");
							out("O User ["+ clientIP.toString().replace("/","")+"] escolheu um comando inválido ("+messageIn+")");
							break;


					}

				} 
				input.close(); 
				output.close();
				s.close();							
			}
			catch (Exception ex){
				ex.printStackTrace();
			}					
		}  
	}

}


