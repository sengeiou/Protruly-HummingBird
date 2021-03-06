/*
 * Copyright (C) 2014 MediaTek Inc.
 * Modification based on code covered by the mentioned copyright
 * and/or permission notice(s).
 */
/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.contacts.common.list;

import com.android.contacts.common.activity.TransactionSafeActivity;
import android.widget.HbSearchView.OnQueryTextListener;
import android.widget.HbSearchView.OnCloseListener;
import android.widget.HbSearchView.OnSuggestionListener;
import android.widget.HbSearchView;
import hb.provider.ContactsContract.Contacts;
import hb.provider.ContactsContract.CommonDataKinds.Phone;
import java.util.ArrayList;
import com.hb.t9search.HbSearchContactsAdapter;
import com.hb.t9search.ContactsHelper.OnContactsLoad;
import hb.widget.HbIndexBar;
import hb.widget.HbIndexBar.Letter;
import com.android.contacts.common.R;
import com.android.contacts.common.activity.TransactionSafeActivity;
import com.hb.t9search.ContactsHelper.OnContactsChanged;
import com.hb.t9search.HbSearchContactsAdapter;
import com.hb.t9search.ContactsHelper;
import com.hb.t9search.ViewUtil;
import com.android.contacts.common.list.ContactListAdapter.ContactQuery;
import com.android.contacts.common.hb.ContactForIndex;
import com.android.contacts.common.hb.DensityUtil;
import com.android.contacts.common.hb.FragmentCallbacks;
import com.android.contacts.common.hb.StarContactsAdapter;

import android.R.integer;
import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import hb.provider.ContactsContract.Directory;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HbSearchView;
import android.widget.HbPreSearchView;
import android.widget.LinearLayout;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;

import com.android.common.widget.CompositeCursorAdapter.Partition;
import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.preference.ContactsPreferences;
import com.android.contacts.common.util.ContactListViewUtils;
import com.hb.t9search.ContactsHelper;
import com.hb.t9search.ViewUtil;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import hb.view.menu.bottomnavigation.BottomNavigationView;
import hb.widget.ActionMode;
import hb.widget.HbIndexBar;
import hb.widget.SliderView;
import com.android.contacts.common.util.PermissionsUtil;
/**
 * Common base class for various contact-related list fragments.
 */
