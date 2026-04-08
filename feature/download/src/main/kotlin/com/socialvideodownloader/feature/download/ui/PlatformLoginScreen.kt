package com.socialvideodownloader.feature.download.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.socialvideodownloader.shared.network.auth.CookieEntry
import com.socialvideodownloader.shared.network.auth.CookieStore
import com.socialvideodownloader.shared.network.auth.NetscapeCookieParser
import com.socialvideodownloader.shared.network.auth.SupportedPlatform

// Far-future expiry (year 2035) so yt-dlp doesn't reject cookies as expired
private const val FAR_FUTURE_EXPIRY = 2051222400L

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlatformLoginScreen(
    platform: SupportedPlatform,
    secureCookieStore: CookieStore,
    onResult: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cookieManager = remember { CookieManager.getInstance() }
    val loginHost = remember { Uri.parse(platform.loginUrl).host ?: "" }
    // Convert domain patterns to URLs that CookieManager.getCookie() understands
    val cookieUrls =
        remember {
            platform.cookieDomains.map { domain ->
                "https://${domain.removePrefix(".")}"
            }
        }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    // For YouTube: after Google login we need to visit youtube.com to pick up YouTube-domain cookies
    var pendingPlatformVisit by remember { mutableStateOf(false) }

    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
    }

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
                webViewRef = this

                webViewClient =
                    object : WebViewClient() {
                        override fun onPageFinished(
                            view: WebView?,
                            url: String?,
                        ) {
                            super.onPageFinished(view, url)
                            canGoBack = view?.canGoBack() == true

                            val currentHost = url?.let { Uri.parse(it).host?.lowercase() } ?: ""

                            // If we already navigated to the platform host to collect domain cookies,
                            // extract everything now and finish.
                            if (pendingPlatformVisit) {
                                // Check that we're on a platform host (not still redirecting)
                                val isPlatformHost =
                                    platform.hostMatches.any { host ->
                                        currentHost == host || currentHost.endsWith(".$host")
                                    }
                                if (isPlatformHost) {
                                    extractAndSaveCookies()
                                }
                                return
                            }

                            // Don't extract cookies while still on the login domain —
                            // e.g. Google sets SID after password but before "Is that you?" confirmation
                            if (currentHost == loginHost || currentHost.endsWith(".$loginHost")) {
                                return
                            }

                            // Check for success cookie across all platform domains
                            val allCookies =
                                cookieUrls.flatMap { cookieUrl ->
                                    val cookieString = cookieManager.getCookie(cookieUrl) ?: return@flatMap emptyList()
                                    cookieString.split(";").map { it.trim() }
                                }

                            val hasSuccessCookie =
                                allCookies.any { cookie ->
                                    cookie.startsWith("${platform.successCookieName}=")
                                }

                            if (hasSuccessCookie) {
                                // For platforms where login host differs from content host (e.g. YouTube:
                                // login is on accounts.google.com but we need .youtube.com cookies too),
                                // navigate to the platform's primary host to trigger cookie creation.
                                val primaryHost = platform.hostMatches.firstOrNull() ?: ""
                                val needsPlatformVisit = !currentHost.endsWith(primaryHost)
                                if (needsPlatformVisit) {
                                    pendingPlatformVisit = true
                                    view?.loadUrl("https://www.$primaryHost")
                                    return
                                }

                                extractAndSaveCookies()
                            }
                        }

                        private fun extractAndSaveCookies() {
                            val cookieEntries =
                                cookieUrls.zip(platform.cookieDomains).flatMap { (cookieUrl, domain) ->
                                    val rawCookies = cookieManager.getCookie(cookieUrl) ?: return@flatMap emptyList()
                                    rawCookies.split(";").mapNotNull { cookie ->
                                        val parts = cookie.trim().split("=", limit = 2)
                                        if (parts.size == 2) {
                                            CookieEntry(
                                                domain = domain,
                                                includeSubdomains = true,
                                                path = "/",
                                                secure = true,
                                                expiry = FAR_FUTURE_EXPIRY,
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

                            // Don't call cookieManager.removeAllCookies() — keeping the
                            // WebView session means "Reconnect" won't require full re-login.
                            onResult(true)
                        }
                    }

                loadUrl(platform.loginUrl)
            }
        },
        modifier = modifier.fillMaxSize(),
    )
}
