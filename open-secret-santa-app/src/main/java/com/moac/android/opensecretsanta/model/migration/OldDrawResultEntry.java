package com.moac.android.opensecretsanta.model.migration;

/**
 * Created with IntelliJ IDEA.
 * User: amelysh
 * Date: 13.11.13
 * Time: 23:17
 * To change this template use File | Settings | File Templates.
 */

import android.provider.BaseColumns;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.moac.android.opensecretsanta.model.PersistableObject;

@DatabaseTable(tableName = "draw_result_entries")
public class OldDrawResultEntry extends PersistableObject implements Comparable<OldDrawResultEntry> {

    public static interface Columns extends BaseColumns {

        // For entries in that result definition.
        public static final String DRAW_RESULT_ID_COLUMN = "DRAW_RESULT_ID";
        public static final String MEMBER_NAME_COLUMN = "MEMBER_NAME";
        public static final String OTHER_MEMBER_NAME_COLUMN = "OTHER_MEMBER_NAME";
        public static final String CONTACT_MODE_COLUMN = "CONTACT_MODE";
        public static final String CONTACT_DETAIL_COLUMN = "CONTACT_DETAIL";
        public static final String VIEWED_DATE_COLUMN = "VIEWED_DATE";
        public static final String SENT_DATE_COLUMN = "SENT_DATE";

        public static final String DEFAULT_SORT_ORDER = MEMBER_NAME_COLUMN + " ASC";

        public static String[] ALL = { _ID, DRAW_RESULT_ID_COLUMN, MEMBER_NAME_COLUMN,
                OTHER_MEMBER_NAME_COLUMN, CONTACT_MODE_COLUMN, CONTACT_DETAIL_COLUMN, VIEWED_DATE_COLUMN, SENT_DATE_COLUMN };
    }

    @DatabaseField(columnName = Columns.MEMBER_NAME_COLUMN, canBeNull = false)
    private String mGiverName;

    @DatabaseField(columnName = Columns.OTHER_MEMBER_NAME_COLUMN, canBeNull = false)
    private String mReceiverName;

    @DatabaseField(columnName = Columns.CONTACT_MODE_COLUMN)
    private int mContactMode = OldConstants.NAME_ONLY_CONTACT_MODE;

    @DatabaseField(columnName = Columns.CONTACT_DETAIL_COLUMN)
    private String mContactDetail; // the email, the  phone number, the whatever.

    @DatabaseField(columnName = Columns.VIEWED_DATE_COLUMN)
    private long mViewedDate = OldConstants.UNVIEWED_DATE;

    @DatabaseField(columnName = Columns.SENT_DATE_COLUMN)
    private long mSentDate = OldConstants.UNSENT_DATE;

    @DatabaseField(columnName = Columns.DRAW_RESULT_ID_COLUMN, foreign = true, canBeNull = false,
            columnDefinition = "integer references draw_results (_id) on delete cascade")
    private OldDrawResult mDrawResult;

    public String getGiverName() { return mGiverName;}

    public String getReceiverName() { return mReceiverName; }

    public long getViewedDate() { return mViewedDate; }

    public void setGiverName(String name) { mGiverName = name; }

    public void setReceiverName(String name) { mReceiverName = name; }

    public int getContactMode() { return mContactMode; }

    public void setContactMode(int contactMode) { mContactMode = contactMode; }

    public String getContactDetail() { return mContactDetail; }

    public void setContactDetail(String contactDetail) { mContactDetail = contactDetail; }

    public void setViewedDate(long viewedDate) { mViewedDate = viewedDate; }

    public long getSentDate() { return mSentDate; }

    public void setSentDate(long _sentDate) { mSentDate = _sentDate; }

    public void setDrawResult(OldDrawResult drawResult) { mDrawResult = drawResult; }

    public boolean isSendable() {
        return mContactMode == OldConstants.EMAIL_CONTACT_MODE || mContactMode == OldConstants.SMS_CONTACT_MODE;
    }

    @Override
    public int compareTo(OldDrawResultEntry another) {
        return String.CASE_INSENSITIVE_ORDER.compare(this.mGiverName, another.mGiverName);
    }
}