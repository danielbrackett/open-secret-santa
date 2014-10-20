package com.moac.android.opensecretsanta.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.moac.android.inject.dagger.InjectingSupportListFragment;
import com.moac.android.opensecretsanta.R;
import com.moac.android.opensecretsanta.activity.Intents;
import com.moac.android.opensecretsanta.adapter.RestrictionListAdapter;
import com.moac.android.opensecretsanta.adapter.RestrictionRowDetails;
import com.moac.android.opensecretsanta.database.DatabaseManager;
import com.moac.android.opensecretsanta.model.Group;
import com.moac.android.opensecretsanta.model.Member;
import com.moac.android.opensecretsanta.model.Restriction;

import java.util.*;

import javax.inject.Inject;

public class RestrictionsListFragment extends InjectingSupportListFragment implements Saveable {

    private static final String TAG = RestrictionsListFragment.class.getSimpleName();

    // Restriction change actions
    private enum Action {Create, Delete}

    @Inject
    DatabaseManager mDb;

    private Member mFromMember;
    private Group mGroup;
    private Map<Long, Action> mChanges;

    public static RestrictionsListFragment create(long groupId, long fromMemberId) {
        Log.i(TAG, "RestrictionsListFragment() - factory creating for groupId: " + groupId + " fromMemberId: " + fromMemberId);
        RestrictionsListFragment fragment = new RestrictionsListFragment();
        Bundle args = new Bundle();
        args.putLong(Intents.GROUP_ID_INTENT_EXTRA, groupId);
        args.putLong(Intents.MEMBER_ID_INTENT_EXTRA, fromMemberId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        long groupId = getArguments().getLong(Intents.GROUP_ID_INTENT_EXTRA);
        long memberId = getArguments().getLong(Intents.MEMBER_ID_INTENT_EXTRA);
        mGroup = mDb.queryById(groupId, Group.class);
        mFromMember = mDb.queryById(memberId, Member.class);
        mChanges = new HashMap<>();

        TextView titleTextView = (TextView) getView().findViewById(R.id.textView_groupName);
        titleTextView.setText(String.format(getString(R.string.restriction_list_title_unformatted), mFromMember.getName()));

        // TODO Make this load asynchronously
        long fromMemberId = mFromMember.getId();
        List<Member> otherMembers = mDb.queryAllMembersForGroupExcept(mGroup.getId(), fromMemberId);
        List<Restriction> restrictionsForMember = mDb.queryAllRestrictionsForMemberId(fromMemberId);
        Set<Long> restrictions = buildRestrictedMembers(restrictionsForMember);
        List<RestrictionRowDetails> rows = buildRowData(fromMemberId, otherMembers, restrictions);
        ListAdapter adapter = new RestrictionListAdapter(getActivity(), rows, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RestrictionRowDetails details = (RestrictionRowDetails) v.getTag();
                handleRestrictionToggle(details);
            }
        });
        setListAdapter(adapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_restrictions_list, container, false);
    }

    /**
     * Keeps track of changes made to the restrictions without
     * writing to the database.
     */
    private void handleRestrictionToggle(RestrictionRowDetails _details) {
        Log.d(TAG, "handleRestrictionToggle() - start");
        long id = _details.getToMemberId();
        if(mChanges.containsKey(id)) {
            Log.v(TAG, "handleRestrictionToggle() - unmarking change");
            mChanges.remove(id);
        } else {
            Log.v(TAG, "handleRestrictionToggle() - marking change");
            Action action = !_details.isRestricted() ? Action.Create : Action.Delete;
            mChanges.put(id, action);
        }
        Log.d(TAG, "handleRestrictionToggle() - change list:" + mChanges);
    }

    private static List<RestrictionRowDetails> buildRowData(long _fromMemberId, List<Member> _otherMembers, Set<Long> _restrictions) {
        List<RestrictionRowDetails> rows = new ArrayList<RestrictionRowDetails>(_otherMembers.size());
        for(Member other : _otherMembers) {
            RestrictionRowDetails rowDetails = new RestrictionRowDetails();
            rowDetails.setFromMemberId(_fromMemberId);
            rowDetails.setToMemberId(other.getId());
            rowDetails.setRestricted(_restrictions.contains(other.getId()));
            rowDetails.setToMemberName(other.getName());
            rowDetails.setContactId(other.getContactId());
            rowDetails.setLookupKey(other.getLookupKey());
            rows.add(rowDetails);
        }
        return rows;
    }

    // Bit clunky, but probably better than iterating through the List<Restriction> multiple times.
    private static Set<Long> buildRestrictedMembers(List<Restriction> _restrictions) {
        Set<Long> result = new HashSet<Long>();
        for(Restriction restriction : _restrictions) {
            result.add(restriction.getOtherMemberId());
        }
        return result;
    }

    public boolean save() {
        boolean isDirty = !mChanges.isEmpty();
        Log.i(TAG, "doSaveAction() - isDirty: " + isDirty);

        // TODO Make this a background task
        if(isDirty) {
            Log.i(TAG, "doSaveAction() - Restrictions have changed: deleting existing assignments");
            // Restrictions have changed - invalidate the draw.
            mDb.deleteAllAssignmentsForGroup(mGroup.getId());

            for(Map.Entry<Long, Action> entry : mChanges.entrySet()) {
                Log.d(TAG, "doSaveAction() - Change Entry: " + entry);
                if(entry.getValue().equals(Action.Create)) {
                    Log.i(TAG, "doSaveAction() - Adding new Restriction");
                    // Add a new Restriction
                    Restriction restriction = new Restriction();
                    restriction.setMember(mFromMember);
                    restriction.setOtherMemberId(entry.getKey());
                    mDb.create(restriction);
                } else {
                    // Delete restriction
                    Log.i(TAG, "doSaveAction() - Deleting Restriction");
                    mDb.deleteRestrictionBetweenMembers(mFromMember.getId(), entry.getKey());
                }
            }
        }
        return isDirty;
    }
}
