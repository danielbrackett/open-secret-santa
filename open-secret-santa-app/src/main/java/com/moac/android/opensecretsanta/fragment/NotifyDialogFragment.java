package com.moac.android.opensecretsanta.fragment;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.moac.android.inject.dagger.InjectingSupportDialogFragment;
import com.moac.android.opensecretsanta.R;
import com.moac.android.opensecretsanta.activity.Intents;
import com.moac.android.opensecretsanta.adapter.AccountAdapter;
import com.moac.android.opensecretsanta.database.DatabaseManager;
import com.moac.android.opensecretsanta.model.Group;
import com.moac.android.opensecretsanta.notify.NotifyAuthorization;
import com.moac.android.opensecretsanta.notify.mail.EmailAuthorization;
import com.moac.android.opensecretsanta.util.AccountUtils;
import com.moac.android.opensecretsanta.util.NotifyUtils;

import java.util.Arrays;

import javax.inject.Inject;

import rx.Observable;
import rx.android.concurrency.AndroidSchedulers;
import rx.concurrency.Schedulers;
import rx.util.functions.Action1;

public class NotifyDialogFragment extends InjectingSupportDialogFragment {

    private static final String TAG = NotifyDialogFragment.class.getSimpleName();
    private static final String MESSAGE_KEY = "message";

    @Inject
    DatabaseManager mDb;

    @Inject
    SharedPreferences mSharedPreferences;

    @Inject
    AccountManager mAccountManager;

    protected EditText mMsgField;
    protected Group mGroup;

    protected long[] mMemberIds;
    private FragmentContainer mFragmentContainer;
    private Spinner mSpinner;
    private TextView mInfoTextView;
    private boolean mIsEmailAuthRequired;
    private ViewGroup mEmailFromContainer;

    private int mMaxMsgLength;
    // Apparently this is how you retain EditText fields in Dialogs - http://code.google.com/p/android/issues/detail?id=18719
    private String mSavedMsg;
    private TextView mCharCountView;

    public static NotifyDialogFragment create(long _groupId, long[] _memberIds) {
        Log.i(TAG, "NotifyDialogFragment() - factory creating for groupId: " + _groupId + " memberIds: " + Arrays.toString(_memberIds));
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
        mMemberIds = getArguments().getLongArray(Intents.MEMBER_ID_ARRAY_INTENT_EXTRA);
        mMaxMsgLength = getResources().getInteger(R.integer.max_notify_msg_length);


        // Inflate layout
        LayoutInflater inflater = getActivity().getLayoutInflater();
        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.fragment_dialog_notify, null);

        // Configure the views
        mMsgField = (EditText) view.findViewById(R.id.tv_notify_msg);
        mCharCountView = (TextView) view.findViewById(R.id.tv_notify_msg_char_count);

        // Add the callback to the field
        mMsgField.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Update the reported character length
                mCharCountView.setText(String.valueOf(mMaxMsgLength - s.length()));
                if (mMsgField.length() == mMaxMsgLength) {
                    mCharCountView.setTextColor(Color.RED);
                } else {
                    mCharCountView.setTextColor(getResources().getColor(R.color.dark_grey));
                }
            }
        });

        // Visibility GONE by default
        mEmailFromContainer = (ViewGroup) view.findViewById(R.id.layout_notify_email_container);
        mSpinner = (Spinner) view.findViewById(R.id.spnr_email_selection);
        mInfoTextView = (TextView) view.findViewById(R.id.tv_notify_info);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setTitle(getString(R.string.notify_dialog_title))
                .setIcon(R.drawable.ic_menu_notify_group)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.notify_send_button_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        final NotifyAuthorization.Builder auth = new NotifyAuthorization.Builder();

                        if (mIsEmailAuthRequired) {
                            Account acc = (Account) mSpinner.getSelectedItem();
                            if (acc != null) {
                                // Set the selected email as the user preference
                                String emailPrefKey = getActivity().getString(R.string.gmail_account_preference);
                                mSharedPreferences.edit().putString(emailPrefKey, acc.name).apply();

                                AccountUtils.getPreferedGmailAuth(getActivity(), mAccountManager, mSharedPreferences, getActivity()).
                                        subscribeOn(Schedulers.newThread()).
                                        observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<EmailAuthorization>() {
                                    @Override
                                    public void call(EmailAuthorization emailAuth) {
                                        Log.d(TAG, "call() - got EmailAuthorization: " + emailAuth.getEmailAddress() + ":" + emailAuth.getToken());
                                        auth.withAuth(emailAuth);
                                        mGroup.setMessage(mMsgField.getText().toString().trim());
                                        mDb.update(mGroup);
                                        executeNotifyDraw(auth.build(), mGroup, mMemberIds);
                                    }
                                });
                            }  // else no email auth available - do nothing.
                        } else {
                            // We have no additional authorization - just send as is
                            // Get the custom message.
                            mGroup.setMessage(mMsgField.getText().toString().trim());
                            mDb.update(mGroup);
                            executeNotifyDraw(auth.build(), mGroup, mMemberIds);
                        }

                    }

                });

        Dialog dialog = builder.create();
        dialog.getWindow().setWindowAnimations(R.style.dialog_animate_overshoot);
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Populate those field that require injected dependencies
        long groupId = getArguments().getLong(Intents.GROUP_ID_INTENT_EXTRA);
        mGroup = mDb.queryById(groupId, Group.class);
        String message = mSavedMsg == null ? mGroup.getMessage() :
                savedInstanceState.getString(MESSAGE_KEY);

        mMsgField.append(message);
        int remainingChars = mMaxMsgLength;
        if (message != null) {
            remainingChars = message.length() >= mMaxMsgLength ? 0 : mMaxMsgLength - message.length();
        }
        mCharCountView.setText(String.valueOf(remainingChars));

        mIsEmailAuthRequired = NotifyUtils.containsEmailSendableEntry(mDb, mMemberIds);
        if (mIsEmailAuthRequired) {
            // Add all Gmail accounts to list
            final Observable<Account[]> accountsObservable = AccountUtils.getAllGmailAccountsObservable(getActivity(), mAccountManager);
            accountsObservable.
                    subscribeOn(Schedulers.newThread()).
                    observeOn(AndroidSchedulers.mainThread()).
                    subscribe(new Action1<Account[]>() {
                                  @Override
                                  public void call(Account[] accounts) {
                                      AccountAdapter aa = new AccountAdapter(getActivity(), accounts);
                                      mSpinner.setAdapter(aa);
                                      mEmailFromContainer.setVisibility(View.VISIBLE);
                                      // TODO Set to preference
                                  }
                              },
                            new Action1<Throwable>() {
                                @Override
                                public void call(Throwable throwable) {
                                    mInfoTextView.setText(throwable.getMessage());
                                    mInfoTextView.setVisibility(View.VISIBLE);
                                }
                            }
                    );
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(MESSAGE_KEY, mMsgField.getText().toString());
        Log.d(TAG, "onSaveInstanceState() msg: " + outState.getString(MESSAGE_KEY));
        mSavedMsg = outState.getString(MESSAGE_KEY);
    }

    @Override
    public void onDestroyView() {
        // Refer to - http://code.google.com/p/android/issues/detail?id=17423
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mFragmentContainer = (FragmentContainer) activity;
        } catch (ClassCastException e) {
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
