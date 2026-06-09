package com.alphatech.cahosp.unidade;

import com.alphatech.cahosp.comum.excecao.ConflitoException;
import com.alphatech.cahosp.comum.excecao.RecursoNaoEncontradoException;
import com.alphatech.cahosp.unidade.dominio.Conectividade;
import com.alphatech.cahosp.unidade.dominio.Porte;
import com.alphatech.cahosp.unidade.dominio.Unidade;
import com.alphatech.cahosp.unidade.dto.AtualizarUnidadeRequest;
import com.alphatech.cahosp.unidade.dto.CriarUnidadeRequest;
import com.alphatech.cahosp.unidade.dto.UnidadeResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Regra de negocio do catalogo de unidades (RF-DAD-06). Leitura aberta a qualquer usuario
 * autenticado; escrita restrita ao perfil TI (RBAC aplicado no controller).
 *
 * <p>Nao ha exclusao fisica (LGPD/auditoria): o desligamento de uma unidade e feito por
 * desativacao ({@code ativo=false}), preservando integridade referencial das fases futuras
 * (estoque, previsao, alerta, recomendacao) que vao apontar para a unidade por FK.
 */
@Service
public class UnidadeService {

    private final UnidadeRepository unidadeRepository;

    public UnidadeService(UnidadeRepository unidadeRepository) {
        this.unidadeRepository = unidadeRepository;
    }

    /** Lista unidades aplicando filtros opcionais. Ordenacao por sigla ASC. RF-DAD-06. */
    @Transactional(readOnly = true)
    public List<UnidadeResponse> listar(Porte porte,
                                       Conectividade conectividade,
                                       Boolean hub,
                                       Boolean ativo,
                                       String busca) {
        String termo = (busca == null || busca.isBlank()) ? null : busca.trim();
        return unidadeRepository.buscarComFiltros(porte, conectividade, hub, ativo, termo,
                        Sort.by("sigla").ascending())
                .stream()
                .map(UnidadeResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public UnidadeResponse buscarPorId(UUID id) {
        return UnidadeResponse.de(obter(id));
    }

    /** Cria unidade. Sigla unica (ignore-case) sob pena de conflito. RF-DAD-06. */
    @Transactional
    public UnidadeResponse criar(CriarUnidadeRequest request) {
        if (unidadeRepository.existsBySiglaIgnoreCase(request.sigla())) {
            throw new ConflitoException(
                    "Ja existe uma unidade com a sigla '" + request.sigla() + "'.");
        }
        Unidade unidade = new Unidade(
                request.nome(),
                request.sigla(),
                request.municipio(),
                request.porte(),
                request.leitos(),
                request.conectividade(),
                request.perfilDemografico(),
                request.hub());
        return UnidadeResponse.de(unidadeRepository.save(unidade));
    }

    /** Atualiza unidade; nova sigla nao pode pertencer a OUTRA unidade. RF-DAD-06. */
    @Transactional
    public UnidadeResponse atualizar(UUID id, AtualizarUnidadeRequest request) {
        Unidade unidade = obter(id);
        if (!unidade.getSigla().equalsIgnoreCase(request.sigla())) {
            unidadeRepository.findBySiglaIgnoreCase(request.sigla())
                    .filter(outra -> !outra.getId().equals(id))
                    .ifPresent(outra -> {
                        throw new ConflitoException(
                                "Ja existe uma unidade com a sigla '" + request.sigla() + "'.");
                    });
        }
        unidade.setNome(request.nome());
        unidade.setSigla(request.sigla());
        unidade.setMunicipio(request.municipio());
        unidade.setPorte(request.porte());
        unidade.setLeitos(request.leitos());
        unidade.setConectividade(request.conectividade());
        unidade.setPerfilDemografico(request.perfilDemografico());
        unidade.setHub(request.hub());
        return UnidadeResponse.de(unidade);
    }

    @Transactional
    public UnidadeResponse alterarStatus(UUID id, boolean ativo) {
        Unidade unidade = obter(id);
        if (ativo) {
            unidade.ativar();
        } else {
            unidade.desativar();
        }
        return UnidadeResponse.de(unidade);
    }

    private Unidade obter(UUID id) {
        return unidadeRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Unidade nao encontrada: " + id + "."));
    }
}
