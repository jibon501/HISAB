# R8 / ProGuard নিয়ম
# অ্যাপে রিফ্লেকশন বা JSON লাইব্রেরি ব্যবহার হয় না, তাই আলাদা keep নিয়ম লাগে না।
# প্রয়োজনে নিচে নিয়ম যোগ করুন।

# ক্র্যাশ রিপোর্টে লাইন নম্বর দেখতে চাইলে এই দুটি লাইনের কমেন্ট তুলে দিন:
# -keepattributes SourceFile,LineNumberTable
# -renamesourcefileattribute SourceFile
