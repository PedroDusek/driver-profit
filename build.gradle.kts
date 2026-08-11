// Build script raiz. Os plugins são declarados aqui com `apply false` e
// aplicados nos módulos. Nenhuma lógica de build deve viver neste arquivo.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}
