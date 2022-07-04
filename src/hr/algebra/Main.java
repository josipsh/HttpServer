package hr.algebra;

import com.google.gson.Gson;
import hr.algebra.sharedModel.UserDto;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final String USER_FILE_PATH = "D:\\Documents\\DurakCardGame\\Users\\User.ser";
    private static List<UserDto> users = new ArrayList<>();

    public static void main(String[] args) {
        try {
            loadUsers();
            acceptRequests();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void acceptRequests() {
        try (ServerSocket serverSocket = new ServerSocket(1020)) {
            System.err.println("Server listening at port 1020");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.err.println("Client connected from port " + clientSocket.getPort());

                new Thread(() -> processRequest(clientSocket)).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processRequest(Socket clientSocket) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String line;
            String username = "";
            while ((line = br.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("GET")) {
                    username = line.substring(line.indexOf("/") + 1, line.lastIndexOf(" "));
                }
                System.out.println(line);
            }

            sendResponse(clientSocket, username);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendResponse(Socket clientSocket, String username) throws IOException {
        UserDto user = findUser(username);
        if (user == null) {
            flushText(clientSocket);
        } else {
            flushJson(clientSocket, user);
            //flushObject(clientSocket, user);
        }
    }

    private static void flushText(Socket clientSocket) {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {
            String body = "Not found";

            bw.write("HTTP/1.1 200 OK\n");
            bw.write("Date: " + ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME) + "\n");
            bw.write("Server: MiliczasServant/2.2.14 (Win64)\n");
            bw.write(" Last-Modified: " + ZonedDateTime.now().minusDays(2).format(DateTimeFormatter.RFC_1123_DATE_TIME) + "\n");
            // very important
            bw.write("Content-Length: " + body.length() + "\n");
            bw.write("Content-Type: text/html; charset=utf-8\n");
            bw.write("Connection: Closed\n");
            // very important
            bw.write("\n");

            bw.write(body);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void flushObject(Socket clientSocket, UserDto user) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream())) {
            oos.writeObject(user);
        }
    }

    private static void flushJson(Socket clientSocket, UserDto user) {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {
            Gson g = new Gson();

            String body = g.toJson(user);;

            bw.write("HTTP/1.1 200 OK\n");
            bw.write("Date: " + ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME) + "\n");
            bw.write("Server: MiliczasServant/2.2.14 (Win64)\n");
            bw.write(" Last-Modified: " + ZonedDateTime.now().minusDays(2).format(DateTimeFormatter.RFC_1123_DATE_TIME) + "\n");
            // very important
            bw.write("Content-Length: " + body.length() + "\n");
            bw.write("Content-Type: application/json; charset=utf-8\n");
            bw.write("Connection: Closed\n");
            // very important
            bw.write("\n");

            bw.write(body);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static UserDto findUser(String username) {
        for (UserDto user : users) {
            if (user.getUserName().equals(username)) {
                return user;
            }
        }
        return null;
    }

    private static void loadUsers() throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(Paths.get(USER_FILE_PATH)))) {
            users = (List<UserDto>) ois.readObject();
        }
    }
}
