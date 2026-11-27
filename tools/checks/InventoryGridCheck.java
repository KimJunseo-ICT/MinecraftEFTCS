import com.minecrafteft.inventory.InventoryGridGeometry;
import java.util.*;

public final class InventoryGridCheck {
    private static int checks;
    private record Rect(int x,int y,int w,int h,int anchor) { }
    private static void check(boolean result,String reason) { checks++;if(!result) throw new AssertionError(reason); }
    public static void main(String[] args) {
        int[] empty=new int[20];Arrays.fill(empty,-1);
        check(!InventoryGridGeometry.fits(4,5,0,5,3,i->empty[i]),"wide case cannot cross row boundaries");
        check(InventoryGridGeometry.fits(4,5,0,3,5,i->empty[i]),"rotated case fits the same backpack");
        int[] fragmented=new int[20];Arrays.fill(fragmented,-1);for(int i:new int[]{1,5,9,13,17}) fragmented[i]=1;
        check(!InventoryGridGeometry.fits(4,5,0,4,3,i->fragmented[i]),"free cell count cannot substitute for a rectangle");
        check(!InventoryGridGeometry.fits(4,5,0,3,4,i->fragmented[i]),"rotating cannot bridge an occupied stripe");
        int[] occupied=new int[20];Arrays.fill(occupied,-1);
        for(int row=0;row<2;row++) for(int col=0;col<4;col++) occupied[row*4+col]=0;
        check(!InventoryGridGeometry.fits(4,5,0,4,2,i->occupied[i]),"same-anchor weapon cannot replace an occupied weapon");
        check(!InventoryGridGeometry.fits(4,5,0,2,4,i->occupied[i]),"rotated weapon cannot reuse an occupied anchor");
        check(!InventoryGridGeometry.fits(4,5,1,1,1,i->occupied[i]),"covered non-anchor cells reject placement too");
        check(InventoryGridGeometry.fits(4,5,8,4,2,i->occupied[i]),"a different completely empty rectangle remains valid");
        Random random=new Random(73021);
        for(int run=0;run<240;run++) {
            int cols=1+random.nextInt(8),rows=1+random.nextInt(20);List<Rect> placed=new ArrayList<>();
            int[] owner=new int[cols*rows];Arrays.fill(owner,-1);
            for(int n=0;n<12;n++) {
                int x=random.nextInt(cols),y=random.nextInt(rows),w=1+random.nextInt(cols-x),h=1+random.nextInt(rows-y),anchor=y*cols+x;
                boolean collision=false;
                for(Rect p:placed) if(x<p.x+p.w && x+w>p.x && y<p.y+p.h && y+h>p.y) collision=true;
                if(collision) continue;
                placed.add(new Rect(x,y,w,h,anchor));
                for(int r=y;r<y+h;r++) for(int c=x;c<x+w;c++) owner[r*cols+c]=anchor;
            }
            for(int anchor=-1;anchor<=cols*rows;anchor++) for(int w=1;w<=9;w++) for(int h=1;h<=7;h++) {
                int x=anchor<0?-1:anchor%cols,y=anchor<0?-1:anchor/cols;
                boolean expected=anchor>=0 && anchor<cols*rows && x+w<=cols && y+h<=rows;
                if(expected) for(Rect p:placed) if(x<p.x+p.w && x+w>p.x && y<p.y+p.h && y+h>p.y) expected=false;
                check(InventoryGridGeometry.fits(cols,rows,anchor,w,h,i->owner[i])==expected,"rectangle/oracle mismatch");
            }
        }
        System.out.println("Inventory rectangular placement checks passed: "+checks);
    }
}
