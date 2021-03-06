/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */
package com.mediatek.contacts.list;

import hb.provider.ContactsContract.RawContacts;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.preference.PreferenceManager;
import hb.provider.ContactsContract;
import hb.provider.ContactsContract.CommonDataKinds.Email;
import hb.provider.ContactsContract.CommonDataKinds.Phone;
import hb.provider.ContactsContract.Contacts;
import hb.provider.ContactsContract.Data;
import hb.provider.ContactsContract.Directory;
import hb.provider.ContactsContract.SearchSnippets;
import android.text.TextUtils;
//import android.util.Log;
import android.view.View;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import com.android.contacts.common.list.ContactListFilter;
import com.android.contacts.common.list.ContactListItemView;
import com.android.contacts.common.preference.ContactsPreferences;

import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.ContactsCommonListUtils;
import com.mediatek.contacts.util.Log;

public class PhoneAndEmailsPickerAdapter extends DataKindBasePickerAdapter {
    private static final String TAG = "PhoneAndEmailsPickerAdapter";

    public static final char SNIPPET_START_MATCH = '\u0001';
    public static final char SNIPPET_END_MATCH = '\u0001';
    public static final String SNIPPET_ELLIPSIS = "\u2026";
    public static final int SNIPPET_MAX_TOKENS = 5;

    public static final String SNIPPET_ARGS = SNIPPET_START_MATCH + "," + SNIPPET_END_MATCH + ","
            + SNIPPET_ELLIPSIS + "," + SNIPPET_MAX_TOKENS;

    public static final Uri PICK_PHONE_EMAIL_URI = Uri
            .parse("content://com.android.contacts/data/phone_email");

    public static final Uri PICK_PHONE_EMAIL_FILTER_URI = Uri.withAppendedPath(
            PICK_PHONE_EMAIL_URI, "filter");

    static final String[] PHONE_EMAIL_PROJECTION = new String[] { Phone._ID, // 0
            Phone.TYPE, // 1
            Phone.LABEL, // 2
            Phone.NUMBER, // 3
            Phone.DISPLAY_NAME_PRIMARY, // 4
            Phone.DISPLAY_NAME_ALTERNATIVE, // 5
            Phone.CONTACT_ID, // 6
            Phone.LOOKUP_KEY, // 7
            Phone.PHOTO_ID, // 8
            Phone.PHONETIC_NAME, // 9
            Phone.MIMETYPE, // 10
            Contacts.INDICATE_PHONE_SIM, // 11
            Contacts.IS_SDN_CONTACT, // 12
            "quanpinyin",//13
            "jianpinyin",//14
            "phonebook_bucket",//15

    };

    protected static final int PHONE_EMAIL_ID_INDEX = 0;
    protected static final int PHONE_EMAIL_TYPE_INDEX = 1;
    protected static final int PHONE_EMAIL_LABEL_INDEX = 2;
    protected static final int PHONE_EMAIL_NUMBER_INDEX = 3;
    protected static final int PHONE_EMAIL_PRIMARY_DISPLAY_NAME_INDEX = 4;
    protected static final int PHONE_EMAIL_ALTERNATIVE_DISPLAY_NAME_INDEX = 5;
    protected static final int PHONE_EMAIL_CONTACT_ID_INDEX = 6;
    protected static final int PHONE_EMAIL_LOOKUPKEY_ID_INDEX = 7;
    protected static final int PHONE_EMAIL_PHOTO_ID_INDEX = 8;
    protected static final int PHONE_EMAIL_PHONETIC_NAME_INDEX = 9;
    protected static final int PHONE_EMAIL_MIMETYPE_INDEX = 10;
    protected static final int PHONE_EMAIL_INDICATE_PHONE_SIM_INDEX = 11;
    protected static final int PHONE_EMAIL_IS_SDN_CONTACT = 12;

    private CharSequence mUnknownNameText;
    private int mAlternativeDisplayNameColumnIndex;

    private Context mContext;

    public PhoneAndEmailsPickerAdapter(Context context, ListView lv) {
        super(context, lv);
        mContext = context;
        mUnknownNameText = context.getText(android.R.string.unknownName);
        super.displayPhotoOnLeft();
    }

