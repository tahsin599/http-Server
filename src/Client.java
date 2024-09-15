import java.io.*;
import java.net.Socket;
import java.util.*;

public class Client {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 6789;
    private static String ROOT_DIR = System.getProperty("user.dir");

    public static void main(String[] args) {
        Scanner sc=new Scanner(System.in);
        String filename=sc.nextLine();
        File file = new File(ROOT_DIR,filename);
        System.out.println(file.getAbsolutePath());
        System.out.println(file.length());
        ;
        // Specify the file to upload
        System.out.println("C:\\Users\\Tahsin Kabir\\Desktop\\demonfg\\demo2\\HTTPSERVER\\");

        if (!file.exists()) {
            System.err.println("File does not exist!");
            try (FileWriter writer = new FileWriter("log.txt", true)) {
                writer.write("Request:Upload "+file.getName());
                writer.write('\n');
                writer.write("Response:File does not exist");
                writer.write('\n');
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        if(!isTextFile(file) && !isImageFile(file)){
            System.err.println("not supported type");
            try (FileWriter writer = new FileWriter("log.txt", true)) {
                writer.write("Request:Upload "+file.getName());
                writer.write('\n');
                writer.write("Response:Not supported type");
                writer.write('\n');
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedOutputStream dataOut = new BufferedOutputStream(socket.getOutputStream())) {

            out.println("UPLOAD " + file.getName());
            byte[] fileData = readFileData(file);
            dataOut.write(fileData);
            dataOut.flush();

            String response = in.readLine();
            System.out.println("Server response: " + response);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] readFileData(File file) throws IOException {
        FileInputStream fileIn = new FileInputStream(file);
        byte[] fileData = new byte[(int) file.length()];
        fileIn.read(fileData);
        fileIn.close();
        return fileData;
    }
    private static boolean isImageFile(File file) {
        String[] imageExtensions = {"jpg", "jpeg", "png", "gif", "bmp", "tiff"};
        String fileName = file.getName().toLowerCase();

        for (String extension : imageExtensions) {
            if (fileName.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private static  boolean isTextFile(File file) {
        String[] textExtensions = {"txt"};
        String fileName = file.getName().toLowerCase();

        for (String extension : textExtensions) {

            if (fileName.endsWith(extension)) {

                return true;
            }
        }
        return false;
    }
}
