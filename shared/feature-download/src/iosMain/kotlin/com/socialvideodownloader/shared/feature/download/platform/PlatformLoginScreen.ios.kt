package com.socialvideodownloader.shared.feature.download.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.socialvideodownloader.shared.network.auth.CookieEntry
import com.socialvideodownloader.shared.network.auth.NetscapeCookieParser
import com.socialvideodownloader.shared.network.auth.SecureCookieStore
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

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformLoginScreen(
    platform: SupportedPlatform,
    secureCookieStore: SecureCookieStore,
    onResult: (Boolean) -> Unit,
    modifier: Modifier,
) {
    val configuration = remember { WKWebViewConfiguration() }

    UIKitView(
        factory = {
            val webView = WKWebView(frame = cValue { CGRectZero }, configuration = configuration)

            val delegate = object : NSObject(), WKNavigationDelegateProtocol {
                override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
                    val cookieStore = WKWebsiteDataStore.defaultDataStore().httpCookieStore
                    cookieStore.getAllCookies { cookies ->
                        @Suppress("UNCHECKED_CAST")
                        val httpCookies = cookies as? List<NSHTTPCookie> ?: return@getAllCookies

                        val hasSuccessCookie = httpCookies.any { cookie ->
                            cookie.name == platform.successCookieName &&
                                platform.cookieDomains.any { domain ->
                                    cookie.domain.endsWith(domain.removePrefix("."))
                                }
                        }

                        if (hasSuccessCookie) {
                            val relevantCookies = httpCookies.filter { cookie ->
                                platform.cookieDomains.any { domain ->
                                    cookie.domain.endsWith(domain.removePrefix("."))
                                }
                            }

                            val entries = relevantCookies.map { cookie ->
                                CookieEntry(
                                    domain = cookie.domain,
                                    includeSubdomains = true,
                                    path = cookie.path,
                                    secure = cookie.isSecure(),
                                    expiry = cookie.expiresDate?.timeIntervalSince1970?.toLong() ?: 0L,
                                    name = cookie.name,
                                    value = cookie.value,
                                )
                            }

                            val netscapeCookies = NetscapeCookieParser.formatToNetscape(entries)
                            secureCookieStore.setCookies(platform, netscapeCookies)

                            // Clear WKWebView cookies after extraction
                            relevantCookies.forEach { cookie ->
                                cookieStore.deleteCookie(cookie, completionHandler = null)
                            }

                            onResult(true)
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
