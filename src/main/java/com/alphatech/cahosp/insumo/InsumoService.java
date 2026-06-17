package com.alphatech.cahosp.insumo;

import com.alphatech.cahosp.comum.excecao.ConflitoException;
import com.alphatech.cahosp.comum.excecao.RecursoNaoEncontradoException;
import com.alphatech.cahosp.insumo.dominio.Criticidade;
import com.alphatech.cahosp.insumo.dominio.CategoriaInsumo;
import com.alphatech.cahosp.insumo.dominio.Insumo;
import com.alphatech.cahosp.insumo.dto.AtualizarInsumoRequest;
import com.alphatech.cahosp.insumo.dto.CriarInsumoRequest;
import com.alphatech.cahosp.insumo.dto.InsumoResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Regra de negocio do catalogo de insumos (RF-DAD-06). Leitura aberta a qualquer usuario
 * autenticado; escrita restrita a TI.
 *
 * <p>Sem exclusao fisica (LGPD/auditoria): desativacao em vez de delete preserva o historico
 * referencial das fases seguintes (estoque, previsao, alerta).
 */
@Service
public class InsumoService {

    private final InsumoRepository insumoRepository;

    public InsumoService(InsumoRepository insumoRepository) {
        this.insumoRepository = insumoRepository;
    }

    /**
     * Lista insumos com filtros opcionais. Ordenacao por codigo ASC. RF-DAD-06.
     *
     * <p>Quando {@code unidadeId} e informado, restringe aos insumos com posicao de
     * estoque naquela unidade (filtro dependente de insumo no front).
     */
    @Transactional(readOnly = true)
    public List<InsumoResponse> listar(CategoriaInsumo categoria,
                                            Criticidade criticidade,
                                            Boolean essencial,
                                            Boolean ativo,
                                            String busca,
                                            UUID unidadeId) {
        String termo = (busca == null || busca.isBlank()) ? null : busca.trim();
        Sort ordem = Sort.by("codigo").ascending();
        List<Insumo> encontrados = unidadeId == null
                ? insumoRepository.buscarComFiltros(categoria, criticidade, essencial, ativo, termo, ordem)
                : insumoRepository.buscarPorUnidadeComFiltros(unidadeId, categoria, criticidade, essencial, ativo, termo, ordem);
        return encontrados.stream()
                .map(InsumoResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public InsumoResponse buscarPorId(UUID id) {
        return InsumoResponse.de(obter(id));
    }

    /** Cria insumo; codigo unico (ignore-case). RF-DAD-06. */
    @Transactional
    public InsumoResponse criar(CriarInsumoRequest request) {
        if (insumoRepository.existsByCodigoIgnoreCase(request.codigo())) {
            throw new ConflitoException(
                    "Ja existe um insumo com o codigo '" + request.codigo() + "'.");
        }
        Insumo insumo = new Insumo(
                request.codigo(),
                request.nome(),
                request.apresentacao(),
                request.categoria(),
                request.unidadeMedida(),
                request.criticidade(),
                request.essencial());
        return InsumoResponse.de(insumoRepository.save(insumo));
    }

    /** Atualiza insumo; novo codigo nao pode pertencer a OUTRO. RF-DAD-06. */
    @Transactional
    public InsumoResponse atualizar(UUID id, AtualizarInsumoRequest request) {
        Insumo insumo = obter(id);
        if (!insumo.getCodigo().equalsIgnoreCase(request.codigo())) {
            insumoRepository.findByCodigoIgnoreCase(request.codigo())
                    .filter(outro -> !outro.getId().equals(id))
                    .ifPresent(outro -> {
                        throw new ConflitoException(
                                "Ja existe um insumo com o codigo '" + request.codigo() + "'.");
                    });
        }
        insumo.setCodigo(request.codigo());
        insumo.setNome(request.nome());
        insumo.setApresentacao(request.apresentacao());
        insumo.setCategoria(request.categoria());
        insumo.setUnidadeMedida(request.unidadeMedida());
        insumo.setCriticidade(request.criticidade());
        insumo.setEssencial(request.essencial());
        return InsumoResponse.de(insumo);
    }

    @Transactional
    public InsumoResponse alterarStatus(UUID id, boolean ativo) {
        Insumo insumo = obter(id);
        if (ativo) {
            insumo.ativar();
        } else {
            insumo.desativar();
        }
        return InsumoResponse.de(insumo);
    }

    private Insumo obter(UUID id) {
        return insumoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Insumo nao encontrado: " + id + "."));
    }
}
