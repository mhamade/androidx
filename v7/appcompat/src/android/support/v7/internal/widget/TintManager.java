/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v7.internal.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.util.LruCache;
import android.support.v7.appcompat.R;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import static android.support.v7.internal.widget.ThemeUtils.getDisabledThemeAttrColor;
import static android.support.v7.internal.widget.ThemeUtils.getThemeAttrColor;
import static android.support.v7.internal.widget.ThemeUtils.getThemeAttrColorStateList;

/**
 * @hide
 */
public final class TintManager {

    static final boolean SHOULD_BE_USED = Build.VERSION.SDK_INT < 21;

    private static final String TAG = "TintManager";
    private static final boolean DEBUG = false;
    private static final PorterDuff.Mode DEFAULT_MODE = PorterDuff.Mode.SRC_IN;

    private static final WeakHashMap<Context, TintManager> INSTANCE_CACHE = new WeakHashMap<>();
    private static final ColorFilterLruCache COLOR_FILTER_CACHE = new ColorFilterLruCache(6);

    /**
     * Drawables which should be tinted with the value of {@code R.attr.colorControlNormal},
     * using the default mode using a raw color filter.
     */
    private static final int[] COLORFILTER_TINT_COLOR_CONTROL_NORMAL = {
            R.drawable.abc_textfield_search_default_mtrl_alpha,
            R.drawable.abc_textfield_default_mtrl_alpha,
            R.drawable.abc_ab_share_pack_mtrl_alpha
    };

    /**
     * Drawables which should be tinted with the value of {@code R.attr.colorControlNormal}, using
     * {@link DrawableCompat}'s tinting functionality.
     */
    private static final int[] TINT_COLOR_CONTROL_NORMAL = {
            R.drawable.abc_ic_ab_back_mtrl_am_alpha,
            R.drawable.abc_ic_go_search_api_mtrl_alpha,
            R.drawable.abc_ic_search_api_mtrl_alpha,
            R.drawable.abc_ic_commit_search_api_mtrl_alpha,
            R.drawable.abc_ic_clear_mtrl_alpha,
            R.drawable.abc_ic_menu_share_mtrl_alpha,
            R.drawable.abc_ic_menu_copy_mtrl_am_alpha,
            R.drawable.abc_ic_menu_cut_mtrl_alpha,
            R.drawable.abc_ic_menu_selectall_mtrl_alpha,
            R.drawable.abc_ic_menu_paste_mtrl_am_alpha,
            R.drawable.abc_ic_menu_moreoverflow_mtrl_alpha,
            R.drawable.abc_ic_voice_search_api_mtrl_alpha
    };

    /**
     * Drawables which should be tinted with the value of {@code R.attr.colorControlActivated},
     * using a color filter.
     */
    private static final int[] COLORFILTER_COLOR_CONTROL_ACTIVATED = {
            R.drawable.abc_textfield_activated_mtrl_alpha,
            R.drawable.abc_textfield_search_activated_mtrl_alpha,
            R.drawable.abc_cab_background_top_mtrl_alpha,
            R.drawable.abc_text_cursor_mtrl_alpha
    };

    /**
     * Drawables which should be tinted with the value of {@code android.R.attr.colorBackground},
     * using the {@link android.graphics.PorterDuff.Mode#MULTIPLY} mode and a color filter.
     */
    private static final int[] COLORFILTER_COLOR_BACKGROUND_MULTIPLY = {
            R.drawable.abc_popup_background_mtrl_mult,
            R.drawable.abc_cab_background_internal_bg,
            R.drawable.abc_menu_hardkey_panel_mtrl_mult
    };

    /**
     * Drawables which should be tinted using a state list containing values of
     * {@code R.attr.colorControlNormal} and {@code R.attr.colorControlActivated}
     */
    private static final int[] TINT_COLOR_CONTROL_STATE_LIST = {
            R.drawable.abc_edit_text_material,
            R.drawable.abc_tab_indicator_material,
            R.drawable.abc_textfield_search_material,
            R.drawable.abc_spinner_mtrl_am_alpha,
            R.drawable.abc_btn_check_material,
            R.drawable.abc_btn_radio_material,
            R.drawable.abc_spinner_textfield_background_material,
            R.drawable.abc_ratingbar_full_material,
            R.drawable.abc_switch_track_mtrl_alpha,
            R.drawable.abc_switch_thumb_material,
            R.drawable.abc_btn_default_mtrl_shape,
            R.drawable.abc_btn_borderless_material
    };

    private final WeakReference<Context> mContextRef;
    private SparseArray<ColorStateList> mTintLists;
    private ColorStateList mDefaultColorStateList;

