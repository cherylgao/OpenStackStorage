package server;

import util.Bits;

public class Disk extends Node {
    private long size = (long) Math.pow(2, 30);
    private int beginPartitionIndex;
    private int endPartitionIndex;
    public Disk(String name){
        super(name);        
    }
    public Disk(String name, long size){
        super(name);
        this.size = size;
    }
    public Disk(Disk disk){
    	super(disk.getName());
    	size = disk.getSize();
    	beginPartitionIndex = disk.getBeginPartitionIndex();
    	endPartitionIndex = disk.getEndPartitionIndex();    	
    }
    public String getHost(){
    	String name = super.getName();
    	String[] parts = name.split(":");
    	return parts[0];
    }
    public String getPortNumber() throws Exception{
    	String name = super.getName();
    	String[] parts = name.split(":");
    	if(parts.length <= 1){
    		throw new Exception("disk name is not available, make sure the name is like this: xxx.xxx.xxx.xxx:xxxx");
    	}
    	return parts[1];
    }
    public long getSize() {
        return size;
    }
    public void setSize(long size) {
        this.size = size;
    }
    public int getBeginPartitionIndex() {
        return beginPartitionIndex;
    }
    public void setBeginPartitionIndex(int beginPartitionIndex) {
        this.beginPartitionIndex = beginPartitionIndex;
    }
    public int getEndPartitionIndex() {
        return endPartitionIndex;
    }
    public void setEndPartitionIndex(int endPartitionIndex) {
        this.endPartitionIndex = endPartitionIndex;
    }
    public static void main(String[] args){
    	Disk d = new Disk("129.160.1.80:9090");
    	System.out.println(d.getHost());
    	try {
			System.out.println(d.getPortNumber());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
