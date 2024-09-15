import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.Date;
import java.util.Objects;

public class WebServer {
    private static final int PORT = 6789;  // Replace with your specific port
    // Root directory for server

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println(clientSocket.getPort());
                new Thread(new ClientHandlers(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

     
}
class ClientHandlers implements Runnable {
    private final Socket clientSocket;
    private String ROOT_DIR = System.getProperty("user.dir");
    private int CHUNK_SIZE=4096;

    public ClientHandlers(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedOutputStream dataOut = new BufferedOutputStream(clientSocket.getOutputStream())) {

            String input = in.readLine();
            System.out.println(input);
            if (input == null) {
                return;
            }

            String[] request = input.split(" ");

            for(int i=0;i< request.length;i++){
                System.out.println(request[i]);
            }
            String method = request[0];
            String path = request[1];
            //System.out.println("Request: " + method + " " + path);
            try (FileWriter writer = new FileWriter("log.txt", true)) {
                writer.write(String.valueOf("Request: "+method + " "+path));
                writer.write('\n');
            } catch (IOException e) {
                e.printStackTrace();
            }


            if (method.equals("GET")) {
                handleGetRequest(path, out, dataOut);
            } else if (method.equals("UPLOAD")) {
                handleUploadRequest(in, dataOut, path);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleGetRequest(String path, PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        File file = new File(ROOT_DIR, path);
        System.out.println(file.getAbsolutePath());
        //ROOT_DIR= file.getParent();
        System.out.println(file.getParentFile());
        if (!file.exists()) {
            send404(out);
            return;
        }

        if (file.isDirectory()) {
            listDirectory(file, out,path);
        } else {
            //sendFile(file, out, dataOut);
            String[] imageExtensions = {"jpg", "jpeg", "png", "gif", "bmp", "tiff" ,"txt"};
            String fileName = file.getName().toLowerCase();

            for (String extension : imageExtensions) {
                if (fileName.endsWith(extension)) {
                    sendFile(file,out,dataOut);
                    return;
                }
            }
            serveFileForDownload(out,dataOut,file);
        }
    }

    private void handleUploadRequest(BufferedReader in, BufferedOutputStream dataOut, String path) throws IOException {
        ROOT_DIR=ROOT_DIR+"\\Upload";
        File file = new File(ROOT_DIR, path);
        System.out.println(file);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            char[] buffer = new char[81920];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(new String(buffer, 0, bytesRead).getBytes());
            }
            fos.flush();
            String response = "HTTP/1.1 200 OK\r\n";
            dataOut.write(response.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            send500(dataOut);
        }
        try (FileWriter writer = new FileWriter("log.txt", true)) {
            writer.write("Response:File Succcessfully Uploaded");
            writer.write('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendFile(File file, PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        int fileLength = (int) file.length();
        String content = Files.probeContentType(file.toPath());
        byte[] fileData = readFileData(file, fileLength);

        out.println("HTTP/1.1 200 OK");
        out.println("Server: Java HTTP Server");
        out.println("Date: " + new Date());
        out.println("Content-type: " + content);
        out.println("Content-length: " + fileLength);

        out.println();
        out.flush();
        try (FileWriter writer = new FileWriter("log.txt", true)) {
            writer.write(String.valueOf("Response: Requested  content shown of "+file.getName()));
            writer.write('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }

        dataOut.write(fileData, 0, fileLength);
        dataOut.flush();
    }

    private byte[] readFileData(File file, int fileLength) throws IOException {
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];
        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        } finally {
            if (fileIn != null) fileIn.close();
        }
        return fileData;
    }

    private void listDirectory(File dir, PrintWriter out,String path) {
        out.println("HTTP/1.1 200 OK");
        out.println("Content-type: text/html");
        out.println();
        out.println("<html><body>");
        out.println("<h1>Directory listing for " + path + "</h1>");
        out.println("<ul>");
        try (FileWriter writer = new FileWriter("log.txt", true)) {
            writer.write(String.valueOf("Response: Directory Listing For "+path));
            writer.write('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
        path=path.replace("/","\\");
        System.out.println(path);


        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                out.println("<li><b><i><a href="+path+"\\"+ file.getName() + "/>" + file.getName() + "/</a></i></b></li>");




            } else {
                out.println("<li><a href="+path+"\\" + file.getName() + "/>" + file.getName() + "</a></li>");
                System.out.println(file.getName());
            }
        }

        out.println("</ul>");
        out.println("</body></html>");
        out.flush();
    }

    private void serveFileForDownload(PrintWriter out, BufferedOutputStream dataOut, File file) throws IOException {
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: application/octet-stream");  // Default content type for downloading files
        out.println("Content-Disposition: attachment; filename=\"" + file.getName() + "\"");
        out.println("Content-Length: " + file.length());
        out.println();
        out.flush();  // Send the headers

        // Send the file in chunks
        try (FileInputStream fileIn = new FileInputStream(file)) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                dataOut.write(buffer, 0, bytesRead);
                dataOut.flush();  // Ensure the chunk is sent immediately
            }
        }
        try (FileWriter writer = new FileWriter("log.txt", true)) {
            writer.write("Response: ");
            writer.write("File Downloaded");
            writer.write('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void send404(PrintWriter out) {
        out.println("HTTP/1.0 404 Not Found");
        out.println("Content-type: text/html");
        out.println();
        out.println("<html><body><h1>404 Not Found</h1></body></html>");
        try (FileWriter writer = new FileWriter("log.txt", true)) {
            writer.write("Response: ");
            writer.write("404 not found");
            writer.write('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
        out.flush();
    }

    private void send500(BufferedOutputStream dataOut) throws IOException {
        String response = "HTTP/1.0 500 Internal Server Error\r\n";
        dataOut.write(response.getBytes());
        try (FileWriter writer = new FileWriter("log.txt", true)) {
            writer.write(response);
            writer.write('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
        dataOut.flush();
    }
}

