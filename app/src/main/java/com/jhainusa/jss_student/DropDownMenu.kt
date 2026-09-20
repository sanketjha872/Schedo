package com.jhainusa.jss_student

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jhainusa.jss_student.GeminiBackend.sendImageToSupabase
import com.jhainusa.jss_student.RoomDatabase.MainVIewModel
import com.jhainusa.jss_student.UserPref.NameViewModel
import com.jhainusa.jss_student.ciaPaperPage.Routes

@Composable
fun DropdownMenuExample(
    vIewModel: MainVIewModel,
    isEditMode: Boolean,
    onEditModeToggle: () -> Unit,
    navController: NavController,
    nameViewModel: NameViewModel = viewModel()
) {
    val context = LocalContext.current
    val userId by nameViewModel.userIdFlow.collectAsState()
    val username by nameViewModel.nameFlow.collectAsState()
    var loading by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            loading = true
            sendImageToSupabase(
                context = context,
                uri = it,
                userIdStr = userId ?: "unknown_user",
                username = username ?: "unknown_name",
                viewModel = vIewModel
            ) { success ->
                loading = false
                Log.d("Supabase", "Success: $success")
            }
        }
    }

    IconButton(
        onClick = {
            if (isEditMode) {
                onEditModeToggle()
            } else {
                expanded = !expanded
            }
        }
    ) {
        Icon(
            painter = painterResource(if (isEditMode) R.drawable.check else R.drawable.menu_hamburger_svgrepo_com),
            contentDescription = if (isEditMode) "Done" else "More Options",
            tint = if (isEditMode) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(29.dp)
        )
    }

    DropdownMenu(
        shape = RoundedCornerShape(12.dp),
        expanded = expanded,
        shadowElevation = 10.dp,
        tonalElevation = 10.dp,
        onDismissRequest = { expanded = false },
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
    ) {
        DropdownMenuItem(onClick = {
            expanded = false
            onEditModeToggle()
        }) {
            Text(
                text = if (isEditMode) "Done" else "Edit",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 15.sp,
                fontFamily = plusJak,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = painterResource(if (isEditMode) R.drawable.baseline_check_24 else R.drawable.edit_svgrepo_com),
                contentDescription = null,
                tint = if (isEditMode) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenuItem(onClick = {
            expanded = false
            launcher.launch("image/*")
        }) {
            Text(
                text = "Upload",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 15.sp,
                fontFamily = plusJak,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(30.dp))
            Icon(
                painter = painterResource(R.drawable.ai_svgrepo_com),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenuItem(onClick = {
            expanded = false
            navController.navigate(Routes.MORE_OPTIONS)
        }) {
            Text(
                text = "More",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 15.sp,
                fontFamily = plusJak,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(30.dp))
            Icon(
                painter = painterResource(R.drawable.setting_2_svgrepo_com),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    if (loading) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            LottieLoader("Analyzing your image...\n AI can make mistakes so please verify it", R.raw.handloader)
        }
    }
}
