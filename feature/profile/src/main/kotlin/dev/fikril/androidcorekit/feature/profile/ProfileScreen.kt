package dev.fikril.androidcorekit.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.fikril.androidcorekit.core.designsystem.theme.AndroidCoreKitTheme

@Composable
fun ProfileScreen(
    onGoToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Placeholder screen",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onGoToHome) {
            Text(text = "Go to Home")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    AndroidCoreKitTheme {
        ProfileScreen(onGoToHome = {})
    }
}
