import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// A chave da API vem de local.properties (não versionado) ou da variável de ambiente.
val localProps = Properties().apply {
    val arquivo = rootProject.file("local.properties")
    if (arquivo.exists()) arquivo.inputStream().use { load(it) }
}
val openRouterKey: String = localProps.getProperty("OPENROUTER_API_KEY")
    ?: System.getenv("OPENROUTER_API_KEY")
    ?: ""

android {
    namespace = "br.com.example.clareai"
    compileSdk = 37

    defaultConfig {
        applicationId = "br.com.example.clareai"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "OPENROUTER_API_KEY", "\"$openRouterKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:ext-tables:4.6.2")

    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")

    testImplementation(libs.junit)
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
