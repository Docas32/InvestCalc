package br.investcalc.app.data.persistence

/**
 * Implementação em memória de [SimulationPersistence], usada nos testes JVM e
 * como fallback determinístico. A produção trocará para a implementação com
 * `DataStore` (mesmo contrato, sem alterar as camadas de domínio/UI).
 */
class InMemorySimulationPersistence : SimulationPersistence {

    private var ultimaSimulacao: UltimaSimulacao? = null

    override suspend fun carregarUltimaSimulacao(): UltimaSimulacao? = ultimaSimulacao

    override suspend fun salvarUltimaSimulacao(simulacao: UltimaSimulacao) {
        ultimaSimulacao = simulacao
    }
}
