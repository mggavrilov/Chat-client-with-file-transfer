package bg.uni.sofia.rmi.client;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class ClientMiniServerThread extends Thread {

	private int port;
	private ServerSocket socket;

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public ServerSocket getSocket() {
		return socket;
	}

	public void setSocket(ServerSocket socket) {
		this.socket = socket;
	}

	@Override
	public void run() {
		try (ServerSocket ss = new ServerSocket(0)) {

			this.socket = ss;

			this.port = ss.getLocalPort();

			while (true) {
				Socket s = ss.accept();
				new ReceiverThread(s).start();
			}

		} catch (SocketException e) {
			System.out.println("Shutting down client mini server thread.");
		} catch (IOException e) {
			System.err.println("A problem with the client mini server socket occurred.");
			e.printStackTrace();
		}
	}
}
