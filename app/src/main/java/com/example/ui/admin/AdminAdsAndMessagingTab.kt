package com.example.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.cloud.AdCardDoc
import com.example.data.cloud.AdPlacement
import com.example.data.cloud.AdminMessageDoc
import com.example.data.cloud.AdminMessagePriority
import com.example.data.cloud.AdminMessageType
import com.example.data.cloud.NoorSecurityValidators
import com.example.data.cloud.UserAccountDoc
import com.example.ui.components.NoorAdCardItem
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.viewmodel.AdminViewModel

/**
 * Super Admin Ads & Admin-to-User Messaging Tab:
 * - Sub-tab 0: Administrative Advertisement Cards (Super Admin exclusive, zero 3P ad SDKs,
 *   zero-permission Android Photo Picker, live preview, placement guard, impressions/clicks).
 * - Sub-tab 1: Admin-to-User Messaging & Notifications (All / Group / Individual User,
 *   types, priority, optional image, scheduling, delivery & read count history).
 */
@Composable
fun AdminAdsAndMessagingTab(
    adminViewModel: AdminViewModel,
    onRequireMfa: (() -> Unit) -> Unit,
    initialSubTab: Int = 0,
    preselectedRecipient: UserAccountDoc? = null,
    onConsumedPreselectedRecipient: () -> Unit = {}
) {
    val lang = LocalAppLanguage.current
    val currentRole by adminViewModel.currentRole.collectAsStateWithLifecycle()
    val adCards by adminViewModel.allAdCards.collectAsStateWithLifecycle()
    val adminMessages by adminViewModel.allAdminMessages.collectAsStateWithLifecycle()
    val allUsers by adminViewModel.allUsers.collectAsStateWithLifecycle()

    var subTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }
    var editingAdCard by remember { mutableStateOf<AdCardDoc?>(null) }
    var showCreateAdDialog by remember { mutableStateOf(false) }
    var deletingAdCard by remember { mutableStateOf<AdCardDoc?>(null) }
    var showComposeMessageDialog by remember(preselectedRecipient) {
        mutableStateOf(preselectedRecipient != null)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_ads_messaging_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminSectionHeader(
                title = L10n.pick(lang, "البطاقات الإعلانية والرسائل الإدارية", "Ad Cards & Admin Messaging"),
                subtitle = L10n.pick(
                    lang,
                    "إعلانات إدارية نظيفة بدون شبكات إعلانية خارجية + رسائل وإشعارات موجهة للمستخدمين",
                    "Clean Super-Admin ad cards (zero 3P SDKs) & targeted user messaging"
                )
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminFilterPill(
                    label = L10n.pick(lang, "بطاقات الإعلانات (${adCards.size})", "Ad Cards (${adCards.size})"),
                    selected = subTab == 0,
                    onClick = { subTab = 0 }
                )
                AdminFilterPill(
                    label = L10n.pick(lang, "رسائل المستخدمين (${adminMessages.size})", "Messaging (${adminMessages.size})"),
                    selected = subTab == 1,
                    onClick = { subTab = 1 }
                )
            }
        }

        if (subTab == 0) {
            if (!currentRole.canManageAds) {
                item {
                    AdminPanelCard(accentBorder = AdminPalette.AccentRose) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = AdminPalette.AccentRose)
                            Text(
                                text = L10n.pick(lang, "صلاحية المالك / المشرف الأعلى فقط", "Super Admin Exclusive Control"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AdminPalette.AccentRose
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = L10n.pick(
                                lang,
                                "إنشاء وتعديل وحذف بطاقات الإعلانات الإدارية محصور حصراً بالمشرف الأعلى.",
                                "Creating, editing, and deleting administrative advertisement cards is strictly restricted to Super Admin."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = AdminPalette.TextSecondary
                        )
                    }
                }
            } else {
                item {
                    AdminPanelCard(accentBorder = AdminPalette.AccentEmerald.copy(alpha = 0.45f)) {
                        Text(
                            text = L10n.pick(
                                lang,
                                "مواضع العرض المسموحة فقط: الشاشة الرئيسية، مركز منظومة نور، المكتبة الصوتية. يُمنع منعاً باتاً ظهور الإعلانات داخل قراءة القرآن أو التسميع أو الحفظ أو مواقيت الصلاة.",
                                "Allowed placements ONLY: Home, Ecosystem Hub, Audio Library. Strictly forbidden inside Quran reading, recitation, memorization, or Prayer Times."
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = AdminPalette.AccentEmerald
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showCreateAdDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("admin_create_ad_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(L10n.pick(lang, "إنشاء بطاقة إعلان إداري جديدة", "Create Advertisement Card"))
                        }
                    }
                }

                if (adCards.isEmpty()) {
                    item {
                        AdminPanelCard {
                            Text(
                                text = L10n.pick(lang, "لا توجد بطاقات إعلانية حالياً.", "No advertisement cards created yet."),
                                color = AdminPalette.TextSecondary
                            )
                        }
                    }
                } else {
                    items(adCards, key = { "ad_card_${it.id}" }) { card ->
                        val activeNow = card.isCurrentlyActive()
                        AdminPanelCard(
                            accentBorder = if (activeNow) AdminPalette.AccentCyan.copy(alpha = 0.5f) else AdminPalette.BorderSubtle
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AdminStatusBadge(
                                        label = AdPlacement.fromKey(card.placement).let { L10n.pick(lang, it.labelAr, it.labelEn) },
                                        color = AdminPalette.AccentCyan
                                    )
                                    AdminStatusBadge(
                                        label = if (activeNow) L10n.pick(lang, "نشط", "ACTIVE") else L10n.pick(lang, "متوقف", "INACTIVE"),
                                        color = if (activeNow) AdminPalette.AccentEmerald else AdminPalette.AccentAmber
                                    )
                                }
                                Switch(
                                    checked = card.enabled,
                                    onCheckedChange = { adminViewModel.toggleAdCardEnabled(card, it) }
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = L10n.pick(lang, card.titleAr, card.titleEn.ifBlank { card.titleAr }),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AdminPalette.TextPrimary
                            )
                            if (card.descriptionAr.isNotBlank() || card.descriptionEn.isNotBlank()) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = L10n.pick(lang, card.descriptionAr, card.descriptionEn.ifBlank { card.descriptionAr }),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AdminPalette.TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Rounded.Visibility, null, tint = AdminPalette.AccentCyan, modifier = Modifier.size(15.dp))
                                        Text("${card.impressionsCount}", style = MaterialTheme.typography.labelSmall, color = AdminPalette.TextPrimary)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Rounded.AdsClick, null, tint = AdminPalette.AccentAmber, modifier = Modifier.size(15.dp))
                                        Text("${card.clicksCount}", style = MaterialTheme.typography.labelSmall, color = AdminPalette.TextPrimary)
                                    }
                                    Text(
                                        text = "#${card.displayOrder}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AdminPalette.TextMuted
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { editingAdCard = card }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Edit Ad", tint = AdminPalette.AccentCyan)
                                    }
                                    IconButton(onClick = { deletingAdCard = card }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Ad", tint = AdminPalette.AccentRose)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Sub-tab 1: Admin-to-User Messaging & Notifications
            item {
                AdminPanelCard(accentBorder = AdminPalette.AccentCyan.copy(alpha = 0.45f)) {
                    Text(
                        text = L10n.pick(
                            lang,
                            "تصل الرسائل فورياً إلى صندوق إشعارات المستخدمين داخل التطبيق (مع تتبع القراءة). ملاحظة: الإشعارات الخلفية عند إغلاق التطبيق تماماً تتطلب خادم FCM خارجي.",
                            "Messages deliver in real time to users' in-app Notification Center with read tracking. Note: background push when the app is terminated requires a server-side FCM sender."
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = AdminPalette.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showComposeMessageDialog = true },
                        enabled = currentRole.canSendAdminMessages,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("admin_compose_message_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(L10n.pick(lang, "إرسال رسالة / إشعار جديد", "Compose Admin Message"))
                    }
                }
            }

            if (adminMessages.isEmpty()) {
                item {
                    AdminPanelCard {
                        Text(
                            text = L10n.pick(lang, "لم يتم إرسال رسائل إدارية بعد.", "No admin messages sent yet."),
                            color = AdminPalette.TextSecondary
                        )
                    }
                }
            } else {
                items(adminMessages, key = { "admin_msg_${it.id}" }) { msg ->
                    val priorityColor = when (msg.priority) {
                        "URGENT" -> AdminPalette.AccentRose
                        "HIGH" -> AdminPalette.AccentAmber
                        "LOW" -> AdminPalette.TextMuted
                        else -> AdminPalette.AccentCyan
                    }
                    AdminPanelCard(accentBorder = priorityColor.copy(alpha = 0.45f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                AdminStatusBadge(label = msg.messageType, color = AdminPalette.AccentIndigo)
                                AdminStatusBadge(label = msg.priority, color = priorityColor)
                            }
                            IconButton(onClick = { adminViewModel.deleteAdminMessage(msg.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Message", tint = AdminPalette.AccentRose)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = L10n.pick(lang, msg.titleAr, msg.titleEn.ifBlank { msg.titleAr }),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AdminPalette.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = L10n.pick(lang, msg.bodyAr, msg.bodyEn.ifBlank { msg.bodyAr }),
                            style = MaterialTheme.typography.bodySmall,
                            color = AdminPalette.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val targetLabel = when (msg.targetType) {
                                "USER" -> L10n.pick(lang, "مستخدم: ${msg.targetUserEmail.ifBlank { msg.targetUserUid }}", "User: ${msg.targetUserEmail.ifBlank { msg.targetUserUid }}")
                                "GROUP" -> L10n.pick(lang, "مجموعة: ${msg.targetGroup}", "Group: ${msg.targetGroup}")
                                else -> L10n.pick(lang, "جميع المستخدمين", "All Users")
                            }
                            Text(targetLabel, style = MaterialTheme.typography.labelSmall, color = AdminPalette.AccentCyan)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.MarkEmailRead, null, tint = AdminPalette.AccentEmerald, modifier = Modifier.size(15.dp))
                                Text(
                                    text = L10n.pick(lang, "قرأها: ${msg.readByUids.size}", "Read: ${msg.readByUids.size}"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AdminPalette.AccentEmerald
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create / Edit Ad Card Dialog with Zero-Permission Photo Picker & Live Preview
    if (showCreateAdDialog || editingAdCard != null) {
        val initial = editingAdCard ?: AdCardDoc()
        AdCardEditorDialog(
            initial = initial,
            onDismiss = {
                showCreateAdDialog = false
                editingAdCard = null
            },
            onSave = { updated ->
                onRequireMfa {
                    adminViewModel.saveAdCard(updated)
                    showCreateAdDialog = false
                    editingAdCard = null
                }
            }
        )
    }

    // Delete Ad Confirmation Dialog
    deletingAdCard?.let { card ->
        AlertDialog(
            onDismissRequest = { deletingAdCard = null },
            title = { Text(L10n.pick(lang, "حذف بطاقة الإعلان", "Delete Advertisement Card")) },
            text = {
                Text(
                    L10n.pick(
                        lang,
                        "هل تريد حذف البطاقة الإعلانية \"${card.titleAr}\" نهائياً؟",
                        "Permanently delete advertisement card \"${card.titleEn.ifBlank { card.titleAr }}\"?"
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRequireMfa {
                            adminViewModel.deleteAdCard(card.id)
                            deletingAdCard = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminPalette.AccentRose)
                ) {
                    Text(L10n.pick(lang, "حذف", "Delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingAdCard = null }) {
                    Text(L10n.pick(lang, "إلغاء", "Cancel"))
                }
            }
        )
    }

    // Compose Admin-to-User Message Dialog
    if (showComposeMessageDialog) {
        ComposeAdminMessageDialog(
            allUsers = allUsers,
            preselectedUser = preselectedRecipient,
            onDismiss = {
                showComposeMessageDialog = false
                onConsumedPreselectedRecipient()
            },
            onSend = { msg ->
                adminViewModel.sendAdminMessage(msg)
                showComposeMessageDialog = false
                onConsumedPreselectedRecipient()
            }
        )
    }
}

@Composable
private fun AdCardEditorDialog(
    initial: AdCardDoc,
    onDismiss: () -> Unit,
    onSave: (AdCardDoc) -> Unit
) {
    val lang = LocalAppLanguage.current
    var titleAr by remember(initial) { mutableStateOf(initial.titleAr) }
    var titleEn by remember(initial) { mutableStateOf(initial.titleEn) }
    var descAr by remember(initial) { mutableStateOf(initial.descriptionAr) }
    var descEn by remember(initial) { mutableStateOf(initial.descriptionEn) }
    var imageUrl by remember(initial) { mutableStateOf(initial.imageUrl) }
    var destinationUrl by remember(initial) { mutableStateOf(initial.destinationUrl) }
    var placement by remember(initial) { mutableStateOf(AdPlacement.fromKey(initial.placement)) }
    var enabled by remember(initial) { mutableStateOf(initial.enabled) }
    var orderText by remember(initial) { mutableStateOf(initial.displayOrder.toString()) }
    var durationDays by remember(initial) { mutableIntStateOf(0) } // 0 = Always Active, 7, 30

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            imageUrl = uri.toString()
        }
    }

    val linkValid = NoorSecurityValidators.isSafeOptionalHttpsUrl(destinationUrl)
    val previewCard = AdCardDoc(
        id = initial.id,
        titleAr = titleAr.ifBlank { "معاينة العنوان" },
        titleEn = titleEn.ifBlank { titleAr.ifBlank { "Title Preview" } },
        descriptionAr = descAr,
        descriptionEn = descEn,
        imageUrl = imageUrl,
        destinationUrl = if (linkValid) destinationUrl else "",
        placement = placement.key,
        enabled = enabled,
        displayOrder = orderText.toIntOrNull()?.coerceIn(1, 999) ?: 1
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                L10n.pick(
                    lang,
                    if (initial.id.isBlank()) "بطاقة إعلان إداري جديدة" else "تعديل بطاقة الإعلان",
                    if (initial.id.isBlank()) "New Advertisement Card" else "Edit Advertisement Card"
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(L10n.pick(lang, "موضع العرض (مسموح فقط):", "Placement (Approved Only):"), style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(AdPlacement.entries) { p ->
                        AdminFilterPill(
                            label = L10n.pick(lang, p.labelAr, p.labelEn),
                            selected = placement == p,
                            onClick = { placement = p }
                        )
                    }
                }

                OutlinedTextField(
                    value = titleAr,
                    onValueChange = { titleAr = it },
                    label = { Text("العنوان (عربي) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = titleEn,
                    onValueChange = { titleEn = it },
                    label = { Text("Title (EN)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descAr,
                    onValueChange = { descAr = it },
                    label = { Text("الوصف (عربي)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descEn,
                    onValueChange = { descEn = it },
                    label = { Text("Description (EN)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(L10n.pick(lang, "اختيار صورة", "Pick Image"))
                    }
                    if (imageUrl.isNotBlank()) {
                        TextButton(onClick = { imageUrl = "" }) {
                            Text(L10n.pick(lang, "إزالة", "Clear"), color = AdminPalette.AccentRose)
                        }
                    }
                }
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text(L10n.pick(lang, "رابط الصورة (https:// أو مختارة)", "Image URL (https:// or picked)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = destinationUrl,
                    onValueChange = { destinationUrl = it },
                    label = { Text(L10n.pick(lang, "رابط الوجهة الاختياري (https://)", "Optional Destination URL (https://)")) },
                    isError = !linkValid,
                    supportingText = if (!linkValid) {
                        { Text(L10n.pick(lang, "يجب أن يبدأ الرابط بـ https:// وبصيغة آمنة", "Must start with https://")) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = orderText,
                        onValueChange = { orderText = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text(L10n.pick(lang, "الترتيب", "Order")) },
                        singleLine = true,
                        modifier = Modifier.width(96.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(L10n.pick(lang, "المدة الزمنية:", "Schedule:"), style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(0 to "دائم", 7 to "٧ أيام", 30 to "٣٠ يوم").forEach { (d, lbl) ->
                                AdminFilterPill(label = lbl, selected = durationDays == d, onClick = { durationDays = d })
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(L10n.pick(lang, "تفعيل العرض فوراً", "Enable Card"))
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }

                Text(
                    text = L10n.pick(lang, "معاينة حية قبل النشر:", "Live Preview:"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AdminPalette.AccentCyan
                )
                NoorAdCardItem(card = previewCard, onClick = {}, isPreview = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val now = System.currentTimeMillis()
                    val startMs = if (durationDays > 0) now else initial.startAtMillis
                    val endMs = if (durationDays > 0) now + durationDays * 86_400_000L else 0L
                    onSave(
                        initial.copy(
                            titleAr = titleAr.ifBlank { titleEn }.trim(),
                            titleEn = titleEn.ifBlank { titleAr }.trim(),
                            descriptionAr = descAr.trim(),
                            descriptionEn = descEn.trim(),
                            imageUrl = imageUrl.trim(),
                            destinationUrl = destinationUrl.trim(),
                            placement = placement.key,
                            enabled = enabled,
                            startAtMillis = startMs,
                            endAtMillis = endMs,
                            displayOrder = orderText.toIntOrNull()?.coerceIn(1, 999) ?: 1
                        )
                    )
                },
                enabled = (titleAr.isNotBlank() || titleEn.isNotBlank()) && linkValid
            ) {
                Text(L10n.pick(lang, "حفظ ونشر", "Save & Publish"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(L10n.pick(lang, "إلغاء", "Cancel"))
            }
        }
    )
}

@Composable
private fun ComposeAdminMessageDialog(
    allUsers: List<UserAccountDoc>,
    preselectedUser: UserAccountDoc?,
    onDismiss: () -> Unit,
    onSend: (AdminMessageDoc) -> Unit
) {
    val lang = LocalAppLanguage.current
    var targetType by remember(preselectedUser) { mutableStateOf(if (preselectedUser != null) "USER" else "ALL") }
    var targetGroup by remember { mutableStateOf("ACTIVE") }
    var selectedUser by remember(preselectedUser) { mutableStateOf(preselectedUser) }
    var messageType by remember(preselectedUser) {
        mutableStateOf(if (preselectedUser != null) AdminMessageType.DIRECT else AdminMessageType.SYSTEM)
    }
    var priority by remember { mutableStateOf(AdminMessagePriority.NORMAL) }
    var titleAr by remember { mutableStateOf("") }
    var titleEn by remember { mutableStateOf("") }
    var bodyAr by remember { mutableStateOf("") }
    var bodyEn by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var scheduleDelayHours by remember { mutableIntStateOf(0) } // 0 = immediate

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) imageUrl = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(L10n.pick(lang, "إرسال رسالة / إشعار إداري", "Send Admin Message / Notification")) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(L10n.pick(lang, "الفئة المستهدفة:", "Target Audience:"), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AdminFilterPill(label = L10n.pick(lang, "الكل", "All"), selected = targetType == "ALL", onClick = { targetType = "ALL" })
                    AdminFilterPill(label = L10n.pick(lang, "مجموعة", "Group"), selected = targetType == "GROUP", onClick = { targetType = "GROUP" })
                    AdminFilterPill(label = L10n.pick(lang, "مستخدم محدد", "User"), selected = targetType == "USER", onClick = { targetType = "USER" })
                }

                if (targetType == "GROUP") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("ACTIVE", "VERIFIED", "ADMINS")) { grp ->
                            AdminFilterPill(label = grp, selected = targetGroup == grp, onClick = { targetGroup = grp })
                        }
                    }
                } else if (targetType == "USER") {
                    if (allUsers.isEmpty()) {
                        Text(
                            text = L10n.pick(lang, "لا يوجد مستخدمون مسجلون في السحابة بعد.", "No cloud users loaded yet."),
                            style = MaterialTheme.typography.labelSmall,
                            color = AdminPalette.AccentAmber
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(allUsers.take(20), key = { it.uid.ifBlank { it.id } }) { u ->
                                AdminFilterPill(
                                    label = u.displayName.ifBlank { u.email.substringBefore("@") },
                                    selected = selectedUser?.uid == u.uid,
                                    onClick = { selectedUser = u }
                                )
                            }
                        }
                    }
                }

                Text(L10n.pick(lang, "نوع الرسالة:", "Message Type:"), style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(AdminMessageType.entries) { mt ->
                        AdminFilterPill(
                            label = L10n.pick(lang, mt.labelAr, mt.labelEn),
                            selected = messageType == mt,
                            onClick = { messageType = mt }
                        )
                    }
                }

                Text(L10n.pick(lang, "الأولوية:", "Priority:"), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AdminMessagePriority.entries.forEach { pr ->
                        AdminFilterPill(
                            label = L10n.pick(lang, pr.labelAr, pr.labelEn),
                            selected = priority == pr,
                            onClick = { priority = pr }
                        )
                    }
                }

                OutlinedTextField(
                    value = titleAr,
                    onValueChange = { titleAr = it },
                    label = { Text("العنوان (عربي) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = titleEn,
                    onValueChange = { titleEn = it },
                    label = { Text("Title (EN)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bodyAr,
                    onValueChange = { bodyAr = it },
                    label = { Text("نص الرسالة (عربي) *") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bodyEn,
                    onValueChange = { bodyEn = it },
                    label = { Text("Message Body (EN)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(L10n.pick(lang, "إرفاق صورة", "Attach Image"))
                    }
                    if (imageUrl.isNotBlank()) {
                        TextButton(onClick = { imageUrl = "" }) {
                            Text(L10n.pick(lang, "إزالة", "Clear"), color = AdminPalette.AccentRose)
                        }
                    }
                }

                Text(L10n.pick(lang, "وقت الإرسال:", "Delivery Time:"), style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "فوراً", 1 to "بعد ساعة", 24 to "بعد يوم").forEach { (h, lbl) ->
                        AdminFilterPill(label = lbl, selected = scheduleDelayHours == h, onClick = { scheduleDelayHours = h })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val scheduled = if (scheduleDelayHours > 0) {
                        System.currentTimeMillis() + scheduleDelayHours * 3_600_000L
                    } else 0L
                    onSend(
                        AdminMessageDoc(
                            targetType = targetType,
                            targetGroup = if (targetType == "GROUP") targetGroup else if (targetType == "ALL") "ALL" else "",
                            targetUserUid = if (targetType == "USER") (selectedUser?.uid ?: "") else "",
                            targetUserEmail = if (targetType == "USER") (selectedUser?.email ?: "") else "",
                            titleAr = titleAr.ifBlank { titleEn }.trim(),
                            titleEn = titleEn.ifBlank { titleAr }.trim(),
                            bodyAr = bodyAr.ifBlank { bodyEn }.trim(),
                            bodyEn = bodyEn.ifBlank { bodyAr }.trim(),
                            messageType = messageType.key,
                            priority = priority.key,
                            imageUrl = imageUrl.trim(),
                            scheduledAtMillis = scheduled,
                            active = true
                        )
                    )
                },
                enabled = (titleAr.isNotBlank() || titleEn.isNotBlank()) &&
                    (bodyAr.isNotBlank() || bodyEn.isNotBlank()) &&
                    (targetType != "USER" || selectedUser != null)
            ) {
                Text(L10n.pick(lang, "إرسال الآن", "Send Message"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(L10n.pick(lang, "إلغاء", "Cancel"))
            }
        }
    )
}
