package io.oneplus.vivothemes;

import android.service.wallpaper.WallpaperService;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.SurfaceHolder;
import java.util.Random;

public class ColorosLiveWallpaper extends WallpaperService {
    public static final int[][] COLORS={
        {0xFF20124C,0xFFAA57CC},{0xFF001924,0xFF0191B3},
        {0xFF5C163A,0xFFF0A35E},{0xFF021B14,0xFF119A78},
        {0xFF03071C,0xFF5262CA},{0xFF230024,0xFFF248A4},
        {0xFF092E5C,0xFF86CDFF},{0xFF101010,0xFF454545}};
    @Override public Engine onCreateEngine(){return new AnimatedEngine();}
    class AnimatedEngine extends Engine {
        final Handler handler=new Handler(Looper.getMainLooper());
        final Paint p=new Paint(3);
        final Random rand=new Random(5050);
        boolean visible=false;
        final float[] x=new float[45],y=new float[45],r=new float[45];
        final Runnable tick=new Runnable(){@Override public void run(){drawFrame();}};
        AnimatedEngine(){for(int i=0;i<x.length;i++){x[i]=rand.nextFloat();y[i]=rand.nextFloat();r[i]=rand.nextFloat();}}
        @Override public void onVisibilityChanged(boolean v){
            visible=v;handler.removeCallbacks(tick);if(v)drawFrame();
        }
        @Override public void onSurfaceChanged(SurfaceHolder h,int format,int width,int height){
            super.onSurfaceChanged(h,format,width,height);drawFrame();
        }
        @Override public void onDestroy(){super.onDestroy();visible=false;handler.removeCallbacks(tick);}
        private void drawFrame(){
            handler.removeCallbacks(tick);
            if(!visible)return;
            SurfaceHolder holder=getSurfaceHolder();Canvas c=null;
            try {
                c=holder.lockCanvas();
                if(c!=null){
                    float w=c.getWidth(),h=c.getHeight();
                    SharedPreferences pr=getSharedPreferences(AodStudioActivity.PREFS,MODE_PRIVATE);
                    int style=pr.getInt("live_style",0)%COLORS.length;
                    int mode=pr.getInt("live_mode",0);
                    float t=(SystemClock.uptimeMillis()%1000000)/1000f;
                    p.reset();p.setAntiAlias(true);
                    p.setShader(new LinearGradient(0,0,w,h,COLORS[style][0],COLORS[style][1],Shader.TileMode.CLAMP));
                    c.drawRect(0,0,w,h,p);p.setShader(null);
                    for(int i=0;i<x.length;i++){
                        float xx=(x[i]*w+(float)Math.sin(t*.13+i)*w*.07f);
                        float yy=(y[i]*h-t*(mode==1?8f:18f)*(r[i]+.2f));
                        yy=(yy%(h+120)+(h+120))%(h+120)-60;
                        if(mode==0){p.setColor(0x80FFFFFF);c.drawCircle(xx,yy,1.5f+5*r[i],p);}
                        else if(mode==1){p.setColor(0x44FFFFFF);c.drawCircle(xx,yy,8+30*r[i],p);}
                        else {p.setColor(0xBBFFFFFF);c.drawCircle(xx,yy,1+2*r[i],p);}
                    }
                    if(mode!=2){
                        p.setShader(new RadialGradient(w*(.5f+.18f*(float)Math.sin(t*.2f)),h*.67f,w*.65f,new int[]{0x55FFFFFF,0x00FFFFFF},null,Shader.TileMode.CLAMP));
                        c.drawCircle(w*.5f,h*.67f,w*.65f,p);
                        p.setShader(null);
                    }
                }
            }catch(Exception ignored){}finally{if(c!=null){try{holder.unlockCanvasAndPost(c);}catch(Exception ignored){}}}
            if(visible)handler.postDelayed(tick,66);
        }
    }
}
