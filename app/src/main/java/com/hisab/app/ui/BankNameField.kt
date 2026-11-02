package com.hisab.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/** লোগো (res/drawable-nodpi/<logo>.png)। ফাইল না থাকলে কিছু দেখায় না */
@Composable
fun BankLogo(logo: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val resId = remember(logo) { ctx.resources.getIdentifier(logo, "drawable", ctx.packageName) }
    if (resId != 0) {
        Image(
            painter = painterResource(resId),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier.clip(RoundedCornerShape(6.dp)).background(Color.White).padding(3.dp),
        )
    }
}

/**
 * অ্যাকাউন্টের নামের ঘর। ধরন "ব্যাংক" বা "মোবাইল" হলে লিখতে লিখতে তালিকা থেকে সাজেশন আসে।
 * বাংলায় লিখলে বাংলা নাম, ইংরেজিতে লিখলে ইংরেজি নাম বসে। তালিকায় না থাকলে লেখা নামই থাকে।
 */
@Composable
fun BankNameField(type: String, name: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TextInput(name, onChange, "অ্যাকাউন্টের নাম")
        val shown = bankSuggestions(type, name)
        if (shown.isNotEmpty()) {
            Surface(
                tonalElevation = 3.dp,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    shown.forEach { b ->
                        val display = b.nameFor(name)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onChange(display) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BankLogo(b.logo, Modifier.width(54.dp).height(32.dp))
                            Text(display, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            }
        }
    }
}
