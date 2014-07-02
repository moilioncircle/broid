/*
 * Copyright (C) 2014 The Broid Open Source Project
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
package com.iamuv.broid.utils;

import android.content.Context;

public class SystemUtils {

    public static int getDefaultThreadPoolSize() {
	int availableProcessors = getAvailableProcessors();
	return 2 * availableProcessors < 1 ? 1 : availableProcessors + 1;
    }

    public static int getDefaultThreadPoolSize(int max) {
	int threadPoolSize = getDefaultThreadPoolSize();
	return threadPoolSize > max ? max : threadPoolSize;
    }

    public static int getAvailableProcessors() {
	return Runtime.getRuntime().availableProcessors();
    }

    public static float dpToPx(Context context, float dp) {
	if (context == null) {
	    return -1;
	}
	return dp * context.getResources().getDisplayMetrics().density;
    }

    public static float pxToDp(Context context, float px) {
	if (context == null) {
	    return -1;
	}
	return px / context.getResources().getDisplayMetrics().density;
    }

    public static float dpToPxInt(Context context, float dp) {
	return (int) (dpToPx(context, dp) + 0.5f);
    }

    public static float pxToDpCeilInt(Context context, float px) {
	return (int) (pxToDp(context, px) + 0.5f);
    }
}