public abstract class ContactEntryListFragment<T extends ContactEntryListAdapter>
extends Fragment
implements OnItemClickListener, OnScrollListener, OnFocusChangeListener, OnTouchListener,
HbIndexBar.OnTouchStateChangedListener,
HbIndexBar.OnSelectListener,
OnItemLongClickListener, 
LoaderCallbacks<Cursor> {
	private static final String TAG = "ContactEntryListFragment";
	public static final String SEARCH_BEGIN_STRING="hb_querystring_for_contact_search_begin";

	// TODO: Make this protected. This should not be used from the PeopleActivity but
	// instead use the new startActivityWithResultFromFragment API
	public static final int ACTIVITY_REQUEST_CODE_PICKER = 1;

	private static final String KEY_LIST_STATE = "liststate";
	private static final String KEY_SECTION_HEADER_DISPLAY_ENABLED = "sectionHeaderDisplayEnabled";
	private static final String KEY_PHOTO_LOADER_ENABLED = "photoLoaderEnabled";
	private static final String KEY_QUICK_CONTACT_ENABLED = "quickContactEnabled";
	private static final String KEY_ADJUST_SELECTION_BOUNDS_ENABLED =
			"adjustSelectionBoundsEnabled";
	private static final String KEY_INCLUDE_PROFILE = "includeProfile";
	private static final String KEY_SEARCH_MODE = "searchMode";
	private static final String KEY_VISIBLE_SCROLLBAR_ENABLED = "visibleScrollbarEnabled";
	private static final String KEY_SCROLLBAR_POSITION = "scrollbarPosition";
	private static final String KEY_QUERY_STRING = "queryString";
	private static final String KEY_DIRECTORY_SEARCH_MODE = "directorySearchMode";
	private static final String KEY_SELECTION_VISIBLE = "selectionVisible";
	private static final String KEY_REQUEST = "request";
	private static final String KEY_DARK_THEME = "darkTheme";
	private static final String KEY_LEGACY_COMPATIBILITY = "legacyCompatibility";
	private static final String KEY_DIRECTORY_RESULT_LIMIT = "directoryResultLimit";

	private static final String DIRECTORY_ID_ARG_KEY = "directoryId";

	private static final int DIRECTORY_LOADER_ID = -1;

	private static final int DIRECTORY_SEARCH_DELAY_MILLIS = 300;
	private static final int DIRECTORY_SEARCH_MESSAGE = 1;

	private static final int DEFAULT_DIRECTORY_RESULT_LIMIT = 20;

	private boolean mSectionHeaderDisplayEnabled;
	private boolean mPhotoLoaderEnabled;
	private boolean mQuickContactEnabled = true;
	private boolean mAdjustSelectionBoundsEnabled = true;
	private boolean mIncludeProfile;
	private boolean mSearchMode;
	private boolean mVisibleScrollbarEnabled;
	private boolean mShowEmptyListForEmptyQuery;
	private int mVerticalScrollbarPosition = getDefaultVerticalScrollbarPosition();
	private int mDirectorySearchMode = DirectoryListLoader.SEARCH_MODE_NONE;
	private boolean mSelectionVisible;
	private boolean mLegacyCompatibility;

	private boolean mEnabled = true;
	private T mAdapter;
	public StarContactsAdapter starAdapter;
	public GridView starGridView;
	private View mView;
	public ListView mListView;

	public FragmentCallbacks mCallbacks;
	public void setCallbacks(FragmentCallbacks mCallbacks) {
		this.mCallbacks = mCallbacks;
	}
	protected ActionMode actionMode;
	protected BottomNavigationView bottomBar;
	protected View header;
	protected HbSearchView mSearchView;
	protected View getHeader() {
		return header;
	}
	public HbSearchView getSearchView() {
		return mSearchView;
	}
	public void setSearchView(HbSearchView mSearchView) {
		this.mSearchView = mSearchView;
	}

	//	public LinearLayout hbSearchViewLayout;
	//	public LinearLayout.LayoutParams hbSearchViewLayoutParams;
	public void showHeader(boolean show){
		if(show){
			header.setVisibility(View.VISIBLE);
			header.setPadding(0, 0, 0, 0);
		}else{
			header.setVisibility(View.GONE);
			header.setPadding(0, -2000, 0, 0);
		}
	}
	public void setBottomBar(BottomNavigationView bottomBar) {
		this.bottomBar = bottomBar;
	}
	public void setActionMode(ActionMode actionMode) {
		this.actionMode = actionMode;
	}

	/**
	 * Used for keeping track of the scroll state of the list.
	 */
	private Parcelable mListState;
	protected boolean isForContactsChoice=false;
	public void setForContactsChoice(boolean isForContactsChoice) {
		Log.d(TAG,"setForContactsChoice:"+isForContactsChoice);
		this.isForContactsChoice=isForContactsChoice;
	}
	private int mDisplayOrder;
	private int mSortOrder;
	private int mDirectoryResultLimit = DEFAULT_DIRECTORY_RESULT_LIMIT;

	protected ContactPhotoManager mPhotoManager;
	private ContactsPreferences mContactsPrefs;

	private boolean mForceLoad;

	private boolean mDarkTheme;

	protected boolean mUserProfileExists;

	/** M: for ALPS01766595 */
	protected static final int PROFILE_NUM = 1;
	private static final int STATUS_NOT_LOADED = 0;
	private static final int STATUS_LOADING = 1;
	private static final int STATUS_LOADED = 2;

	private int mDirectoryListStatus = STATUS_NOT_LOADED;

	/**
	 * Indicates whether we are doing the initial complete load of data (false) or
	 * a refresh caused by a change notification (true)
	 */
	private boolean mLoadPriorityDirectoriesOnly;

	private Context mContext;

	private LoaderManager mLoaderManager;
	//	protected AbsListIndexer mAlphbetIndexView;


	private Handler mDelayedDirectorySearchHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == DIRECTORY_SEARCH_MESSAGE) {
				loadDirectoryPartition(msg.arg1, (DirectoryPartition) msg.obj);
			}
		}
	};
	private int defaultVerticalScrollbarPosition;

	protected abstract View inflateView(LayoutInflater inflater, ViewGroup container);
	protected abstract T createListAdapter();

	/**
	 * @param position Please note that the position is already adjusted for
	 *            header views, so "0" means the first list item below header
	 *            views.
	 */
	protected abstract void onItemClick(int position, long id);

	/**
	 * @param position Please note that the position is already adjusted for
	 *            header views, so "0" means the first list item below header
	 *            views.
	 */
	protected boolean onItemLongClick(int position, long id) {
		return false;
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG,"onResume");
		//		final ContentResolver contentResolver = getActivity().getContentResolver();
		//		//add by lgy start
		//		if(PermissionsUtil.hasContactsPermissions(getActivity())) {
		//			contentResolver.query(Uri.parse("content://com.android.contacts/hb_dialer_search_init"),
		//					null, null, null, null);
		//		}
		//		//add by lgy end
	}

	@Override
	public void onAttach(Activity activity) {
		Log.d(TAG,"onAttacth,activity:"+activity);
		super.onAttach(activity);
		setContext(activity);
		setLoaderManager(super.getLoaderManager());
	}

	/**
	 * Sets a context for the fragment in the unit test environment.
	 */
	public void setContext(Context context) {
		mContext = context;		
	}

	public Context getContext() {
		return mContext;
	}

	public void setEnabled(boolean enabled) {
		if (mEnabled != enabled) {
			mEnabled = enabled;
			if (mAdapter != null) {
				if (mEnabled) {
					reloadData();
				} else {
					mAdapter.clearPartitions();
				}
			}
		}
	}

	/**
	 * Overrides a loader manager for use in unit tests.
	 */
	public void setLoaderManager(LoaderManager loaderManager) {
		mLoaderManager = loaderManager;
	}

	@Override
	public LoaderManager getLoaderManager() {
		return mLoaderManager;
	}

	public T getAdapter() {
		return mAdapter;
	}

	@Override
	public View getView() {
		return mView;
	}

	public ListView getListView() {
		return mListView;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_SECTION_HEADER_DISPLAY_ENABLED, mSectionHeaderDisplayEnabled);
		outState.putBoolean(KEY_PHOTO_LOADER_ENABLED, mPhotoLoaderEnabled);
		outState.putBoolean(KEY_QUICK_CONTACT_ENABLED, mQuickContactEnabled);
		outState.putBoolean(KEY_ADJUST_SELECTION_BOUNDS_ENABLED, mAdjustSelectionBoundsEnabled);
		outState.putBoolean(KEY_INCLUDE_PROFILE, mIncludeProfile);
		outState.putBoolean(KEY_SEARCH_MODE, mSearchMode);
		outState.putBoolean(KEY_VISIBLE_SCROLLBAR_ENABLED, mVisibleScrollbarEnabled);
		outState.putInt(KEY_SCROLLBAR_POSITION, mVerticalScrollbarPosition);
		outState.putInt(KEY_DIRECTORY_SEARCH_MODE, mDirectorySearchMode);
		outState.putBoolean(KEY_SELECTION_VISIBLE, mSelectionVisible);
		outState.putBoolean(KEY_LEGACY_COMPATIBILITY, mLegacyCompatibility);
		outState.putString(KEY_QUERY_STRING, mQueryString);
		outState.putInt(KEY_DIRECTORY_RESULT_LIMIT, mDirectoryResultLimit);
		outState.putBoolean(KEY_DARK_THEME, mDarkTheme);

		if (mListView != null) {
			outState.putParcelable(KEY_LIST_STATE, mListView.onSaveInstanceState());
		}
	}

	protected ContactListFilter hbFilter;
	public void setHbFilter(ContactListFilter filter) {
		hbFilter = filter;
	}

	protected String hbFilterString;	
	public void setHbFilterString(String hbFilterString) {
		this.hbFilterString = hbFilterString;
	}
	@Override
	public void onCreate(Bundle savedState) {
		Log.d(TAG,"onCreate, this:"+this);
		super.onCreate(savedState);
		//M:moved to last
		//restoreSavedState(savedState);
		mAdapter = createListAdapter();
		mContactsPrefs = new ContactsPreferences(mContext);
		restoreSavedState(savedState); 
	}

	public void restoreSavedState(Bundle savedState) {
		if (savedState == null) {
			return;
		}

		mSectionHeaderDisplayEnabled = savedState.getBoolean(KEY_SECTION_HEADER_DISPLAY_ENABLED);
		mPhotoLoaderEnabled = savedState.getBoolean(KEY_PHOTO_LOADER_ENABLED);
		mQuickContactEnabled = savedState.getBoolean(KEY_QUICK_CONTACT_ENABLED);
		mAdjustSelectionBoundsEnabled = savedState.getBoolean(KEY_ADJUST_SELECTION_BOUNDS_ENABLED);
		mIncludeProfile = savedState.getBoolean(KEY_INCLUDE_PROFILE);
		mSearchMode = savedState.getBoolean(KEY_SEARCH_MODE);
		mVisibleScrollbarEnabled = savedState.getBoolean(KEY_VISIBLE_SCROLLBAR_ENABLED);
		mVerticalScrollbarPosition = savedState.getInt(KEY_SCROLLBAR_POSITION);
		mDirectorySearchMode = savedState.getInt(KEY_DIRECTORY_SEARCH_MODE);
		mSelectionVisible = savedState.getBoolean(KEY_SELECTION_VISIBLE);
		mLegacyCompatibility = savedState.getBoolean(KEY_LEGACY_COMPATIBILITY);
		mQueryString = savedState.getString(KEY_QUERY_STRING);
		mDirectoryResultLimit = savedState.getInt(KEY_DIRECTORY_RESULT_LIMIT);
		mDarkTheme = savedState.getBoolean(KEY_DARK_THEME);

		// Retrieve list state. This will be applied in onLoadFinished
		mListState = savedState.getParcelable(KEY_LIST_STATE);
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG,"onStart(),isSearchMode:"+isSearchMode());
		mContactsPrefs.registerChangeListener(mPreferencesChangeListener);

		mForceLoad = loadPreferences();
		mForceLoad=true;

		mDirectoryListStatus = STATUS_NOT_LOADED;
		mLoadPriorityDirectoriesOnly = true;

		if(!isSearchMode()){
			//			startLoading();
			reloadData();
		}
	}

	public void startLoad(){
		startLoading();
	}

	protected void startLoading() {
		Log.d(TAG, "startLoading");
		if (mAdapter == null) {
			// The method was called before the fragment was started
			Log.d(TAG, "[statLoading] mAdapter is null");
			return;
		}

		configureAdapter();
		int partitionCount = mAdapter.getPartitionCount();
		Log.d(TAG, "startLoading,partitionCount:"+partitionCount);
		for (int i = 0; i < partitionCount; i++) {
			Partition partition = mAdapter.getPartition(i);
			if (partition instanceof DirectoryPartition) {
				Log.d(TAG, "startLoading1");
				DirectoryPartition directoryPartition = (DirectoryPartition)partition;
				if (directoryPartition.getStatus() == DirectoryPartition.STATUS_NOT_LOADED||isSearchMode()) {
					if (directoryPartition.isPriorityDirectory() || !mLoadPriorityDirectoriesOnly) {
						startLoadingDirectoryPartition(i);
					}
				}
			} else {
				Log.d(TAG,"startLoading2");
				getLoaderManager().initLoader(i, null, this);
			}
		}

		// Next time this method is called, we should start loading non-priority directories
		mLoadPriorityDirectoriesOnly = false;
	}

	private CursorLoader mLoader;
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Log.d(TAG, "onCreateLoader,id:"+id);
		if (id == DIRECTORY_LOADER_ID) {
			DirectoryListLoader loader = new DirectoryListLoader(mContext);
			loader.setDirectorySearchMode(mAdapter.getDirectorySearchMode());
			loader.setLocalInvisibleDirectoryEnabled(
					ContactEntryListAdapter.LOCAL_INVISIBLE_DIRECTORY_ENABLED);
			return loader;
		} else {
			CursorLoader loader = createCursorLoader(mContext);
			mLoader=loader;
			long directoryId = args != null && args.containsKey(DIRECTORY_ID_ARG_KEY)
					? args.getLong(DIRECTORY_ID_ARG_KEY)
							: Directory.DEFAULT;
					/// M: Set the value whether show the sdn number.
					mAdapter.setShowSdnNumber(isShowSdnNumber());

					Log.d(TAG, "[onCreateLoader] loader: " + loader + ",id:" + id+" directoryId:"+directoryId);
					mAdapter.configureLoader(loader, directoryId);
					return loader;
		}
	}

	public CursorLoader createCursorLoader(Context context) {
		Log.d(TAG, "createCursorLoader11");
		return new CursorLoader(context, null, null, null, null, null) {
			@Override
			protected Cursor onLoadInBackground() {
				try {
					return super.onLoadInBackground();
				} catch (RuntimeException e) {
					// We don't even know what the projection should be, so no point trying to
					// return an empty MatrixCursor with the correct projection here.
					Log.w(TAG, "RuntimeException while trying to query ContactsProvider.");
					return null;
				}
			}
		};
	}

	private void startLoadingDirectoryPartition(int partitionIndex) {
		Log.d(TAG, "startLoadingDirectoryPartition:"+partitionIndex);
		DirectoryPartition partition = (DirectoryPartition)mAdapter.getPartition(partitionIndex);
		partition.setStatus(DirectoryPartition.STATUS_LOADING);
		long directoryId = partition.getDirectoryId();
		if (mForceLoad) {
			if (directoryId == Directory.DEFAULT) {
				Log.d(TAG, "startLoadingDirectoryPartition1:"+partitionIndex);
				loadDirectoryPartition(partitionIndex, partition);
			} else {
				Log.d(TAG, "startLoadingDirectoryPartition2:"+partitionIndex);
				loadDirectoryPartitionDelayed(partitionIndex, partition);
			}
		} else {
			Log.d(TAG, "startLoadingDirectoryPartition3:"+partitionIndex);
			Bundle args = new Bundle();
			args.putLong(DIRECTORY_ID_ARG_KEY, directoryId);
			getLoaderManager().initLoader(partitionIndex, args, this);
		}
	}

	/**
	 * Queues up a delayed request to search the specified directory. Since
	 * directory search will likely introduce a lot of network traffic, we want
	 * to wait for a pause in the user's typing before sending a directory request.
	 */
	private void loadDirectoryPartitionDelayed(int partitionIndex, DirectoryPartition partition) {
		mDelayedDirectorySearchHandler.removeMessages(DIRECTORY_SEARCH_MESSAGE, partition);
		Message msg = mDelayedDirectorySearchHandler.obtainMessage(
				DIRECTORY_SEARCH_MESSAGE, partitionIndex, 0, partition);
		mDelayedDirectorySearchHandler.sendMessageDelayed(msg, DIRECTORY_SEARCH_DELAY_MILLIS);
	}

	/**
	 * Loads the directory partition.
	 */
	protected void loadDirectoryPartition(int partitionIndex, DirectoryPartition partition) {
		Log.d(TAG, "loadDirectoryPartition:"+partitionIndex);
		Bundle args = new Bundle();
		args.putLong(DIRECTORY_ID_ARG_KEY, partition.getDirectoryId());
		Log.d(TAG,"restartLoader");
		getLoaderManager().restartLoader(partitionIndex, args, this);
	}

	/**
	 * Cancels all queued directory loading requests.
	 */
	private void removePendingDirectorySearchRequests() {
		mDelayedDirectorySearchHandler.removeMessages(DIRECTORY_SEARCH_MESSAGE);
	}

	public interface ViewContactListener{
		void onViewContactAction(Uri uri);
	}


	protected int mStarredCount;

	protected ArrayList<ContactForIndex> contactForIndexs;
	protected ArrayList<Integer> indexArrayList;
	protected HbIndexBar mIndexBar;
	public HbIndexBar getmIndexBar() {
		return mIndexBar;
	}

	public void setmIndexBar(HbIndexBar mIndexBar) {
		Log.d(TAG,"setmIndexBar:"+mIndexBar+" this:"+this);
		this.mIndexBar = mIndexBar;
		this.mIndexBar.setOnSelectListener(this);
		this.mIndexBar.setOnTouchStateChangedListener(this);

	}
	protected MatrixCursor allCursor;
	public int allCount=0;
	protected int loaderId;
	@Override
	public void onLoadFinished(Loader<Cursor> loader, final Cursor data) {

		allCount=data==null?0:data.getCount();

		if(mLoader!=null && mLoader instanceof ProfileAndContactsLoader){
			mStarredCount=((ProfileAndContactsLoader)mLoader).getCursorCount();
		}

		Log.d(TAG, "[onLoadFinished] loader:" + loader + ",data:" + data+" allCount:"+allCount+" mStarredCount:"+mStarredCount);

		/// M: check whether the fragment still in Activity @{
		if (!isAdded()) {
			Log.d(TAG, "onLoadFinished(),This Fragment is not add to the Activity now.data:"
					+ data);
			return;
		}
		/// @}

		if (!mEnabled) {
			Log.d(TAG, "return in onLoad finish,mEnabled:" + mEnabled);
			return;
		}

		loaderId = loader.getId();
		if (loaderId == DIRECTORY_LOADER_ID) {
			mDirectoryListStatus = STATUS_LOADED;
			mAdapter.changeDirectories(data);
			Log.d(TAG, "onLoadFinished startloading,loaderId:" + loaderId);
			startLoading();
		} else {
			if(isSearchMode() && !isForDialerSearch &&data!=null) {
				SortCursor sortCursor=new SortCursor(data,mQueryString);
				onPartitionLoaded(loaderId, sortCursor);
			}else{
				onPartitionLoaded(loaderId, data);
			}
			if (isSearchMode()) {/*
				int directorySearchMode = getDirectorySearchMode();
				if (directorySearchMode != DirectoryListLoader.SEARCH_MODE_NONE) {
					if (mDirectoryListStatus == STATUS_NOT_LOADED) {
						Log.d(TAG, "onloadfinish4");
						mDirectoryListStatus = STATUS_LOADING;
						getLoaderManager().initLoader(DIRECTORY_LOADER_ID, null, this);
					} else {
						Log.d(TAG, "onloadfinish5");
						startLoading();
					}
				}
			 */} else {
				 mDirectoryListStatus = STATUS_NOT_LOADED;
				 getLoaderManager().destroyLoader(DIRECTORY_LOADER_ID);
			 }
		}

		Log.d(TAG,"isForDialerSearch:"+isForDialerSearch);

		if(isForDialerSearch) return;
		//for:字母导航条begin
		if(data!=null&&!isSearchMode()){
			allCursor = new MatrixCursor(_PROJECTION){
				@Override
				public Bundle getExtras() {
					// Need to get the extras from the contacts cursor.
					return data == null ? new Bundle() : data.getExtras();
				}
			};


			Object[] objs=null;
			try{
				//								Log.d(TAG,"_PROJECTION:"+Arrays.toString(_PROJECTION)+" this:"+ContactEntryListFragment.this);				
				//								for(int i=0;i<data.getColumnCount();i++) Log.d(TAG,"i:"+i+" column name:"+data.getColumnName(i));				
				for(int i=0;i<allCount;i++){
					data.moveToPosition(i);
					objs = new Object[_PROJECTION.length];
					for(int j=0;j<_PROJECTION.length;j++){
						objs[j] = data.getString(j);
					}
					allCursor.addRow(objs);
				}
			}catch(Exception e){
				Log.d(TAG,"e:"+e);
			}

			if(mIndexBar!=null){
				if(allCount>0&&!isSearchMode()){
					Log.d(TAG,"onLoadFinished111");
					if(mTask!=null) {
						mTask.cancel(true);
						mTask=null;
					}
					mTask =new InitIndexBarTask();
					mTask.execute();
				}
			}
		}
		//字母导航条end



		//控制搜索栏、字母导航条与空页面的显示与隐藏
		if (data == null || data.getCount() == 0) {
			if(mListView!=null && mListView.getVisibility()!=View.GONE) mListView.setVisibility(View.GONE);
		}else {
			if(mListView!=null && mListView.getVisibility()!=View.VISIBLE) mListView.setVisibility(View.VISIBLE);
		}

		if(mHbIsSearchMode){
			if(hb_search_view_include.getVisibility()!=View.VISIBLE) hb_search_view_include.setVisibility(View.VISIBLE);
			if(mIndexBar!=null && mIndexBar.getVisibility()!=View.GONE) mIndexBar.setVisibility(View.GONE);
			if(TextUtils.isEmpty(getQueryString()) && mListView.getVisibility()!=View.GONE) mListView.setVisibility(View.GONE);
		}else{
			if(hbPreSearchview!=null) hbPreSearchview.setQueryHint(getActivity().getString(R.string.hb_searchview_contact_count_hint,(allCount-mStarredCount)));
			if (data == null || data.getCount() == 0){
				if(hb_search_view_include!=null && hb_search_view_include.getVisibility()!=View.GONE) hb_search_view_include.setVisibility(View.GONE);
				if(mIndexBar!=null && mIndexBar.getVisibility()!=View.GONE) mIndexBar.setVisibility(View.GONE);
			}else {
				if(hb_search_view_include!=null && hb_search_view_include.getVisibility()!=View.VISIBLE) hb_search_view_include.setVisibility(View.VISIBLE);
				if(mIndexBar!=null && mIndexBar.getVisibility()!=View.VISIBLE) mIndexBar.setVisibility(View.VISIBLE);
			}
		}
	}


	public static class SortEntry {
		public int key;  //根据这个key排序。
		public int order;  //相当于原始cursor的指针。
	}
	/**
	 * 用来重新排序Cursor
	 * add by liyang 2017-4-1
	 */
	public class SortCursor extends CursorWrapper implements Comparator<SortEntry>{

		public SortCursor(Cursor cursor) {  
			super(cursor);  
		}  

		Cursor mCursor;  
		ArrayList<SortEntry> sortList = new ArrayList<SortEntry>();
		int mPos = -1; 


		@Override
		public int compare(SortEntry entry1, SortEntry entry2) { 
			return entry1.key - entry2.key;
		}

		public SortCursor(Cursor cursor,String queryString) {
			super(cursor);  
			mCursor = cursor;  
			if (mCursor != null && mCursor.getCount() > 0) {  
				int i = 0;   
				for (mCursor.moveToFirst(); !mCursor.isAfterLast(); mCursor.moveToNext(), i++) {
					SortEntry sortKey = new SortEntry();  
					String snippet = cursor.getString(snippetColumnIndex);

					Log.d(TAG,"snippet:"+snippet+" end ");
					sortKey.key=1;
					if(TextUtils.isEmpty(snippet)) sortKey.key=0;
					else{
						String name=cursor.getString(nameColumnIndex);
						String jianPinyin=cursor.getString(jianpinyinColumnIndex);
						Log.d(TAG,"name:"+name+" query:"+queryString+" jianPinyin:"+jianPinyin);

						if(snippet!=null && snippet.replace(" ", "").toUpperCase().indexOf(queryString.toUpperCase())<0) {
							sortKey.key=0;
						}else{
							if(!TextUtils.isEmpty(name) && name.replace(" ", "").toUpperCase().indexOf(queryString.toUpperCase())>-1) sortKey.key=0;
							if(!TextUtils.isEmpty(jianPinyin) && jianPinyin.replace(" ", "").toUpperCase().indexOf(queryString.toUpperCase())>-1) sortKey.key=0;
						}
					}
					sortKey.order = i;  
					sortList.add(sortKey);  
				}  
			}  
			Collections.sort(sortList, this);  
		}  

		public int getKey(int position){
			return sortList.get(position).key; 
		}
		public boolean moveToPosition(int position) {
			if (position >= 0 && position < sortList.size()) {  
				mPos = position;  
				int order = sortList.get(position).order;  
				return mCursor.moveToPosition(order);  
			}  
			if (position < 0) {  
				mPos = -1;  
			}  
			if (position >= sortList.size()) {  
				mPos = sortList.size();  
			}  
			return mCursor.moveToPosition(position);  
		}  
		public boolean moveToFirst() {  
			return moveToPosition(0);  
		}  
		public boolean moveToLast() {  
			return moveToPosition(getCount() - 1);  
		}  
		public boolean moveToNext() {  
			return moveToPosition(mPos + 1);  
		}  
		public boolean moveToPrevious() {  
			return moveToPosition(mPos - 1);  
		}  
		public boolean move(int offset) {  
			return moveToPosition(mPos + offset);  
		}  
		public int getPosition() {  
			return mPos;  
		}
	}

	public boolean isLetter(String cha){
		if(cha == null || cha.length() <= 0) return false;
		char first = cha.toUpperCase().charAt(0);
		if(first >= 'A' && first <= 'Z'){
			return true;
		}
		return false;
	}


	protected static final String[] PHONES_PROJECTION = new String[] { Phone._ID, // 0
			Phone.TYPE, // 1
			Phone.LABEL, // 2
			Phone.NUMBER, // 3
			Phone.DISPLAY_NAME_PRIMARY, // 4
			Phone.DISPLAY_NAME_ALTERNATIVE, // 5
			Phone.CONTACT_ID, // 6
			Phone.LOOKUP_KEY, // 7
			Phone.PHOTO_ID, // 8
			Phone.PHONETIC_NAME, // 9
			Contacts.INDICATE_PHONE_SIM, // 10
			Contacts.IS_SDN_CONTACT, // 11
			"quanpinyin",//12
			"jianpinyin",//13
			"phonebook_bucket",//14
	};
	public static final String EXIT_SEARCH = "HB_EXIT_SEARCH_STRING";

	//	public void setListViewHeightBasedOnChildren() {  
	//		// 获取listview的adapter  
	//		if (starAdapter == null||starGridView==null) {  
	//			return;  
	//		}  
	//		// 固定列宽，有多少列  
	//		int col = 5;// listView.getNumColumns();  
	//		int totalHeight = 0;  
	//		// i每次加5，相当于listAdapter.getCount()小于等于5时 循环一次，计算一次item的高度，  
	//		// listAdapter.getCount()小于等于10时计算两次高度相加  
	//		for (int i = 0; i <mStarredCount; i += col) {  
	//			// 获取listview的每一个item  
	//			View listItem = starAdapter.getView(i, null, starGridView);  
	//			listItem.measure(0, 0);  
	//			// 获取item的高度和  
	//			totalHeight += listItem.getMeasuredHeight();  
	//			totalHeight+=starGridView.getVerticalSpacing();
	//		}
	//
	//		Log.d(TAG,"totalHeight:"+totalHeight);
	//
	//		// 获取listview的布局参数  
	//		ViewGroup.LayoutParams params = starGridView.getLayoutParams();  
	//		// 设置高度  
	//		params.height = totalHeight;  
	//		// 设置margin  
	//		//           ((MarginLayoutParams) params).setMargins(10, 10, 10, 10);  
	//		// 设置参数  
	//		starGridView.setLayoutParams(params);  
	//	}  




	protected void onPartitionLoaded(int partitionIndex, Cursor data) {
		if (partitionIndex >= mAdapter.getPartitionCount()) {
			// When we get unsolicited data, ignore it.  This could happen
			// when we are switching from search mode to the default mode.
			Log.d(TAG, "[onPartitionLoaded] return");
			return;
		}

		//		Log.d(TAG, "[onPartitionLoaded],data:"+data+" count:"+(data==null?0:data.getCount()));

		mAdapter.setCursor(data);
		mAdapter.changeCursor(partitionIndex, data);

		setProfileHeader();

		if (!isLoading()) {
			completeRestoreInstanceState();
		}
	}

	public boolean isLoading() {
		if (mAdapter != null && mAdapter.isLoading()) {
			return true;
		}

		if (isLoadingDirectoryList()) {
			return true;
		}

		return false;
	}

	public boolean isLoadingDirectoryList() {
		return isSearchMode() && getDirectorySearchMode() != DirectoryListLoader.SEARCH_MODE_NONE
				&& (mDirectoryListStatus == STATUS_NOT_LOADED
				|| mDirectoryListStatus == STATUS_LOADING);
	}

	@Override
	public void onStop() {
		Log.d(TAG, "onStop");
		super.onStop();
		mContactsPrefs.unregisterChangeListener();
		//		if(!isForDialerSearch && !isSearchMode()) mAdapter.clearPartitions();
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		if(mTask!=null) {
			mTask.cancel(true);
			mTask=null;
		}
	}

	public void reloadData() {
		Log.d(TAG,"reloadData()");
		removePendingDirectorySearchRequests();
		mAdapter.onDataReload();
		mLoadPriorityDirectoriesOnly = true;
		mForceLoad = true;
		startLoading();
	}

	/**
	 * Shows a view at the top of the list with a pseudo local profile prompting the user to add
	 * a local profile. Default implementation does nothing.
	 */
	protected void setProfileHeader() {
		mUserProfileExists = false;
	}

	/**
	 * Provides logic that dismisses this fragment. The default implementation
	 * does nothing.
	 */
	protected void finish() {
	}

	public void setSectionHeaderDisplayEnabled(boolean flag) {
		if (mSectionHeaderDisplayEnabled != flag) {
			mSectionHeaderDisplayEnabled = flag;
			if (mAdapter != null) {
				mAdapter.setSectionHeaderDisplayEnabled(flag);
			}
			configureVerticalScrollbar();
		}
	}

	public boolean isSectionHeaderDisplayEnabled() {
		return mSectionHeaderDisplayEnabled;
	}

	public void setVisibleScrollbarEnabled(boolean flag) {
		if (mVisibleScrollbarEnabled != flag) {
			mVisibleScrollbarEnabled = flag;
			configureVerticalScrollbar();
		}
	}

	public boolean isVisibleScrollbarEnabled() {
		return mVisibleScrollbarEnabled;
	}

	public void setVerticalScrollbarPosition(int position) {
		if (mVerticalScrollbarPosition != position) {
			mVerticalScrollbarPosition = position;
			configureVerticalScrollbar();
		}
	}

	private void configureVerticalScrollbar() {
		//		boolean hasScrollbar = isVisibleScrollbarEnabled() && isSectionHeaderDisplayEnabled();

		boolean hasScrollbar=false;//liyang modify
		if (mListView != null) {
			mListView.setFastScrollEnabled(hasScrollbar);
			mListView.setFastScrollAlwaysVisible(hasScrollbar);
			mListView.setVerticalScrollbarPosition(mVerticalScrollbarPosition);
			mListView.setScrollBarStyle(ListView.SCROLLBARS_OUTSIDE_OVERLAY);
		}
	}

	public void setPhotoLoaderEnabled(boolean flag) {
		mPhotoLoaderEnabled = flag;
		configurePhotoLoader();
	}

	public boolean isPhotoLoaderEnabled() {
		return mPhotoLoaderEnabled;
	}

	/**
	 * Returns true if the list is supposed to visually highlight the selected item.
	 */
	public boolean isSelectionVisible() {
		return mSelectionVisible;
	}

	public void setSelectionVisible(boolean flag) {
		this.mSelectionVisible = flag;
	}

	public void setQuickContactEnabled(boolean flag) {
		this.mQuickContactEnabled = flag;
	}

	public void setAdjustSelectionBoundsEnabled(boolean flag) {
		mAdjustSelectionBoundsEnabled = flag;
	}

	public void setIncludeProfile(boolean flag) {
		mIncludeProfile = flag;
		if (mAdapter != null) {
			mAdapter.setIncludeProfile(flag);
		}
	}

	/**
	 * Enter/exit search mode. This is method is tightly related to the current query, and should
	 * only be called by {@link #setQueryString}.
	 *
	 * Also note this method doesn't call {@link #reloadData()}; {@link #setQueryString} does it.
	 */
	public void setSearchMode(boolean flag) {
		if (mSearchMode != flag) {
			mSearchMode = flag;
			setSectionHeaderDisplayEnabled(!mSearchMode);

			if (!flag) {
				mDirectoryListStatus = STATUS_NOT_LOADED;
				getLoaderManager().destroyLoader(DIRECTORY_LOADER_ID);
			}

			if (mAdapter != null) {
				mAdapter.setSearchMode(flag);

				mAdapter.clearPartitions();
				if (!flag) {
					// If we are switching from search to regular display, remove all directory
					// partitions after default one, assuming they are remote directories which
					// should be cleaned up on exiting the search mode.
					mAdapter.removeDirectoriesAfterDefault();
				}
				mAdapter.configureDefaultPartition(false, flag);
			}

			if (mListView != null) {
				//				mListView.setFastScrollEnabled(!flag);
				mListView.setFastScrollEnabled(false);
			}
		}
	}

	public final boolean isSearchMode() {
		return mSearchMode;
	}

	public final String getQueryString() {
		return mQueryString;
	}

	private boolean isForDialerSearch=false;	

	public boolean isForDialerSearch() {
		return isForDialerSearch;
	}
	public void setForDialerSearch(boolean isForDialerSearch) {
		this.isForDialerSearch = isForDialerSearch;
	}
	protected void initHbSearchView() {
		initHbSearchView(false);
	}
	protected void initHbSearchView(final boolean hasActionMode) {
		mEmptyView = (TextView) getView().findViewById(R.id.contact_list_empty);
		emptyLayout= getView().findViewById(R.id.empty_layout);
		if (mEmptyView != null) {
			mEmptyView.setText(R.string.hb_search_contacts_empty);
		}

		//		if(hasActionMode) {	
		//			hbSearchViewLayoutParams = (LinearLayout.LayoutParams)hbSearchViewLayout.getLayoutParams();
		//			setSearchViewMarginTop(getActivity().getApplicationContext().getResources().getDimensionPixelSize(R.dimen.hb_choice_activity_searchview_front_margin_top));
		//		}

		mHbSearchView=(HbSearchView) getView().findViewById(R.id.hb_searchview);		
		//		mHbSearchView.setQueryHintTextColor(getActivity().getResources().getColor(R.color.hb_searchview_hint_text_color));
		mHbSearchView.setIconifiedByDefault(false);
		mHbSearchView.setQueryHint(getActivity().getString(R.string.hb_searchview_hint));
		mHbSearchView.setOnQueryTextListener(new OnQueryTextListener(){
			@Override
			public boolean onQueryTextChange(String queryString) {
				Log.d(TAG,"onQueryTextChange,queryString:"+queryString);

				if (queryString.equals(mQueryString)) {
					Log.d(TAG,"onQueryTextChange1");
					return true;
				}

				Log.d(TAG,"onQueryTextChange3");

				setQueryString(queryString, true);
				//				setVisibleScrollbarEnabled(!isSearchMode());
				return false;
			}

			@Override
			public boolean onQueryTextSubmit(String str) {
				return false;
			}
		});

		mHbSearchView.setOnQueryTextFocusChangeListener(new OnFocusChangeListener(){
			public void onFocusChange(View v, boolean hasFocus){
				Log.d(TAG,"onFocusChange,mHbIsSearchMode:"+mHbIsSearchMode);
				if(!mHbIsSearchMode){
					enterSearchMode(hasActionMode);
					setShowEmptyListForNullQuery(true);
					setQueryString(null,true);
				}
			}
		});

		mCancelSearchButton=getView().findViewById(R.id.hb_cancel_search);
		mCancelSearchButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				exitSearchMode(hasActionMode);
			}
		});

		hb_search_view_include= getView().findViewById(R.id.hb_search_view_include);
		setShowEmptyListForNullQuery(true);

		hbPreSearchview=(HbPreSearchView) getView().findViewById(R.id.hb_presearchview);
		hbPreSearchview.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				hbPreSearchview.setVisibility(View.GONE);
				if(mHbSearchView!=null) {
					mHbSearchView.setVisibility(View.VISIBLE);
					mHbSearchView.setIconified(false);
				}
			}
		});
	}


	public void setQueryString(String queryString, boolean delaySelection) {
		Log.d(TAG,"setQueryString,queryString:"+queryString+" mQueryString:"+mQueryString+" mShowEmptyListForEmptyQuery:"+mShowEmptyListForEmptyQuery+" mAdapter:"+mAdapter+" mListView:"+mListView);
		if (!TextUtils.equals(mQueryString, queryString)) {
			if (mShowEmptyListForEmptyQuery && mAdapter != null && mListView != null) {
				if (TextUtils.isEmpty(mQueryString) || TextUtils.equals(EXIT_SEARCH, queryString)) {
					// Restore the adapter if the query used to be empty.
					Log.d(TAG,"setAdapter2");
					mListView.setAdapter(mAdapter);
				} else if (TextUtils.isEmpty(queryString)) {
					// Instantly clear the list view if the new query is empty.
					Log.d(TAG,"setAdapter null");
					mListView.setAdapter(null);
				} 
			}

			queryString= TextUtils.equals(EXIT_SEARCH, queryString)?null:queryString;
			mQueryString = queryString;
			setSearchMode(!TextUtils.isEmpty(mQueryString)/* || mShowEmptyListForEmptyQuery*/);
			if (mAdapter != null) {
				mAdapter.setQueryString(queryString);
				reloadData();
			}
		}else if(TextUtils.isEmpty(queryString)){
			mListView.setAdapter(null);
			mQueryString = queryString;
		}
	}


	protected Uri getGroupUriFromIdAndAccountInfo(long groupId, String accountName,
			String accountType){
		return null;
	}

	protected ContactsHelper mContactsHelper;

	public void setContactsHelper(ContactsHelper mContactsHelper) {
		this.mContactsHelper = mContactsHelper;
	}

	private TextView mSearchZero;
