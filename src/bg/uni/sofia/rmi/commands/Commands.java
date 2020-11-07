package bg.uni.sofia.rmi.commands;

public enum Commands {
	QUIT(-1), FETCH_USERS(0), REGISTER(1), SEND(2), SENDALL(3), SENDFILE(4);

	private final int command;

	private Commands(int command) {
		this.command = command;
	}

	public int getCommand() {
		return command;
	}
}
