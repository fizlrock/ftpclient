
package test_ui_project.Models.Telnet;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class FTPController {
	private static String default_ip = "91.222.128.11";
	private static int default_port = 21;

	private String username;
	private String password;

	private StringJoiner logs;

	private InetAddress server_adress;
	private Socket server;
	private BufferedReader in;
	private PrintWriter out;
	private ControllerState controllerState;

	public ControllerState getControllerState() {
		return controllerState;
	}

	public void setControllerState(ControllerState controller_state) {
		this.controllerState = controller_state;
	}

	private ClientState client_state;

	public FTPController(String ipAdress, String username, String password) {
		logs = new StringJoiner("\n");
		this.username = username;
		this.password = password;
		controllerState = ControllerState.Waiting;
		client_state = ClientState.Null;

		try {
			server_adress = InetAddress.getByName(ipAdress);
		} catch (UnknownHostException e) {
			controllerState = ControllerState.Failed;
			log(Arrays.toString(e.getStackTrace())); // Это не очень хорошо
		}

	}

	public void connect() {
		controllerState = ControllerState.Connecting;
		log("Запуск клиента. Сервер по умолчанию %s:%s", server_adress.getHostAddress(), default_port);
		try {
			server = new Socket(server_adress, default_port);
			if (server.isConnected())
				log("Успешно подключён к серверу");

			in = new BufferedReader(new InputStreamReader(server.getInputStream()));
			out = new PrintWriter(server.getOutputStream());

			controllerState = ControllerState.Connected;
			client_state = ClientState.ConnectionConfirmation;

			confimConnect();

		} catch (IOException e) {
			controllerFailed(Arrays.toString(e.getStackTrace())); // Это не очень хорошо
		}

	}

	private void confimConnect() {

		Message m;
		try {
			m = getMessage();
			if (m.getCode() == 220) {
				log("Сервер подтвердил соединение: %s", m);
				client_state = ClientState.Authorization;
			} else {
				controllerFailed("Сервер не подтвердил соединение: %s. Ошибка", m);
				server.close();
			}
		} catch (IOException e) {
			controllerFailed("Ошибка подключения к серверу");
		}
	}

	public void login() {
		try {
			login_nosafe();
		} catch (IOException e) {
			log(Arrays.toString(e.getStackTrace())); // Это не очень хорошо
		}
	}

	private void login_nosafe() throws IOException {
		if (controllerState == ControllerState.Connected && client_state == ClientState.Authorization) {
			Message username_request, password_request;
			log("Авторизация...");
			sendMessage("USER " + username);
			username_request = getMessage();

			if (username_request.getCode() != 331) {
				controllerFailed("Ошибка ввода логина: %s", username_request);
				return;
			}

			sendMessage("PASS " + password);
			password_request = getMessage();

			if (password_request.getCode() != 230) {
				controllerFailed("Ошибка ввода пароля: %s", password_request);
				return;
			}

			log("Авторизация успешно пройдена");
			client_state = ClientState.WaitingCommand;

		}
	}

	public String[] getFilesList(String path) throws IOException {
		String[] file_list = null;

		if (!(controllerState == ControllerState.Connected &&
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
			controllerState = ControllerState.Failed;
			e.printStackTrace();
		}

		return file_list;

	}

	private String[] readData(Message pasv_responce) throws UnknownHostException {

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

	public void disconnect() {
		if (controllerState == ControllerState.Connected) {

			try {
				sendMessage("quit");
				getMessage();

				controllerState = ControllerState.Disconnected;

			} catch (IOException e) {
				log(Arrays.toString(e.getStackTrace())); // Это не очень хорошо
			}
		}

	}

	private void sendMessage(String message) {
		log("Отправка сообщения: %s", message);
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

	private void log(String message, Object... args) {
		System.out.printf(message + "\n", args);
		logs.add(String.format(message, args));
	}

	private void controllerFailed(String message, Object... args) {
		log(message, args);
		controllerState = ControllerState.Failed;
	}

	public String getLogs() {
		return logs.toString();
	}

	public enum ControllerState {
		Waiting, Connecting, Connected, Disconnected, Failed
	}

	enum ClientState {
		Null, ConnectionConfirmation, Authorization, WaitingCommand
	}

}
