package com.hfad.atomgit.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

/** 外链优先用 Custom Tabs，没有可用浏览器内核时退回普通浏览器、 */
fun openExternalLink(context: Context, url: String) {
    val uri = runCatching { url.toUri() }.getOrNull() ?: return
    val customTabs = CustomTabsIntent.Builder().setShowTitle(true).build()
    try {
        customTabs.launchUrl(context, uri)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            // 设备上没有浏览器，忽略
        }
    } catch (_: IllegalArgumentException) {
        // 地址不可打开，忽略
    }
}
