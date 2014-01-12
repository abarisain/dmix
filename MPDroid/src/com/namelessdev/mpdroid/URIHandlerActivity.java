
package com.namelessdev.mpdroid;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.namelessdev.mpdroid.fragments.StreamsFragment;

public class URIHandlerActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_urihandler);
        if (!getIntent().getAction().equals("android.intent.action.VIEW")) {
            finish();
        }
        final StreamsFragment sf = (StreamsFragment) getSupportFragmentManager().findFragmentById(
                R.id.streamsFragment);
        sf.addEdit(-1, getIntent().getDataString());
    }

}
