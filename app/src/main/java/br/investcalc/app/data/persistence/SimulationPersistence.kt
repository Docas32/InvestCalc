package br.investcalc.app.data.persistence

import br.investcalc.app.domain.model.MonetaryValue

/**
 * Contrato de persistência da última simulação (DataStore em produção; a
 * interface garante que a camada de UI/domínio permaneça independente do
 * mecanismo concreto e que os testes JVM injetem um fake em memória).
 */
interface SimulationPersistence {

    /** @return última simulação salva, ou `null` quando ainda não existe. */
    suspend fun carregarUltimaSimulacao(): UltimaSimulacao?

    /** Substitui a última simulação persistida. */
    suspend fun salvarUltimaSimulacao(simulacao: UltimaSimulacao)
}

/**
 * Snapshot imutável da última simulação do usuário, usada para restauração
 * automática na próxima sessão (valores monetários em centavos).
 */
data class UltimaSimulacao(
    val valorInicial: MonetaryValue,
    val aporteMensal: MonetaryValue,
    val percentualCdi: Double,
    val taxaCdiAnual: Double,
    val meses: Int,
)
