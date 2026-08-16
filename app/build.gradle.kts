import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // AGP 9+ traz suporte a Kotlin embutido: o plugin 'org.jetbrains.kotlin.android'
    // foi removido de propósito. Ver https://kotl.in/gradle/agp-built-in-kotlin
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.driverprofit"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.driverprofit"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()

        // versionName segue o Semantic Versioning do projeto (PRD §40).
        versionCode = 17
        versionName = "0.9.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        debug {
            // Permite instalar debug e release lado a lado no mesmo aparelho.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    // Expõe os schemas exportados como assets do androidTest para que o
    // MigrationTestHelper consiga abrir o banco na versão antiga (PRD §45).
    sourceSets.getByName("androidTest") {
        assets.directories.add("$projectDir/schemas")
    }

    lint {
        // Lint faz parte do gate de CI (PRD §36): warnings viram erro.
        warningsAsErrors = true
        abortOnError = true
        checkDependencies = true

        // "Existe uma versão mais nova desta biblioteca" não é defeito do
        // código: com warningsAsErrors, essa checagem faz um commit que
        // passava hoje falhar amanhã sozinho, só porque alguém publicou uma
        // release. Isso quebraria o critério do PRD §58 — conseguir fazer
        // checkout de uma tag antiga e reproduzir aquele estado.
        //
        // Atualização de dependência é decisão consciente, feita em um PR
        // próprio (PRD §55), não um erro de build.
        //
        // "Typos" compara cada palavra dos textos com um dicionário **inglês**.
        // Este app é inteiramente em português: a checagem acusa "eles" como
        // erro de digitação de "eels", e com warningsAsErrors isso transforma
        // qualquer frase nova da interface em falha de build. Nenhum acerto
        // possível, só falso positivo. `tools:locale="pt-BR"` em strings.xml
        // não desarma a checagem nesta versão do lint.
        disable += setOf("GradleDependency", "NewerVersionAvailable", "Typos")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

room {
    // Schemas exportados são versionados no Git — base para testar migrações (PRD §45).
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // --- AndroidX base ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // --- Compose (BOM controla as versões dos artefatos abaixo) ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // --- Persistência ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // --- Concorrência ---
    implementation(libs.kotlinx.coroutines.android)

    // --- Alinhamento de kotlinx-serialization ---
    //
    // O projeto não usa serialização diretamente: ela chega por navigation e
    // lifecycle, que trazem a versão 1.7.3, e por room-testing, que pede 1.8.1.
    //
    // A "consistent resolution" do AGP obriga o classpath de androidTest a
    // acompanhar o do app. Como room-testing só existe no androidTest, o `core`
    // ficava preso em 1.7.3 enquanto o `json` resolvia em 1.8.1 — e as classes
    // de schema do Room, compiladas contra 1.8.x, estouravam com
    // AbstractMethodError ao ler o schema exportado. Efeito prático: **nenhum
    // teste de migração conseguia sequer começar**.
    //
    // O BOM alinha as duas pontas numa versão só.
    implementation(platform(libs.kotlinx.serialization.bom))

    // --- Testes unitários (JVM, sem Android) ---
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // --- Testes instrumentados ---
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
