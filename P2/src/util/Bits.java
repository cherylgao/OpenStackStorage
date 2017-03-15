package util;

public class Bits {
    private int value;
    public static void main(String[] args) {
        Bits v = new Bits(Integer.MAX_VALUE);
        v.setToZero(3,5,7);
        //System.out.println(v.getBit(3));
    }
    public Bits(){
        value = 0;
    }
    public Bits(int value){
        this.value = value;
    }
    public void setToOne(int index) {
        value |= 1 << index;
    }

    public void setToOne(int fromIndex, int toIndex) {
        int mark1 = ~((1 << (fromIndex)) - 1);
        int mark2 = (1 << (toIndex + 1))-1;
        int mark = mark1 & mark2;
        value |= mark;
    }
    public void setToZero(int index){
        value &=  ~(1 << index);
    }
    public void setToZero(int fromIndex, int toIndex, int max) {
        int mark3 = 0;
        if(max == Integer.MAX_VALUE){
            mark3 = Integer.MAX_VALUE;
        }
        else{
            mark3 = (1<<(max+1))-1;
        }
        int mark1 = ~((1 << (fromIndex)) - 1);
        int mark2 = (1 << (toIndex + 1))-1;
        int mark = mark1 & mark2;
        value &= (~mark)&mark3;
    }
    public boolean getBit(int index){
        return ((value & (1<<index))!=0);
    }
    public int get(){
        return value;
    }
}
