package server;

import static util.StringChecking.getCommandFromInput;
import static util.StringChecking.changeFromServerFormatToCServerFormat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import util.Constants;

public class CClient {
	private Socket socket;
	public CClient(Disk disk) throws Exception{
		String host = disk.getHost();
		String port = disk.getPortNumber();
		socket = new Socket(host, Integer.parseInt(port));
	}
	public String sendToCServer(String commandInput, Disk disk, String file) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
		OutputStream stream = socket.getOutputStream();
		PrintWriter out = new PrintWriter(stream);
		String cServerInput = changeFromServerFormatToCServerFormat(commandInput);
		out.println(cServerInput);
		if(getCommandFromInput(commandInput).equals(Constants.UPLOAD)){
			out.println(file);
		}
		out.flush();
		String response = in.readLine();
		System.out.println("Response from cserver "+disk.getName()+":"+response);
		in.close();
		out.close();
		socket.close();
		return response;		
	}
	
}
