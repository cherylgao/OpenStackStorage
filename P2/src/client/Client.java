package client;

import static util.StringChecking.getCommandFromInput;
import static util.StringChecking.getUsrAndFilePathFromInput;
import static util.StringChecking.getFilePathFromUsrAndFile;
import static util.StringChecking.getUsrAndFileNameFromFilePath;
import static util.FileOperation.isFileExist;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

import util.Constants;
import util.StringChecking;

public class Client {
    private Socket socket;
    public void config(){
        try {
        	System.out.println("please input your config command like this: client localhost 1024");
            Scanner scanner = new Scanner(System.in);            
            scanner.next();
            String ip = scanner.next();
            String port = scanner.next();
            socket = new Socket(ip, Integer.parseInt(port));
            //socket = new Socket("localhost", 8000);
            System.out.println("Connected, please input your command");            
        } catch (IOException ex) {
            //Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }
    public static void main(String[] args) throws Exception {  
        //Socket socket = new Socket("localhost", 10000);
        Client client = new Client();
        client.config();
        
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getSocket().getInputStream()));  
        OutputStream stream =client.getSocket().getOutputStream();
        PrintWriter out = new PrintWriter(stream);  
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));  
        
        while (true) {  
            String msg = reader.readLine();
            if(getCommandFromInput(msg).equals(Constants.UPLOAD)){
            	String usrAndFile = getUsrAndFilePathFromInput(msg);
            	String filePath = getFilePathFromUsrAndFile(usrAndFile);
            	if(isFileExist(filePath)){
            		String usrAndFileName = getUsrAndFileNameFromFilePath(usrAndFile);
            		out.println(Constants.UPLOAD+" "+usrAndFileName);
            		StringBuffer fileContext = new StringBuffer();
            		Files.lines(Paths.get(filePath)).forEachOrdered((s)->{
            			fileContext.append(s);
            		});
            		out.println(fileContext.toString());
            		out.flush();
            	}else{
            		System.err.println("file doesn't exist");
            		continue;
            	}
            }else{
            	out.println(msg); 
            	out.flush();
            }
            if (msg.equals("bye")) {  
                break;  
            }
            StringBuffer response = new StringBuffer();
            
            String line = in.readLine();
            System.out.println(line); 
        }  
        client.getSocket().close();
    }
    
    public Socket getSocket() {
        return socket;
    }
    public void setSocket(Socket socket) {
        this.socket = socket;
    }  

}
