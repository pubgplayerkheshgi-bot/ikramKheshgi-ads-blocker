package com.ikramkheshgi.popupdetector;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;
import java.text.DateFormat;
import java.util.Date;

public class DetectionDetailsActivity extends Activity {
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private TextView t(String s,float z,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);return v;}
    @Override protected void onCreate(Bundle b){super.onCreate(b);
        String pkg=getIntent().getStringExtra("package"); String label=getIntent().getStringExtra("label"); String reason=getIntent().getStringExtra("reason"); long time=getIntent().getLongExtra("time",0);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20),dp(24),dp(20),dp(24)); root.setGravity(Gravity.CENTER_HORIZONTAL); root.setBackgroundColor(Color.rgb(7,17,31));
        ScrollView sv=new ScrollView(this); sv.addView(root);
        ImageView icon=new ImageView(this); try{icon.setImageDrawable(getPackageManager().getApplicationIcon(pkg));}catch(Exception e){icon.setImageResource(android.R.drawable.sym_def_app_icon);} root.addView(icon,new LinearLayout.LayoutParams(dp(88),dp(88)));
        TextView title=t(label==null?pkg:label,24,Color.WHITE); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); title.setGravity(Gravity.CENTER); root.addView(title,new LinearLayout.LayoutParams(-1,-2));
        TextView p=t(pkg==null?"":pkg,12,Color.rgb(145,175,200)); p.setGravity(Gravity.CENTER); root.addView(p);
        TextView badge=t("POSSIBLE POPUP SOURCE",13,Color.WHITE); badge.setGravity(Gravity.CENTER); badge.setPadding(dp(15),dp(9),dp(15),dp(9)); GradientDrawable gb=new GradientDrawable();gb.setColor(Color.rgb(190,40,55));gb.setCornerRadius(40);badge.setBackground(gb); LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-2,-2);bp.setMargins(0,dp(16),0,dp(14));root.addView(badge,bp);
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(15),dp(16),dp(15));GradientDrawable cb=new GradientDrawable();cb.setColor(Color.rgb(16,30,48));cb.setCornerRadius(dp(18));card.setBackground(cb);
        card.addView(t("WHY IT WAS FLAGGED",12,Color.rgb(25,184,255))); card.addView(t(reason==null?"Popup-style activity detected.":reason,16,Color.WHITE)); card.addView(t("Detected: "+DateFormat.getDateTimeInstance().format(new Date(time)),12,Color.rgb(145,175,200))); root.addView(card,new LinearLayout.LayoutParams(-1,-2));
        TextView note=t("This is a detection signal, not a guarantee that the app is malicious. Review the app before uninstalling it.",13,Color.rgb(165,185,205));note.setPadding(dp(4),dp(15),dp(4),dp(10));root.addView(note);
        Button info=new Button(this);info.setText("APP INFO");info.setOnClickListener(v->openAppInfo(pkg));root.addView(info,new LinearLayout.LayoutParams(-1,-2));
        Button whitelist=new Button(this);whitelist.setText(DetectionStore.isWhitelisted(this,pkg)?"REMOVE FROM WHITELIST":"ADD TO WHITELIST");whitelist.setOnClickListener(v->{boolean x=!DetectionStore.isWhitelisted(this,pkg);DetectionStore.setWhitelisted(this,pkg,x);whitelist.setText(x?"REMOVE FROM WHITELIST":"ADD TO WHITELIST");});root.addView(whitelist,new LinearLayout.LayoutParams(-1,-2));
        Button uninstall=new Button(this);uninstall.setText("UNINSTALL APP");uninstall.setOnClickListener(v->uninstall(pkg));root.addView(uninstall,new LinearLayout.LayoutParams(-1,-2));
        Button close=new Button(this);close.setText("CLOSE");close.setOnClickListener(v->finish());root.addView(close,new LinearLayout.LayoutParams(-1,-2));
        setContentView(sv);
    }
    private void openAppInfo(String pkg){try{startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:"+pkg)));}catch(Exception ignored){}}
    private void uninstall(String pkg){try{startActivity(new Intent(Intent.ACTION_DELETE,Uri.parse("package:"+pkg)));}catch(Exception ignored){}}
}
