import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import java.net.Socket;

public class Client {

    private static Socket sock;
    private static Scanner scan;
    private static DataInputStream in;
    private static DataOutputStream out;
    private static boolean connected = false;

    public static void main(String[] args) throws Exception {

    
        /* *************************************** */
        // Before all : we demand the addresses of the server and the user's name/password
        /* *************************************** */


        scan = new Scanner(System.in);
        System.out.print("\n");

        // Asking for ip adress
        System.out.print("Enter the server address : ");
        String addrServ = scan.next();

        while(!ipValidator(addrServ)) {

            // Asking for ip adress again after a wrong one was passed
            System.out.print("Enter the server address (a valid one please :): ");
            addrServ = scan.next();
        }

        // Asking for port
        System.out.print("Enter the port number (make sure it is an INTEGER value): ");
        int port = scan.nextInt();

        while (5000 > port || 5050 < port) {

            // Asking for port again after a wrong number was passed
            System.out.print("Give a valid port number (5000 -> 5050) : ");
            port = scan.nextInt();
        }

        // Creating the socket and preparing to receive any incoming messages from the server
        sock = new Socket(addrServ, port);
        in = new DataInputStream(sock.getInputStream());

        String answer = in.readUTF();
        System.out.println("\nServer says : \n" + answer + "\n");

        System.out.print("Enter your name here : ");
        String loginName = scan.next();

        System.out.print("Enter your password here : ");
        String loginpassword = scan.next();
        
        String login = loginName + " " + loginpassword;

        // Sending our credentials to the server, then reading its answer
        out = new DataOutputStream(sock.getOutputStream());
        out.writeUTF(login);
        in = new DataInputStream(sock.getInputStream());
        answer = in.readUTF();

        if(answer.equals("Wrong password !")) { 
            
            System.out.println("Wrong password, restart client.\n");
            sock.close();
            in.close();
            out.close();
            scan.close();
        }
        

        /* *************************************** */
        // We can now ask the client for the operations he whishes to execute 
        /* *************************************** */


        connected = true;

        System.out.print("\n");
        System.out.println(answer);
        System.out.print("\n");

        // While the client remains active, he can choose to do things with the server
        while(connected) {

            System.out.print("Choose your interaction with the server : QUIT or UPLOAD ?  ");

            String choice = scan.next();
            String res = choice.toUpperCase();

            System.out.print("\n");
            String serverResp;

            switch(res) {

                case "QUIT" :
                    System.out.println("\nYou are disconnecting from the server . . .\n"); 
                    connected = false;

                    sock.close();
                    out.close();
                    in.close();
                    break;
                    
                case "OFF" :
                    System.out.println("\nYou are trying the shutdown the server !\n");
                    
                    out.flush();
                    out.writeUTF(res);

                    connected = false;

                    sock.close();
                    out.close();
                    in.close();
                    break; 
                    
                case "UPLOAD" :
                    out.flush();
                    out.writeUTF(res);
                
                    in = new DataInputStream(sock.getInputStream());
                    serverResp = in.readUTF();

                    System.out.print(serverResp);

                    choice = scan.next();
                    File img = new File(choice);

                    if(img.exists()) { // if the file exists then we can transfer its data to an array of bytes

                        byte[] imageData = readImageToByteArray(choice);

                        // Writing the data to the server
                        out.writeInt(imageData.length);
                        out.write(imageData);

                        System.out.println("File sent, waiting to receive it back \n");

                        int imageLength = in.readInt();

                        // Reading the data from the server
                        byte[] imageDataRcvd = new byte[imageLength];
                        in.readFully(imageDataRcvd);

                        BufferedImage imageRcvd = byteArrayToBufferedImage(imageDataRcvd);

                        System.out.print("Choose a name for this image (no need to mention the image format): ");
                        String name = scan.next();
                        System.out.print("\n");

                        ImageIO.write(imageRcvd, "jpg", new File(name + ".jpg"));
                    }

                    else { 
                        
                        System.out.println("No file found here \n"); 
                        out.writeInt(-1);
                    }
                    
                    break;
            }
        }
    }

    // Useful functions

    public static byte[] readImageToByteArray(String path) throws IOException {

        FileInputStream fis = new FileInputStream(path);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int bytesRead;

        while((bytesRead = fis.read(buffer)) != -1) { baos.write(buffer, 0, bytesRead); }

        fis.close();
        return baos.toByteArray();
    }

    public static BufferedImage byteArrayToBufferedImage(byte[] data) throws IOException {

        ByteArrayInputStream input = new ByteArrayInputStream(data);
        BufferedImage buffImg = ImageIO.read(input);

        return(buffImg);
    }

    public static boolean ipValidator(String ip) {

        String regex = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(ip);

        return(matcher.matches());
    }
}
