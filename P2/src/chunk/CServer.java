package chunk;

import static util.FileOperation.writeToFile;
import static util.FileOperation.readFile;
import static util.FileOperation.deleteFile;
import static util.FileOperation.isFileExist;
import static util.StringChecking.getCommandFromInput;
import static util.StringChecking.getFilePathFromUsrAndFile;
import static util.StringChecking.getUsrAndFilePathFromInput;
import static util.StringChecking.isNullOrEmpty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import server.Disk;
import util.Constants;
import util.FileOperation;

public class CServer {
	private ServerSocket serverSocket;
	private int portNumber;
	private static final String PATH = "/tmp/";
	private static final int HASH_CHUNK_SIZE = 4;
	private static final int NUMBER_OF_REPLICA = 2;
	private static final int HEX_LENGTH = 8;
	private static final int BINARY_LENGTH = 32;
	private HashMap<String, Long> fileToChecksum = new HashMap<>();

	public ServerSocket getServerSocket() {
		return serverSocket;
	}

	public void setServerSocket(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
	}

	public static void main(String[] args) {
		// Server server = new Server(0, null);
		// server.hashValue("Listen to the music");
		CServer server = CServer.config();
		server.start();
	}

	public static CServer config() {
		CServer server = new CServer();
		try {
			server.setServerSocket(getAvailableServerSocket());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return server;
	}

	public static ServerSocket getAvailableServerSocket() throws Exception {
		for (int i = 1024; i <= 65535; i++) {
			try {
				return new ServerSocket(i);
			} catch (Exception e) {
				continue;
			}
		}
		throw new Exception("no available port number");
	}

	public void start() {
		portNumber = serverSocket.getLocalPort();
		System.out.println(
				"Cserver is started, portNumber is " + serverSocket.getLocalPort() + ", wait for client request.");
		try {
			Socket client = serverSocket.accept();
			PrintWriter out = new PrintWriter(client.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			while (true) {
				try {
					String commandInput = in.readLine();
					if(commandInput != null){
						System.out.println(commandInput);
						String command = getCommandFromInput(commandInput);
						String content = null;
						if (command.equals(Constants.UPLOAD)) {
	
							StringBuffer file = new StringBuffer();
							while (in.ready()) {
								String line = in.readLine();
								file.append(line);
							}
	
							String usrAndFile = getUsrAndFilePathFromInput(commandInput);
							String fileName = getFilePathFromUsrAndFile(usrAndFile);
							content = file.toString();
						}
						String res = executeCommand(commandInput, content);
						out.println(res);
						out.flush();
						if (commandInput.equals("bye")) {
							break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			client.close();
			// serverSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String executeCommand(String command, String content) {
		if (isNullOrEmpty(command)) {
			return "invalid command";

		}
		String[] args = command.trim().split(" ");
		if (args == null || args.length == 0) {
			return "invalid command";

		}
		String res = null;
		try {
			switch (args[0]) {
			case Constants.DOWNLOAD:
				res = downloadFile(args[1]);
				break;
			case Constants.UPLOAD:
				res = uploadFile(args[1], content);
				break;
			case Constants.DELETE:
				res = deleteFile(args[1]);
				break;
			default:
				res = "Unknown command, please try again";
			}
		} catch (Exception e) {
			e.printStackTrace();
			res = "Chunk Server Operation failure";
		}
		System.out.println(res);
		return res;
	}

	public String deleteFile(String fileName) {
		try {
			FileOperation.deleteFile(PATH+fileName);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return Constants.SUCCESS;
	}

	public String uploadFile(String fileName, String content) {
		try {
			long checkSumValue = checkSum(content);
			fileToChecksum.put(fileName, checkSumValue);
			writeToFile(PATH + fileName, content);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return Constants.SUCCESS;
	}

	public String downloadFile(String fileName) {
		try {
			if (!isFileExist(PATH+fileName)) {
				return null;
			}
			String res = readFile(PATH+fileName);
			long oldChecksum = fileToChecksum.get(fileName);
			long newChecksum = checkSum(res);
			if (newChecksum != oldChecksum) {
				return null;
			} else {
				return res;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	private Long checkSum(String input) {
		if (isNullOrEmpty(input)) {
			return null;
		}
		List<String> dividedNames = divideInputNameAccordingToChunkSize(input);
		List<Long> dividedNamesDec = new ArrayList<>();
		int[] index = { 0 };
		for (int i = 0; i < dividedNames.size(); i++) {
			String str = dividedNames.get(i);
			String hex = toHex(str);
			hex = addZero(hex, HEX_LENGTH);
			if (index[0] % 2 == 0) {
				long decimal = Long.valueOf(hex, 16);
				StringBuffer binary = new StringBuffer(Long.toBinaryString(decimal));
				String reversedBinary = binary.reverse().toString();
				reversedBinary = addZero(reversedBinary, BINARY_LENGTH);
				long reversedDecimal = Long.valueOf(reversedBinary, 2);
				// String reversedHex = Long.toHexString(reversedDecimal);
				dividedNamesDec.add(reversedDecimal);
			} else {
				long decimal = Long.valueOf(hex, 16);
				dividedNamesDec.add(decimal);
			}
			index[0]++;

		}
		long[] value = { 0 };
		for (int i = 0; i < dividedNames.size(); i++) {
			long v = dividedNamesDec.get(i);
			value[0] ^= v;
		}
		return value[0];
	}

	private List<String> divideInputNameAccordingToChunkSize(String inputName) {
		int beginIndex = 0;
		int endIndex = HASH_CHUNK_SIZE;
		List<String> dividedNames = new ArrayList<String>();
		while (beginIndex < inputName.length()) {
			if (endIndex < inputName.length())
				dividedNames.add(inputName.substring(beginIndex, endIndex));
			else {
				dividedNames.add(inputName.substring(beginIndex, inputName.length()));
			}
			beginIndex += HASH_CHUNK_SIZE;
			endIndex += HASH_CHUNK_SIZE;
		}
		return dividedNames;
	}

	private String toHex(String arg) {
		return String.format("%x", new BigInteger(1, arg.getBytes()));
	}

	private String addZero(String input, int length) {
		StringBuffer str = new StringBuffer(input);
		int size = input.length() % length;
		size = size == 0 ? 0 : length - size;

		for (int i = 0; i < size; i++) {
			str.append("0");
		}
		return str.toString();
	}
}