//	private View mSearchProgress;
	//	private FrameLayout headerContainer;
	//	private TextView mSearchProgressText;
	private boolean isWaitingQuerying=false;
	public void setLoadSearchContactsState(int loadSearchContactsState){

	}


	public void setShowEmptyListForNullQuery(boolean show) {
		Log.d(TAG,"setShowEmptyListForNullQuery:"+show);
		mShowEmptyListForEmptyQuery = show;
	}

	public int getDirectoryLoaderId() {
		return DIRECTORY_LOADER_ID;
	}

	public int getDirectorySearchMode() {
		return mDirectorySearchMode;
	}

	public void setDirectorySearchMode(int mode) {
		mDirectorySearchMode = mode;
	}

	public boolean isLegacyCompatibilityMode() {
		return mLegacyCompatibility;
	}

	public void setLegacyCompatibilityMode(boolean flag) {
		mLegacyCompatibility = flag;
	}

	protected int getContactNameDisplayOrder() {
		return mDisplayOrder;
	}

	protected void setContactNameDisplayOrder(int displayOrder) {
		mDisplayOrder = displayOrder;
		if (mAdapter != null) {
			mAdapter.setContactNameDisplayOrder(displayOrder);
		}
	}

	public int getSortOrder() {
		return mSortOrder;
	}

	public void setSortOrder(int sortOrder) {
		mSortOrder = sortOrder;
		if (mAdapter != null) {
			mAdapter.setSortOrder(sortOrder);
		}
	}

	public void setDirectoryResultLimit(int limit) {
		mDirectoryResultLimit = limit;
	}

	protected boolean loadPreferences() {
		boolean changed = false;
		if (getContactNameDisplayOrder() != mContactsPrefs.getDisplayOrder()) {
			setContactNameDisplayOrder(mContactsPrefs.getDisplayOrder());
			changed = true;
		}

		if (getSortOrder() != mContactsPrefs.getSortOrder()) {
			setSortOrder(mContactsPrefs.getSortOrder());
			changed = true;
		}

		return changed;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		onCreateView(inflater, container);
		/// M: should create list Adapter here for child class init the right status.
		//		mAdapter = createListAdapter();

		boolean searchMode = isSearchMode();
		mAdapter.setSearchMode(searchMode);
		mAdapter.configureDefaultPartition(false, searchMode);
		mAdapter.setPhotoLoader(mPhotoManager);
		Log.d(TAG,"setAdapter");

		if(mListView!=null){
			mListView.setAdapter(mAdapter);

			if (!isSearchMode()) {
				mListView.setFocusableInTouchMode(true);
				mListView.requestFocus();
			}
		}

		getAdapter().setFragmentRootView(getView());
		return mView;
	}

	protected View emptyView;
	protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
		mView = inflateView(inflater, container);
		createView();

	}

	private void createView(){
		mListView = (ListView)mView.findViewById(android.R.id.list);
		mListView.setVerticalScrollBarEnabled(false);
		if(isForContactsChoice) {
			LinearLayout.LayoutParams params=(LinearLayout.LayoutParams) mListView.getLayoutParams();
			params.setMarginEnd(mContext.getResources().getDimensionPixelOffset(R.dimen.hb_contact_listview_margin_right));
			mListView.setLayoutParams(params);
		}
		Log.d(TAG,"createView(),mListView:"+mListView);
		if (mListView == null) {
			throw new RuntimeException(
					"Your content must have a ListView whose id attribute is " +
					"'android.R.id.list'");
		}

		View emptyView = mView.findViewById(android.R.id.empty);
		if (emptyView != null) {
			Log.d(TAG,"setEmptyView");
			mListView.setEmptyView(emptyView);
		}

		mListView.setOnItemClickListener(this);
		mListView.setOnItemLongClickListener(this);
		mListView.setOnFocusChangeListener(this);
		mListView.setOnTouchListener(this);
		//		mListView.setFastScrollEnabled(!isSearchMode());
		mListView.setFastScrollEnabled(false);

		// Tell list view to not show dividers. We'll do it ourself so that we can *not* show
		// them when an A-Z headers is visible.
		mListView.setDividerHeight(0);

		// We manually save/restore the listview state
		mListView.setSaveEnabled(false);

		configureVerticalScrollbar();

		configurePhotoLoader();

		getAdapter().setFragmentRootView(getView());

		ContactListViewUtils.applyCardPaddingToView(getResources(), mListView, mView);
		mListView.setVisibility(View.VISIBLE);
		mListView.setOnScrollListener(this);
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);
		if (getActivity() != null && getView() != null && !hidden) {
			// If the padding was last applied when in a hidden state, it may have been applied
			// incorrectly. Therefore we need to reapply it.
			ContactListViewUtils.applyCardPaddingToView(getResources(), mListView, getView());
		}
	}

	protected void configurePhotoLoader() {
		if (isPhotoLoaderEnabled() && mContext != null) {
			if (mPhotoManager == null) {
				mPhotoManager = ContactPhotoManager.getInstance(mContext);
			}			
			if (mAdapter != null) {
				mAdapter.setPhotoLoader(mPhotoManager);
			}
		}
	}

	protected void configureAdapter() {
		if (mAdapter == null) {
			return;
		}

		mAdapter.setQuickContactEnabled(mQuickContactEnabled);
		mAdapter.setAdjustSelectionBoundsEnabled(mAdjustSelectionBoundsEnabled);
		mAdapter.setIncludeProfile(mIncludeProfile);
		//		mAdapter.setQueryString(mQueryString);
		mAdapter.setDirectorySearchMode(mDirectorySearchMode);
		mAdapter.setPinnedPartitionHeadersEnabled(false);
		mAdapter.setContactNameDisplayOrder(mDisplayOrder);
		mAdapter.setSortOrder(mSortOrder);
		mAdapter.setSectionHeaderDisplayEnabled(mSectionHeaderDisplayEnabled);
		mAdapter.setSelectionVisible(mSelectionVisible);
		mAdapter.setDirectoryResultLimit(mDirectoryResultLimit);
		mAdapter.setDarkTheme(mDarkTheme);
		mAdapter.setmCallbacks(mCallbacks);
		mAdapter.setForContactsChoice(isForContactsChoice);
	}



	public int mPosition=0;
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		//		if (scrollState == OnScrollListener.SCROLL_STATE_FLING) {
		//			mPhotoManager.pause();
		//		} else if (isPhotoLoaderEnabled()) {
		//			mPhotoManager.resume();
		//		}

		//		// 不滚动时保存当前滚动到的位置  
		//        if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
		//            mPosition = mListView.getFirstVisiblePosition(); 
		//            Log.d(TAG,"onScrollStateChanged,mPosition:"+mPosition);
		//        }  
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		hideSoftKeyboard();

		int adjPosition = position - mListView.getHeaderViewsCount();
		if (adjPosition >= 0) {
			onItemClick(adjPosition, id);
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		int adjPosition = position - mListView.getHeaderViewsCount();

		if (adjPosition >= 0) {
			return onItemLongClick(adjPosition, id);
		}
		return false;
	}

	private void hideSoftKeyboard() {
		// Hide soft keyboard, if visible
		InputMethodManager inputMethodManager = (InputMethodManager)
				mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(mListView.getWindowToken(), 0);
	}

	/**
	 * Dismisses the soft keyboard when the list takes focus.
	 */
	@Override
	public void onFocusChange(View view, boolean hasFocus) {
		if (view == mListView && hasFocus) {
			hideSoftKeyboard();
		}
	}

	/**
	 * Dismisses the soft keyboard when the list is touched.
	 */
	@Override
	public boolean onTouch(View view, MotionEvent event) {
		if (view == mListView) {
			hideSoftKeyboard();
		}
		return false;
	}

	@Override
	public void onPause() {
		Log.d(TAG,"onPause");
		super.onPause();
		removePendingDirectorySearchRequests();
	}

	/**
	 * Restore the list state after the adapter is populated.
	 */
	protected void completeRestoreInstanceState() {
		if (mListState != null) {
			mListView.onRestoreInstanceState(mListState);
			Log.d(TAG, "completeRestoreInstanceState(),the Activity may be killed." +
					"Restore the listview state last");
			mListState = null;
		}
	}

	public void setDarkTheme(boolean value) {
		mDarkTheme = value;
		if (mAdapter != null) mAdapter.setDarkTheme(value);
	}

	/**
	 * Processes a result returned by the contact picker.
	 */
	public void onPickerResult(Intent data) {
		throw new UnsupportedOperationException("Picker result handler is not implemented.");
	}

	private ContactsPreferences.ChangeListener mPreferencesChangeListener =
			new ContactsPreferences.ChangeListener() {
		@Override
		public void onChange() {
			Log.d(TAG,"ContactsPreferences onChange");
			loadPreferences();
			reloadData();
		}
	};

	private int getDefaultVerticalScrollbarPosition() {
		final Locale locale = Locale.getDefault();
		final int layoutDirection = TextUtils.getLayoutDirectionFromLocale(locale);
		switch (layoutDirection) {
		case View.LAYOUT_DIRECTION_RTL:
			return View.SCROLLBAR_POSITION_LEFT;
		case View.LAYOUT_DIRECTION_LTR:
		default:
			return View.SCROLLBAR_POSITION_RIGHT;
		}
	}


	/**
	 * M: in some case,restore the ListView last user will replace the new state.
	 * so should provider this function to child class to prevent restore state.
	 */
	protected void clearListViewLastState() {
		Log.d(TAG, "#clearListViewLastState()");
		mListState = null;
	}

	/**
	 * M: By default we show SDN Number in screen.
	 */
	public boolean isShowSdnNumber() {
		return true;
	}
	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// TODO Auto-generated method stub

	}

	//	/**  
	//	 * 获取汉字串拼音，英文字符不变  
	//	 * @param chinese 汉字串  	
	//	 * @return 汉语拼音  
	//	 */   
	//	private HanyuPinyinOutputFormat format =null;
	//	private StringBuilder quanPinyinBuilder=new StringBuilder();
	//	private StringBuilder jianPinyinBuilder=new StringBuilder();
	//	public void getFullSpell(String chinese) {
	//		if(TextUtils.isEmpty(chinese)) return;
	//		quanPinyinBuilder=new StringBuilder();
	//		jianPinyinBuilder=new StringBuilder();
	//		StringBuffer pybf = new StringBuffer();   
	//		char[] arr = chinese.toCharArray();    
	//		for (int i = 0; i < arr.length; i++) {   
	//			if (arr[i] > 128) {   
	//				try {   
	//					String[] temp = PinyinHelper.toHanyuPinyinStringArray(arr[i], format);   
	//					if (temp != null) {   						
	//						quanPinyinBuilder.append(temp[0]); 
	//						jianPinyinBuilder.append(temp[0].charAt(0)); 
	//					}   
	//
	//				} catch (BadHanyuPinyinOutputFormatCombination e) {   
	//					e.printStackTrace();   
	//				}   
	//			} else {   
	//				quanPinyinBuilder.append(arr[i]);   
	//				jianPinyinBuilder.append(arr[i]); 
	//			}   
	//		}   
	//	}  

	//add by liyang	
	private enum TaskStatus {
		NEW, RUNNING, FINISHED, CANCELED
	}

	private class InitIndexBarTask extends AsyncTask<Void, Void, Boolean> {
		private InitIndexBarTask mInstance = null;
		private TaskStatus mTaskStatus;
		private boolean mResult;
		private String str_other = mContext.getString(R.string.hb_other_string);


		private InitIndexBarTask() {
			super();
			mTaskStatus = TaskStatus.NEW;
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			mTaskStatus = TaskStatus.CANCELED;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			Log.d(TAG,"onPostExecute,result:"+result);
			mIndexBar.invalidate();
			super.onPostExecute(result);
			mTaskStatus = TaskStatus.FINISHED;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			if(allCursor==null||allCursor.getCount()==0) return false;
			mTaskStatus = TaskStatus.RUNNING;
			long time1=System.currentTimeMillis();
			final Cursor data=allCursor;
			Log.d(TAG,"doInBackground,data:"+data);

			//			Log.d(TAG,"_PROJECTION111:"+Arrays.toString(_PROJECTION)+" this:"+ContactEntryListFragment.this);				
			//			for(int i=0;i<data.getColumnCount();i++) Log.d(TAG,"i:"+i+" column name:"+data.getColumnName(i));


			contactForIndexs=new ArrayList<ContactForIndex>();
			indexArrayList=new ArrayList<Integer>();
			data.moveToFirst();
			String last = null;

			String pinyin="*";
			String jian="*";
			String name;
			ContactForIndex contactForIndex;
			int phonebook_bucket;
			String fistLetter;
			ContactForIndex g;
			int indexCount=0;
			boolean hasAddOther=false;



			if(mStarredCount>0) {
				++indexCount;
				data.moveToPosition(mStarredCount);
			}
			Log.d(TAG, "nameColumnIndex:"+nameColumnIndex+" quanpinyinColumnIndex:"+quanpinyinColumnIndex+" jianpinyinColumnIndex:"+jianpinyinColumnIndex);

			for(int i=mStarredCount;i<allCount;i++){
				contactForIndex=new ContactForIndex();
				if(data.isAfterLast()) break;
				name=data.getString(nameColumnIndex);
				String tempPinyin=data.getString(quanpinyinColumnIndex);
				String tempJian=data.getString(jianpinyinColumnIndex);
				if(!TextUtils.isEmpty(tempPinyin)) {
					tempPinyin=tempPinyin.toUpperCase();
					pinyin=tempPinyin;
				}
				if(!TextUtils.isEmpty(tempJian)) {
					tempJian=tempJian.toUpperCase();
					jian=tempJian;
				}

				contactForIndex.name=name;
				contactForIndex.pinyin=pinyin;

				phonebook_bucket=data.getInt(phonebookbucketColumnIndex);
				if(phonebook_bucket<27){
					for (int k = 0;k<jian.length(); k++) {
						fistLetter = jian.substring(k,k+1);
						if(k == 0){
							if(!fistLetter.equals(last)){
								g = new ContactForIndex();
								g.name = fistLetter;
								g.type = 1;
								contactForIndexs.add(g);
								indexArrayList.add(i+indexCount);
								indexCount++;
							}
							last = fistLetter;
						}
						contactForIndex.firstLetter.add(fistLetter);
					}
				}else{
					if(!hasAddOther){
						g = new ContactForIndex();
						g.name = str_other;
						g.type = 1;
						contactForIndexs.add(g);
						hasAddOther=true;
					}
					contactForIndex.firstLetter.add(str_other);
				}

				contactForIndexs.add(contactForIndex);
				data.moveToNext();
			}
			Log.d(TAG,"spend1:"+(System.currentTimeMillis()-time1));

			initIndexBar(contactForIndexs);
			Log.d(TAG,"spend2:"+(System.currentTimeMillis()-time1));

			return true;

		}

		public boolean isTaskRunning() {
			return mTaskStatus == TaskStatus.RUNNING;
		}

		public boolean getResult() {
			return mResult;
		}

		public boolean isTaskFinished() {
			return mTaskStatus == TaskStatus.FINISHED;
		}

		public void abort() {
			if (mInstance != null) {
				Log.d(TAG, "mInstance.cancel(true)");
				mInstance.cancel(true);
				mInstance = null;
			}
		}

		public InitIndexBarTask createNewTask() {
			if (mInstance != null) {
				Log.d(TAG, "cancel existing task instance");
				mInstance.abort();
			}
			mInstance = new InitIndexBarTask();
			return mInstance;
		}

		public InitIndexBarTask getExistTask() {
			return mInstance;
		}

	}


	private InitIndexBarTask mTask;


	@Override
	public void onStateChanged(HbIndexBar.TouchState old, HbIndexBar.TouchState news) {
		Log.d(TAG,"Touch state : "+news);
		if(getAdapter().isSelectMode()) return;
		if(TextUtils.equals("DOWN", news.toString())){
			((TransactionSafeActivity)getActivity()).showFAB(false);
		}else if(TextUtils.equals("UP", news.toString())){
			((TransactionSafeActivity)getActivity()).showFAB(true);
		}

	}

	@Override
	public void onSelect(int index, int layer, HbIndexBar.Letter letter) {
		//		Log.d(TAG,"onSelect0,index:"+index+" layer:"+layer+" letter:"+letter.text);
		if(index==0&&layer == 0){
			getListView().setSelection(0);
			return;
		}
		int listindex = letter.list_index;
		//		Log.d(TAG,"listindex0:"+listindex);
		//		listindex--;
		if(layer == 0){
		}
		int offset=1;

		//		for(int k:indexArrayList) Log.d(TAG, "k:"+k);
		for(int k:indexArrayList){
			if(k<listindex) offset++;
			if(k>=listindex) break;
		}
		//		Log.d(TAG,"onSelect,index:"+index+" letter:"+letter.text+" listindex:"+listindex+" offset:"+offset
		//				+" indexArrayList:"+indexArrayList+"getListView().getHeaderViewsCount():"+getListView().getHeaderViewsCount());

		if(mStarredCount==0) listindex++;
		getListView().setSelection/*FromTop*/(listindex+getListView().getHeaderViewsCount()-offset/*,
				layer==0?-DensityUtil.dip2px(getContext(),9):0*/);
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		//		Log.d(TAG,"onScroll,firstVisibleItem:"+firstVisibleItem+" visibleItemCount:"+visibleItemCount+" totalItemCount:"+totalItemCount);
		if(mIndexBar==null) return;
		int position=firstVisibleItem;
		int section = getAdapter().getSectionForPosition(position);
		if(section<0) return;
		if(section==0 && mStarredCount>0) {
			//			Log.d(TAG,"onScroll1,section:"+section);
			mIndexBar.setFocus(0);
			return;
		}
		String fir= (String)getAdapter().getSections()[section];

		int index = -1;
		index = mIndexBar.getIndex(fir);

		if(index == -1){
			index = mIndexBar.size() - 1;
		}

		//		Log.d(TAG,"onScroll2,index   :"+index+" mStarredCount:"+mStarredCount);
		mIndexBar.setFocus(index);
	}


	private void initIndexBar(ArrayList<ContactForIndex> array){
		//				Log.d(TAG,"initIndexBar1:"+array);
		//				for(ContactForIndex item:array){
		//					Log.d(TAG,"item:"+item.toString());
		//				}
		if(array==null) return;
		for(int m=0;m<28;m++){
			mIndexBar.setEnables(false,m);
		}


		if(mStarredCount>0) mIndexBar.setEnables(true,0);

		String last = "";
		boolean changed = false;
		ContactForIndex c=null;
		int namesize;
		int index;
		String firletter=null;
		Letter letter=null;
		for(int p=0;p<array.size();p++){
			c = (ContactForIndex) array.get(p);
			namesize = c.firstLetter.size();
			firletter = "";
			for(int i=0;i<namesize;i++) {
				if(i == 0) {
					firletter = c.firstLetter.get(0);
					changed = !firletter.equals(last);
					last = firletter;
				}
			}
			if(changed){
				if(!"".equals(firletter)) {
					index = mIndexBar.getIndex(firletter);

					if(index == -1){//其他（#）的索引
						index = 27;
					}
					//设置第一个字母对应的列表索引
					letter = mIndexBar.getLetter(index);
					if(letter != null){
						letter.list_index = p+mStarredCount;
					}
					Log.d(TAG,"setEnables,index:"+index);
					mIndexBar.setEnables(true,index);
				}
			}
		}
	}

	//	private void setSearchViewMarginTop(int marginTop){
	//		int marginLeft=getActivity().getApplicationContext().getResources().getDimensionPixelSize(R.dimen.hb_contactlist_searchview_margin_left);
	//		hbSearchViewLayoutParams.setMargins(marginLeft, 
	//				marginTop, 
	//				marginLeft,
	//				marginLeft);
	//		hbSearchViewLayout.setLayoutParams(hbSearchViewLayoutParams);	
	//	}

	public void exitSearchMode(boolean hasActionMode){
		Log.d(TAG,"exitSearchMode:"+hasActionMode);
		if(mHbSearchView==null) return;
		final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		final View currentFocus = getActivity().getCurrentFocus();
		if (imm != null && currentFocus != null) {
			imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
		}
		
		mCancelSearchButton.setVisibility(View.GONE);
		((TransactionSafeActivity)getActivity()).toolbar.setVisibility(View.VISIBLE);
		if(hasActionMode) {
			((TransactionSafeActivity)getActivity()).showActionMode(true);
		}
		
		
		if(mHbSearchView!=null) {
			mHbSearchView.clearFocus();
			mHbSearchView.setVisibility(View.GONE);
			if(TextUtils.isEmpty(mHbSearchView.getQuery())){
				setQueryString(EXIT_SEARCH, true);
			}else{
				setShowEmptyListForNullQuery(false);
				mHbSearchView.setQuery(null,false);
			}
		}

		mHbIsSearchMode=false;
		mIndexBar.setVisibility(View.VISIBLE);
		mListView.setVisibility(View.VISIBLE);
		mQueryString=null;
		
		if(hbPreSearchview!=null) hbPreSearchview.setVisibility(View.VISIBLE);
	}

	public void enterSearchMode(boolean hasActionMode){
		Log.d(TAG,"enterSearchMode:"+hasActionMode);
		if(mHbSearchView==null) return;
		mCancelSearchButton.setVisibility(View.VISIBLE);
		((TransactionSafeActivity)getActivity()).toolbar.setVisibility(View.GONE);
		if(hasActionMode) {			
			((TransactionSafeActivity)getActivity()).showActionMode(false);
		}
		mHbIsSearchMode=true;
		mIndexBar.setVisibility(View.GONE);
		mListView.setVisibility(View.GONE);
		mQueryString=null;
	}

	public boolean mHbIsSearchMode;
	public String mQueryString;
	public HbSearchView mHbSearchView;
	public HbPreSearchView hbPreSearchview;
	public View mCancelSearchButton;
	public TextView mEmptyView = null;
	public View emptyLayout;
	public View hb_search_view_include;
	public int nameColumnIndex;
	public int quanpinyinColumnIndex;
	public int jianpinyinColumnIndex;
	public int phonebookbucketColumnIndex;
	public int contactIdIndex;
	public int numberIndex;
	public static String[] _PROJECTION;
	public int snippetColumnIndex;

	
	protected HbSearchContactsAdapter mContactsAdapter;
	private void refreshContactsLv() {
		if (null == mListView) {
			return;
		}
		mContactsAdapter.setContacts(mContactsHelper
                .getSearchContacts());
		Log.d(TAG,"mListView:"+mListView+"\ncontactsAdapter:"+mContactsAdapter+"\nlistview.getAdapter:"+mListView.getAdapter()
				+"\nsize:"+mContactsHelper
				.getSearchContacts().size()+"\nmContactsAdapter.getCount() :"+mContactsAdapter.getCount());
		if (null != mContactsAdapter) {
			mContactsAdapter.notifyDataSetChanged();
			if (mContactsAdapter.getCount() > 0) {
				if(mSearchZero!=null) mSearchZero.setVisibility(View.GONE);
				ViewUtil.showView(mListView);
			} else {
				if(mSearchZero!=null){
					mSearchZero.setVisibility(View.VISIBLE);
					mSearchZero.setText(R.string.hb_search_contacts_empty);
				}
				ViewUtil.hideView(mListView);
			}
		}
	}
	
	private OnContactsChanged onContactsChanged=new OnContactsChanged() {

		@Override
		public void onContactsChanged() {
			// TODO Auto-generated method stub
			Log.d(TAG,"onContactsChanged");
			setQueryStringHb(mQueryString);
		}
	};
	
	public void setQueryStringHb(String queryString){
		Log.d(TAG,"setQueryStringHb:"+queryString+" isForDialerSearch:"+isForDialerSearch+" mContactsAdapter:"+mContactsAdapter);

		if(mContactsHelper==null) return;
		
		if(mContactsAdapter==null) {
			mContactsAdapter = new HbSearchContactsAdapter(getContext(),true);

			mContactsAdapter.setContacts(mContactsHelper.getSearchContacts());
			mContactsAdapter.setActivity(getActivity());
			//mContactsAdapter.setOnClickGroupListener(onClickGroupListener);
//			ListView listview = (ListView)getView().findViewById(android.R.id.list);
//			listview.setVisibility(View.GONE);
//			mListView = (ListView)getView().findViewById(R.id.hb_search_listview);
			mListView.setBackgroundColor(mContext.getResources().getColor(R.color.contact_main_background));
			if(mSearchZero==null){		
//				mSearchProgress = getView().findViewById(R.id.search_progress);
				mSearchZero=(TextView)getView().findViewById(R.id.mSearchZero);
			}

			//			mListView.addHeaderView(headerContainer, null, false);
			mListView.setOnItemClickListener(this);
			mListView.setOnItemLongClickListener(this);
			mListView.setOnFocusChangeListener(this);
			mListView.setOnTouchListener(this);
			//		mListView.setFastScrollEnabled(!isSearchMode());
			mListView.setFastScrollEnabled(false);
			// Tell list view to not show dividers. We'll do it ourself so that we can *not* show
			// them when an A-Z headers is visible.
			mListView.setDividerHeight(0);
			// We manually save/restore the listview state
			mListView.setSaveEnabled(false);		
			mListView.setAdapter(mContactsAdapter);
//			mIndexBar.setVisibility(View.GONE);
			mContactsHelper.setmOnContactsChanged(onContactsChanged);
			mContactsHelper.t9Search(null);
//			mContactsHelper.setOnContactsLoad(this);		
		}

//		if(TextUtils.equals(SEARCH_BEGIN_STRING, queryString)){}

		if(isForDialerSearch&&TextUtils.equals(queryString, mQueryString)) return;

		if(mContactsAdapter!=null&&mListView!=null){
			if (TextUtils.isEmpty(mQueryString)) {
				Log.d(TAG,"mQueryString null");
				// Restore the adapter if the query used to be empty.
				mListView.setAdapter(mContactsAdapter);
				mContactsHelper.setmOnContactsChanged(onContactsChanged);
				mContactsHelper.t9Search(null);
			}
			mQueryString = queryString;
			if (TextUtils.isEmpty(queryString)) {
				// Instantly clear the list view if the new query is empty.
				mContactsHelper.t9Search(null);
				mListView.setAdapter(null);
//				mContactsHelper.setOnContactsLoad(this);
			}else{
				Log.d(TAG,"allCount:"+allCount+" isLoadingContacts:"+mContactsHelper.isLoadingContacts()+" isStartedLoadingContacts:"+mContactsHelper.isStartedLoadingContacts());
				if(mContactsHelper.isLoadingContacts()||!mContactsHelper.isStartedLoadingContacts()){//如果联系人正在准备中					
					isWaitingQuerying=true;
					//					mSearchHeaderView.setVisibility(View.VISIBLE);
					//						mSearchProgressText.setText(R.string.search_results_searching);
					showSearchProgress(true);
					//					mSearchZero.setText("正在准备搜索数据...");
					//					mSearchZero.setVisibility(View.VISIBLE);
					return;
				}
				mContactsHelper.query(queryString);		        
			}

			mContactsAdapter.setQueryString(queryString);
			setSearchMode(!TextUtils.isEmpty(mQueryString) || mShowEmptyListForEmptyQuery);

			refreshContactsLv();
		}
	}
	
	private void showSearchProgress(boolean show) {
//		if (mSearchProgress != null) {
//			mSearchProgress.setVisibility(show ? View.VISIBLE : View.GONE);
//		}
	}
}
