package tp1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Scanner;

public class FTPServer {
	private static final HashMap<String, String> userCredentials = new HashMap<>();
	static {
		// creation de deux utilisateurs
		userCredentials.put("user1", "pass1");
		userCredentials.put("user2", "pass2");

	}

	public static void main(String[] args) {
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(2020);
			System.out.println("FTP Server is running on port 2020");
			// chaque connexion ftp une nouvelle sokets est cree
			while (true) {
				Socket clientSocket = serverSocket.accept();
				handleClient(clientSocket);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void handleClient(Socket clientSocket) throws IOException {
		try (InputStream inputStream = clientSocket.getInputStream();
				OutputStream outputStream = clientSocket.getOutputStream();
				Scanner scanner = new Scanner(inputStream)) {

			String response = "220 Service ready for new user.\r\n";
			outputStream.write(response.getBytes());

			// String currentDirectory = "C:\\Users\\badr-\\Downloads\\Car";
			String currentDirectory = "C:/Users/badr-/Downloads/Car/tp1";

			while (true) {
				String command = scanner.next().toUpperCase();

				switch (command) {
					case "USER":
						handleUserCommand(scanner, outputStream);
						break;
					case "PASS":
						handlePassCommand(scanner, outputStream);
						break;
					case "DIR":
						handleDirCommand(currentDirectory, outputStream);
						break;
					case "LS":
						handleDirCommand(currentDirectory, outputStream);
						break;
					case "CD":
						currentDirectory = handleCdCommand(scanner, currentDirectory, outputStream);
						break;
					case "GET":
						String fileName = scanner.next();
						handleGetCommand(fileName, currentDirectory, outputStream);
						break;
					case "QUIT":
						handleQuitCommand(outputStream, clientSocket);
						return;
					default:
						sendResponse(outputStream, "500 Syntax error, command not recognized.\r\n");
				}
			}
		}
	}

	private static void handleUserCommand(Scanner scanner, OutputStream outputStream) throws IOException {
		String username = scanner.next();
		if (testConnectionUserName(username)) {
			sendResponse(outputStream, "331 User " + username + " OK. Password required.\r\n");
		} else {
			sendResponse(outputStream, "userName incorrect.\r\n");
		}

	}

	private static void handlePassCommand(Scanner scanner, OutputStream outputStream) throws IOException {
		String password = scanner.next();

		if (testConnectionUserPass(password)) {
			sendResponse(outputStream, "230 User logged in.\r\n");
		} else {
			sendResponse(outputStream, "530 Login incorrect.\r\n");
		}
	}

	private static void handleQuitCommand(OutputStream outputStream, Socket clientSocket) throws IOException {
		try {
			sendResponse(outputStream, "221 Service closing control connection.\r\n");
		} finally {
			clientSocket.close();
		}
	}

	private static void handleGetCommand(String fileName, String currentDirectory, OutputStream outputStream)
			throws IOException {
		Path filePath = Paths.get(currentDirectory, fileName);

		if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
			byte[] fileBytes = Files.readAllBytes(filePath);
			sendResponse(outputStream, "150 Opening data connection for " + fileName + "\r\n");
			outputStream.write(fileBytes);
			sendResponse(outputStream, "226 Transfer complete.\r\n");
		} else {
			sendResponse(outputStream, "550 File not found or not a regular file.\r\n");
		}
	}

	private static void handleDirCommand(String currentDirectory, OutputStream outputStream) throws IOException {
		Path directoryPath = Paths.get(currentDirectory);

		if (Files.isDirectory(directoryPath)) {
			Files.list(directoryPath).forEach(path -> {
				try {
					sendResponse(outputStream, path.getFileName() + "\r\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			sendResponse(outputStream, "226 Directory listing completed.\r\n");
		} else {
			sendResponse(outputStream,
					"550 Requested action not taken. Directory not found or is not a directory.\r\n");
			System.out.println(directoryPath + "path" + "" + Files.isDirectory(directoryPath));
		}
	}

	private static String handleCdCommand(Scanner scanner, String currentDirectory, OutputStream outputStream)
			throws IOException {
		String newDirectory = scanner.nextLine().trim(); // Read the entire line for the directory name
		Path newDirectoryPath = Paths.get(currentDirectory, newDirectory);

		if (Files.isDirectory(newDirectoryPath)) {
			sendResponse(outputStream, "250 Requested file action okay, completed.\r\n");
			return newDirectoryPath.toString();
		} else {
			sendResponse(outputStream,
					"550 Requested action not taken. Directory not found or is not a directory.\r\n");
			return currentDirectory;
		}
	}

	private static boolean testConnectionUserName(String username) {
		return userCredentials.containsKey(username);
	}

	private static boolean testConnectionUserPass(String password) {
		return userCredentials.containsValue(password);
	}

	// envoyer des message lors de la connexion ftp
	private static void sendResponse(OutputStream outputStream, String response) throws IOException {
		outputStream.write(response.getBytes());
	}
}