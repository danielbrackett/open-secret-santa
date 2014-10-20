package com.moac.android.opensecretsanta.activity;

import android.os.Bundle;

import com.moac.android.opensecretsanta.R;
import com.moac.android.opensecretsanta.fragment.MemberEditFragment;
import com.moac.android.opensecretsanta.fragment.RestrictionsListFragment;
import com.moac.android.opensecretsanta.fragment.Saveable;
import com.moac.android.opensecretsanta.model.PersistableObject;
/**
 * Host the RestrictionsListFragment
 */
public class RestrictionsActivity extends BaseEditorActivity {

    @Override
    protected void createEditorFragment(Bundle savedInstance) {
        if (savedInstance == null) {
            // Add the restrictions list fragment
            final long groupId = getIntent().getLongExtra(Intents.GROUP_ID_INTENT_EXTRA, PersistableObject.UNSET_ID);
            final long memberId = getIntent().getLongExtra(Intents.MEMBER_ID_INTENT_EXTRA, PersistableObject.UNSET_ID);
            RestrictionsListFragment fragment = RestrictionsListFragment.create(groupId, memberId);
            getSupportFragmentManager().beginTransaction().add(R.id.container_content, fragment).commit();
            mSaveableFragment = fragment;
        } else {
            mSaveableFragment = (Saveable) getSupportFragmentManager().findFragmentByTag(MemberEditFragment.class.getName());
        }
    }
}
