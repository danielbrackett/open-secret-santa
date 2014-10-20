package com.moac.android.opensecretsanta.activity;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.moac.android.inject.dagger.InjectingActivityBarActivity;
import com.moac.android.opensecretsanta.R;

public abstract class BaseActivity extends InjectingActivityBarActivity {

    private Toolbar mActionBarToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        getActionBarToolbar();
    }

    protected Toolbar getActionBarToolbar() {
        if (mActionBarToolbar == null) {
            mActionBarToolbar = (Toolbar) findViewById(R.id.toolbar_actionBar);
            if (mActionBarToolbar != null) {
                setSupportActionBar(mActionBarToolbar);
            }
        }
        return mActionBarToolbar;
    }
}
