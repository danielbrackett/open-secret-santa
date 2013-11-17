package com.moac.android.opensecretsanta.fragment;

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import com.moac.android.opensecretsanta.OpenSecretSantaApplication;
import com.moac.android.opensecretsanta.R;
import com.moac.android.opensecretsanta.activity.Intents;
import com.moac.android.opensecretsanta.adapter.AccountAdapter;
import com.moac.android.opensecretsanta.database.DatabaseManager;
import com.moac.android.opensecretsanta.model.Group;
import com.moac.android.opensecretsanta.notify.EmailAuthorization;
import com.moac.android.opensecretsanta.notify.NotifyAuthorization;
import com.moac.android.opensecretsanta.util.AccountUtils;
import rx.Observable;
import rx.android.concurrency.AndroidSchedulers;
import rx.concurrency.Schedulers;
import rx.util.functions.Action1;

public class NotifyDialogFragment extends DialogFragment {

    private static final String TAG = NotifyDialogFragment.class.getSimpleName();
    private static final String MESSAGE_KEY = "message";

    protected EditText mMsgField;
    protected DatabaseManager mDb;
    protected Group mGroup;
    protected long[] mMemberIds;

    // Apparently this is how you retain EditText fields - http://code.google.com/p/android/issues/detail?id=18719
    private String mSavedMsg;
    private FragmentContainer mFragmentContainer;
    private Spinner mSpinner;

    /**
     * Factory method for this fragment class
     *
     * We do this because according to the Fragment docs -
     *
     * "It is strongly recommended that subclasses do not have other constructors with parameters"
     */
    public static NotifyDialogFragment create(long _groupId, long[] _memberIds) {
        Log.i(TAG, "NotifyDialogFragment() - factory creating for groupId: " + _groupId + " memberIds: " + _memberIds);
        NotifyDialogFragment fragment = new NotifyDialogFragment();
        Bundle args = new Bundle();
        args.putLong(Intents.GROUP_ID_INTENT_EXTRA, _groupId);
        args.putLongArray(Intents.MEMBER_ID_ARRAY_INTENT_EXTRA, _memberIds);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Log.i(TAG, "onCreateDialog() - start: " + this);
        mDb = OpenSecretSantaApplication.getInstance().getDatabase();
        long groupId = getArguments().getLong(Intents.GROUP_ID_INTENT_EXTRA);
        mMemberIds = getArguments().getLongArray(Intents.MEMBER_ID_ARRAY_INTENT_EXTRA);
        mGroup = mDb.queryById(groupId, Group.class);

        String message = mSavedMsg == null ? mGroup.getMessage() :
          savedInstanceState.getString(MESSAGE_KEY);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Inflate layout
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_dialog_notify, null);

        // Take the values and populate the dialog
        builder.setTitle("Notify Group");
        builder.setIcon(R.drawable.ic_menu_notify);

        mMsgField = (EditText) view.findViewById(R.id.tv_notify_msg);
        mMsgField.setText(message);

        final TextView charCountView = (TextView) view.findViewById(R.id.tv_notify_msg_char_count);
        charCountView.setText(String.valueOf(mMsgField.length()));

        // Add the callback to the field
        mMsgField.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Update the reported character length
                charCountView.setText(String.valueOf(s.length()));
            }
        });

        mSpinner = (Spinner) view.findViewById(R.id.spnr_email_selection);

        // Add all Gmail accounts to list
        final Observable<Account[]> accountsObservable = AccountUtils.getAllGmailAccountsObservable(getActivity());
        accountsObservable.
          subscribeOn(Schedulers.newThread()).
          observeOn(AndroidSchedulers.mainThread()).
          subscribe(new Action1<Account[]>() {
              @Override
              public void call(Account[] accounts) {
                  Log.i(TAG, "Found accounts: " + accounts);
                  AccountAdapter aa = new AccountAdapter(getActivity(), accounts);
                  mSpinner.setAdapter(aa);
                  // TODO Set to preference
              }
          });

//        if(mMemberIds != null) {
//            LinearLayout container = (LinearLayout) view.findViewById(R.id.layout_avatar_container);
//            for(long id : mMemberIds) {
//                Member member = mDb.queryById(id, Member.class);
//                Uri uri = member.getContactUri(getActivity());
//                if(uri != null) {
//                    Log.v(TAG, "onCreateDialog() - adding avatar: " + member.getName());
//                    Log.v(TAG, "onCreateDialog() - uri: " + uri);
//                    ImageView avatar = new ImageView(getActivity());
//                    avatar.setLayoutParams(new LinearLayout.LayoutParams(80, 80));
//                    avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
//                    container.addView(avatar);
//                    Picasso.with(getActivity()).load(uri).into(avatar);
//                }
//            }
//        }

        builder.setCancelable(true);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Handle NO EMAIL required etc
                final NotifyAuthorization.Builder auth = new NotifyAuthorization.Builder();
                // Set the selected email as the user preference
                Account acc = (Account)mSpinner.getSelectedItem();
                String emailPrefKey = getActivity().getString(R.string.gmail_account_preference);
                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString(emailPrefKey, acc.name).commit();

                // Get the custom message.
                mGroup.setMessage(mMsgField.getText().toString().trim());
                mDb.update(mGroup);

                // TODO Check if required
                AccountUtils.getPreferedGmailAuth(getActivity().getApplicationContext(), getActivity()).
                  subscribeOn(Schedulers.newThread()).
                  observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<EmailAuthorization>() {
                    @Override
                    public void call(EmailAuthorization emailAuth) {
                        Log.d(TAG, "call() - got EmailAuthorization: " + emailAuth.getEmailAddress() + ":" + emailAuth.getToken());
                        auth.withAuth(emailAuth);
                        executeNotifyDraw(auth.build(), mGroup, mMemberIds);
                    }
                });
            }
        });

        builder.setView(view);
        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.i(TAG, "onSaveInstanceState()");
        super.onSaveInstanceState(outState);
        outState.putString(MESSAGE_KEY, mMsgField.getText().toString());
        Log.d(TAG, "onSaveInstanceState() msg: " + outState.getString(MESSAGE_KEY));
        mSavedMsg = outState.getString(MESSAGE_KEY);
    }

    @Override
    public void onDestroyView() {
        // Refer to - http://code.google.com/p/android/issues/detail?id=17423
        if(getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mFragmentContainer = (FragmentContainer) activity;
        } catch(ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement FragmentContainer");
        }
    }

    void executeNotifyDraw(NotifyAuthorization auth, Group group, long[] members) {
        mFragmentContainer.executeNotifyDraw(auth, group, members);
    }

    public interface FragmentContainer {
        public void executeNotifyDraw(NotifyAuthorization auth, Group group, long[] members);
    }
}
