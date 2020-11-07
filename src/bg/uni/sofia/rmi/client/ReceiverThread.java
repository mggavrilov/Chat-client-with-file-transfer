package bg.uni.sofia.rmi.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Base64;

public class ReceiverThread extends Thread {

	private Socket socket;

	public ReceiverThread(Socket socket) {
		this.socket = socket;
	}

	@Override
	public void run() {

		try (PrintWriter out = new PrintWriter(socket.getOutputStream());
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));) {

			String line = in.readLine();

			System.out.println();
			System.out.println(line);

			if (line.startsWith("500")) {
				// file transfer

				String[] parts = line.split(" ");
				String filename = parts[3];

				String reply;

				try (FileOutputStream fileWriter = new FileOutputStream(filename + "_received")) {
					while ((reply = in.readLine()) != null && reply.length() > 0) {
						byte[] outputArr = Base64.getDecoder().decode(reply);

						fileWriter.write(outputArr);
					}
				}

				File receivedFile = new File(filename + "_received");
				if (receivedFile.exists()) {
					out.println("200 file accepted sucessfully");
				} else {
					out.println("100 client transfer error");
				}

				out.flush();
			}

			System.out.print("Command: ");

		} catch (IOException e) {
			System.err.println("A problem with the downloader thread occured.");
			e.printStackTrace();
		}
	}

}
