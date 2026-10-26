plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// מספר הגרסה מגיע מ-GitHub Actions (מספר הריצה). כל push = גרסה חדשה וגבוהה יותר.
// למה? כך האפליקציה יודעת להשוות: אם ב-GitHub יש מספר גבוה משלה, יש עדכון.
val ciVersionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1

// מפתח החתימה מגיע מ-Secrets של GitHub. בלי אותו מפתח בדיוק, אנדרואיד מסרב
// להתקין גרסה חדשה מעל הישנה. לכן המפתח קבוע ושמור, ולא נוצר מחדש בכל בנייה.
val keystorePath: String? = System.getenv("KEYSTORE_PATH")

android {
    namespace = "com.edan.kesef"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.edan.kesef"
        minSdk = 26
        targetSdk = 35
        versionCode = ciVersionCode
        versionName = "1.$ciVersionCode"
        // מאיזה repo האפליקציה בודקת עדכונים.
        buildConfigField("String", "GITHUB_REPO", "\"pooopic/kesef\"")
    }

    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = "kesef"
                keyPassword = System.getenv("KEYSTORE_PASSWORD")
                storeType = "pkcs12"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (keystorePath != null) {
                signingConfigs.getByName("release")
            } else {
                // בנייה מקומית בלי המפתח: חתימת debug, רק לבדיקות.
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        // שגיאות lint לא יפילו את הבנייה בענן. אפשר להריץ lint ידנית כשרוצים.
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Room: מסד נתונים מקומי (SQLite) בטלפון. הנתונים לא יוצאים מהמכשיר.
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // WorkManager: משימות רקע מתוזמנות (תזכורת יומית, בדיקת עדכונים).
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    testImplementation("junit:junit:4.13.2")
}
