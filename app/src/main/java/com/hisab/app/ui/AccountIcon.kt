package com.hisab.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hisab.app.data.Account

/**
 * লোগো ফাইলের সম্ভাব্য নাম (app/src/main/res/drawable-nodpi/ ফোল্ডারে রাখলে অ্যাপ নিজেই ব্যবহার করবে)।
 * ১) acc_id_<অ্যাকাউন্ট আইডি>  ২) নাম দেখে acc_bkash / acc_nagad / acc_ab_bank / acc_tfic
 * ৩) acc_type_<ধরন>, যেমন acc_type_bank
 */
private fun logoKeys(a: Account): List<String> {
    val n = a.name.lowercase()
    val keys = mutableListOf("acc_id_${a.id}")
    when {
        "bkash" in n || "বিকাশ" in n -> keys += "acc_bkash"
        "nagad" in n || "নগদ" in n -> keys += "acc_nagad"
        n.startsWith("ab ") || n == "ab" -> keys += "acc_ab_bank"
        "tfic" in n || "ific" in n -> keys += "acc_tfic"
    }
    keys += "acc_type_${a.type}"
    return keys
}

/**
 * অ্যাকাউন্টের ছবি: লোগো ফাইল থাকলে সেটা, না থাকলে বেছে নেওয়া আইকন (AccountBadge)।
 */
@Composable
fun AccountAvatar(a: Account, size: Dp = 40.dp) {
    val ctx = LocalContext.current
    val resId = remember(a.id, a.name, a.type) {
        logoKeys(a).firstNotNullOfOrNull { key ->
            ctx.resources.getIdentifier(key, "drawable", ctx.packageName).takeIf { it != 0 }
        } ?: 0
    }
    if (resId != 0) {
        Image(
            painter = painterResource(resId),
            contentDescription = a.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(CircleShape),
        )
    } else {
        AccountBadge(iconFor(a), size)
    }
}
