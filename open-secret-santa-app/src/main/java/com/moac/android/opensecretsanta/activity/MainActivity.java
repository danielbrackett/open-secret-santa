package com.moac.android.opensecretsanta.activity;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.common.primitives.Longs;
import com.moac.android.opensecretsanta.OpenSecretSantaApplication;
import com.moac.android.opensecretsanta.R;
import com.moac.android.opensecretsanta.adapter.DrawerButtonItem;
import com.moac.android.opensecretsanta.adapter.DrawerListAdapter;
import com.moac.android.opensecretsanta.adapter.GroupDetailsRow;
import com.moac.android.opensecretsanta.database.DatabaseManager;
import com.moac.android.opensecretsanta.draw.MemberEditor;
import com.moac.android.opensecretsanta.fragment.MemberListFragment;
import com.moac.android.opensecretsanta.fragment.NotifyDialogFragment;
import com.moac.android.opensecretsanta.fragment.NotifyExecutorFragment;
import com.moac.android.opensecretsanta.model.Group;
import com.moac.android.opensecretsanta.model.Member;
import com.moac.android.opensecretsanta.model.PersistableObject;
import com.moac.android.opensecretsanta.notify.NotifyAuthorization;
import com.moac.android.opensecretsanta.util.GroupUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

// FIXME(PT) This class is too big
public class MainActivity extends BaseActivity implements MemberListFragment.FragmentContainer, NotifyDialogFragment.FragmentContainer, MemberEditor {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String MEMBERS_LIST_FRAGMENT_TAG = "MemberListFragment";
    private static final String NOTIFY_DIALOG_FRAGMENT_TAG = "NotifyDialogFragment";
    private static final String NOTIFY_EXECUTOR_FRAGMENT_TAG = "NotifyExecutorFragment";

    @Inject
    DatabaseManager mDb;

    @Inject
    SharedPreferences mSharedPreferences;

    protected DrawerLayout mDrawerLayout;
    protected ActionBarDrawerToggle mDrawerToggle;
    protected ListView mDrawerList;
    private NotifyExecutorFragment mNotifyExecutorFragment;
    private DrawerListAdapter mDrawerListAdapter;
    private long mCurrentGroupId = Group.UNSET_ID;

