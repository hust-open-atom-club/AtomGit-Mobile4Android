package com.hfad.atomgit.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.hfad.atomgit.R
import com.hfad.atomgit.data.repository.LoginFailure

/** PAT 登录页 */
@Composable
fun LoginScreen(
    state: LoginUiState,
    expired: Boolean,
    onTokenChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tokenVisible by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .testTag("screen-login"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "🎉欢迎登录你的AtomGit账号",
            style = MaterialTheme.typography.headlineMedium,
        )
        if (expired) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "登录已失效，请重新输入令牌。",
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login-expired"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = state.token,
            onValueChange = onTokenChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login-token"),
            label = { Text("访问令牌") },
            singleLine = true,
            enabled = !state.submitting,
            isError = state.failure != null,
            visualTransformation = if (tokenVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { if (state.canSubmit) onSubmit() },
            ),
            trailingIcon = {
                IconButton(
                    onClick = { tokenVisible = !tokenVisible },
                    enabled = !state.submitting,
                    modifier = Modifier.testTag("login-token-visibility"),
                ) {
                    Icon(
                        painter = painterResource(
                            if (tokenVisible) {
                                R.drawable.ic_visibility_off
                            } else {
                                R.drawable.ic_visibility
                            },
                        ),
                        contentDescription = if (tokenVisible) "隐藏令牌" else "显示令牌",
                    )
                }
            },
            supportingText = {
                Text(
                    text = "在网页创建 PAT",
                    modifier = Modifier.clickable { uriHandler.openUri(PAT_PAGE) },
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                )
            },
        )
        state.failure?.let { failure ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = failure.message(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login-error"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onSubmit,
            enabled = state.canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login-submit"),
        ) {
            if (state.submitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(12.dp))
            }
            Text(if (state.submitting) "校验中…" else "登录")
        }
    }
}

private fun LoginFailure.message(): String = when (this) {
    LoginFailure.InvalidToken -> "令牌无效或已撤销，请检查后重试。"
    LoginFailure.Network -> "网络不可用，请检查连接后重试。"
    LoginFailure.Unexpected -> "登录失败，请稍后重试。"
}

private const val PAT_PAGE = "https://atomgit.com/setting/token-classic"