    protected CharSequence getUnknownNameText() {
        return mUnknownNameText;
    }

    @Override
    protected Uri configLoaderUri(long directoryId) {
        final Builder builder;
        Uri uri = null;
        boolean isSearchMode = isSearchMode();
        Log.i(TAG, "[configLoaderUri]directoryId = " + directoryId + ",isSearchMode = "
                + isSearchMode);

        if (isSearchMode) {
            String query = getQueryString();
            if (query == null) {
                query = "";
            }
            query = query.trim();
            if (TextUtils.isEmpty(query)) {
                // Regardless of the directory, we don't want anything returned,
                // so let's just send a "nothing" query to the local directory.
                builder = PICK_PHONE_EMAIL_URI.buildUpon();
            } else {
                builder = PICK_PHONE_EMAIL_FILTER_URI.buildUpon();
                builder.appendPath(query); // Builder will encode the query
                builder.appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                        String.valueOf(directoryId));
                if (directoryId != Directory.DEFAULT && directoryId != Directory.LOCAL_INVISIBLE) {
                    builder.appendQueryParameter(ContactsContract.LIMIT_PARAM_KEY,
                            String.valueOf(getDirectoryResultLimit()));
                }
                builder.appendQueryParameter(SearchSnippets.SNIPPET_ARGS_PARAM_KEY, SNIPPET_ARGS);
                builder.appendQueryParameter(SearchSnippets.DEFERRED_SNIPPETING_KEY, "1");
            }
        } else {
            builder = PICK_PHONE_EMAIL_URI.buildUpon();

            builder.appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                    String.valueOf(directoryId));
        }
        builder.appendQueryParameter("checked_ids_arg", PICK_PHONE_EMAIL_URI.toString());

        uri = builder.build();
        if (isSectionHeaderDisplayEnabled()) {
            uri = buildSectionIndexerUri(uri);
        }
        Log.d(TAG,"uri:"+uri);
        return uri;
    }

    @Override
    protected String[] configProjection() {
        return PHONE_EMAIL_PROJECTION;
    }



    @Override
    public String getContactDisplayName(int position) {
        return ((Cursor) getItem(position)).getString(mDisplayNameColumnIndex);
    }

    @Override
    public void setContactNameDisplayOrder(int displayOrder) {
        super.setContactNameDisplayOrder(displayOrder);
        if (getContactNameDisplayOrder() == ContactsPreferences.DISPLAY_ORDER_PRIMARY) {
            mDisplayNameColumnIndex = PHONE_EMAIL_PRIMARY_DISPLAY_NAME_INDEX;
            mAlternativeDisplayNameColumnIndex = PHONE_EMAIL_ALTERNATIVE_DISPLAY_NAME_INDEX;
        } else {
            mDisplayNameColumnIndex = PHONE_EMAIL_ALTERNATIVE_DISPLAY_NAME_INDEX;
            mAlternativeDisplayNameColumnIndex = PHONE_EMAIL_PRIMARY_DISPLAY_NAME_INDEX;
        }
        Log.i(TAG, "[setContactNameDisplayOrder]displayOrder = " + displayOrder +
                ",mDisplayNameColumnIndex = " + mDisplayNameColumnIndex);
    }

    @Override
    public void bindName(ContactListItemView view, Cursor cursor) {
        view.showDisplayName(cursor, mDisplayNameColumnIndex, mAlternativeDisplayNameColumnIndex);
        view.showPhoneticName(cursor, getPhoneticNameColumnIndex());
    }

    @Override
    protected void bindData(ContactListItemView view, Cursor cursor) {
        CharSequence label = null;
        if (!cursor.isNull(getDataTypeColumnIndex())) {
            final int type = cursor.getInt(getDataTypeColumnIndex());
            final String customLabel = cursor.getString(getDataLabelColumnIndex());

            // TODO cache
            final String mimeType = cursor.getString(PHONE_EMAIL_MIMETYPE_INDEX);
            Log.i(TAG, "[bindData]type = " + type +
                    ",customLabel = " + customLabel + ",mimeType = " + mimeType);

            if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
                label = Email.getTypeLabel(mContext.getResources(), type, customLabel);
            } else {
                label = Phone.getTypeLabel(mContext.getResources(), type, customLabel);
            }

            label = ExtensionManager
                    .getInstance()
                    .getAasExtension()
                    .getLabelForBindData(mContext.getResources(), type, customLabel, mimeType,
                            cursor, label);

        }
        //M:fixed CR ALPS02323055.use emptyLabel cover label position.
        TextView emptyLabel = view.getLabelView();
        emptyLabel.setText("");

     // view.setLabel(label);
        view.showData(cursor, getDataColumnIndex());
    }

    @Override
    public void bindQuickContact(ContactListItemView view, int partitionIndex, Cursor cursor) {
        return;
    }

    @Override
    public int getContactIDColumnIndex() {
        return PHONE_EMAIL_CONTACT_ID_INDEX;
    }

    @Override
    public int getDataColumnIndex() {
        return PHONE_EMAIL_NUMBER_INDEX;
    }

    @Override
    public int getDataLabelColumnIndex() {
        return PHONE_EMAIL_LABEL_INDEX;
    }

    @Override
    public int getDataTypeColumnIndex() {
        return PHONE_EMAIL_TYPE_INDEX;
    }

    @Override
    public Uri getDataUri(int position) {
        long id = ((Cursor) getItem(position)).getLong(PHONE_EMAIL_ID_INDEX);
        return ContentUris.withAppendedId(Data.CONTENT_URI, id);
    }

    @Override
    public long getDataId(int position) {
        /** M: Bug Fix for ALPS01537241 @{ */
        if (null != (Cursor) getItem(position)) {
            return ((Cursor) getItem(position)).getLong(PHONE_EMAIL_ID_INDEX);
        } else {
            Log.w(TAG, "The getItem is null");
            return -1;
        }
        /** @} */
    }

    @Override
    public int getDisplayNameColumnIdex() {
        return PHONE_EMAIL_PRIMARY_DISPLAY_NAME_INDEX;
    }

    @Override
    public int getPhoneticNameColumnIndex() {
        return PHONE_EMAIL_PHONETIC_NAME_INDEX;
    }

    @Override
    public int getPhotoIDColumnIndex() {
        return PHONE_EMAIL_PHOTO_ID_INDEX;
    }

    @Override
    public int getIndicatePhoneSIMColumnIndex() {
        return PHONE_EMAIL_INDICATE_PHONE_SIM_INDEX;
    }

    @Override
    public int getIsSdnContactColumnIndex() {
        return PHONE_EMAIL_IS_SDN_CONTACT;
    }

    @Override
    public int getLookupKeyColumnIndex() {
        return PHONE_EMAIL_LOOKUPKEY_ID_INDEX;
    }
    
  /// M: New Feature SDN.
    @Override
  	protected void configureSelection(
  			CursorLoader loader, long directoryId, ContactListFilter filter) {
  		Log.d(TAG,"configureSelection,filtertype:"+filter.filterType);
  		if (filter == null) {
  			return;
  		}

  		if (directoryId != Directory.DEFAULT) {
  			return;
  		}

  		StringBuilder selection = new StringBuilder();
  		List<String> selectionArgs = new ArrayList<String>();

  		switch (filter.filterType) {
  		case ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS: {
  			// We have already added directory=0 to the URI, which takes care of this
  			// filter
  			/** M: New Feature SDN. */
  			selection.append(RawContacts.IS_SDN_CONTACT + " < 1");
  			break;
  		}
  		case ContactListFilter.FILTER_TYPE_SINGLE_CONTACT: {
  			// We have already added the lookup key to the URI, which takes care of this
  			// filter
  			break;
  		}
  		case ContactListFilter.FILTER_TYPE_STARRED: {
  			selection.append(Contacts.STARRED + "!=0");
  			break;
  		}
  		case ContactListFilter.FILTER_TYPE_WITH_PHONE_NUMBERS_ONLY: {
  			selection.append(Contacts.HAS_PHONE_NUMBER + "=1");
  			/** M: New Feature SDN. */
  			selection.append(" AND " + RawContacts.IS_SDN_CONTACT + " < 1");
  			break;
  		}
  		case ContactListFilter.FILTER_TYPE_CUSTOM: {
  			selection.append(Contacts.IN_VISIBLE_GROUP + "=1");
  			if (isCustomFilterForPhoneNumbersOnly()) {
  				selection.append(" AND " + Contacts.HAS_PHONE_NUMBER + "=1");
  			}
  			/** M: New Feature SDN. */
  			selection.append(" AND " + RawContacts.IS_SDN_CONTACT + " < 1");
  			break;
  		}
  		case ContactListFilter.FILTER_TYPE_ACCOUNT: {
  			// We use query parameters for account filter, so no selection to add here.
  			/** M: Change Feature: As Local Phone account contains null account and Phone
  			 * Account, the Account Query Parameter could not meet this requirement. So,
  			 * We should keep to query contacts with selection. @{ */
  			buildSelectionForFilterAccount(filter, selection, selectionArgs);
  			break;
  		}

  		case ContactListFilter.FILTER_TYPE_PRIVACY_CONTACT:
  			Log.d(TAG,"configureSelection FILTER_TYPE_PRIVACY_CONTACT");
  			int privacyId=Integer.parseInt(filter.extra.toString());
  			if(privacyId==0) privacyId=-1;
  			Log.d(TAG,"privacyId:"+privacyId);
  			selection.append("is_privacy="+privacyId);
  			break;

  		case ContactListFilter.FILTER_TYPE_ADD_PRIVACY_CONTACT:
  			Log.d(TAG,"configureSelection FILTER_TYPE_ADD_PRIVACY_CONTACT");
  			selection.append("is_privacy=0");
  			break;
  		}
  		Log.d(TAG, "[configureSelection] selection: " + selection.toString()
  		+ ", filter.filterType = " + filter.filterType);
  		loader.setSelection(selection.toString());
  		loader.setSelectionArgs(selectionArgs.toArray(new String[0]));
  	}
  	
  	/**
	 * M: Change Feature: As Local Phone account contains null account and Phone
	 * Account, the Account Query Parameter could not meet this requirement. So,
	 * We should keep to query contacts with selection. */
	private void buildSelectionForFilterAccount(ContactListFilter filter, StringBuilder selection,
			List<String> selectionArgs) {
		if (AccountTypeUtils.ACCOUNT_TYPE_LOCAL_PHONE.equals(filter.accountType)) {
			selection.append("EXISTS ("
					+ "SELECT DISTINCT " + RawContacts.CONTACT_ID
					+ " FROM view_raw_contacts"
					+ " WHERE ( ");
			selection.append(RawContacts.IS_SDN_CONTACT + " < 1 AND ");
			selection.append(RawContacts.CONTACT_ID + " = " + "data.contact_id"
					+ " AND (" + RawContacts.ACCOUNT_TYPE + " IS NULL "
					+ " AND " + RawContacts.ACCOUNT_NAME + " IS NULL "
					+ " AND " +  RawContacts.DATA_SET + " IS NULL "
					+ " OR " + RawContacts.ACCOUNT_TYPE + "=? "
					+ " AND " + RawContacts.ACCOUNT_NAME + "=? ");
		} else {
			selection.append("EXISTS ("
					+ "SELECT DISTINCT " + RawContacts.CONTACT_ID
					+ " FROM view_raw_contacts"
					+ " WHERE ( ");
			selection.append(RawContacts.IS_SDN_CONTACT + " < 1 AND ");
			selection.append(RawContacts.CONTACT_ID + " = " + "data."
					+ Contacts._ID
					+ " AND (" + RawContacts.ACCOUNT_TYPE + "=?"
					+ " AND " + RawContacts.ACCOUNT_NAME + "=?");
		}
		ContactsCommonListUtils.buildSelectionForFilterAccount(filter, selection, selectionArgs);
	}
	private boolean isCustomFilterForPhoneNumbersOnly() {
		// TODO: this flag should not be stored in shared prefs.  It needs to be in the db.
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
		return prefs.getBoolean(ContactsPreferences.PREF_DISPLAY_ONLY_PHONES,
				ContactsPreferences.PREF_DISPLAY_ONLY_PHONES_DEFAULT);
	}
  	
}
