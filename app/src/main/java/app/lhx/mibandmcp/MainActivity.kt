package app.lhx.mibandmcp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import app.lhx.mibandmcp.ui.MiBandMcpApp
import app.lhx.mibandmcp.ui.MainViewModel
import app.lhx.mibandmcp.ui.theme.MiBandMcpTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiBandMcpTheme {
                val context = LocalContext.current
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = {},
                )
                val documentLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument(),
                    onResult = viewModel::onExportFileSelected,
                )
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                MiBandMcpApp(
                    viewModel = viewModel,
                    onSelectExportFile = {
                        documentLauncher.launch(arrayOf("*/*"))
                    },
                )
            }
        }
    }
}
