import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

# 1. Add decode function and SMS manager imports
imports = """import android.telephony.SmsManager
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import com.example.util.LiveTestState"""

content = content.replace("import com.example.util.WebServerState", imports + "\nimport com.example.util.WebServerState")

# 2. Add decode function at top level
decode_func = """
fun decodeBase64Image(base64Str: String): androidx.compose.ui.graphics.ImageBitmap? {
    if (base64Str.isEmpty()) return null
    return try {
        val cleanBase64 = base64Str.substringAfter("base64,")
        val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
    } catch (e: Exception) { null }
}
"""
content = content.replace("@OptIn", decode_func + "\n@OptIn")

# 3. Update candidate card layout
old_card_layout = """                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text(session.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                            Text("Roll: ${session.rollNumber}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                        }"""

new_card_layout = """                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.Top
                                                    ) {
                                                        Row {
                                                            val portraitBmp = decodeBase64Image(session.latestFrameBase64.ifEmpty { session.portraitBase64 })
                                                            if (portraitBmp != null) {
                                                                Image(
                                                                    bitmap = portraitBmp,
                                                                    contentDescription = "Candidate Portrait",
                                                                    contentScale = ContentScale.Crop,
                                                                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                                                )
                                                                Spacer(modifier = Modifier.width(12.dp))
                                                            }
                                                            Column {
                                                                Text(session.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                                Text("Roll: ${session.rollNumber}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                                if (session.mobile.isNotEmpty()) {
                                                                    Text("Mob: ${session.mobile}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                                }
                                                            }
                                                        }"""
content = content.replace(old_card_layout, new_card_layout)

old_buttons = """                                                                Button(
                                                                    onClick = {
                                                                        if (!session.isDispatched) {
                                                                            viewModel.dispatchCandidateMarksheet(session.rollNumber)
                                                                            Toast.makeText(context, "Marksheet successfully dispatched to candidate ${session.name}!", Toast.LENGTH_SHORT).show()
                                                                        }
                                                                    },
                                                                    modifier = Modifier.weight(1f).height(32.dp),
                                                                    contentPadding = PaddingValues(0.dp),
                                                                    colors = ButtonDefaults.buttonColors(
                                                                        containerColor = if (session.isDispatched) Color(0xFF047857) else MaterialTheme.colorScheme.primary
                                                                    )
                                                                ) {
                                                                    if (session.isDispatched) {
                                                                        Text("Dispatched ✓", style = MaterialTheme.typography.bodySmall, color = Color.White)
                                                                    } else {
                                                                        Text("Dispatch", style = MaterialTheme.typography.bodySmall)
                                                                    }
                                                                }"""

new_buttons = """                                                                Button(
                                                                    onClick = {
                                                                        if (!session.isDispatched) {
                                                                            viewModel.dispatchCandidateMarksheet(session.rollNumber)
                                                                            Toast.makeText(context, "Marksheet successfully dispatched to candidate ${session.name}!", Toast.LENGTH_SHORT).show()
                                                                            // Also send SMS
                                                                            if (session.mobile.isNotEmpty()) {
                                                                                try {
                                                                                    val msg = "Exam Result: Dear ${session.name} (Roll: ${session.rollNumber}), your score is ${session.score}/${session.totalMarks}. Status: ${session.status}."
                                                                                    SmsManager.getDefault().sendTextMessage(session.mobile, null, msg, null, null)
                                                                                    Toast.makeText(context, "SMS Marksheet sent!", Toast.LENGTH_SHORT).show()
                                                                                } catch (e: Exception) {
                                                                                    Toast.makeText(context, "Failed to send SMS (permission missing?)", Toast.LENGTH_SHORT).show()
                                                                                }
                                                                            }
                                                                        }
                                                                    },
                                                                    modifier = Modifier.weight(1f).height(32.dp),
                                                                    contentPadding = PaddingValues(0.dp),
                                                                    colors = ButtonDefaults.buttonColors(
                                                                        containerColor = if (session.isDispatched) Color(0xFF047857) else MaterialTheme.colorScheme.primary
                                                                    )
                                                                ) {
                                                                    if (session.isDispatched) {
                                                                        Text("Dispatched ✓", style = MaterialTheme.typography.bodySmall, color = Color.White)
                                                                    } else {
                                                                        Text("Dispatch & SMS", style = MaterialTheme.typography.bodySmall)
                                                                    }
                                                                }"""
content = content.replace(old_buttons, new_buttons)

warn_buttons = """                                                    if (session.status == "Testing") {
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                                            Button(
                                                                onClick = {
                                                                    LiveTestState.setWarning(session.rollNumber, "Please keep your eyes on the screen and do not talk.")
                                                                    Toast.makeText(context, "Text Warning Sent to Client Screen", Toast.LENGTH_SHORT).show()
                                                                },
                                                                modifier = Modifier.weight(1f).height(32.dp),
                                                                contentPadding = PaddingValues(0.dp),
                                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                                            ) {
                                                                Text("Issue Warning", style = MaterialTheme.typography.bodySmall)
                                                            }
                                                        }
                                                    }"""
                                                    
content = content.replace("Spacer(modifier = Modifier.height(12.dp))", "Spacer(modifier = Modifier.height(12.dp))\n" + warn_buttons)


with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
