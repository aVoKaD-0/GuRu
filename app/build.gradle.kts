plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.ruege.mobile"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ruege.mobile"
        minSdk = 27
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "RECAPTCHA_SITE_KEY", "\"6LeiIHUrAAAAAMhviqcr7rWgh-A_6JpDbjJjy5j1\"")
        }
        debug {
            buildConfigField("String", "RECAPTCHA_SITE_KEY", "\"6LeiIHUrAAAAAMhviqcr7rWgh-A_6JpDbjJjy5j1\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    
    ksp {
        arg("correctErrorTypes", "true")
        arg("ksp.incremental", "false")
        arg("ksp.incremental.log", "true")
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "false")
    }
}

val roomVersion = "2.6.1"
val hiltVersion = "2.51.1"
val moshiVersion = "1.15.1"
val kspVersion = "1.9.20-1.0.14"
val kotlinVersion = "1.9.20"
val workVersion = "2.9.0"
val markwonVersion = "4.6.2"

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation("com.squareup:javapoet:1.13.0")
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")

    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-analytics")
    
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    
    implementation("androidx.cardview:cardview:1.0.0")
    
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-process:2.8.2")
    
    implementation("androidx.work:work-runtime:$workVersion")
    implementation("androidx.work:work-runtime-ktx:$workVersion")

    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")
    
    implementation("com.github.bumptech.glide:glide:4.16.0")
    ksp("com.github.bumptech.glide:ksp:4.16.0")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.16.0")

    implementation("com.facebook.shimmer:shimmer:0.5.0")
    
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.android.gms:play-services-base:18.5.0")
    
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")

    implementation("com.squareup.moshi:moshi:$moshiVersion")
    implementation("com.squareup.moshi:moshi-kotlin:$moshiVersion")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:$moshiVersion")
    
    implementation("com.google.firebase:firebase-auth-ktx")
    
    implementation("androidx.credentials:credentials:1.2.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")

    implementation("com.jakewharton.timber:timber:5.0.1")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.webkit:webkit:1.11.0")
    

    implementation("com.github.mhiew:android-pdf-viewer:3.2.0-beta.3")

    implementation("com.getkeepsafe.taptargetview:taptargetview:1.13.3")

    implementation("io.noties.markwon:core:$markwonVersion")
    implementation("io.noties.markwon:ext-strikethrough:$markwonVersion")
    implementation("io.noties.markwon:ext-tables:$markwonVersion")
    implementation("io.noties.markwon:html:$markwonVersion")
    implementation("io.noties.markwon:linkify:$markwonVersion")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.google.android.recaptcha:recaptcha:18.8.0-beta01")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
}
