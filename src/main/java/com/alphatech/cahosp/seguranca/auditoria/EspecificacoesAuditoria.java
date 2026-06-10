package com.alphatech.cahosp.seguranca.auditoria;

import com.alphatech.cahosp.seguranca.auditoria.dominio.CategoriaAuditoria;
import com.alphatech.cahosp.seguranca.auditoria.dominio.LogAuditoria;
import com.alphatech.cahosp.usuario.dominio.Perfil;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Monta a {@link Specification} de filtro da trilha de auditoria a partir dos parametros opcionais
 * da listagem (categoria, perfil, assistido por IA, busca textual). Cada filtro ausente e ignorado;
 * a busca casa acao, usuario ou recurso (case-insensitive). RF-SEG-02.
 */
final class EspecificacoesAuditoria {

    private EspecificacoesAuditoria() {
    }

    static Specification<LogAuditoria> comFiltros(CategoriaAuditoria categoria, Perfil perfil,
                                                  Boolean assistidoPorIa, String busca) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (categoria != null) {
                predicados.add(cb.equal(root.get("categoria"), categoria));
            }
            if (perfil != null) {
                predicados.add(cb.equal(root.get("perfil"), perfil));
            }
            if (assistidoPorIa != null) {
                predicados.add(cb.equal(root.get("assistidoPorIa"), assistidoPorIa));
            }
            if (busca != null && !busca.isBlank()) {
                String termo = "%" + busca.trim().toLowerCase() + "%";
                predicados.add(cb.or(
                        cb.like(cb.lower(root.get("acao")), termo),
                        cb.like(cb.lower(root.get("usuarioNome")), termo),
                        cb.like(cb.lower(root.get("recurso")), termo)));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }
}
