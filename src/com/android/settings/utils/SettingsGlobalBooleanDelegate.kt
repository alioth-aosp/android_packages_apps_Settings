/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.utils

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.provider.Settings
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun Context.observeSettingsGlobalBoolean(
    name: String,
    lifecycle: Lifecycle,
    onChange: (newValue: Boolean) -> Unit,
) {
    val field by settingsGlobalBoolean(name)
    val contentObserver = object : ContentObserver(Handler.getMain()) {
        override fun onChange(selfChange: Boolean) {
            onChange(field)
        }
    }
    val uri = Settings.Global.getUriFor(name)
    lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            contentResolver.registerContentObserver(uri, false, contentObserver)
            onChange(field)
        }

        override fun onStop(owner: LifecycleOwner) {
            contentResolver.unregisterContentObserver(contentObserver)
        }
    })
}

fun Context.settingsGlobalBoolean(name: String): ReadWriteProperty<Any?, Boolean> =
    SettingsGlobalBooleanDelegate(this, name)

private class SettingsGlobalBooleanDelegate(context: Context, private val name: String) :
    ReadWriteProperty<Any?, Boolean> {

    private val contentResolver: ContentResolver = context.contentResolver

    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean =
        Settings.Global.getInt(contentResolver, name, 0) != 0

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
        Settings.Global.putInt(contentResolver, name, if (value) 1 else 0)
    }
}