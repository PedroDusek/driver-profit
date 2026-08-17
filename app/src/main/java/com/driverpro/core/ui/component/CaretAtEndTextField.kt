package com.driverpro.core.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import com.driverpro.core.ui.theme.DriverProTheme

/**
 * Campo numérico com o **cursor sempre no fim**, como em aplicativo de banco.
 *
 * ### O defeito que isso corrige
 *
 * Um campo que exibe o texto já formatado muda de comprimento a cada tecla:
 * `R$ 3,00` vira `R$ 32,00`. O `OutlinedTextField` que recebe uma `String`
 * simples não controla a seleção, e o Compose preserva o **deslocamento
 * numérico** do cursor — que, no texto reformatado, cai num lugar visual
 * diferente. O cursor "anda sozinho" enquanto se digita.
 *
 * ### Por que ancorar no fim, em vez de calcular a posição certa
 *
 * Porque não existe posição certa. Ao teclar `5` em `R$ 3,00`, os dígitos
 * inteiros deslizam uma casa e a vírgula fica onde estava: nenhuma regra de
 * mapeamento agrada em todos os casos, e cada uma erra num canto diferente.
 *
 * Ancorar no fim elimina o problema em vez de administrá-lo. Vírgula e
 * separador de milhar passam a ser puramente visuais, e o número só cresce ou
 * encolhe pela direita — exatamente o comportamento de aplicativo de banco,
 * que o motorista já conhece. Errou um dígito do meio? Apaga até ele. É mais
 * previsível do que um cursor que se move sozinho.
 *
 * ### Onde **não** usar
 *
 * Campos de texto livre — nome do veículo, observação, posto, oficina. Neles a
 * edição no meio é legítima e esperada, e ancorar o cursor seria hostil.
 */
@Composable
fun CaretAtEndTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Number,
    supportingText: String? = null,
) {
    // Derivado a cada composição, e de propósito sem `remember`: o cursor deve
    // estar sempre no fim, então não existe estado de seleção a preservar.
    // Guardá-lo só criaria a chance de ele ficar dessincronizado do texto.
    val fieldValue = TextFieldValue(text = value, selection = TextRange(value.length))

    OutlinedTextField(
        value = fieldValue,
        onValueChange = { onValueChange(it.text) },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        supportingText = supportingText?.let { { Text(it) } },
    )
}

@Preview(showBackground = true)
@Composable
private fun CaretAtEndTextFieldPreview() {
    DriverProTheme(dynamicColor = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CaretAtEndTextField(
                value = "R$ 320,50",
                onValueChange = {},
                label = "Valor recebido",
            )
            CaretAtEndTextField(
                value = "",
                onValueChange = {},
                label = "Km rodados",
                isError = true,
                supportingText = "Campo obrigatório",
            )
        }
    }
}
