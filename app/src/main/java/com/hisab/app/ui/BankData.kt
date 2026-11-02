package com.hisab.app.ui

/** ব্যাংক / মোবাইল ব্যাংকিংয়ের নাম (ইংরেজি ও বাংলা) এবং লোগো ফাইলের নাম (res/drawable-nodpi) */
data class BankInfo(val en: String, val bn: String, val logo: String)

val BANK_LIST = listOf(
    BankInfo("AB Bank", "এবি ব্যাংক", "bank_02"),
    BankInfo("Agrani Bank", "অগ্রণী ব্যাংক", "bank_03"),
    BankInfo("Al-Arafah Islami Bank", "আল-আরাফাহ ইসলামী ব্যাংক", "bank_04"),
    BankInfo("Bangladesh Commerce Bank", "বাংলাদেশ কমার্স ব্যাংক", "bank_05"),
    BankInfo("Bangladesh Development Bank", "বাংলাদেশ ডেভেলপমেন্ট ব্যাংক", "bank_06"),
    BankInfo("Bangladesh Krishi Bank", "বাংলাদেশ কৃষি ব্যাংক", "bank_07"),
    BankInfo("Bank Alfalah", "ব্যাংক আলফালাহ", "bank_08"),
    BankInfo("Bank Asia", "ব্যাংক এশিয়া", "bank_09"),
    BankInfo("BASIC Bank", "বেসিক ব্যাংক", "bank_10"),
    BankInfo("Bengal Commercial Bank", "বেঙ্গল কমার্শিয়াল ব্যাংক", "bank_11"),
    BankInfo("BRAC Bank", "ব্র্যাক ব্যাংক", "bank_12"),
    BankInfo("Citibank", "সিটিব্যাংক", "bank_13"),
    BankInfo("Citizens Bank", "সিটিজেন্স ব্যাংক", "bank_14"),
    BankInfo("City Bank", "সিটি ব্যাংক", "bank_15"),
    BankInfo("Commercial Bank of Ceylon", "কমার্শিয়াল ব্যাংক অব সিলন", "bank_16"),
    BankInfo("Community Bank", "কমিউনিটি ব্যাংক", "bank_17"),
    BankInfo("Dhaka Bank", "ঢাকা ব্যাংক", "bank_18"),
    BankInfo("Dutch-Bangla Bank", "ডাচ-বাংলা ব্যাংক", "bank_19"),
    BankInfo("Eastern Bank", "ইস্টার্ন ব্যাংক", "bank_20"),
    BankInfo("EXIM Bank", "এক্সিম ব্যাংক", "bank_21"),
    BankInfo("FSIB PLC", "ফার্স্ট সিকিউরিটি ইসলামী ব্যাংক পিএলসি", "bank_22"),
    BankInfo("Global Islami Bank", "গ্লোবাল ইসলামী ব্যাংক", "bank_23"),
    BankInfo("Habib Bank", "হাবিব ব্যাংক", "bank_24"),
    BankInfo("HSBC Bank", "এইচএসবিসি ব্যাংক", "bank_25"),
    BankInfo("ICB Islamic Bank", "আইসিবি ইসলামিক ব্যাংক", "bank_26"),
    BankInfo("IFIC Bank", "আইএফআইসি ব্যাংক", "bank_27"),
    BankInfo("Islami Bank", "ইসলামী ব্যাংক", "bank_28"),
    BankInfo("Jamuna Bank", "যমুনা ব্যাংক", "bank_29"),
    BankInfo("Janata Bank", "জনতা ব্যাংক", "bank_30"),
    BankInfo("Meghna Bank", "মেঘনা ব্যাংক", "bank_31"),
    BankInfo("Mercantile Bank", "মার্কেটাইল ব্যাংক", "bank_32"),
    BankInfo("Midland Bank", "মিডল্যান্ড ব্যাংক", "bank_33"),
    BankInfo("Modhumoti Bank", "মধুমতি ব্যাংক", "bank_34"),
    BankInfo("Mutual Trust Bank", "মিউচুয়াল ট্রাস্ট ব্যাংক", "bank_35"),
    BankInfo("National Bank", "ন্যাশনাল ব্যাংক", "bank_36"),
    BankInfo("National Bank of Pakistan", "ন্যাশনাল ব্যাংক অব পাকিস্তান", "bank_37"),
    BankInfo("NCC Bank", "এনসিসি ব্যাংক", "bank_38"),
    BankInfo("NRB Bank", "এনআরবি ব্যাংক", "bank_39"),
    BankInfo("NRBC Bank", "এনআরবিসি ব্যাংক", "bank_40"),
    BankInfo("One Bank", "ওয়ান ব্যাংক", "bank_41"),
    BankInfo("Padma Bank", "পদ্মা ব্যাংক", "bank_42"),
    BankInfo("Premier Bank", "প্রিমিয়ার ব্যাংক", "bank_43"),
    BankInfo("Prime Bank", "প্রাইম ব্যাংক", "bank_44"),
    BankInfo("Probashi Kallyan Bank", "প্রবাসী কল্যাণ ব্যাংক", "bank_45"),
    BankInfo("Pubali Bank", "পূবালী ব্যাংক", "bank_46"),
    BankInfo("Rajshahi Krishi Unnayan Bank", "রাজশাহী কৃষি উন্নয়ন ব্যাংক", "bank_47"),
    BankInfo("Rupali Bank", "রূপালী ব্যাংক", "bank_48"),
    BankInfo("SBAC Bank", "এসবিএসি ব্যাংক", "bank_49"),
    BankInfo("Shahjalal Islami Bank", "শাহজালাল ইসলামী ব্যাংক", "bank_50"),
    BankInfo("Shimanto Bank", "সীমান্ত ব্যাংক", "bank_51"),
    BankInfo("Social Islami Bank", "সোশ্যাল ইসলামী ব্যাংক", "bank_52"),
    BankInfo("Sonali Bank", "সোনালী ব্যাংক", "bank_53"),
    BankInfo("Southeast Bank", "সাউথইস্ট ব্যাংক", "bank_54"),
    BankInfo("Standard Chartered Bank", "স্ট্যান্ডার্ড চার্টার্ড ব্যাংক", "bank_55"),
    BankInfo("Standard Islami Bank", "স্ট্যান্ডার্ড ইসলামী ব্যাংক", "bank_56"),
    BankInfo("State Bank of India", "স্টেট ব্যাংক অব ইন্ডিয়া", "bank_57"),
    BankInfo("Trust Bank", "ট্রাস্ট ব্যাংক", "bank_58"),
    BankInfo("Union Bank", "ইউনিয়ন ব্যাংক", "bank_59"),
    BankInfo("United Commercial Bank", "ইউনাইটেড কমার্শিয়াল ব্যাংক", "bank_60"),
    BankInfo("Uttara Bank", "উত্তরা ব্যাংক", "bank_61"),
    BankInfo("Woori Bank", "উরি ব্যাংক", "bank_62"),
)

