package com.ikramkheshgi.popupdetector;

import android.app.Activity;
import android.os.Bundle;

public class AddWhitelistActivity extends Activity { @Override protected void onCreate(Bundle b){super.onCreate(b);startActivity(new android.content.Intent(this,WhitelistActivity.class));finish();} }
