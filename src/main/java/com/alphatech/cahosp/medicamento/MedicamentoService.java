package com.alphatech.cahosp.medicamento;

import com.alphatech.cahosp.comum.excecao.ConflitoException;
import com.alphatech.cahosp.comum.excecao.RecursoNaoEncontradoException;
import com.alphatech.cahosp.medicamento.dominio.Criticidade;
import com.alphatech.cahosp.medicamento.dominio.FamiliaTerapeutica;
import com.alphatech.cahosp.medicamento.dominio.Medicamento;
import com.alphatech.cahosp.medicamento.dto.AtualizarMedicamentoRequest;
import com.alphatech.cahosp.medicamento.dto.CriarMedicamentoRequest;
import com.alphatech.cahosp.medicamento.dto.MedicamentoResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Regra de negocio do catalogo de medicamentos (RF-DAD-06). Leitura aberta a qualquer usuario
 * autenticado; escrita restrita a TI.
 *
 * <p>Sem exclusao fisica (LGPD/auditoria): desativacao em vez de delete preserva o historico
 * referencial das fases seguintes (estoque, previsao, alerta).
 */
@Service
public class MedicamentoService {

    private final MedicamentoRepository medicamentoRepository;

    public MedicamentoService(MedicamentoRepository medicamentoRepository) {
        this.medicamentoRepository = medicamentoRepository;
    }

    /** Lista medicamentos com filtros opcionais. Ordenacao por codigo ASC. RF-DAD-06. */
    @Transactional(readOnly = true)
    public List<MedicamentoResponse> listar(FamiliaTerapeutica familia,
                                            Criticidade criticidade,
                                            Boolean essencial,
                                            Boolean ativo,
                                            String busca) {
        String termo = (busca == null || busca.isBlank()) ? null : busca.trim();
        return medicamentoRepository.buscarComFiltros(familia, criticidade, essencial, ativo, termo,
                        Sort.by("codigo").ascending())
                .stream()
                .map(MedicamentoResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public MedicamentoResponse buscarPorId(UUID id) {
        return MedicamentoResponse.de(obter(id));
    }

    /** Cria medicamento; codigo unico (ignore-case). RF-DAD-06. */
    @Transactional
    public MedicamentoResponse criar(CriarMedicamentoRequest request) {
        if (medicamentoRepository.existsByCodigoIgnoreCase(request.codigo())) {
            throw new ConflitoException(
                    "Ja existe um medicamento com o codigo '" + request.codigo() + "'.");
        }
        Medicamento medicamento = new Medicamento(
                request.codigo(),
                request.nome(),
                request.apresentacao(),
                request.familia(),
                request.unidadeMedida(),
                request.criticidade(),
                request.essencial());
        return MedicamentoResponse.de(medicamentoRepository.save(medicamento));
    }

    /** Atualiza medicamento; novo codigo nao pode pertencer a OUTRO. RF-DAD-06. */
    @Transactional
    public MedicamentoResponse atualizar(UUID id, AtualizarMedicamentoRequest request) {
        Medicamento medicamento = obter(id);
        if (!medicamento.getCodigo().equalsIgnoreCase(request.codigo())) {
            medicamentoRepository.findByCodigoIgnoreCase(request.codigo())
                    .filter(outro -> !outro.getId().equals(id))
                    .ifPresent(outro -> {
                        throw new ConflitoException(
                                "Ja existe um medicamento com o codigo '" + request.codigo() + "'.");
                    });
        }
        medicamento.setCodigo(request.codigo());
        medicamento.setNome(request.nome());
        medicamento.setApresentacao(request.apresentacao());
        medicamento.setFamilia(request.familia());
        medicamento.setUnidadeMedida(request.unidadeMedida());
        medicamento.setCriticidade(request.criticidade());
        medicamento.setEssencial(request.essencial());
        return MedicamentoResponse.de(medicamento);
    }

    @Transactional
    public MedicamentoResponse alterarStatus(UUID id, boolean ativo) {
        Medicamento medicamento = obter(id);
        if (ativo) {
            medicamento.ativar();
        } else {
            medicamento.desativar();
        }
        return MedicamentoResponse.de(medicamento);
    }

    private Medicamento obter(UUID id) {
        return medicamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Medicamento nao encontrado: " + id + "."));
    }
}
