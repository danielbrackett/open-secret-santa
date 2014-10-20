package com.moac.android.opensecretsanta.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.moac.android.opensecretsanta.R;
import com.squareup.picasso.Picasso;

public class RevealDialogFragment extends DialogFragment {

    private static final String TAG = RevealDialogFragment.class.getSimpleName();

    private static final String GIVER_NAME_EXTRA = "GiverName";
    private static final String RECEIVER_NAME_EXTRA = "ReceiverName";
    private static final String AVATAR_URL_EXTRA = "AvatarUrl";

    public static RevealDialogFragment create(String _giverName, String _receiverName, String _avatarUri) {
        RevealDialogFragment fragment = new RevealDialogFragment();
        Bundle args = new Bundle();
        args.putString(GIVER_NAME_EXTRA, _giverName);
        args.putString(RECEIVER_NAME_EXTRA, _receiverName);
        args.putString(AVATAR_URL_EXTRA, _avatarUri);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Get a layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Pass null as the parent view because its going in the dialog layout
        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.fragment_dialog_assignment, null);

        // Extract arguments
        String giverName = getArguments().getString(GIVER_NAME_EXTRA);
        String receiverName = getArguments().getString(RECEIVER_NAME_EXTRA);
        String avatarUrl = getArguments().getString(AVATAR_URL_EXTRA);

        // Set the values
        TextView giver = (TextView) view.findViewById(R.id.giver_name_textview);
        giver.setText(getString(R.string.gift_giving_msg_unformatted, giverName));
        TextView receiver = (TextView) view.findViewById(R.id.receiver_name_textview);
        receiver.setText(receiverName);

        ImageView avatarView = (ImageView) view.findViewById(R.id.receiver_avatar_imageview);
        if(avatarUrl != null) {
            Picasso.with(getActivity()).load(avatarUrl).error(R.drawable.ic_contact_picture).into(avatarView);
        } else {
            Picasso.with(getActivity()).load(R.drawable.ic_contact_picture).into(avatarView);
        }

        builder.setView(view);
        Dialog dialog = builder.create();
        dialog.getWindow().setWindowAnimations(R.style.dialog_animate_overshoot);
        return dialog;
    }
}
