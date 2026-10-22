plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.rabit.clonadora"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rabit.clonadora"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("clonadora") {
            storeFile = rootProject.file("clonadora.p12")
            storePassword = "clonadora123"
            keyAlias = "clonadora"
            keyPassword = "clonadora123"
            storeType = "pkcs12"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("clonadora")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
