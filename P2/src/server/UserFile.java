package server;

public class UserFile extends Node {
    private long size;
    private String user;
    private String location;
    public UserFile(String usr ,String name){
        super(name);
        user = usr;
    }
    public UserFile(String name, long size) {
        super(name);
        // TODO Auto-generated constructor stub
        this.size = size;
    }
    public long getSize() {
        return size;
    }
    public void setSize(long size) {
        this.size = size;
    }
    public String getUser() {
        return user;
    }
    public void setUser(String user) {
        this.user = user;
    }
    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }
    

}
