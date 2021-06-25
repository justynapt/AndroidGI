package gin.util;

public class JankyFrames {
    public long jankyFrames;
    public long totalFrames;
    public long per50th;
    public long per90th;
    public long per95th;
    public long per99th;


    public JankyFrames(long jankyFrames, long totalFrames){
        this.jankyFrames = jankyFrames;
        this.totalFrames = totalFrames;
        this.per50th = 0;
        this.per90th = 0;
        this.per95th = 0;
        this.per99th = 0;
    }

    public float getJankiness(){
        return (float) (jankyFrames) / (float) (totalFrames);
    }

    public String toString(){
        String out = "jankyFrames: " + jankyFrames + "\n totalFrames: " + totalFrames + "\n 50th Per: " + per50th  + "\n 90th Per: " + per90th + "\n 95th Per: " + per95th + "\n 99th Per: " + per99th ;
        return out;
    }
}
