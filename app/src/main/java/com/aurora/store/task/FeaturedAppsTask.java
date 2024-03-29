/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Aurora Store is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Aurora Store is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package com.aurora.store.task;

import android.content.Context;

import com.aurora.store.api.CategoryAppsIterator2;
import com.aurora.store.iterator.CustomAppListIterator;
import com.aurora.store.model.App;
import com.aurora.store.utility.Util;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FeaturedAppsTask extends BaseTask {

    public FeaturedAppsTask(Context context) {
        super(context);
    }

    public List<App> getApps(String categoryId, GooglePlayAPI.SUBCATEGORY subCategory) throws Exception {
        final GooglePlayAPI api = getApi();
        final CustomAppListIterator iterator = new CustomAppListIterator(new CategoryAppsIterator2(api, categoryId, subCategory));
        List<App> apps = new ArrayList<>();
        while (iterator.hasNext() && apps.isEmpty()) {
            apps.addAll(iterator.next());
        }
        if (Util.filterGoogleAppsEnabled(context))
            return filterGoogleApps(apps);
        else
            return apps;
    }
}