    /**
     * A helper method to get a {@link TintManager} and then call {@link #getDrawable(int)}.
     * This method should not be used routinely.
     */
    public static Drawable getDrawable(Context context, int resId) {
        if (isInTintList(resId)) {
            return TintManager.get(context).getDrawable(resId);
        } else {
            return ContextCompat.getDrawable(context, resId);
        }
    }

    /**
     * Get a {@link android.support.v7.internal.widget.TintManager} instance.
     */
    public static TintManager get(Context context) {
        TintManager tm = INSTANCE_CACHE.get(context);
        if (tm == null) {
            tm = new TintManager(context);
            INSTANCE_CACHE.put(context, tm);
        }
        return tm;
    }

    private TintManager(Context context) {
        mContextRef = new WeakReference<>(context);
    }

    public Drawable getDrawable(int resId) {
        final Context context = mContextRef.get();
        if (context == null) return null;

        Drawable drawable = ContextCompat.getDrawable(context, resId);

        if (drawable != null) {
            drawable = drawable.mutate();

            final ColorStateList tintList = getTintList(resId);
            if (tintList != null) {
                // First wrap the Drawable and set the tint list
                drawable = DrawableCompat.wrap(drawable);
                DrawableCompat.setTintList(drawable, tintList);

                // If there is a blending mode specified for the drawable, use it
                final PorterDuff.Mode tintMode = getTintMode(resId);
                if (tintMode != null) {
                    DrawableCompat.setTintMode(drawable, tintMode);
                }
            } else if (resId == R.drawable.abc_cab_background_top_material) {
                return new LayerDrawable(new Drawable[] {
                        getDrawable(R.drawable.abc_cab_background_internal_bg),
                        getDrawable(R.drawable.abc_cab_background_top_mtrl_alpha)
                });
            } else {
                tintDrawableUsingColorFilter(resId, drawable);
            }
        }
        return drawable;
    }

    final void tintDrawableUsingColorFilter(final int resId, Drawable drawable) {
        final Context context = mContextRef.get();
        if (context == null) return;

        PorterDuff.Mode tintMode = null;
        boolean colorAttrSet = false;
        int colorAttr = 0;
        int alpha = -1;

        if (arrayContains(COLORFILTER_TINT_COLOR_CONTROL_NORMAL, resId)) {
            colorAttr = R.attr.colorControlNormal;
            colorAttrSet = true;
        } else if (arrayContains(COLORFILTER_COLOR_CONTROL_ACTIVATED, resId)) {
            colorAttr = R.attr.colorControlActivated;
            colorAttrSet = true;
        } else if (arrayContains(COLORFILTER_COLOR_BACKGROUND_MULTIPLY, resId)) {
            colorAttr = android.R.attr.colorBackground;
            colorAttrSet = true;
            tintMode = PorterDuff.Mode.MULTIPLY;
        } else if (resId == R.drawable.abc_list_divider_mtrl_alpha) {
            colorAttr = android.R.attr.colorForeground;
            colorAttrSet = true;
            alpha = Math.round(0.16f * 255);
        }

        if (colorAttrSet) {
            final int color = getThemeAttrColor(context, colorAttr);
            setPorterDuffColorFilter(drawable, color, tintMode);

            if (alpha != -1) {
                drawable.setAlpha(alpha);
            }

            if (DEBUG) {
                Log.d(TAG, "Tinted Drawable: " + context.getResources().getResourceName(resId) +
                        " with color: #" + Integer.toHexString(color));
            }
        }
    }

