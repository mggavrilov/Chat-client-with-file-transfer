package bg.uni.sofia.rmi.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.Scanner;

import bg.uni.sofia.rmi.commands.Commands;
import bg.uni.sofia.rmi.server.Server;

public class ClientMain {

	private static final int BYTES = 512;

	public static void register(Client client, String[] arguments) throws IOException {

		String username = arguments[1];

		if (client.getLocalUsername().equals("")) {
			// register user
			client.setLocalUsername(username);
		} else {
			if (!client.getLocalUsername().equals(username)) {
				System.err.println("Wrong username.");
				return;
			}
		}

		client.getOut().println(Commands.REGISTER.getCommand());

		client.getOut().println(username);

		client.getOut().println(client.getClientMiniServerThread().getPort());

		client.getOut().flush();

		String reply;

		while ((reply = client.getIn().readLine()) != null && reply.length() > 0) {
			System.out.println(reply);
		}
	}

	public static void sendMessage(boolean sendAll, Client client, String[] arguments) throws IOException {
		if (client.getLocalUsername().equals("")) {
			System.err.println("Not registered! Please register first with \"user <username>\"");
			return;
		}

		String message;
		String username;

		if (sendAll) {
			username = null;
			message = arguments[1];
		} else {
			username = arguments[1];
			message = arguments[2];
		}

		if (sendAll) {
			client.getOut().println(Commands.SENDALL.getCommand());
			client.getOut().println(message);
		} else {
			client.getOut().println(Commands.SEND.getCommand());
			client.getOut().println(username);
			client.getOut().println(message);
		}

		client.getOut().flush();

		String reply;

		while ((reply = client.getIn().readLine()) != null && reply.length() > 0) {
			System.out.println(reply);
		}
	}

	public static void sendFile(Client client, String[] arguments) throws IOException {
		if (client.getLocalUsername().equals("")) {
			System.err.println("Not registered! Please register first with \"user <username>\"");
			return;
		}

		String username = arguments[1];
		String filename = arguments[2];

		File filepath = new File(filename);

		if (filepath.exists() && !filepath.isDirectory()) {
			try (FileInputStream fin = new FileInputStream(filepath.getAbsolutePath())) {
				long fileSize = filepath.length();
				int blockSize = (int) Math.ceil((double) fileSize / BYTES);

				client.getOut().println(Commands.SENDFILE.getCommand());
				client.getOut().println(username);
				client.getOut().println(filepath.getName());
				client.getOut().println(blockSize);

				int counter = BYTES;
				byte[] bytes = new byte[BYTES];

				while ((counter = fin.read(bytes, 0, counter)) > 0) {
					if (counter < BYTES) {
						byte[] actualBytes = new byte[counter];

						for (int i = 0; i < counter; i++) {
							actualBytes[i] = bytes[i];
						}

						bytes = actualBytes;
					}

					String payload = Base64.getEncoder().encodeToString(bytes);

					client.getOut().println(payload);
				}

				client.getOut().println();
				client.getOut().flush();

				System.out.println(client.getIn().readLine());
			}
		} else {
			System.err.println("File not found!");
		}
	}

	public static void listUsers(Client client) throws IOException {
		if (client.getLocalUsername().equals("")) {
			System.err.println("Not registered! Please register first with \"user <username>\"");
			return;
		}

		client.getOut().println(Commands.FETCH_USERS.getCommand());
		client.getOut().flush();

		String reply = client.getIn().readLine();
		System.out.println(reply);
	}

	public static void quit(Client client) throws IOException {
		System.out.println("Shutting down client.");
		client.getOut().println(Commands.QUIT.getCommand());
		client.getOut().flush();
		client.getSocket().close();
	}

	public static void main(String[] args) {
		Client client = new Client();

		try (Socket s = new Socket("localhost", Server.SERVER_PORT);
				PrintWriter out = new PrintWriter(s.getOutputStream());
				BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
				Scanner sc = new Scanner(System.in);) {

			client.setSocket(s);
			client.setOut(out);
			client.setIn(in);

			// client.setFetchUsersThread(new FetchUsersThread(s));
			client.setClientMiniServerThread(new ClientMiniServerThread());

			// client.getFetchUsersThread().start();
			client.getClientMiniServerThread().start();

			while (true) {
				System.out.print("Command: ");

				String line;

				try {
					line = sc.nextLine();
				} catch (NoSuchElementException e) {
					// Ctrl-Z EOF
					line = "quit";
				}

				// https://stackoverflow.com/a/18893443
				// split arguments on space but don't split arguments enclosed in inverted
				// commas "".
				String[] arguments = line.split(" (?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

				// remove "" from the arguments which were enclosed in them
				for (int i = 0; i < arguments.length; i++) {
					arguments[i] = arguments[i].replace("\"", "");
				}

				String cmd = arguments[0];

				if (cmd.equals("send_to")) {

					sendMessage(false, client, arguments);

				} else if (cmd.equals("send_all")) {

					sendMessage(true, client, arguments);

				} else if (cmd.equals("send_file_to")) {

					sendFile(client, arguments);

				} else if (cmd.equals("user")) {

					register(client, arguments);

				} else if (cmd.equals("list")) {

					listUsers(client);

				} else if (cmd.equals("bye")) {

					quit(client);
					break;

				} else {
					System.err.println(
							"Invalid command. Available commands: user, send_to, send_all, list, send_file_to and bye.");
				}
			}

		} catch (IOException e) {
			System.err.println("An error occured with the client.");
			e.printStackTrace();
		}

		try {
			client.getClientMiniServerThread().getSocket().close();
		} catch (IOException e) {
			System.err.println("Error stopping the client mini server thread.");
			e.printStackTrace();
		}

	}
}
