package util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

public class StringChecking {
    

    public static boolean isNullOrEmpty(String s) {
        if (s == null)
            return true;
        if (s.length() == 0)
            return true;
        return false;
    }

    public static boolean isNullOrEmpty(Collection<?> l) {
        if (l == null)
            return true;
        if (l.size() == 0)
            return true;
        return false;
    }
    public static String getFilePathFromUsrAndFile(String usrAndFile){
    	String[] strs = usrAndFile.split("/");
    	if(strs.length  <= 1){
    		return null;
    	}
    	StringBuffer fileName = new StringBuffer();
    	for(int i = 1;i < strs.length;i++){
    		fileName.append(strs[i]+"/");
    	}
    	return fileName.toString();
    }
    
    public static String getCommandFromInput(String command){
    	String[] strs = command.split(" ");
    	if(strs.length  <= 1){
    		return null;
    	}
    	
    	return strs[0];
    }
    public static String getUsrAndFilePathFromInput(String input){
    	String[] strs = input.split(" ");
    	if(strs.length  <= 1){
    		return null;
    	}
    	
    	return strs[1];
    }
    public static String getUsrAndFileNameFromFilePath(String filePath){
    	String[] strs = filePath.trim().split("/");
    	if(strs.length <= 1){
    		return null;
    	}
    	int start = 0;
    	int end = strs.length - 1;
    	String usr = null;
    	String fileName = null;
    	while(start < end){
    		if(isNullOrEmpty(strs[start])){
    			start++;
    		}else{
    			usr = strs[start];
    			break;
    		}    		
    	}
    	while(start < end){
    		if(isNullOrEmpty(strs[end])){
    			end--;
    		}else{
    			fileName = strs[end];
    			break;
    		}    		
    	}
    	return usr+"/"+fileName;
    }
    public static String changeFromServerFormatToCServerFormat(String serverFileName){
    	return serverFileName.replace('/', ':');
    }
    public static String changeFromCServerFormatToServerFormat(String cServerFileName){
    	return cServerFileName.replace(':', '/');
    }
    public static void main(String[] args){
    	String str = changeFromCServerFormatToServerFormat("upload m:README.txt");    	
    	System.out.println(str);
    }
    
}
