# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/miha/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

### greenDAO 3
-keepclassmembers class * extends org.greenrobot.greendao.AbstractDao {
  public static java.lang.String TABLENAME;
}
-keep class **$Properties
# If you do not use SQLCipher:
-dontwarn org.greenrobot.greendao.database.**
# If you do not use RxJava:
-dontwarn rx.**


-keep public class * extends android.view.View {
 public <init>(android.content.Context);
 public <init>(android.content.Context, android.util.AttributeSet);
 public <init>(android.content.Context, android.util.AttributeSet, int);
 public <init>(android.content.Context, android.util.AttributeSet, int, int);
 public void set*(...);
}
-keepclasseswithmembers class * {
 public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
 public <init>(android.content.Context, android.util.AttributeSet, int);
}


-keep class * implements android.os.Parcelable {
 public static final android.os.Parcelable$Creator *;
}