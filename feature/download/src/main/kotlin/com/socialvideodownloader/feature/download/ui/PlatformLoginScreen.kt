package com.socialvideodownloader.feature.download.ui

import android.annotation.SuppressLint
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.socialvideodownloader.shared.network.auth.CookieEntry
import com.socialvideodownloader.shared.network.auth.CookieStore
import com.socialvideodownloader.shared.network.auth.NetscapeCookieParser
import com.socialvideodownloader.shared.network.auth.SupportedPlatform

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlatformLoginScreen(
    platform: SupportedPlatform,
    secureCookieStore: CookieStore,
    onResult: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cookieManager = remember { CookieManager.getInstance() }

    DisposableEffect(Unit) {
        onDispose {
            // Ensure cookies are cleaned up even if composable is disposed
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES

                webViewClient =
                    object : WebViewClient() {
                        override fun onPageFinished(
                            view: WebView?,
                            url: String?,
                        ) {
                            super.onPageFinished(view, url)
                            // Check for success cookie across all platform domains
                            val allCookies =
                                platform.cookieDomains.flatMap { domain ->
                                    val cookieString = cookieManager.getCookie(domain) ?: return@flatMap emptyList()
                                    cookieString.split(";").map { it.trim() }
                                }

                            val hasSuccessCookie =
                                allCookies.any { cookie ->
                                    cookie.startsWith("${platform.successCookieName}=")
                                }

                            if (hasSuccessCookie) {
                                // Extract all cookies and format as Netscape
                                val cookieEntries =
                                    platform.cookieDomains.flatMap { domain ->
                                        val rawCookies = cookieManager.getCookie(domain) ?: return@flatMap emptyList()
                                        rawCookies.split(";").mapNotNull { cookie ->
                                            val parts = cookie.trim().split("=", limit = 2)
                                            if (parts.size == 2) {
                                                CookieEntry(
                                                    domain = domain,
                                                    includeSubdomains = true,
                                                    path = "/",
                                                    secure = true,
                                                    expiry = 0L,
                                                    name = parts[0].trim(),
                                                    value = parts[1].trim(),
                                                )
                                            } else {
                                                null
                                            }
                                        }
                                    }

                                val netscapeCookies = NetscapeCookieParser.formatToNetscape(cookieEntries)
                                secureCookieStore.setCookies(platform, netscapeCookies)

                                // Clear WebView cookies
                                cookieManager.removeAllCookies(null)

                                onResult(true)
                            }
                        }
                    }

                loadUrl(platform.loginUrl)
            }
        },
        modifier = modifier.fillMaxSize(),
    )
}
