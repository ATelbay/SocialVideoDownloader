package com.socialvideodownloader.shared.network.auth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

@OptIn(ExperimentalForeignApi::class)
actual class SecureCookieStore : CookieStore {
    actual fun getCookies(platform: SupportedPlatform): String? = keychainRead(platform.accountKey)

    actual fun setCookies(
        platform: SupportedPlatform,
        cookies: String,
    ) {
        // Upsert pattern: delete then add
        clearCookies(platform)
        val data = NSString.create(string = cookies).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val query =
            mapOf<Any?, Any?>(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to SERVICE_NAME,
                kSecAttrAccount to platform.accountKey,
                kSecValueData to data,
            )
        @Suppress("UNCHECKED_CAST")
        SecItemAdd(CFBridgingRetain(query) as CFDictionaryRef, null)
    }

    actual fun clearCookies(platform: SupportedPlatform) {
        val query =
            mapOf<Any?, Any?>(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to SERVICE_NAME,
                kSecAttrAccount to platform.accountKey,
            )
        @Suppress("UNCHECKED_CAST")
        SecItemDelete(CFBridgingRetain(query) as CFDictionaryRef)
    }

    actual fun isConnected(platform: SupportedPlatform): Boolean = getCookies(platform) != null

    actual fun connectedPlatforms(): List<SupportedPlatform> = SupportedPlatform.entries.filter { isConnected(it) }

    private fun keychainRead(account: String): String? {
        val query =
            mapOf<Any?, Any?>(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to SERVICE_NAME,
                kSecAttrAccount to account,
                kSecMatchLimit to kSecMatchLimitOne,
                kSecReturnData to true,
            )
        memScoped {
            val resultRef = alloc<CFTypeRefVar>()

            @Suppress("UNCHECKED_CAST")
            val status =
                SecItemCopyMatching(
                    CFBridgingRetain(query) as CFDictionaryRef,
                    resultRef.ptr,
                )
            if (status != errSecSuccess) return null
            val data = CFBridgingRelease(resultRef.value) as? NSData ?: return null
            return NSString.create(data = data, encoding = NSUTF8StringEncoding) as? String
        }
    }

    companion object {
        private const val SERVICE_NAME = "com.socialvideodownloader.cookies"
    }
}

private val SupportedPlatform.accountKey: String
    get() = "cookies_${name.lowercase()}"
