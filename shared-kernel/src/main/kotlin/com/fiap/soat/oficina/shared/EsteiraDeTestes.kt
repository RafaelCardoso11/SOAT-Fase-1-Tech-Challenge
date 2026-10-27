package com.fiap.soat.oficina.shared

/**
 * Classe de exemplo APENAS para validar a esteira de testes e o gate de cobertura (Jacoco ≥ 80%).
 * Deve ser removida quando os Value Objects reais do shared-kernel (Cpf, Cnpj, Placa, Dinheiro)
 * forem implementados.
 */
object EsteiraDeTestes {
    fun ativo(): Boolean = true

    fun soma(a: Int, b: Int): Int = a + b
}