    private static boolean arrayContains(int[] array, int value) {
        for (int id : array) {
            if (id == value) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInTintList(int drawableId) {
        return arrayContains(TINT_COLOR_CONTROL_NORMAL, drawableId) ||
                arrayContains(COLORFILTER_TINT_COLOR_CONTROL_NORMAL, drawableId) ||
                arrayContains(COLORFILTER_COLOR_CONTROL_ACTIVATED, drawableId) ||
                arrayContains(TINT_COLOR_CONTROL_STATE_LIST, drawableId) ||
                arrayContains(COLORFILTER_COLOR_BACKGROUND_MULTIPLY, drawableId) ||
                drawableId == R.drawable.abc_cab_background_top_material;
    }

    final PorterDuff.Mode getTintMode(final int resId) {
        PorterDuff.Mode mode = null;

        if (resId == R.drawable.abc_switch_thumb_material) {
            mode = PorterDuff.Mode.MULTIPLY;
        }

        return mode;
    }

    final ColorStateList getTintList(int resId) {
        final Context context = mContextRef.get();
        if (context == null) return null;

        // Try the cache first (if it exists)
        ColorStateList tint = mTintLists != null ? mTintLists.get(resId) : null;

        if (tint == null) {
            // ...if the cache did not contain a color state list, try and create one
            if (resId == R.drawable.abc_edit_text_material) {
                tint = createEditTextColorStateList(context);
            } else if (resId == R.drawable.abc_switch_track_mtrl_alpha) {
                tint = createSwitchTrackColorStateList(context);
            } else if (resId == R.drawable.abc_switch_thumb_material) {
                tint = createSwitchThumbColorStateList(context);
            } else if (resId == R.drawable.abc_btn_default_mtrl_shape
                    || resId == R.drawable.abc_btn_borderless_material) {
                tint = createButtonColorStateList(context);
            } else if (resId == R.drawable.abc_spinner_mtrl_am_alpha
                    || resId == R.drawable.abc_spinner_textfield_background_material) {
                tint = createSpinnerColorStateList(context);
            } else if (arrayContains(TINT_COLOR_CONTROL_NORMAL, resId)) {
                tint = getThemeAttrColorStateList(context, R.attr.colorControlNormal);
            } else if (arrayContains(TINT_COLOR_CONTROL_STATE_LIST, resId)) {
                tint = getDefaultColorStateList(context);
            }

            if (tint != null) {
                if (mTintLists == null) {
                    // If our tint list cache hasn't been set up yet, create it
                    mTintLists = new SparseArray<>();
                }
                // Add any newly created ColorStateList to the cache
                mTintLists.append(resId, tint);
            }
        }
        return tint;
    }

    private ColorStateList getDefaultColorStateList(Context context) {
        if (mDefaultColorStateList == null) {
            /**
             * Generate the default color state list which uses the colorControl attributes.
             * Order is important here. The default enabled state needs to go at the bottom.
             */

            final int colorControlNormal = getThemeAttrColor(context, R.attr.colorControlNormal);
            final int colorControlActivated = getThemeAttrColor(context,
                    R.attr.colorControlActivated);

            final int[][] states = new int[7][];
            final int[] colors = new int[7];
            int i = 0;

            // Disabled state
            states[i] = new int[] { -android.R.attr.state_enabled };
            colors[i] = getDisabledThemeAttrColor(context, R.attr.colorControlNormal);
            i++;

            states[i] = new int[] { android.R.attr.state_focused };
            colors[i] = colorControlActivated;
            i++;

            states[i] = new int[] { android.R.attr.state_activated };
            colors[i] = colorControlActivated;
            i++;

            states[i] = new int[] { android.R.attr.state_pressed };
            colors[i] = colorControlActivated;
            i++;

            states[i] = new int[] { android.R.attr.state_checked };
            colors[i] = colorControlActivated;
            i++;

            states[i] = new int[] { android.R.attr.state_selected };
            colors[i] = colorControlActivated;
            i++;

            // Default enabled state
            states[i] = new int[0];
            colors[i] = colorControlNormal;
            i++;

            mDefaultColorStateList = new ColorStateList(states, colors);
        }
        return mDefaultColorStateList;
    }

    private ColorStateList createSwitchTrackColorStateList(Context context) {
        final int[][] states = new int[3][];
        final int[] colors = new int[3];
        int i = 0;

        // Disabled state
        states[i] = new int[]{-android.R.attr.state_enabled};
        colors[i] = getThemeAttrColor(context, android.R.attr.colorForeground, 0.1f);
        i++;

        states[i] = new int[]{android.R.attr.state_checked};
        colors[i] = getThemeAttrColor(context, R.attr.colorControlActivated, 0.3f);
        i++;

        // Default enabled state
        states[i] = new int[0];
        colors[i] = getThemeAttrColor(context, android.R.attr.colorForeground, 0.3f);
        i++;

        return new ColorStateList(states, colors);
    }

    private ColorStateList createSwitchThumbColorStateList(Context context) {
        final int[][] states = new int[3][];
        final int[] colors = new int[3];
        int i = 0;

        final ColorStateList thumbColor = getThemeAttrColorStateList(context,
                R.attr.colorSwitchThumbNormal);

        if (thumbColor != null && thumbColor.isStateful()) {
            // If colorSwitchThumbNormal is a valid ColorStateList, extract the default and
            // disabled colors from it

            // Disabled state
            states[i] = new int[]{-android.R.attr.state_enabled};
            colors[i] = thumbColor.getColorForState(states[i], 0);
            i++;

            states[i] = new int[]{android.R.attr.state_checked};
            colors[i] = getThemeAttrColor(context, R.attr.colorControlActivated);
            i++;

            // Default enabled state
            states[i] = new int[0];
            colors[i] = thumbColor.getDefaultColor();
            i++;
        } else {
            // Else we'll use an approximation using the default disabled alpha

            // Disabled state
            states[i] = new int[]{-android.R.attr.state_enabled};
            colors[i] = getDisabledThemeAttrColor(context, R.attr.colorSwitchThumbNormal);
            i++;

            states[i] = new int[]{android.R.attr.state_checked};
            colors[i] = getThemeAttrColor(context, R.attr.colorControlActivated);
            i++;

            // Default enabled state
            states[i] = new int[0];
            colors[i] = getThemeAttrColor(context, R.attr.colorSwitchThumbNormal);
            i++;
        }

        return new ColorStateList(states, colors);
    }

    private ColorStateList createEditTextColorStateList(Context context) {
        final int[][] states = new int[3][];
        final int[] colors = new int[3];
        int i = 0;

        // Disabled state
        states[i] = new int[]{-android.R.attr.state_enabled};
        colors[i] = getDisabledThemeAttrColor(context, R.attr.colorControlNormal);
        i++;

        states[i] = new int[]{-android.R.attr.state_pressed, -android.R.attr.state_focused};
        colors[i] = getThemeAttrColor(context, R.attr.colorControlNormal);
        i++;

        // Default enabled state
        states[i] = new int[0];
        colors[i] = getThemeAttrColor(context, R.attr.colorControlActivated);
        i++;

        return new ColorStateList(states, colors);
    }

    private ColorStateList createButtonColorStateList(Context context) {
        final int[][] states = new int[4][];
        final int[] colors = new int[4];
        int i = 0;

        // Disabled state
        states[i] = new int[]{-android.R.attr.state_enabled};
        colors[i] = getDisabledThemeAttrColor(context, R.attr.colorButtonNormal);
        i++;

        states[i] = new int[]{android.R.attr.state_pressed};
        colors[i] = getThemeAttrColor(context, R.attr.colorControlHighlight);
        i++;

        states[i] = new int[]{android.R.attr.state_focused};
        colors[i] = getThemeAttrColor(context, R.attr.colorControlHighlight);
        i++;

        // Default enabled state
        states[i] = new int[0];
        colors[i] = getThemeAttrColor(context, R.attr.colorButtonNormal);
        i++;

        return new ColorStateList(states, colors);
    }

    private ColorStateList createSpinnerColorStateList(Context context) {
        final int[][] states = new int[3][];
        final int[] colors = new int[3];
        int i = 0;

        // Disabled state
        states[i] = new int[]{-android.R.attr.state_enabled};
        colors[i] = getDisabledThemeAttrColor(context, R.attr.colorControlNormal);
        i++;

        states[i] = new int[]{-android.R.attr.state_pressed, -android.R.attr.state_focused};
        colors[i] = getThemeAttrColor(context, R.attr.colorControlNormal);
        i++;

        states[i] = new int[0];
        colors[i] = getThemeAttrColor(context, R.attr.colorControlActivated);
        i++;

        return new ColorStateList(states, colors);
    }

    private static class ColorFilterLruCache extends LruCache<Integer, PorterDuffColorFilter> {

        public ColorFilterLruCache(int maxSize) {
            super(maxSize);
        }

        PorterDuffColorFilter get(int color, PorterDuff.Mode mode) {
            return get(generateCacheKey(color, mode));
        }

        PorterDuffColorFilter put(int color, PorterDuff.Mode mode, PorterDuffColorFilter filter) {
            return put(generateCacheKey(color, mode), filter);
        }

        private static int generateCacheKey(int color, PorterDuff.Mode mode) {
            int hashCode = 1;
            hashCode = 31 * hashCode + color;
            hashCode = 31 * hashCode + mode.hashCode();
            return hashCode;
        }
    }

    public static void tintViewBackground(View view, TintInfo tint) {
        final Drawable background = view.getBackground();
        if (tint.mTintList != null) {
            setPorterDuffColorFilter(
                    background,
                    tint.mTintList.getColorForState(view.getDrawableState(),
                            tint.mTintList.getDefaultColor()),
                    tint.mTintMode != null ? tint.mTintMode : null);
        } else {
            background.clearColorFilter();
        }
    }

    private static void setPorterDuffColorFilter(Drawable d, int color, PorterDuff.Mode mode) {
        if (mode == null) {
            // If we don't have a blending mode specified, use our default
            mode = DEFAULT_MODE;
        }

        // First, lets see if the cache already contains the color filter
        PorterDuffColorFilter filter = COLOR_FILTER_CACHE.get(color, mode);

        if (filter == null) {
            // Cache miss, so create a color filter and add it to the cache
            filter = new PorterDuffColorFilter(color, mode);
            COLOR_FILTER_CACHE.put(color, mode, filter);
        }

        d.setColorFilter(filter);
    }
}
