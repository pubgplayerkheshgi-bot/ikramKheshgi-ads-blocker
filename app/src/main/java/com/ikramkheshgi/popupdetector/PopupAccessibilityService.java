package com.ikramkheshgi.popupdetector;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Display;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Background detector. Important: normal content-changed events are NOT treated as detections.
 * A detection requires popup-like evidence (ad text, notification text, or a small overlay-style
 * window from an app with overlay capability while another app remains underneath).
 */
public class PopupAccessibilityService extends AccessibilityService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String,Long> cooldown = new HashMap<>();
    private WindowManager wm;
    private Context windowContext;
    private TextView floating;
    private String foregroundPackage = "";
    private String lastSourcePackage = "";
    private long lastForegroundChange;
    private JSONObject pending;

    private static final long COOLDOWN_MS = 12000L;
    private static final String[] AD_WORDS = {
            "advertisement","advertising","sponsored","sponsored content","skip ad",
            "close ad","rewarded ad","reward ad","ad choices","ads by","install now",
            "download now","open ad","learn more","continue to app","remove ads"
    };

    @Override protected void onServiceConnected(){
        super.onServiceConnected();
        AccessibilityServiceInfo i=new AccessibilityServiceInfo();
        i.eventTypes=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | AccessibilityEvent.TYPE_WINDOWS_CHANGED | AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
        i.feedbackType=AccessibilityServiceInfo.FEEDBACK_GENERIC;
        i.flags=AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS | AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        i.notificationTimeout=100;
        setServiceInfo(i);
        if(Settings.canDrawOverlays(this)) showFloating();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e){
        if(e==null) return;
        String pkg=e.getPackageName()==null?"":e.getPackageName().toString();
        if(pkg.isEmpty() || pkg.equals(getPackageName()) || isSystemApp(pkg)) return;
        int type=e.getEventType();
        if(type==AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED){
            String text=collectEventText(e);
            if(containsAdWord(text) && requestsOverlay(pkg)) detect(pkg,"Ad-like notification text: "+shorten(text,150));
            return;
        }
        if(type!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && type!=AccessibilityEvent.TYPE_WINDOWS_CHANGED) return;

        boolean changed=!pkg.equals(foregroundPackage);
        if(changed){
            String previous=foregroundPackage;
            foregroundPackage=pkg;
            lastForegroundChange=System.currentTimeMillis();
            String text=collectEventText(e);
            if(containsAdWord(text)){
                detect(pkg,"Ad-like text: "+shorten(text,150));
                return;
            }
            if(previous!=null && !previous.isEmpty() && !previous.equals(pkg) && requestsOverlay(pkg)){
                handler.postDelayed(()->inspectForOverlayPopup(pkg,previous),180);
            }
        } else if(type==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){
            String text=collectEventText(e);
            if(containsAdWord(text) && requestsOverlay(pkg)) detect(pkg,"Ad-like text: "+shorten(text,150));
        }
    }

    private void inspectForOverlayPopup(String pkg,String underlying){
        if(!pkg.equals(foregroundPackage)) return;
        boolean small=false, hasUnderlying=false;
        try{
            for(AccessibilityWindowInfo w:getWindows()){
                if(w==null) continue;
                String wp=w.getRoot()==null?null:null;
                AccessibilityNodeInfo r=w.getRoot();
                String wPkg=r!=null && r.getPackageName()!=null?r.getPackageName().toString():"";
                Rect b=new Rect(); w.getBoundsInScreen(b);
                long area=(long)b.width()*b.height();
                if(pkg.equals(wPkg) && area>0){
                    int sw=getResources().getDisplayMetrics().widthPixels;
                    int sh=getResources().getDisplayMetrics().heightPixels;
                    if(area < (long)(sw*sh*0.82f)) small=true;
                }
                if(underlying.equals(wPkg)) hasUnderlying=true;
                if(r!=null) r.recycle();
            }
        }catch(Exception ignored){}
        // Stronger signal: app can draw overlays + its visible window is small while previous app remains.
        if(small && hasUnderlying) detect(pkg,"Small overlay-style window appeared above "+appLabel(underlying));
    }

    private String collectEventText(AccessibilityEvent e){
        StringBuilder b=new StringBuilder();
        for(CharSequence c:e.getText()) if(c!=null) b.append(c).append(' ');
        CharSequence d=e.getContentDescription(); if(d!=null)b.append(d).append(' ');
        AccessibilityNodeInfo s=e.getSource();
        if(s!=null){ collectNodeText(s,b,0); s.recycle(); }
        return b.toString().toLowerCase(Locale.ROOT).trim();
    }
    private void collectNodeText(AccessibilityNodeInfo n,StringBuilder b,int depth){
        if(n==null||depth>8||b.length()>4000)return;
        CharSequence t=n.getText(); if(t!=null)b.append(t).append(' ');
        CharSequence d=n.getContentDescription(); if(d!=null)b.append(d).append(' ');
        for(int x=0;x<n.getChildCount()&&x<30;x++){AccessibilityNodeInfo c=n.getChild(x);if(c!=null){collectNodeText(c,b,depth+1);c.recycle();}}
    }
    private boolean containsAdWord(String s){for(String w:AD_WORDS)if(s.contains(w))return true;return false;}
    private boolean requestsOverlay(String pkg){
        try{PackageInfo p=getPackageManager().getPackageInfo(pkg,PackageManager.GET_PERMISSIONS);if(p.requestedPermissions!=null)for(String x:p.requestedPermissions)if("android.permission.SYSTEM_ALERT_WINDOW".equals(x))return true;}catch(Exception ignored){}return false;
    }
    private boolean isSystemApp(String pkg){
        try{ApplicationInfo a=getPackageManager().getApplicationInfo(pkg,0);return (a.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0;}catch(Exception e){return true;}
    }
    private String appLabel(String pkg){try{return getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(Exception e){return pkg;}}
    private String shorten(String s,int n){return s.length()<=n?s:s.substring(0,n)+"…";}

    private void detect(String pkg,String reason){
        long now=System.currentTimeMillis();
        Long old=cooldown.get(pkg); if(old!=null && now-old<COOLDOWN_MS)return;
        cooldown.put(pkg,now);
        String label=appLabel(pkg);
        DetectionStore.add(this,pkg,label,reason,now);
        pending=new JSONObject(); try{pending.put("package",pkg);pending.put("label",label);pending.put("reason",reason);pending.put("time",now);}catch(Exception ignored){}
        updateFloating(true,label);
        Toast.makeText(this,"Possible popup source: "+label,Toast.LENGTH_LONG).show();
    }

    private void showFloating(){
        if(floating!=null || !Settings.canDrawOverlays(this))return;
        try{
            wm=(WindowManager)getSystemService(WINDOW_SERVICE);
            floating=new TextView(this);
            floating.setText("✓"); floating.setTextColor(Color.WHITE); floating.setTextSize(16); floating.setGravity(Gravity.CENTER);
            floating.setPadding(18,12,18,12); floating.setContentDescription("Popup detector");
            setBubble(false);
            floating.setOnClickListener(v->{if(pending!=null)openDetails();else openMain();});
            int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;
            WindowManager.LayoutParams p=new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,PixelFormat.TRANSLUCENT);
            p.gravity=Gravity.RIGHT|Gravity.CENTER_VERTICAL;p.x=12;p.y=0;
            wm.addView(floating,p);
        }catch(Exception e){floating=null;}
    }
    private void setBubble(boolean alert){
        if(floating==null)return; GradientDrawable g=new GradientDrawable();g.setColor(alert?Color.rgb(205,42,55):Color.rgb(24,94,140));g.setCornerRadius(80);floating.setBackground(g);floating.setText(alert?"!":"✓");
    }
    private void updateFloating(boolean alert,String label){if(floating==null)showFloating();if(floating!=null){setBubble(alert);floating.setContentDescription(alert?"Possible popup from "+label:"Popup detector ready");}}
    private void openDetails(){
        Intent i=new Intent(this,DetectionDetailsActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("package",pending.optString("package"));i.putExtra("label",pending.optString("label"));i.putExtra("reason",pending.optString("reason"));i.putExtra("time",pending.optLong("time"));startActivity(i);
    }
    private void openMain(){Intent i=new Intent(this,MainActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);startActivity(i);}
    @Override public void onInterrupt(){}
    @Override public void onDestroy(){if(floating!=null&&wm!=null)try{wm.removeView(floating);}catch(Exception ignored){}floating=null;super.onDestroy();}
}
