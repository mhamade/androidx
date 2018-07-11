/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.mediarouter.app;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.fragment.app.DialogFragment;

/**
 * Media route controller dialog fragment.
 * <p>
 * Creates a {@link MediaRouteControllerDialog}.  The application may subclass
 * this dialog fragment to customize the media route controller dialog.
 * </p>
 */
public class MediaRouteControllerDialogFragment extends DialogFragment {
    // Intermediate constant for development purpose
    // TODO: Remove this before official release
    private static final boolean USE_SUPPORT_DYNAMIC_GROUP =
            Log.isLoggable("UseSupportDynamicGroup", Log.DEBUG);

    private Dialog mDialog;
    /**
     * Creates a media route controller dialog fragment.
     * <p>
     * All subclasses of this class must also possess a default constructor.
     * </p>
     */
    public MediaRouteControllerDialogFragment() {
        setCancelable(true);
    }

    /**
     * Called when the cast dialog is being created.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public MediaRouteCastDialog onCreateCastDialog(Context context) {
        return new MediaRouteCastDialog(context);
    }

    /**
     * Called when the controller dialog is being created.
     * <p>
     * Subclasses may override this method to customize the dialog.
     * </p>
     */
    public MediaRouteControllerDialog onCreateControllerDialog(
            Context context, Bundle savedInstanceState) {
        return new MediaRouteControllerDialog(context);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (USE_SUPPORT_DYNAMIC_GROUP) {
            mDialog = onCreateCastDialog(getContext());
        } else {
            mDialog = onCreateControllerDialog(getContext(), savedInstanceState);
        }
        return mDialog;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mDialog != null) {
            if (!USE_SUPPORT_DYNAMIC_GROUP) {
                ((MediaRouteControllerDialog) mDialog).clearGroupListAnimation(false);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDialog != null) {
            if (!USE_SUPPORT_DYNAMIC_GROUP) {
                ((MediaRouteControllerDialog) mDialog).updateLayout();
            }
        }
    }
}
