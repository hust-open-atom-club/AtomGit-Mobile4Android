package com.hfad.atomgit.ui.code

import com.hfad.atomgit.MainDispatcherRule
import com.hfad.atomgit.data.model.CodeViewerPreferences
import com.hfad.atomgit.data.model.RepoFileContent
import com.hfad.atomgit.data.remote.error.ApiResult
import com.hfad.atomgit.data.repository.CodeViewerPreferencesRepository
import com.hfad.atomgit.data.repository.RepoContentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CodeViewerViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val contents = FakeContents()
    private val preferences = object : CodeViewerPreferencesRepository {
        override val preferences = MutableStateFlow(CodeViewerPreferences())
        override suspend fun setShowLineNumbers(enabled: Boolean) = Unit
        override suspend fun setAlwaysDarkTheme(enabled: Boolean) = Unit
        override suspend fun setOverrideSystemFontSize(enabled: Boolean) = Unit
        override suspend fun setFontSizeSp(sizeSp: Int) = Unit
        override suspend fun setWordWrap(enabled: Boolean) = Unit
    }

    @Test fun knownBinaryDoesNotDownloadAndIsNotReloadedOnReturn() = runTest {
        val vm = CodeViewerViewModel(contents, preferences)
        repeat(2) { vm.start("o", "r", "image.PNG", "main", false) }
        assertEquals(0, contents.calls)
        assertFalse(vm.state.value.previewSupported)
        assertFalse(vm.state.value.isLoading)
        assertNotNull(vm.state.value.previewMessage)
    }

    @Test fun emptyTextFileIsLoadedOnlyOnce() = runTest {
        val vm = CodeViewerViewModel(contents, preferences)
        repeat(2) { vm.start("o", "r", "empty.txt", "main", false) }
        assertEquals(1, contents.calls)
        assertTrue(vm.state.value.previewSupported)
        assertEquals("", vm.state.value.content)
    }

    @Test fun hugeFileLimitsRenderingButKeepsFullContentForCopyAndShare() = runTest {
        contents.text = ("x".repeat(499) + "\n").repeat(500)
        val vm = CodeViewerViewModel(contents, preferences)
        vm.start("o", "r", "large.txt", "feature/a", false)
        assertEquals(250_000, vm.state.value.content.length)
        assertEquals(200_000, vm.state.value.previewContent!!.length)
        assertNotNull(vm.state.value.previewMessage)
        assertEquals("feature/a", contents.ref)
    }

    private class FakeContents : RepoContentRepository {
        var calls = 0
        var text = ""
        var ref = ""
        override suspend fun getFile(owner: String, repo: String, path: String, ref: String): ApiResult<RepoFileContent> {
            calls++
            this.ref = ref
            return ApiResult.Success(RepoFileContent(path, path.substringAfterLast('/'), text, null, text.length.toLong(), null))
        }

        override suspend fun getFileBytes(owner: String, repo: String, path: String, ref: String): ApiResult<ByteArray> =
            ApiResult.Success(text.toByteArray())
    }
}
