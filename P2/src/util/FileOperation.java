package util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileOperation {
	public static boolean isFileExist(String fileName) {
		File file = new File(fileName);
		return file.exists();
	}
	public static void writeToFile(String fileName,String line) {
		byte[] data = line.getBytes();
		Path p = Paths.get(fileName);
		try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(p))) {
			out.write(data, 0, data.length);
		} catch (IOException x) {
			System.err.println(x);
		}
	}
	public static String readFile(String fileName){
		StringBuffer content = new StringBuffer();
		try {
			Files.readAllLines(Paths.get(fileName)).forEach((s)->{
				content.append(s);
			});
			return content.toString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
		return null;
	}
	public static boolean deleteFile(String fileName){
		try {
			boolean isDelete = Files.deleteIfExists(Paths.get(fileName));
			return isDelete;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}		
	}
	
	public static boolean checkSum(){
		return false;
	}
	public static void main(String[] args){
		String filename = "testFile";
		String res = readFile(filename);
		writeToFile("/tmp/"+"chengao:"+filename,res);
		System.out.println(res);
	}
}
