package io.oneplus.vivothemes;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AodStudioActivity extends Activity {
    public static final String PREFS = "vivo_bridge_settings";
    private static final int PICK_IMAGE = 8100;
    private static final int[] COLORS = {0xFFFFFFFF, 0xFFB8A6FF, 0xFF91F7D0, 0xFFFFB9CF, 0xFFFFD48D, 0xFF92CDFF};
    private boolean preview = false;
    private SharedPreferences prefs;
    private int bg = 0xFF111119, panel = 0xFF252535, white=0xFFFFFFFF, muted=0xFFBBC0D4;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        render();
    }
    private int dp(int n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    private GradientDrawable shape(int color) {
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(17));
        return d;
    }
    private TextView label(String s,int size,int color,boolean bold) {
        TextView t=new TextView(this);
        t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private void add(LinearLayout p,View v,int top){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.topMargin=dp(top);p.addView(v,lp);
    }
    private TextView button(String s, Runnable action){
        TextView t=label(s,15,white,true);
        t.setGravity(Gravity.CENTER);t.setPadding(dp(16),dp(14),dp(16),dp(14));t.setBackground(shape(panel));
        t.setOnClickListener(v->action.run());
        return t;
    }
    private void render(){
        if(preview){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            AodCanvasView view=new AodCanvasView(this);
            view.setOnClickListener(v-> { preview=false;render(); });
            setContentView(view);
            return;
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(0);
        ScrollView sc=new ScrollView(this);
        sc.setFillViewport(true);
        sc.setBackgroundColor(bg);
        LinearLayout col=new LinearLayout(this);col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(20),dp(26),dp(20),dp(34));
        sc.addView(col);
        setContentView(sc);
        add(col,label("ORIGIN-STYLE  /  COLOROS",12,0xFFA99BFF,true),0);
        add(col,label("AOD Студия",31,white,true),9);
        add(col,label("Своя картинка, цвет и стиль часов. Без root.",14,muted,false),8);
        add(col,label("ПРЕДПРОСМОТР",12,0xFFA99BFF,true),23);
        AodCanvasView canvas=new AodCanvasView(this);
        canvas.setBackground(shape(0xFF050505));
        canvas.setOnClickListener(v->{preview=true;render();});
        LinearLayout.LayoutParams previewP=new LinearLayout.LayoutParams(-1,dp(300));
        previewP.topMargin=dp(11);col.addView(canvas,previewP);
        add(col,button("На весь экран — нажми для закрытия",()->{preview=true;render();}),12);
        add(col,label("КАРТИНКА AOD",18,white,true),25);
        add(col,button("Выбрать фотографию / PNG",this::pickImage),10);
        add(col,button("Убрать картинку",()->{prefs.edit().remove("aod_photo").apply();render();}),9);
        add(col,label("СТИЛЬ ЧАСОВ",18,white,true),23);
        String[] styles={"Тонкие","Жирные","Классические","Цифровые"};
        LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);
        for(int i=0;i<styles.length;i++){
            final int idx=i;
            TextView b=button(styles[i],()->{prefs.edit().putInt("clock_style",idx).apply();render();});
            b.setTextSize(12);
            int chosen=prefs.getInt("clock_style",0);
            b.setBackground(shape(chosen==i?0xFF6253A9:panel));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(49),1f);
            if(i>0)lp.leftMargin=dp(5);
            line.addView(b,lp);
        }
        add(col,line,11);
        add(col,label("ЦВЕТ",18,white,true),23);
        LinearLayout colors=new LinearLayout(this);colors.setOrientation(LinearLayout.HORIZONTAL);
        for(int i=0;i<COLORS.length;i++){
            final int idx=i;
            TextView b=label("●",30,COLORS[i],true); b.setGravity(Gravity.CENTER);
            b.setBackground(shape(panel));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(49),1);
            if(i>0)lp.leftMargin=dp(6);
            colors.addView(b,lp);
            b.setOnClickListener(v->{prefs.edit().putInt("clock_color",COLORS[idx]).apply();render();});
        }
        add(col,colors,11);
        add(col,label("РАСПОЛОЖЕНИЕ ЧАСОВ",18,white,true),23);
        add(col,button("Переместить часы (верх → центр → низ)",()->{
            prefs.edit().putInt("clock_pos",(prefs.getInt("clock_pos",0)+1)%3).apply();render();
        }),11);
        add(col,label("ИСПОЛЬЗОВАНИЕ",18,white,true),24);
        add(col,label("Превью работает внутри приложения. Если ColorOS поддерживает Android-заставки, этот AOD-стиль можно использовать как заставку при зарядке.",13,muted,false),9);
        add(col,button("Открыть настройки заставки Android",()->{
            try {startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS));}
            catch(Exception e){Toast.makeText(this,"На этой ColorOS нет настроек заставок",Toast.LENGTH_LONG).show();}
        }),10);
        add(col,label("Это НЕ системный AOD ColorOS: внешний APK не может подменить часы и анимации системного AOD без интеграции в прошивку.",12,0xFFFFC29C,false),17);
    }
    private void pickImage(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i,PICK_IMAGE);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==PICK_IMAGE&&result==RESULT_OK&&data!=null&&data.getData()!=null){
            Uri uri=data.getData();
            try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
            prefs.edit().putString("aod_photo",uri.toString()).apply();
            render();
        }
    }
    @Override public void onBackPressed(){
        if(preview){preview=false;render();}else super.onBackPressed();
    }
    public static class AodCanvasView extends View {
        private final Paint p=new Paint(3);
        private Bitmap picture;
        private String oldUri;
        public AodCanvasView(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        private void paintText(Canvas c,String s,float size,int color,Typeface font,float x,float y){
            p.reset();p.setAntiAlias(true);p.setColor(color);p.setTypeface(font);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);
            c.drawText(s,x,y,p);
        }
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float w=getWidth(),h=getHeight();
            c.drawColor(Color.BLACK);
            SharedPreferences pr=getContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);
            String uri=pr.getString("aod_photo","");
            if(!uri.equals(oldUri)){
                if(picture!=null){picture.recycle();picture=null;}
                oldUri=uri;
                if(!uri.isEmpty()){
                    try(InputStream input=getContext().getContentResolver().openInputStream(Uri.parse(uri))){
                        BitmapFactory.Options opt=new BitmapFactory.Options();opt.inSampleSize=2;picture=BitmapFactory.decodeStream(input,null,opt);
                    }catch(Exception ignored){}
                }
            }
            if(picture!=null&&!picture.isRecycled()){
                float maxW=w*.67f,maxH=h*.40f;
                float scale=Math.min(maxW/picture.getWidth(),maxH/picture.getHeight());
                float destW=picture.getWidth()*scale,destH=picture.getHeight()*scale;
                float cx=w/2,cy=h*.55f;
                p.reset();p.setAntiAlias(true);p.setFilterBitmap(true);p.setAlpha(172);
                c.drawBitmap(picture,null,new RectF(cx-destW/2,cy-destH/2,cx+destW/2,cy+destH/2),p);
            }
            int color=pr.getInt("clock_color",Color.WHITE);
            int style=pr.getInt("clock_style",0);
            int pos=pr.getInt("clock_pos",0);
            float y=h*(pos==0?.26f:pos==1?.48f:.74f);
            Typeface f=(style==1?Typeface.create("sans-serif-black",Typeface.BOLD):
                       style==2?Typeface.create("serif",Typeface.NORMAL):
                       style==3?Typeface.create("monospace",Typeface.BOLD):
                       Typeface.create("sans-serif-light",Typeface.NORMAL));
            String time=new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date());
            paintText(c,time,Math.min(w*.22f,h*.13f),color,f,w/2,y);
            paintText(c,new SimpleDateFormat("EEEE, d MMMM",new Locale("ru")).format(new Date()),Math.min(25,w*.042f),0xFFC4C4CA,Typeface.DEFAULT,w/2,y+h*.055f);
            Intent b=getContext().registerReceiver(null,new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if(b!=null){
                int val=b.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
                int max=b.getIntExtra(BatteryManager.EXTRA_SCALE,100);
                if(val>=0&&max>0)paintText(c,(val*100/max)+"%  •  AOD Studio",Math.min(23,w*.04f),0xFFAAAAAA,Typeface.DEFAULT,w/2,h*.93f);
            }
            postInvalidateDelayed(15000);
        }
        @Override protected void onDetachedFromWindow(){
            super.onDetachedFromWindow();
            if(picture!=null){picture.recycle();picture=null;}
        }
    }
}
