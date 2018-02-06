import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerToClientConnection implements Runnable {

  private Socket clientSocket;
  private BufferedReader bufferedReader = null;
  private ServerSocket serverDataSocket;
  private PrintStream printStream;
  private static String USER = "";

  public ServerToClientConnection(Socket client) {
   this.clientSocket = client;
  }

  @Override
public void run() {
 try {
    int flag=0;
    while(flag!=1){
    String clientSelection;    
    synchronized (this) {
        bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        printStream = new PrintStream(clientSocket.getOutputStream());
        clientSelection = checkIns();
    
    if(clientSelection != null)
        switch (clientSelection) {
        
        	case "USER":
	        	String user = bufferedReader.readLine();
	        	String userset = "true";
	        	if(Authentication.keyCheck(user, user)){
	        		userset = "true";
	        		USER = user;
	            	File a = new File(user);
	            	if(!a.exists())
	            		a.mkdir();
	        	}
	        	else{
	        		userset = "false";
	        	}
	        	printStream.println(userset);
	        	break;
	        	
        	case "LS":
        		File folder = new File(USER);
        		File[] listOfFiles = folder.listFiles();
        		printStream.println(listOfFiles.length);
        		
        		 for (int i = 0; i < listOfFiles.length; i++) {
        		      if (listOfFiles[i].isFile()) {
        		    	  printStream.println(listOfFiles[i].getName());
        		      }
        		 }
        		break;
        		
        	case "MODE":
        		String mode_code = bufferedReader.readLine();
        		String mode = "";
        		if(mode_code.equalsIgnoreCase("s")){
        			mode = "Stream";
        		}
        		else if(mode_code.equalsIgnoreCase("b")){
        			mode = "block";
        		}
        		else if(mode_code.equalsIgnoreCase("c")){
        			mode = "compressed";
        		}
        		System.out.println("Mode : "+mode);
				printStream.println(mode);
				break;
	   
			case "TYPE":
				String type_code = bufferedReader.readLine();
        		String type = "";
        		if(type_code.equalsIgnoreCase("a")){
        			type = "ASCII";
        		}
        		else if(type_code.equalsIgnoreCase("b")){
        			type = "EBCDIC";
        		}
        		System.out.println("Type : "+type);
				printStream.println(type);
				break;
	        	
            case "PUT":
                retrieve();
                break;
                
            case "GET":
                String outGoingFileName;
                System.out.println("Recieved file name");
                while ((outGoingFileName = bufferedReader.readLine()) != null) {
                	System.out.println(outGoingFileName);
                    store(outGoingFileName);
                         
                }
                break;
                
            case "NOOP":
                System.out.println("Noop Done");
				printStream.println("200 [NOOP] Command okay");
            	break;
                
            case "PORT":
            	String pt=bufferedReader.readLine();
            	ServerSocket a = null;
            	System.out.println("going inside server port"+pt);
            	try {
                	
            		a = new ServerSocket(Integer.parseInt(pt));
                    System.out.println("this port number started "+pt);
                    System.out.println("New Data Server started.");
                } catch (Exception e) {
                    System.err.println("DataPort already in use.");
                    System.exit(1);
                }
                  try {
                    	System.out.println("going inside data port creation");
                    	this.clientSocket = a.accept();
                        System.out.println("Accepted connection : " + clientSocket);
                        Thread h = new Thread(new ServerToClientConnection(clientSocket));
                        h.start();
                    } catch (Exception e) {
                        System.err.println("Error in connection attempt.");
                    }
                break;
                
            default:
                System.out.println("Incorrect command received.");
                System.out.println(clientSelection);
                flag=0;
                break;
        }
    }
     }

     } catch (IOException ex) {
    Logger.getLogger(ServerToClientConnection.class.getName()).log(Level.SEVERE, null, ex);
   }
}

  
  	public String checkIns() throws IOException{
  		String clientSelection = null;
  		while ((clientSelection = bufferedReader.readLine()) != null) {
  			return clientSelection;
  		}
  		return clientSelection;
  	} 
  	
   public synchronized void retrieve() {
   try {
	   System.out.println("Going inside recieve file function");
     int bytesRead;
     DataInputStream clientData;
    clientData = new DataInputStream(clientSocket.getInputStream());
    String fileName = clientData.readUTF();
    File a = new File(USER+"\\"+fileName);
    OutputStream output = new FileOutputStream((a));
    long size = clientData.readLong();
    byte[] buffer = new byte[1024];
    while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
        output.write(buffer, 0, bytesRead);
        size -= bytesRead;
    }
    System.out.println("File "+fileName+" received from client.");
	printStream.println(fileName+ " Received. This s server resp");
} catch (IOException ex) {
    System.err.println("Client error. Connection closed.");
      }
   }

   public synchronized void store(String fileName) {
    try {
    File myFile = new File(USER+"\\"+fileName);
    byte[] mybytearray = new byte[(int) myFile.length()];
    FileInputStream fileInputStream = new FileInputStream(myFile);
    BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
    DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);
    dataInputStream.readFully(mybytearray, 0, mybytearray.length);
    OutputStream outputStream = clientSocket.getOutputStream();
    DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
    dataOutputStream.writeUTF(myFile.getName());
    dataOutputStream.writeLong(mybytearray.length);
    dataOutputStream.write(mybytearray, 0, mybytearray.length);
    dataOutputStream.flush();
    System.out.println("File "+fileName+" sent to client.");
 } catch (Exception e) {
    System.err.println("File does not exist!");
  } 
   }
}