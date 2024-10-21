import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Serveur {

    // Variables :
    private static ServerSocket socketServer;
    private static Socket socketClient;
    private static ClientHandler handler;

    private static String ip;
    private static int port;

    private static boolean isOn = true;
    private static int totalClients = 0;

    private static Scanner scan;
    private static HashSet<String[]> clientData = new HashSet<String[]>();


    public static void main(String[] args) throws Exception {

        // Instanciating the socket
        socketServer = new ServerSocket();
        socketServer.setReuseAddress(true);

        // Here is the file used as a database for all of our users
        File database = new File("./Database.txt");
        
        scan = new Scanner(System.in);
        System.out.print("\n");

        // Asking for ip adress
        System.out.print("Enter the server address : ");
        ip = scan.next();

        while(!ipValidator(ip)) {

            // Asking for ip adress again after a wrong one was passed
            System.out.print("Enter the server address (a valid one please :): ");
            ip = scan.next();
        }

        // Asking for port
        System.out.print("Enter the port number (make sure it is an INTEGER value): ");
        port = scan.nextInt();

        while (5000 > port || 5050 < port) {

            // Asking for port again after a wrong number was passed
            System.out.print("Give a valid port number (5000 -> 5050) : ");
            port = scan.nextInt();
        }

        // Binding the server's socket to its ip adress
        InetAddress serverIP = InetAddress.getByName(ip);
        socketServer.bind(new InetSocketAddress(serverIP, port));

        System.out.println("\nServer open, running on : " + ip + " --- " + port + "\n");

        try { 

            BufferedReader fileReader = null;
            

            /* *************************************** */
            // First we check the status of our user database 
            /* *************************************** */


            if(database.isFile()) { // We read the contents of the file and store it in the hashset 

                fileReader = new BufferedReader(new FileReader(database));

                System.out.println("INFO : File exists\n");
                String user;
                
                while((user = fileReader.readLine()) != null) {

                    String[] credentials = user.split(" ");
                    clientData.add(credentials); 
                }
            }

            // We create a new file
            else { 
                
                System.out.println("INFO : File does not exist\n");
                database.createNewFile(); 
                
                System.out.println("*** File created ***\n");

                fileReader = new BufferedReader(new FileReader(database)); 

                BufferedWriter fileWriter = new BufferedWriter(new FileWriter(database));
                fileWriter.write("admin root");
                fileWriter.close();
            }


            /* *************************************** */
            // Then we wait for incoming connections 
            /* *************************************** */


            while(getOn()) {

                socketClient = socketServer.accept();
                handler = new ClientHandler(socketClient, ++totalClients);
                handler.start();
            }


            /* *************************************** */
            // Finally we store the newly acquired data 
            /* *************************************** */


            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(database));
            Iterator<String[]> iterator = clientData.iterator();

            while(iterator.hasNext()) {

                String[] next = iterator.next();
                System.out.println(next);
                fileWriter.write(next[0] + " " + next[1] + "\n");
            }

            System.out.println("Serveur closed by admin, saving data . . .\n");
            socketServer.close();
            fileReader.close();
            fileWriter.close();
        }

        catch(Exception e) {
            
            System.out.print("There's an error somewhere :/\n");
        }
    }

    // Useful functions :

    public static boolean ipValidator(String ip) {

        String regex = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(ip);

        return(matcher.matches());
    }

    public static void setOn() { isOn = true; }
    public static void setOff() { isOn = false; }
    public static boolean getOn() { return isOn; }

    public static void decrementClients() { totalClients--; }
    public static void addClient(String[] data) { clientData.add(data); }

    public static boolean checkClient(String data) { 
        
        Iterator<String[]> iterator = clientData.iterator();

            while(iterator.hasNext()) {

                String[] next = iterator.next();
                
                if(next[0].equals(data)) {

                    return(true);
                }
            }

            return(false);
    }

    public static boolean checkAll(String data1, String data2) {

        Iterator<String[]> iterator = clientData.iterator();

            while(iterator.hasNext()) {

                String[] next = iterator.next();
                
                if(next[0].equals(data1) && next[1].equals(data2)) {

                    return(true);
                }
            }

        return(false);
    }

    public static void closeClient() throws IOException { 
        
        Socket closingSocket = new Socket(ip, port); // Creating a fake client to finish the while loop
        closingSocket.close();

        System.out.println("Closing all listening processes . . .");
    } 
}
