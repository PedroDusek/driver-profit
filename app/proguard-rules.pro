# Regras de R8/ProGuard para o build release.
#
# O projeto não usa reflexão fora do que Room/Compose já tratam com suas
# próprias regras consumidas automaticamente, então este arquivo começa vazio.
# Adicione regras aqui apenas quando um problema real de shrinking for
# identificado, e documente o motivo junto da regra.

# Mantém as linhas de código nos stack traces do release.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
