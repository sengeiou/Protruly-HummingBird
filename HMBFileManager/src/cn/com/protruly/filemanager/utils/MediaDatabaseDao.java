/**
  * Generated by smali2java 1.0.0.558
  * Copyright (C) 2013 Hensence.com
  */

package cn.com.protruly.filemanager.utils;

import android.Manifest;
import android.content.Context;
import android.content.ContentResolver;

import cn.com.protruly.filemanager.CategoryActivity;
import cn.com.protruly.filemanager.enums.Category;
import cn.com.protruly.filemanager.utils.GlobalConstants;
import cn.com.protruly.filemanager.utils.MediaFileUtil;
import cn.com.protruly.filemanager.utils.Util;

import android.content.pm.PackageManager;
import android.provider.MediaStore;
import android.text.TextUtils;
//import cn.com.protruly.filemanager.GlobalParams;
import java.util.ArrayList;
import java.util.List;

import android.net.Uri;
import android.content.ContentValues;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.database.Cursor;

public class MediaDatabaseDao {
    private Context mContext;
    private ContentResolver mResolver;
    
    public MediaDatabaseDao(Context context) {
        mContext = context;
        if(null != mContext) {
            mResolver = mContext.getContentResolver();
        }
    }

    public Uri getContentUriFromPath(String path,int category){
        if(null == mResolver) return null;
        Uri uri = getUriByType(category);
        String[] projection = {"_id"};
        String[] selectionArgs = {path};
        Cursor cursor = null;
        try{
            cursor = mResolver.query(uri,projection,"_data like ?",selectionArgs,null);
            Log.d("bql","getContentUriFromPath cursor:"+cursor);
            if(cursor != null && cursor.moveToNext()){
                String _id = cursor.getString(cursor.getColumnIndex("_id"));
                if(uri==GlobalConstants.PICTURE_URI){
                    return Uri.withAppendedPath(GlobalConstants.PICTURE_URI,_id);
                }else if(uri==GlobalConstants.VIDEO_URI){
                    return Uri.withAppendedPath(GlobalConstants.VIDEO_URI,_id);
                }
            }
        }catch(SQLiteException e){
            if(cursor != null){
                cursor.close();
            }
        }finally {
            if(cursor != null){
                cursor.close();
            }
        }
        return null;
    }

    public Uri getContentUriFromPathForMusic(String path,int category){
        if(null == mResolver) return null;
        Uri uri = getUriByType(category);
        String[] projection = {"_id"};
        String[] selectionArgs = {path};
        Cursor cursor = null;
        try{
            cursor = mResolver.query(uri,projection,"_data like ?",selectionArgs,null);
            Log.d("bql","getContentUriFromPath cursor:"+cursor);
            if(cursor != null && cursor.moveToNext()){
                String _id = cursor.getString(cursor.getColumnIndex("_id"));
                if(uri==GlobalConstants.PICTURE_URI){
                    return Uri.withAppendedPath(GlobalConstants.PICTURE_URI,_id);
                }else if(uri==GlobalConstants.VIDEO_URI){
                    return Uri.withAppendedPath(GlobalConstants.VIDEO_URI,_id);
                }else if(uri==GlobalConstants.MUSIC_URI){
                    return Uri.withAppendedPath(GlobalConstants.MUSIC_URI,_id);
                }
            }
        }catch(SQLiteException e){
            if(cursor != null){
                cursor.close();
            }
        }finally {
            if(cursor != null){
                cursor.close();
            }
        }
        return null;
    }

    public Uri getUriByType(int  category){
        switch(category){
            case Category.Music:
                return GlobalConstants.MUSIC_URI;
            case Category.Picture:
                return GlobalConstants.PICTURE_URI;
            case Category.Video:
                return GlobalConstants.VIDEO_URI;
            default:
                return GlobalConstants.FILES_URI;
        }
    }

    public int bulkInsert(List<ContentValues> valuesList) {
        if(null == mResolver) return -2;
        Uri uri = GlobalConstants.FILES_URI;
        try {
            return mResolver.bulkInsert(uri, (ContentValues[])valuesList.toArray(new ContentValues[valuesList.size()]));
        } catch(SQLiteException e) {
            Log.w("bql", e);
        }
        return -1;
    }

    public Cursor getHomeHistoryInfo(Context context) {
        if(null == mResolver) return null;
        Uri uri = GlobalConstants.FILES_URI;
        String[] projection = {"_data", "format"};
        String selection = buildHistorySelection(context,Category.History);
        String[] selectionArgs = buildHistorySelectionArgs();
        String sortOrder = "date_modified desc";
        try {
            return mResolver.query(uri, projection, selection, selectionArgs, sortOrder);
        } catch (SQLiteException e) {
            Log.d("bql", e+"");
        }
        return null;
    }

    public Cursor getHistoryInfo(Context context) {
        if(null == mResolver) return null;
        Uri uri = GlobalConstants.FILES_URI;
        String[] projection = {"_data", "date_modified", "format", "_size"};
        String selection = buildHistorySelection(context,Category.History);
        String[] selectionArgs = buildHistorySelectionArgs();
        String sortOrder = "date_modified desc";
        try {
            return mResolver.query(uri, projection, selection, selectionArgs, sortOrder);
        } catch (SQLiteException localSQLiteException) {
            Log.d("bql", localSQLiteException+"");
        }
        return null;
    }

