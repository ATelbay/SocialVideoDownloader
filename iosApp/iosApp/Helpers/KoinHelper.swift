import Foundation
import shared_data
import shared_feature_download
import shared_feature_history
import shared_feature_library

/// Swift-side wrapper around the Kotlin KoinHelper object.
///
/// Provides typed factory methods that are more idiomatic Swift
/// than calling the Kotlin object directly.
enum KoinViewModelFactory {

    /// Create a new SharedDownloadViewModel.
    /// Caller is responsible for calling `vm.cleanup()` on disappear.
    static func makeDownloadViewModel() -> SharedDownloadViewModel {
        KoinHelper.shared.getDownloadViewModel()
    }

    /// Create a new SharedHistoryViewModel.
    static func makeHistoryViewModel() -> SharedHistoryViewModel {
        KoinHelper.shared.getHistoryViewModel()
    }

    /// Create a new SharedLibraryViewModel.
    static func makeLibraryViewModel() -> SharedLibraryViewModel {
        KoinHelper.shared.getLibraryViewModel()
    }
}
