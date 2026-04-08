package com.socialvideodownloader.shared.network.auth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
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
    actual override fun getCookies(platform: SupportedPlatform): String? = keychainRead(platform.accountKey)

    actual override fun setCookies(
        platform: SupportedPlatform,
        cookies: String,
    ) {
        // Upsert pattern: delete then add
        clearCookies(platform)
        @Suppress("CAST_NEVER_SUCCEEDS")
        val data = (cookies as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val query =
            mapOf<Any?, Any?>(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to SERVICE_NAME,
                kSecAttrAccount to platform.accountKey,
                kSecValueData to data,
            )

        @Suppress("UNCHECKED_CAST")
        val cfQuery = CFBridgingRetain(query) as CFDictionaryRef?
        try {
            SecItemAdd(cfQuery, null)
        } finally {
            cfQuery?.let { CFBridgingRelease(it) }
        }
    }

    actual override fun clearCookies(platform: SupportedPlatform) {
        val query =
            mapOf<Any?, Any?>(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to SERVICE_NAME,
                kSecAttrAccount to platform.accountKey,
            )

        @Suppress("UNCHECKED_CAST")
        val cfQuery = CFBridgingRetain(query) as CFDictionaryRef?
        try {
            SecItemDelete(cfQuery)
        } finally {
            cfQuery?.let { CFBridgingRelease(it) }
        }
    }

    actual override fun isConnected(platform: SupportedPlatform): Boolean = getCookies(platform) != null

    actual override fun connectedPlatforms(): List<SupportedPlatform> = SupportedPlatform.entries.filter { isConnected(it) }

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
            val cfQuery = CFBridgingRetain(query) as CFDictionaryRef?
            val status =
                try {
                    SecItemCopyMatching(cfQuery, resultRef.ptr)
                } finally {
                    cfQuery?.let { CFBridgingRelease(it) }
                }
            if (status != errSecSuccess) return null
            val nsData = CFBridgingRelease(resultRef.value) as? NSData ?: return null
            val length = nsData.length.toInt()
            if (length == 0) return ""
            val bytes = nsData.bytes?.readBytes(length) ?: return null
            return bytes.decodeToString()
        }
    }

    companion object {
        private const val SERVICE_NAME = "com.socialvideodownloader.cookies"
    }
}

private val SupportedPlatform.accountKey: String
    get() = "cookies_${name.lowercase()}"
