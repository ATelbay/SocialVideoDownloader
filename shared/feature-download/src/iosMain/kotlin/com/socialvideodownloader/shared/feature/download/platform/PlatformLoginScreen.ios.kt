package com.socialvideodownloader.shared.feature.download.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.socialvideodownloader.shared.network.auth.CookieEntry
import com.socialvideodownloader.shared.network.auth.CookieStore
import com.socialvideodownloader.shared.network.auth.NetscapeCookieParser
import com.socialvideodownloader.shared.network.auth.SupportedPlatform
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSHTTPCookie
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebsiteDataStore
import platform.darwin.NSObject

// Far-future expiry (year 2035) so yt-dlp doesn't reject session cookies as expired
private const val FAR_FUTURE_EXPIRY = 2051222400L

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformLoginScreen(
    platform: SupportedPlatform,
    secureCookieStore: CookieStore,
    onResult: (Boolean) -> Unit,
    modifier: Modifier,
) {
    val configuration = remember { WKWebViewConfiguration() }
    val loginHost =
        remember {
            platform.loginUrl.substringAfter("://").substringBefore("/").substringBefore("?")
        }

    UIKitView(
        factory = {
            val webView = WKWebView(frame = cValue { CGRectZero }, configuration = configuration)
            var pendingPlatformVisit = false

            val delegate =
                object : NSObject(), WKNavigationDelegateProtocol {
                    override fun webView(
                        webView: WKWebView,
                        didFinishNavigation: WKNavigation?,
                    ) {
                        val currentUrl = webView.URL?.absoluteString ?: ""
                        val currentHost =
                            currentUrl.substringAfter("://")
                                .substringBefore("/").substringBefore("?").lowercase()

                        val cookieStore = WKWebsiteDataStore.defaultDataStore().httpCookieStore
                        cookieStore.getAllCookies { cookies ->
                            @Suppress("UNCHECKED_CAST")
                            val httpCookies = cookies as? List<NSHTTPCookie> ?: return@getAllCookies

                            // If we navigated to the platform host to collect domain cookies, extract now.
                            if (pendingPlatformVisit) {
                                val isPlatformHost =
                                    platform.hostMatches.any { host ->
                                        currentHost == host || currentHost.endsWith(".$host")
                                    }
                                if (isPlatformHost) {
                                    extractAndSaveCookies(httpCookies, platform, secureCookieStore, onResult)
                                }
                                return@getAllCookies
                            }

                            // Don't extract while still on the login domain
                            if (currentHost == loginHost || currentHost.endsWith(".$loginHost")) {
                                return@getAllCookies
                            }

                            val hasSuccessCookie =
                                httpCookies.any { cookie ->
                                    cookie.name == platform.successCookieName &&
                                        platform.cookieDomains.any { domain ->
                                            cookie.domain.endsWith(domain.removePrefix("."))
                                        }
                                }

                            if (hasSuccessCookie) {
                                // For platforms where login host differs from content host (e.g. YouTube:
                                // login on accounts.google.com but cookies needed from .youtube.com),
                                // navigate to the platform's primary host to trigger cookie creation.
                                val primaryHost = platform.hostMatches.firstOrNull() ?: ""
                                val needsPlatformVisit = !currentHost.endsWith(primaryHost)
                                if (needsPlatformVisit) {
                                    pendingPlatformVisit = true
                                    val platformUrl = NSURL.URLWithString("https://www.$primaryHost")!!
                                    webView.loadRequest(NSURLRequest.requestWithURL(platformUrl))
                                    return@getAllCookies
                                }

                                extractAndSaveCookies(httpCookies, platform, secureCookieStore, onResult)
                            }
                        }
                    }
                }

            webView.navigationDelegate = delegate
            val url = NSURL.URLWithString(platform.loginUrl)!!
            webView.loadRequest(NSURLRequest.requestWithURL(url))

            webView
        },
        modifier = modifier,
    )
}

private fun extractAndSaveCookies(
    httpCookies: List<NSHTTPCookie>,
    platform: SupportedPlatform,
    secureCookieStore: CookieStore,
    onResult: (Boolean) -> Unit,
) {
    val relevantCookies =
        httpCookies.filter { cookie ->
            platform.cookieDomains.any { domain ->
                cookie.domain.endsWith(domain.removePrefix("."))
            }
        }

    val entries =
        relevantCookies.map { cookie ->
            CookieEntry(
                domain = cookie.domain,
                includeSubdomains = true,
                path = cookie.path,
                secure = cookie.isSecure(),
                expiry = cookie.expiresDate?.timeIntervalSince1970?.toLong() ?: FAR_FUTURE_EXPIRY,
                name = cookie.name,
                value = cookie.value,
            )
        }

    val netscapeCookies = NetscapeCookieParser.formatToNetscape(entries)
    secureCookieStore.setCookies(platform, netscapeCookies)

    // Don't clear WKWebView cookies — keeping the session means "Reconnect" won't require full re-login.
    onResult(true)
}
