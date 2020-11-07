package bg.uni.sofia.rmi.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
	public static int SERVER_PORT = 4444;
	public static int MAX_USERS = 100;
	public static boolean QUIET = false;

	static Map<String, InetSocketAddress> users = new ConcurrentHashMap<>();

	/*
	 * args[0] = port args[1] = max users args[2] = quiet
	 */
	public static void main(String[] args) {
		if (args.length != 0) {
			SERVER_PORT = Integer.parseInt(args[0]);
			MAX_USERS = Integer.parseInt(args[1]);

			if (args[2].equals("-q")) {
				QUIET = true;
			}
		}

		try (ServerSocket ss = new ServerSocket(SERVER_PORT);) {

			while (true) {
				Socket s = ss.accept();
				new ServerThread(s).start();
			}

		} catch (IOException e) {
			System.err.println("A problem with the server socket occurred.");
			e.printStackTrace();
		}
	}
}
