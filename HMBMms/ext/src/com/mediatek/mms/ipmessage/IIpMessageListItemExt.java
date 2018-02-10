/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
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

package com.mediatek.mms.ipmessage;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;

import com.mediatek.mms.callback.IMessageListItemCallback;;

public interface IIpMessageListItemExt {

    /**
     * M: called on onFinishInflateView
     * @param context: context
     * @param bodyTextView: bodyTextView
     * @param ipMessageItemCallback: MessageItem callback
     * @param handler: handler
     * @param itemView: itemView
     * @return boolean
     */
    public boolean onIpFinishInflate(Context context,
            TextView bodyTextView, IMessageListItemCallback ipMessageItemCallback,
            Handler handler, LinearLayout itemView);

    /**
     * M: called on onBind
     * @param ipMessageItem: ipMessageItem
     * @param msgId: msgId
     * @param ipMessageId: ipMessageId
     * @param isDeleteMode: isDeleteMode
     * @return boolean
     */
    public boolean onIpBind(IIpMessageItemExt ipMessageItem,
            long msgId, long ipMessageId, boolean isDeleteMode);

    /**
     * M: called on onDetachedFromWindow
     * @return boolean
     */
    public boolean onIpDetachedFromWindow();

    /**
     * M: called on onMessageListItemClick
     * @return boolean
     */
    public boolean onIpMessageListItemClick();

    /**
     * M: called on setBodyTextSize
     * @param size: size
     * @return boolean
     */
    public boolean onIpSetBodyTextSize(float size);

    /**
     * M: called on bindDefault
     * @return boolean
     */
    public boolean onIpBindDefault(IIpMessageItemExt item);

    /**
     * M: called on setImage
     * @return boolean
     */
    public boolean onIpSetMmsImage();

    /**
     * M: called on findView
     * @param id: view id
     * @param view: root view
     * @return boolean
     */
    public View findIpView(int id, View view);

    /**
     * M: called in setMessageListItemAdapter
     * @param adapter: IpMessageListAdapter
     */
    public void setIpMessageListItemAdapter(IIpMessageListAdapterExt adapter);

    /**
     * called in unbind
     */
    public void onIpUnbind();

    /**
     *
     * @param context Context
     * @param deliveredIndicator ImageView
     */
    public void drawRightStatusIndicator(Context context, IIpMessageItemExt item,
                                         ImageView deliveredIndicator, ImageView detailsIndicator);
}

