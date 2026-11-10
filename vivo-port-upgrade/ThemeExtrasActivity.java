package io.oneplus.vivothemes;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ThemeExtrasActivity extends Activity {
    private static final int PICK_ITCH = 8120;
    private final int bg=0xFF10111B,panel=0xFF262639, white=0xFFFFFFFF;
    private SharedPreferences prefs;
    private Bitmap imported;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        prefs=getSharedPreferences(AodStudioActivity.PREFS,MODE_PRIVATE);
        render();
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView label(String s,int size,int color){
        TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);return t;
    }
    private TextView button(String s,Runnable r){
        TextView t=label(s,15,white);t.setGravity(Gravity.CENTER);
        t.setPadding(dp(9),dp(14),dp(9),dp(14));
        GradientDrawable d=new GradientDrawable();d.setColor(panel);d.setCornerRadius(dp(16));t.setBackground(d);
        t.setOnClickListener(v->r.run());return t;
    }
    private void add(LinearLayout box,View v,int top){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);box.addView(v,p);
    }
    private void render(){
        ScrollView sc=new ScrollView(this);sc.setBackgroundColor(bg);
        LinearLayout col=new LinearLayout(this);col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(20),dp(30),dp(20),dp(36));sc.addView(col);setContentView(sc);
        add(col,label("ORIGIN-STYLE  /  COLOROS",12,0xFFA78BFA),0);
        add(col,label("Живые обои и импорт",29,white),12);
        add(col,label("Анимация создаётся Android WallpaperService, без компонентов Vivo.",14,0xFFBDBCD5),9);
        add(col,label("1. ВЫБЕРИ ЦВЕТ ЖИВЫХ ОБОЕВ",16,white),26);
        final String[] names={"Фиолет","Океан","Закат","Изумруд","Звёзды","Розовый неон","Лёд","Тёмный"};
        for(int i=0;i<names.length;i+=2){
            LinearLayout row=new LinearLayout(this);
            int j=i;
            TextView a=button(names[i],()->{prefs.edit().putInt("live_style",j).apply();Toast.makeText(this,"Стиль: "+names[j],Toast.LENGTH_SHORT).show();});
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);row.addView(a,p);
            final int k=i+1;
            TextView b=button(names[k],()->{prefs.edit().putInt("live_style",k).apply();Toast.makeText(this,"Стиль: "+names[k],Toast.LENGTH_SHORT).show();});
            LinearLayout.LayoutParams p2=new LinearLayout.LayoutParams(0,-2,1);p2.leftMargin=dp(10);row.addView(b,p2);
            add(col,row,10);
        }
        add(col,label("2. ВЫБЕРИ АНИМАЦИЮ",16,white),24);
        String[] motions={"Мерцающие частицы","Плавающие круги","Звёздное небо"};
        for(int i=0;i<motions.length;i++){
            int idx=i;
            add(col,button(motions[i],()->{prefs.edit().putInt("live_mode",idx).apply();Toast.makeText(this,"Эффект: "+motions[idx],Toast.LENGTH_SHORT).show();}),9);
        }
        add(col,button("Установить живые обои",this::setLiveWallpaper),15);
        add(col,label("3. ИМПОРТ ТЕМ VIVO (.ITZ)",16,white),29);
        add(col,label("Можно выбрать файл .itz или ZIP. Приложение ищет в нём подходящую фотографию и предлагает использовать её как обои. Иконки, AOD и системные части темы OriginOS не импортируются.",13,0xFFBDBCD5),10);
        add(col,button("Открыть .itz и извлечь обои",this::pickTheme),14);
        add(col,button("Редактор кастомного AOD",()->startActivity(new Intent(this,AodStudioActivity.class))),11);
    }
    private void setLiveWallpaper(){
        Intent intent=new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,new ComponentName(this,ColorosLiveWallpaper.class));
        try{startActivity(intent);}
        catch(Exception e){
            try{startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));}
            catch(Exception ex){Toast.makeText(this,"В этой прошивке нет выбора живых обоев",Toast.LENGTH_LONG).show();}
        }
    }
    private void pickTheme(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);
        try{startActivityForResult(i,PICK_ITCH);}catch(Exception e){Toast.makeText(this,"Выбор файлов недоступен",Toast.LENGTH_LONG).show();}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=PICK_ITCH||result!=RESULT_OK||data==null||data.getData()==null)return;
        final Uri uri=data.getData();
        Toast.makeText(this,"Проверяю содержимое темы…",Toast.LENGTH_SHORT).show();
        new Thread(()->{
            Bitmap resultImage=extractWallpaper(uri);
            runOnUiThread(()->{
                if(resultImage==null){
                    new AlertDialog.Builder(this).setTitle("Обои не найдены")
                     .setMessage("Формат темы не содержит читаемых JPG/PNG/WebP или данные зашифрованы. Перенести закрытый формат тем Vivo напрямую нельзя.")
                     .setPositiveButton("Понятно",null).show();
                }else{
                    if(imported!=null&&!imported.isRecycled())imported.recycle();
                    imported=resultImage;
                    new AlertDialog.Builder(this).setTitle("Найдены обои Vivo")
                     .setMessage("Из файла темы удалось извлечь изображение. Куда его установить?")
                     .setItems(new String[]{"Главный экран","Экран блокировки","Оба экрана"},(d,which)->applyImported(which))
                     .setNegativeButton("Отмена",null).show();
                }
            });
        }).start();
    }
    private Bitmap extractWallpaper(Uri uri){
        byte[] best=null;long bestScore=0;
        try(InputStream raw=getContentResolver().openInputStream(uri);
            ZipInputStream zip=new ZipInputStream(raw)){
            ZipEntry e;int total=0;
            while((e=zip.getNextEntry())!=null&&total++<450){
                String name=e.getName().toLowerCase(Locale.ROOT);
                if(e.isDirectory()||!(name.endsWith(".png")||name.endsWith(".jpg")||name.endsWith(".jpeg")||name.endsWith(".webp")))continue;
                if(e.getSize()>12*1024*1024L)continue;
                ByteArrayOutputStream out=new ByteArrayOutputStream();
                byte[] buf=new byte[8192];int n,size=0;
                while((n=zip.read(buf))!=-1){
                    size+=n;if(size>12*1024*1024)break;
                    out.write(buf,0,n);
                }
                if(size>12*1024*1024)continue;
                byte[] bytes=out.toByteArray();
                BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
                BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
                if(options.outWidth<500||options.outHeight<750)continue;
                long score=(long)options.outWidth*options.outHeight;
                if(name.contains("wallpaper")||name.contains("lockscreen")||name.contains("lock_screen"))score*=5;
                if(name.contains("preview")||name.contains("thumbnail"))score/=4;
                if(score>bestScore){bestScore=score;best=bytes;}
            }
        }catch(Exception ignored){}
        if(best==null)return null;
        try{
            BitmapFactory.Options o=new BitmapFactory.Options();
            o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(best,0,best.length,o);
            int sample=1;
            while(o.outWidth/sample>2160||o.outHeight/sample>4320)sample*=2;
            o.inJustDecodeBounds=false;o.inSampleSize=sample;
            return BitmapFactory.decodeByteArray(best,0,best.length,o);
        }catch(Exception e){return null;}
    }
    private void applyImported(int which){
        final Bitmap image=imported;if(image==null)return;
        final int flags=which==0?WallpaperManager.FLAG_SYSTEM:which==1?WallpaperManager.FLAG_LOCK:WallpaperManager.FLAG_LOCK|WallpaperManager.FLAG_SYSTEM;
        new Thread(()->{
            try{WallpaperManager.getInstance(this).setBitmap(image,null,true,flags);
                runOnUiThread(()->Toast.makeText(this,"Обои из темы установлены",Toast.LENGTH_LONG).show());
            }catch(Exception e){runOnUiThread(()->Toast.makeText(this,"ColorOS отклонила обои: "+e.getMessage(),Toast.LENGTH_LONG).show());}
        }).start();
    }
    @Override protected void onDestroy(){super.onDestroy();/* Bitmap may be in use by applyImported worker; let GC reclaim it. */}
}
