/* Copyright 1995-2014 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 *
 */

package com.esri.arcgisruntime.opensourceapps.mapsapp.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;

/**
 * Implements a dialog that shows a progress indicator and a message. The dialog
 * host (Activity or Fragment) needs to implement OnCancelListener if it wants
 * to be notified when the progress dialog is canceled.
 */
public class ProgressDialogFragment extends DialogFragment {
	private static final String KEY_PROGRESS_MESSAGE = "KEY_PROGRESS_MESSAGE";

	private OnCancelListener mOnCancelListener;

	public ProgressDialogFragment() {
	}

	/**
	 * Creates a new instance of ProgressDialogFragment.
	 *
	 * @param message
	 *            the progress message
	 * @return an instance of ProgressDialogFragment
	 */
	public static ProgressDialogFragment newInstance(String message) {
		ProgressDialogFragment dlg = new ProgressDialogFragment();

		Bundle args = new Bundle();
		args.putString(KEY_PROGRESS_MESSAGE, message);

		dlg.setArguments(args);
		return dlg;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		Bundle args = getArguments();
		String msg = args.getString(KEY_PROGRESS_MESSAGE);

		ProgressDialog progressDlg = new ProgressDialog(getActivity());
		progressDlg.setIndeterminate(true);
		progressDlg.setMessage(msg);
		progressDlg.setCanceledOnTouchOutside(false);

		return progressDlg;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// check if the host activity or fragment implements OnCancelListener
		//
		Fragment targetFragment = getTargetFragment();
		if (targetFragment instanceof OnCancelListener) {
			mOnCancelListener = (OnCancelListener) targetFragment;
		} else if (activity instanceof OnCancelListener) {
			mOnCancelListener = (OnCancelListener) activity;
		}
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);

		if (mOnCancelListener != null) {
			mOnCancelListener.onCancel(dialog);
		}
	}
}
