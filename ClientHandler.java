import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;

import javax.imageio.ImageIO;

public class ClientHandler extends Thread {

    private Socket sock;
    private int clientNb;
    private boolean isOn = false;

    // Here's the constructor of the class " ClientHandler ", it will be called each time the server accepts a new connection
    public ClientHandler(Socket socket, int number) {

        this.sock = socket;
        this.clientNb = number;

        System.out.println("You have a new client connection !\nNumber of clients : "+ number + "\n");
    }

    public void run() { // The " run " function, proper to threads will begin each time the " start " method is called on the server class

        try{


            /* *************************************** */
            // First we check the credentials of the user on this thread
            /* *************************************** */


            String welcome = "Hello user, please give us your name and your password.";

            DataOutputStream out = new DataOutputStream(sock.getOutputStream());
            out.writeUTF(welcome);

            DataInputStream in = new DataInputStream(sock.getInputStream());
            String answer = in.readUTF();

            String[] credentials = answer.split(" ");            
            String answer1 = credentials[0];
            String answer2 = credentials[1];

            if(Serveur.checkClient(answer1)) { 

                if(answer1.equals("admin") && answer2.equals("root")) {

                    out.flush();
                    out.writeUTF("You are the admin of this server, you can thus shut it down using the command : OFF"); 
                }
                else {

                    if(Serveur.checkAll(answer1, answer2)) {

                        out.flush();
                        out.writeUTF("Welcome, you are " + clientNb + " in total on the server"); 
                    }
                    else {

                        out.flush();
                        out.writeUTF("Wrong password !");

                        out.close();
                        in.close();

                        sock.close(); // Radical measures in case the wrong password is given xD
                    }
                    
                }
            }
            else {

                out.flush();
                out.writeUTF("It seems you are new here : an account was thus created upon your arrival");

                String[] newClient = new String[2];
                newClient[0] = answer1;
                newClient[1] = answer2;

                Serveur.addClient(newClient); // We add the client to the hashset if he is new, this way we can easily write on the txt file afterwards
            }

            isOn = true;

            /* *************************************** */
            // Then we read and apply the commands sent by this same user 
            /* *************************************** */


            out = new DataOutputStream(sock.getOutputStream());
            in = new DataInputStream(sock.getInputStream());

            while(isOn) {

                String command = in.readUTF();

                System.out.println("Client " + clientNb + " asked for : " + command + "\n");

                switch(command) {

                    case "QUIT" :
                        System.out.println("Client " + clientNb + " disconnected from the server\n");
                        Serveur.decrementClients();

                        isOn = false;
                        
                        break;

                    case "OFF" :
                        if(answer1.equals("admin") && answer2.equals("root")) {
                            System.out.println("Client " + clientNb + " is admin");
                            System.out.println("Closing the server . . . \n");

                            isOn = false;

                            Serveur.setOff();
                            Serveur.closeClient();
                        }
                        break;

                    case "UPLOAD" :
                        out.flush();
                        out.writeUTF("Give us the name of the image file you would like to upload : ");

                        int imageLength = in.readInt();

                        if(imageLength == -1) { break; }

                        byte[] imageDataRcvd = new byte[imageLength];
                        in.readFully(imageDataRcvd);

                        System.out.println("[ " + answer1 + " " + sock.getInetAddress() + " : " +  sock.getPort() + " on " + LocalDateTime.now() + " ] " + "image received for filtering");

                        BufferedImage imageRcvd = byteArrayToBufferedImage(imageDataRcvd);
                        BufferedImage imageToSend = Filtre.process(imageRcvd);

                        System.out.println("Client " + clientNb + " filter was applied, sending new image to user !\n");

                        byte[] imageDataToSend = bufferedImageToByteArray(imageToSend);

                        out.writeInt(imageDataToSend.length);
                        out.write(imageDataToSend);

                        System.out.println("Image sent :)\n");
                        break;
                }
            }
        }
        catch (IOException e) { System.out.println("INFO : Exception caught by client handler of number " + clientNb + "\n"); }
    }

    // Useful functions

    public static BufferedImage byteArrayToBufferedImage(byte[] data) throws IOException {

        ByteArrayInputStream input = new ByteArrayInputStream(data);
        BufferedImage buffImg = ImageIO.read(input);

        return(buffImg);
    }

    public static byte[] bufferedImageToByteArray(BufferedImage img) throws IOException{

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", output);

        byte[] imageData = output.toByteArray();
        return(imageData);
    }
}