    @Override
    public void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        findOrCreateWorkerFragment();
        initialiseUI();
    }

    private void findOrCreateWorkerFragment() {
        // Find or create existing worker fragment
        FragmentManager fm = getSupportFragmentManager();
        mNotifyExecutorFragment = (NotifyExecutorFragment) fm.findFragmentByTag(NOTIFY_EXECUTOR_FRAGMENT_TAG);

        if (mNotifyExecutorFragment == null) {
            mNotifyExecutorFragment = NotifyExecutorFragment.create();
            fm.beginTransaction().add(mNotifyExecutorFragment, NOTIFY_EXECUTOR_FRAGMENT_TAG).commit();
        }
    }

    private void initialiseUI() {
        setContentView(R.layout.activity_main);
        mDrawerList = (ListView) findViewById(R.id.left_drawer_list);

        // Add Groups list header - *before adapter is set*
        View headerView = getLayoutInflater().inflate(R.layout.drawer_section_header_view, mDrawerList, false);
        mDrawerList.addHeaderView(headerView);

        mDrawerListAdapter = new DrawerListAdapter(this);
        mDrawerList.setAdapter(mDrawerListAdapter);
        populateDrawerListView(mDrawerListAdapter);
        mDrawerList.setOnItemClickListener(new GroupItemClickListener());

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                getActionBarToolbar(),  /* nav drawer icon to replace 'Up' caret */
                R.string.drawer_open_accesshint,  /* "open drawer" description */
                R.string.drawer_close_accesshint) /* "close drawer" description */ {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(getString(R.string.app_name));
                getSupportActionBar().setIcon(R.drawable.icon);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        //  Fetch the most recently used Group Id from preferences
        long groupId = mSharedPreferences.
                getLong(OpenSecretSantaApplication.MOST_RECENT_GROUP_KEY, PersistableObject.UNSET_ID);

        // Ensure most recent Group is valid
        if (groupId > PersistableObject.UNSET_ID) {
            int adapterPosition = getItemAdapterPosition(mDrawerListAdapter, groupId);
            if (adapterPosition >= 0) {
                // Check the valid list item
                mDrawerList.setItemChecked(toListViewPosition(mDrawerList, adapterPosition), true);
                showGroup(groupId, false);
            } else {
                Log.w(TAG, "Most recent groupId was invalid: " + groupId);
                mSharedPreferences.
                        edit().remove(OpenSecretSantaApplication.MOST_RECENT_GROUP_KEY).apply();
                // Show the drawer to allow Group creation/selection by user
                mDrawerLayout.openDrawer(mDrawerList);
            }
        } else {
            // Show the drawer to allow Group creation by user
            mDrawerLayout.openDrawer(mDrawerList);
        }
    }

    private static int getItemAdapterPosition(DrawerListAdapter adapter, long groupId) {
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItemId(i) == groupId) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.menu_item_settings:
                launchActivityWithSlideIn(AllPreferencesActivity.class);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onEditMember(long _memberId) {
        Bundle extra = new Bundle();
        extra.putLong(Intents.MEMBER_ID_INTENT_EXTRA, _memberId);
        launchActivityWithSlideIn(EditActivity.class, extra);
    }

    @Override
    public void onRestrictMember(long _groupId, long _memberId) {
        Bundle extras = new Bundle();
        extras.putLong(Intents.GROUP_ID_INTENT_EXTRA, _groupId);
        extras.putLong(Intents.MEMBER_ID_INTENT_EXTRA, _memberId);
        launchActivityWithSlideIn(RestrictionsActivity.class, extras);
    }

    @Override
    public void requestNotifyDraw(Group _group, long[] _memberIds) {
        Log.i(TAG, "onNotifyDraw() - Requesting Notify member set size:" + _memberIds.length);
        // Check the requirement for the notify
        NotifyDialogFragment dialog = NotifyDialogFragment.create(_group.getId(), _memberIds);
        dialog.show(getSupportFragmentManager(), NOTIFY_DIALOG_FRAGMENT_TAG);
    }

    @Override
    public void requestNotifyDraw(Group _group) {
        Log.i(TAG, "onNotifyDraw() - Requesting Notify entire Group");
        // TODO Background
        List<Member> members = mDb.queryAllMembersForGroup(_group.getId());
        List<Long> memberIds = new ArrayList<Long>(members.size());
        for (Member member : members) {
            memberIds.add(member.getId());
        }
        requestNotifyDraw(_group, Longs.toArray(memberIds));
    }

    @Override
    public void executeNotifyDraw(NotifyAuthorization auth, final Group group, final long[] members) {
        mNotifyExecutorFragment.notifyDraw(auth, group, members);
    }

    @Override
    public void deleteGroup(long groupId) {
        mDb.delete(groupId, Group.class);
        populateDrawerListView(mDrawerListAdapter);

        // Show the first group, or create another if we have none
        long nextGroupId;
        Group group = mDb.queryForFirstGroup();
        if (group == null) {
            nextGroupId = createNewGroup();
        } else {
            nextGroupId = group.getId();
            int adapterPosition = getItemAdapterPosition(mDrawerListAdapter, nextGroupId);
            mDrawerList.setItemChecked(toListViewPosition(mDrawerList, adapterPosition), true);

        }
        showGroup(nextGroupId, false);
    }

    @Override
    public void renameGroup(long groupId, String newGroupName) {
        mDb.updateGroupName(groupId, newGroupName);
        // Refresh display
        populateDrawerListView(mDrawerListAdapter);
        showGroup(mCurrentGroupId, true);
    }

    private void populateDrawerListView(DrawerListAdapter drawerListAdapter) {

        List<DrawerListAdapter.Item> drawerListItems = new ArrayList<DrawerListAdapter.Item>();

        // Add "Add Group" button item
        Drawable addIcon = getResources().getDrawable(R.drawable.ic_content_new);
        drawerListItems.add(new DrawerButtonItem(addIcon, getString(R.string.add_group), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Clicked Add Group Button");
                long id = createNewGroup();
                showGroup(id, false);
                mDrawerLayout.closeDrawer(mDrawerList);
            }
        }));

        // Add each Group item
        List<Group> groups = mDb.queryAll(Group.class);
        Log.v(TAG, "initialiseUI() - group count: " + groups.size());
        for (Group g : groups) {
            drawerListItems.add(new GroupDetailsRow(g.getId(), g.getName(), g.getCreatedAt()));
        }
        drawerListAdapter.clear();
        drawerListAdapter.addAll(drawerListItems);
    }

    private long createNewGroup() {
        Log.i(TAG, "Creating new Group");
        // Create a new Group with incrementing name
        Group group = GroupUtils.createIncrementingGroup(mDb, getString(R.string.base_group_name));

        // Create corresponding adapter view model entry
        GroupDetailsRow item = new GroupDetailsRow(group.getId(), group.getName(), group.getCreatedAt());
        mDrawerListAdapter.add(item);

        // Select the item in the list view
        int adapterPosition = mDrawerListAdapter.getPosition(item);
        mDrawerList.setItemChecked(toListViewPosition(mDrawerList, adapterPosition), true);

        return group.getId();
    }

    /*
     *
     * The DrawerList includes the header as a position, so when
     * we request to check select a position using the indices from the
     * adapter we need to offset it by +1 to correctly translate into the
     * ListView index space.
     */
    private static int toListViewPosition(ListView list, int adapterPosition) {
        return adapterPosition + list.getHeaderViewsCount();
    }

    @Override
    public MemberEditor getMemberEditor() {
        // FIXME for now implement as this activity.
        return this;
    }

    private class GroupItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> _parent, View _view, int _position, long _id) {
            Log.d(TAG, "onItemClick() - position: " + _position + " id: " + _id);
            if (_id <= PersistableObject.UNSET_ID)
                return;

            // Highlight the selected item, update the title, and close the drawer
            showGroup(_id, false);
            mDrawerList.setItemChecked(_position, true);
            mDrawerLayout.closeDrawer(mDrawerList);
        }
    }

    private void showGroup(long _groupId, boolean forceUpdate) {
        Log.i(TAG, "showGroup() - start. groupId: " + _groupId);

        //  If the correct fragment already exists
        if (_groupId == mCurrentGroupId && !forceUpdate) return;

        mCurrentGroupId = _groupId;

        FragmentManager fragmentManager = getSupportFragmentManager();
        MemberListFragment existing = (MemberListFragment) fragmentManager.findFragmentByTag(MEMBERS_LIST_FRAGMENT_TAG);

        // Replace existing MemberListFragment
        // Note: Can't call replace, seems to replace ALL fragments in the layout.
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (existing != null) {
            Log.i(TAG, "showGroup() - removing existing fragment");
            transaction.remove(existing);
        }

        Log.i(TAG, "showGroup() - creating new fragment");
        MemberListFragment newFragment = MemberListFragment.create(_groupId);
        transaction.add(R.id.container_content, newFragment, MEMBERS_LIST_FRAGMENT_TAG)
                .commit();

        // Update preferences to save last viewed Group
        mSharedPreferences.
                edit().putLong(OpenSecretSantaApplication.MOST_RECENT_GROUP_KEY, _groupId).apply();
    }

    private void launchActivityWithSlideIn(Class<? extends Activity> activityClass) {
        launchActivityWithSlideIn(activityClass, null);
    }

    private void launchActivityWithSlideIn(Class<? extends Activity> activityClass, Bundle extras) {

        Intent intent = new Intent(this, activityClass);
        if (extras != null) {
            intent.putExtras(extras);
        }

        // Activity options is since API 16.
        // Refer: Android Dev Bytes video - https://www.youtube.com/watch?v=Ho8vk61lVIU
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
        } else {
            Bundle translateBundle = ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_left, R.anim.slide_out_left).toBundle();
            startActivity(intent, translateBundle);
        }
    }
}