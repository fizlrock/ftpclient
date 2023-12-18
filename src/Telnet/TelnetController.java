
package Telnet;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class TelnetController {
	private static String default_ip = "91.222.128.11";
	private static int default_port = 21;

	private InetAddress server_adress;
	private Socket server;
	private BufferedReader in;
	private PrintWriter out;
	private ControllerState controller_state;
	private ClientState client_state;

	public TelnetController() throws IOException {

		server_adress = InetAddress.getByName(default_ip);
		controller_state = ControllerState.Waiting;
		client_state = ClientState.Null;

	}

	public void connect() throws IOException {
		controller_state = ControllerState.Connecting;
		log("Запуск клиента. Сервер по умолчанию %s:%s", server_adress.getHostAddress(), default_port);
		server = new Socket(server_adress, default_port);

		if (server.isConnected())
			log("Успешно подключён к серверу");

		in = new BufferedReader(new InputStreamReader(server.getInputStream()));
		out = new PrintWriter(server.getOutputStream());

		controller_state = ControllerState.Connected;
		client_state = ClientState.ConnectionConfirmation;

		confimConnect();
	}

	private void confimConnect() throws IOException {

		Message m = getMessage();
		if (m.getCode() == 220) {
			log("Сервер подтвердил соединение: %s", m);
			client_state = ClientState.Authorization;
		} else {
			log("Сервер не подтвердил соединение: %s. Ошибка", m);
			server.close();
			controller_state = ControllerState.Failed;
		}
	}

	public void login() throws IOException {
		if (controller_state == ControllerState.Connected && client_state == ClientState.Authorization) {
			Message username_request, password_request;
			log("Авторизация...");
			sendMessage("USER testftp_guest");
			username_request = getMessage();

			if (username_request.getCode() != 331) {
				log("Ошибка ввода логина: %s", username_request);
				return;
			}

			sendMessage("PASS 12345");
			password_request = getMessage();

			if (password_request.getCode() != 230) {
				log("Ошибка ввода пароля: %s", password_request);
				return;
			}

			log("Авторизация успешно пройдена");
			client_state = ClientState.WaitingCommand;

		}
	}

	public String[] getFilesList(String path) throws IOException {
		String[] file_list = null;

		if (!(controller_state == ControllerState.Connected &&
				client_state == ClientState.WaitingCommand))
			return file_list;

		Message pasv_responce;
		sendMessage("PASV");
		pasv_responce = getMessage();

		if (pasv_responce.getCode() != 227) {
			log("Ошибка входа в пассивный режим, %s", pasv_responce);
			return file_list;
		}

		Callable<String[]> task = () -> {
			return readData(pasv_responce);
		};
		FutureTask<String[]> future = new FutureTask<String[]>(task);
		new Thread(future).start();

		sendMessage("LIST " + path);
		getMessage();
		getMessage();

		try {
			file_list = future.get();
		} catch (InterruptedException | ExecutionException e) {
			log("Ошибка чтений из потока данных");
			controller_state = ControllerState.Failed;
			e.printStackTrace();
		}

		return file_list;

	}

	private static String[] readData(Message pasv_responce) throws UnknownHostException {

		ArrayList<String> lines = new ArrayList<String>();
		int l, r;
		String text = pasv_responce.getText();
		l = text.lastIndexOf('(');
		r = text.lastIndexOf(')');
		String[] connection_string = text.substring(l + 1, r).split(",");

		var address = getAddress(connection_string);
		var port = getPort(connection_string);
		log("3");
		log("Чтение данных из потока под адресу %s:%s", address.getHostAddress(), port);
		try (Socket socket = new Socket(address, port)) {

			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String line;
			while ((line = in.readLine()) != null)
				lines.add(line);

		} catch (UnknownHostException e) {
			throw e;
		} catch (IOException e) {
			e.printStackTrace();
		}
		// log("Адрес для передачи данных %s", connection_string);

		return lines.toArray(new String[lines.size()]);

	}

	private static InetAddress getAddress(String[] connection_string) throws UnknownHostException {

		int[] ip = new int[4];
		for (int i = 0; i < 4; i++) {
			ip[i] = Integer.parseInt(connection_string[i]);
		}
		String address = String.format("%s.%s.%s.%s", ip[0], ip[1], ip[2], ip[3]);
		return InetAddress.getByName(address);
	}

	private static int getPort(String[] connection_string) {
		int a = Integer.parseInt(connection_string[4]);
		int b = Integer.parseInt(connection_string[5]);
		int port = a * 256 + b;
		return port;
	}

	public void disconnect() throws IOException {
		if (controller_state == ControllerState.Connected) {
			sendMessage("quit");
			getMessage();
			controller_state = ControllerState.Disconnected;
		}
	}

	private void sendMessage(String message) {
		out.println(message);
		out.flush();
	}

	private Message getMessage() throws IOException {
		String line;
		int c;
		String t;
		Message m;

		line = in.readLine();
		c = Integer.parseInt(line.substring(0, 3));
		t = line.substring(3).trim();
		m = new Message(c, t);
		log("Сообщение от сервера %s", m);
		return m;
	}

	private static void log(String message, Object... args) {
		System.out.printf(message + "\n", args);
	}

	enum ControllerState {
		Waiting, Connecting, Connected, Disconnected, Failed
	}

	enum ClientState {
		Null, ConnectionConfirmation, Authorization, WaitingCommand
	}

}