    public Cursor getCategoryInfo(Context context,Integer category) {
        if(null == mResolver) return null;
        Uri uri = getUriByCategory(category);
        String[] projection = {"_data"};
        String selection = buildSelection(context,category);
        String[] selectionArgs = buildSelectionArgs();
        try {
            Log.d("bql","uri:"+uri);
            Log.d("bql","projection:"+projection);
            Log.d("bql","selection:"+selection);
            return mResolver.query(uri, projection, selection, selectionArgs, null);
        } catch (SQLiteException e) {
            Log.w("bql", e);
        } catch (Exception e) {
            Log.w("bql", e);
        }finally {

        }
        return null;
    }

    private String[] buildSelectionArgs() {
        ArrayList<String> selectionArgs = new ArrayList<String>();
        for(String storagePath : Util.getStoragePathListSecond(mContext)) {
            String selection = storagePath + "/%";
            selectionArgs.add(selection);
        }
        Log.d("bql","selectionArgs:"+selectionArgs);
        return selectionArgs.toArray(new String[selectionArgs.size()]);
    }

    private String[] buildHistorySelectionArgs() {
        ArrayList<String> selectionArgs = new ArrayList<String>();
        selectionArgs.add("/storage/emulated/0/Android/data/com.tencent.mobileqq/Tencent/QQfile_recv/%");
        selectionArgs.add("/storage/emulated/0/Android/data/com.tencent.mobileqq/files/download/%");
        selectionArgs.add("/storage/emulated/0/Pictures/Screenshots/%");
        selectionArgs.add("/storage/emulated/0/Movies/%");
        selectionArgs.add("/storage/emulated/0/Music/%");
        selectionArgs.add("/storage/emulated/0/Bluetooth/%");
        selectionArgs.add("/storage/emulated/0/Download/%");
        selectionArgs.add("/storage/emulated/0/tencent/MicroMsg/WeiXin/%");
        return selectionArgs.toArray(new String[selectionArgs.size()]);
    }

    public Uri getUriByCategory(Integer category){
        switch(category){
            case Category.Music:
                return GlobalConstants.MUSIC_URI;
            case Category.Picture:
                return GlobalConstants.PICTURE_URI;
            case Category.Video:
                return GlobalConstants.VIDEO_URI;
            default:
                return GlobalConstants.FILES_URI;
        }
    }

    public Uri insert(ContentValues values){
        Uri uri = GlobalConstants.FILES_URI;
        try{
            return mResolver.insert(uri,values);
        }catch(SQLiteException sqLiteException){
            Log.d("bql",sqLiteException+"'");
        }
        return null;
    }

    public int deleteSingle(String deleteStr){
        if(null == mResolver) return -2;
        String[] selectionArgs = new String[]{deleteStr};
        Uri uri = GlobalConstants.FILES_URI;
        try{
            return mResolver.delete(uri,"_data like ?",selectionArgs);
        }catch(SQLiteException sqLiteException){
            Log.d("bql",sqLiteException+"'");
        }
        return -1;

    }

    public int delete(String deleteStr){
        if(null == mResolver) return -2;
        String[] selectionArgs = new String[]{deleteStr,deleteStr+"/%"};
        Uri uri = GlobalConstants.FILES_URI;
        try{
            return mResolver.delete(uri,"_data like ? OR _data like ?",selectionArgs);
        }catch(SQLiteException sqLiteException){
            Log.d("bql",sqLiteException+"'");
        }
        return -1;
    }

    private String buildSelection(Context context,int category) {
        return buildFilterIllegalStr() + buildCategorySelection(category) +buildFilterHiddenStr(context);
    }

    private String buildHistorySelection(Context context,int category){
        return buildHIstoryFilterIllegalStr() + buildCategorySelection(category) + buildFilterHiddenStr(context) + buildFilterCurrentWeekStr(1);
    }

    private String buildFilterCurrentWeekStr(int state) {
        if (state == 1) {
            long l = (System.currentTimeMillis()/1000 - 7*24*60*60);
            return " AND (date_modified > " + l + ")";
        }
        return "";
    }

    private String buildFilterIllegalStr() {
        StringBuilder localStringBuilder = new StringBuilder();
        for (int i = 0; i < Util.getStoragePathListSecond(mContext).size(); i++)
            localStringBuilder.append("_data like ?" + " OR ");
        return "(" + localStringBuilder.substring(0, localStringBuilder.lastIndexOf(" OR ")) + ")";
    }

    private String buildHIstoryFilterIllegalStr() {
        StringBuilder localStringBuilder = new StringBuilder();
        for (int i = 0; i < 8; i++)
             localStringBuilder.append("_data like ?" + " OR ");
        return "(" + localStringBuilder.substring(0, localStringBuilder.lastIndexOf(" OR ")) + ")";
    }

    private String buildCategorySelection(int category) {
        String categorySelection = MediaFileUtil.getTypeSet(category);
        if(TextUtils.isEmpty(categorySelection)) {
            return "";
        }
        return " AND " + categorySelection;
    }

    private String buildFilterHiddenStr(Context context) {
        if(!((CategoryActivity)context).getHidenStatus()) {
            return "";
        }
        return " AND (_data not like \'%/.%\')";
    }
}
    