val MFS_LIST = listOf(
    BankInfo("bkash", "বিকাশ", "mfs_02"),
    BankInfo("Nagad", "নগদ", "mfs_03"),
    BankInfo("Rocket", "রকেট", "mfs_04"),
    BankInfo("Upay", "উপায়", "mfs_05"),
    BankInfo("SureCash", "শিউরক্যাশ", "mfs_06"),
    BankInfo("mCash", "এমক্যাশ", "mfs_07"),
    BankInfo("MYCash", "মাইক্যাশ", "mfs_08"),
    BankInfo("Tap", "ট্যাপ", "mfs_09"),
    BankInfo("Islamic Wallet", "ইসলামিক ওয়ালেট", "mfs_10"),
    BankInfo("OK Wallet", "ওকে ওয়ালেট", "mfs_11"),
    BankInfo("TeleCash", "টেলিক্যাশ", "mfs_12"),
    BankInfo("Tap ‘n Pay", "ট্যাপ এন পে", "mfs_13"),
)

/** মেলানোর জন্য নাম সহজ করা: ছোট হাতের অক্ষর, ফাঁকা ও চিহ্ন বাদ */
private fun norm(s: String): String =
    s.lowercase().filterNot { it.isWhitespace() || it in "-.'\u2018\u2019" }

fun hasBangla(s: String): Boolean = s.any { it in '\u0980'..'\u09FF' }

/** বাংলায় লিখলে বাংলা নাম, ইংরেজিতে লিখলে ইংরেজি নাম */
fun BankInfo.nameFor(typed: String): String = if (hasBangla(typed)) bn else en

/** নাম (বাংলা বা ইংরেজি, যেকোনোটা) তালিকার সাথে পুরো মিললে সেই ব্যাংক/মোবাইল */
fun findBank(name: String): BankInfo? {
    val n = norm(name)
    if (n.isEmpty()) return null
    return (MFS_LIST + BANK_LIST).firstOrNull { norm(it.en) == n || norm(it.bn) == n }
}

/** ধরন অনুযায়ী সাজেশন: "mobile" হলে মোবাইল ব্যাংকিং, "bank" হলে ব্যাংক; অন্য ধরনে নেই */
fun bankSuggestions(type: String, typed: String): List<BankInfo> {
    val list = when (type) {
        "mobile" -> MFS_LIST
        "bank" -> BANK_LIST
        else -> return emptyList()
    }
    val q = norm(typed)
    if (q.isEmpty()) return emptyList()
    return list
        .filter { norm(it.en).contains(q) || norm(it.bn).contains(q) }
        .filter { norm(it.nameFor(typed)) != q } // পুরো নাম লেখা হয়ে গেলে আর দেখানোর দরকার নেই
        .sortedByDescending { norm(it.en).startsWith(q) || norm(it.bn).startsWith(q) }
        .take(6)
}
