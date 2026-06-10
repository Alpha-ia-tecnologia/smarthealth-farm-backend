package com.alphatech.cahosp.integracao;

import com.alphatech.cahosp.integracao.dominio.IntegracaoApi;
import com.alphatech.cahosp.integracao.dominio.StatusIntegracao;
import com.alphatech.cahosp.integracao.dto.IntegracaoApiResponse;
import com.alphatech.cahosp.integracao.dto.ProvedorIaResponse;
import com.alphatech.cahosp.integracao.dto.ResumoIntegracaoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regra de negocio da integracao EMSERH (RF-INT): consulta das conexoes/APIs, dos provedores de
 * IA do AI Gateway e KPIs do painel. Modulo de governanca, somente leitura.
 */
@Service
@Transactional(readOnly = true)
public class IntegracaoService {

    private final IntegracaoApiRepository integracaoRepository;
    private final ProvedorIaRepository provedorRepository;
    private final CalculadoraIntegracao calculadora;

    public IntegracaoService(IntegracaoApiRepository integracaoRepository,
                             ProvedorIaRepository provedorRepository,
                             CalculadoraIntegracao calculadora) {
        this.integracaoRepository = integracaoRepository;
        this.provedorRepository = provedorRepository;
        this.calculadora = calculadora;
    }

    /** Lista as integracoes/conexoes ordenadas (RF-INT-01/04). */
    public List<IntegracaoApiResponse> listarIntegracoes() {
        return integracaoRepository.findAllByOrderByOrdemAsc().stream()
                .map(IntegracaoApiResponse::de)
                .toList();
    }

    /** Lista os provedores de IA do AI Gateway (RF-INT-06). */
    public List<ProvedorIaResponse> listarProvedoresIa() {
        return provedorRepository.findAllByOrderByOrdemAsc().stream()
                .map(ProvedorIaResponse::de)
                .toList();
    }

    /** KPIs do painel de integracao (RF-INT-01/02/05/06). */
    public ResumoIntegracaoResponse resumo() {
        List<IntegracaoApi> integracoes = integracaoRepository.findAllByOrderByOrdemAsc();
        int latenciaMedia = calculadora.latenciaMediaMs(
                integracoes.stream().map(IntegracaoApi::getLatenciaMs).toList());
        return new ResumoIntegracaoResponse(
                integracaoRepository.countByStatus(StatusIntegracao.OPERACIONAL),
                integracoes.size(),
                latenciaMedia,
                integracaoRepository.somarRegistrosBuffer(),
                provedorRepository.count());
    }
}
