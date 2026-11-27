import com.minecrafteft.client.StashDragPlacement;

/** The cursor-centred footprint, landing highlight and click use the same grid anchor. */
public final class BagPlacementCheck {
    public static void main(String[] args) {
        int checks=0;
        for(int columns:new int[]{4,8,10}) for(int rows:new int[]{5,7,13})
            for(int w=1;w<=8;w++) for(int h=1;h<=7;h++)
                for(int col=0;col<columns;col++) for(int row=0;row<rows;row++) {
                    int gx=173,gy=91;
                    double mx=gx+col*18+w*9,my=gy+row*18+h*9;
                    var target=StashDragPlacement.target(mx,my,gx,gy,w,h);
                    if(target.column()!=col || target.row()!=row || target.index(columns)!=row*columns+col)
                        throw new AssertionError("Cursor centre does not select the preview anchor");
                    boolean inside=col+w<=columns && row+h<=rows;
                    if(target.inside(w,h,columns,rows)!=inside) throw new AssertionError("Footprint wraps or crosses bag boundary");
                    checks+=2;
                }
        var edge=StashDragPlacement.target(173+9,91+9,173,91,4,2);
        if(edge.inside(4,2,4,5)) throw new AssertionError("Wide cursor near left edge must not snap under an unrelated cell");
        System.out.println("BagPlacementCheck: "+(checks+1)+" checks passed");
    }
}
