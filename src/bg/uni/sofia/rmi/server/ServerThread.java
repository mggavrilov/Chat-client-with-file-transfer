package bg.uni.sofia.rmi.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;

import bg.uni.sofia.rmi.server.Server;
import bg.uni.sofia.rmi.commands.Commands;

public class ServerThread extends Thread {

	private Socket socket;
	private String localUsername;

	public Socket getSocket() {
		return socket;
	}

	public String getLocalUsername() {
		return localUsername;
	}

	public ServerThread(Socket socket) {
		this.socket = socket;
		localUsername = "";
	}

	public void register(BufferedReader in, PrintWriter out) throws IOException {
		String username = in.readLine();

		int port = Integer.parseInt(in.readLine());

		if (Server.users.size() == Server.MAX_USERS) {
			out.println("100 err max users reached!");
			out.flush();
			return;
		}

		if (Server.users.putIfAbsent(username, new InetSocketAddress(socket.getInetAddress(), port)) != null) {
			// Race condition, another thread already registered this username; return error
			out.println("100 err " + username + " already taken!");

			if (!Server.QUIET) {
				System.out.println("100 err " + username + " already taken!");
			}

		} else {
			this.localUsername = username;
			out.println("200 ok " + username + " successfully registered");

			if (!Server.QUIET) {
				System.out.println("200 ok " + username + " successfully registered");
			}
		}

		out.println();
		out.flush();
	}

	public void sendMessage(boolean sendAll, BufferedReader in, PrintWriter out) throws IOException {
		String username = null;
		String message = null;

		if (sendAll) {
			message = in.readLine();
		} else {
			username = in.readLine();
			message = in.readLine();
		}

		if (username != null) {
			if (!Server.users.containsKey(username)) {
				out.println("100 err " + username + " does not exist!");

				if (!Server.QUIET) {
					System.out.println("100 err " + username + " does not exist!");
				}
			} else {
				InetSocketAddress targetUser = Server.users.get(username);

				try (Socket s = new Socket(targetUser.getAddress(), targetUser.getPort())) {
					PrintWriter targetOut = new PrintWriter(s.getOutputStream());

					targetOut.println("300 msg_from " + this.localUsername + " " + message);
					targetOut.flush();
				} catch (IOException e) {
					out.println("100 err server error!");
				}

				out.println("200 ok message to " + username + " sent successfully.");

				if (!Server.QUIET) {
					System.out.println("200 ok message to " + username + " sent successfully.");
				}
			}
		} else {
			// sendAll
			if (Server.users.size() <= 1) {
				// there's no one to send the message to other that the user themselves
				out.println("100 err server error!");
			} else {
				for (Map.Entry<String, InetSocketAddress> entry : Server.users.entrySet()) {
					if (!entry.getKey().equals(this.localUsername)) {
						InetSocketAddress targetUser = entry.getValue();

						try (Socket s = new Socket(targetUser.getAddress(), targetUser.getPort())) {
							PrintWriter targetOut = new PrintWriter(s.getOutputStream());

							targetOut.println("300 msg_from " + this.localUsername + " " + message);
							targetOut.flush();
						} catch (IOException e) {
							out.println("100 err server error!");
						}

						out.println("200 ok message to " + entry.getKey() + " sent successfully.");

						if (!Server.QUIET) {
							System.out.println("200 ok message to " + entry.getKey() + " sent successfully.");
						}
					}
				}
			}
		}

		out.println();
		out.flush();
	}

	public void fetchUsers(PrintWriter out) {
		if (Server.users.size() <= 1) {
			out.println("100 err server error!");
		} else {
			String returnString = "200 ok ";

			for (Map.Entry<String, InetSocketAddress> entry : Server.users.entrySet()) {
				if (this.localUsername != entry.getKey()) {
					returnString += (entry.getKey() + " ");
				}
			}

			out.println(returnString);
		}

		out.flush();
	}

	public void sendFile(BufferedReader in, PrintWriter out) throws IOException {
		String username = in.readLine();
		String filename = in.readLine();
		int blockSize = Integer.parseInt(in.readLine());

		if (!Server.users.containsKey(username)) {
			out.println("100 server transfer error");

			if (!Server.QUIET) {
				System.out.println("100 server transfer error");
			}

		} else {
			InetSocketAddress targetUser = Server.users.get(username);

			try (Socket s = new Socket(targetUser.getAddress(), targetUser.getPort())) {
				PrintWriter targetOut = new PrintWriter(s.getOutputStream());

				targetOut.println("500 file_from " + this.localUsername + " " + filename + " " + blockSize);
				targetOut.flush();

				String payload;

				while ((payload = in.readLine()) != null && payload.length() > 0) {
					targetOut.println(payload);
				}

				targetOut.println();
				targetOut.flush();

				BufferedReader targetIn = new BufferedReader(new InputStreamReader(s.getInputStream()));
				System.out.println(targetIn.readLine());

				out.println("200 file transferred successfully");
			} catch (IOException e) {
				out.println("100 client transfer error");
			}
		}

		out.flush();
	}

	public void quit() throws IOException {
		if (!this.localUsername.equals("")) {
			System.out.println("Closing connection with client " + this.localUsername);

			Server.users.remove(this.localUsername);

			for (Map.Entry<String, InetSocketAddress> entry : Server.users.entrySet()) {
				InetSocketAddress targetUser = entry.getValue();

				try (Socket s = new Socket(targetUser.getAddress(), targetUser.getPort())) {
					PrintWriter targetOut = new PrintWriter(s.getOutputStream());

					targetOut.println("400 user gone " + this.localUsername);
					targetOut.flush();
				} catch (IOException e) {
					System.out.println("100 err server error while closing connection with client!");
				}

				System.out.println("Connection with " + this.localUsername + " successfully closed.");
			}
		}

		this.socket.close();
	}

	@Override
	public void run() {
		try (PrintWriter out = new PrintWriter(socket.getOutputStream());
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));) {

			while (!socket.isClosed()) {
				int command = Integer.parseInt(in.readLine());

				if (command == Commands.QUIT.getCommand()) {

					quit();

				} else if (command == Commands.FETCH_USERS.getCommand()) {

					fetchUsers(out);

				} else if (command == Commands.REGISTER.getCommand()) {

					register(in, out);

				} else if (command == Commands.SEND.getCommand()) {

					sendMessage(false, in, out);

				} else if (command == Commands.SENDALL.getCommand()) {

					sendMessage(true, in, out);

				} else if (command == Commands.SENDFILE.getCommand()) {

					sendFile(in, out);

				} else {
					System.err.println("Unrecognized command sent to server.");
				}
			}

		} catch (SocketException e) {
			try {
				quit();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (IOException e) {
			System.err.println("A problem with the server socket thread occured.");
			e.printStackTrace();
		}
	}
}
