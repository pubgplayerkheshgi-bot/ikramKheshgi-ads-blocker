package com.ikramkheshgi.popupdetector;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
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
        String pkg=getIntent().getStringExtra("package");
        String label=getIntent().getStringExtra("label");
        String reason=getIntent().getStringExtra("reason");
        long time=getIntent().getLongExtra("time",0);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(22),dp(28),dp(22),dp(28)); root.setGravity(Gravity.CENTER_HORIZONTAL); root.setBackgroundColor(Color.rgb(7,17,31));
        ScrollView sv=new ScrollView(this); sv.addView(root);
        ImageView icon=new ImageView(this); try{icon.setImageDrawable(getPackageManager().getApplicationIcon(pkg));}catch(Exception e){icon.setImageResource(android.R.drawable.sym_def_app_icon);} root.addView(icon,new LinearLayout.LayoutParams(dp(92),dp(92)));
        TextView title=t(label==null?pkg:label,25,Color.WHITE); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); title.setGravity(Gravity.CENTER); LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.setMargins(0,dp(16),0,dp(5));root.addView(title,tp);
        TextView packageText=t(pkg,13,Color.rgb(140,175,205));packageText.setGravity(Gravity.CENTER);root.addView(packageText);
        TextView badge=t("POSSIBLE POPUP SOURCE",13,Color.rgb(255,255,255));badge.setGravity(Gravity.CENTER);badge.setPadding(dp(14),dp(9),dp(14),dp(9));GradientDrawable gb=new GradientDrawable();gb.setColor(Color.rgb(190,40,55));gb.setCornerRadius(dp(30));badge.setBackground(gb);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-2,-2);bp.setMargins(0,dp(20),0,dp(18));root.addView(badge,bp);
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(16),dp(16),dp(16));GradientDrawable cb=new GradientDrawable();cb.setColor(Color.rgb(16,30,48));cb.setCornerRadius(dp(18));card.setBackground(cb);
        card.addView(t("Why it was flagged",13,Color.rgb(25,184,255)));card.addView(t(reason==null?"Popup-like window activity detected.":reason,16,Color.WHITE));card.addView(t("Detected: "+DateFormat.getDateTimeInstance().format(new Date(time)),12,Color.rgb(145,175,200)));root.addView(card,new LinearLayout.LayoutParams(-1,-2));
        TextView note=t("This is a detection signal, not a guarantee that the app is malicious. Review the app before uninstalling it.",13,Color.rgb(165,185,205));note.setPadding(dp(4),dp(16),dp(4),dp(12));root.addView(note);
        Button info=new Button(this);info.setText("APP INFO");info.setOnClickListener(v->openAppInfo(pkg));root.addView(info,new LinearLayout.LayoutParams(-1,-2));
        Button uninstall=new Button(this);uninstall.setText("UNINSTALL APP");uninstall.setOnClickListener(v->uninstall(pkg));root.addView(uninstall,new LinearLayout.LayoutParams(-1,-2));
        Button close=new Button(this);close.setText("CLOSE");close.setOnClickListener(v->finish());root.addView(close,new LinearLayout.LayoutParams(-1,-2));
        setContentView(sv);
    }
    private void openAppInfo(String pkg){try{startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:"+pkg)));}catch(Exception ignored){}}
    private void uninstall(String pkg){try{Intent i=new Intent(Intent.ACTION_DELETE,Uri.parse("package:"+pkg));startActivity(i);}catch(Exception ignored){}}
}
