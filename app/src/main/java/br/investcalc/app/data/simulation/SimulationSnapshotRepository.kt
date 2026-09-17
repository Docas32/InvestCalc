package br.investcalc.app.data.simulation

/**
 * Persistência da última simulação concluída, para restauração automática no
 * próximo uso do app ("continuar de onde você parou").
 *
 * Interface pura para que os testes JVM usem um `InMemory`/fake; a
 * implementação de produção grava via `DataStore` (Preferences).
 */
interface SimulationSnapshotRepository {

    suspend fun salvar(snapshot: UltimateSimulationSnapshot)

    suspend fun carregar(): UltimateSimulationSnapshot?
}
