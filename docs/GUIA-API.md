# Guia da API — Smart Health CAHOSP (resumo por domínio)

Visão rápida do que cada domínio faz, o que cada endpoint resolve e em qual tela do front é usado.
Para dúvidas, é só perguntar. Detalhes técnicos vivem no [`CLAUDE.md`](../CLAUDE.md) e no
[`README.md`](../README.md); a referência interativa completa está no **Swagger**.

## Básico que vale para tudo

- **Base URL:** `http://localhost:3002/api` · **Swagger:** `/api/swagger-ui/index.html`
- **Envelope único** em toda resposta:
  - sucesso → `{ "success": true, "data": ..., "total": 12 }` (`total` só em listas)
  - erro → `{ "success": false, "error": "mensagem em pt-BR", "codigo": "VALIDACAO" }`
- **Autenticação:** JWT no header `Authorization: Bearer <token>`.
- **Perfis (RBAC):** `Operador` (operação/leitura), `Gestor` (decisões), `TI` (administração).
- **Regra geral de acesso:** ler = qualquer usuário logado; **escrever catálogo = TI**;
  **ações de decisão (aprovar/recalibrar/gerar) = Gestor**; **auditoria = Gestor/TI**.
- Filtros por enum aceitam o rótulo em pt-BR (ex.: `?familia=Antibióticos`, `?status=Aberto`).

---

## 1. Autenticação — `/auth`
**Para que serve:** entrar no sistema e identificar o usuário. Base de toda a segurança.
**Tela:** login (transversal) + alimenta a Administração.

| Método | Rota | O que faz |
|---|---|---|
| POST | `/auth/login` | Recebe `{ email, password }` e devolve `{ usuario, token }`. |
| GET | `/auth/me` | Retorna o usuário do token (revalida a sessão ao abrir o app). |
| POST | `/auth/logout` | Stateless — o front só descarta o token. |

## 2. Administração de usuários — `/admin/usuarios` *(perfil TI)*
**Entidade:** `Usuario` (nome, e-mail, `perfil`, `unidade`, `ativo`). Senha em BCrypt.
**Para que serve:** o TI gerencia quem acessa o sistema. Sem DELETE — desliga por desativação (LGPD).
**Tela:** Administração (`/admin`).

| Método | Rota | O que faz |
|---|---|---|
| GET | `/admin/usuarios` | Lista; filtros `?perfil=&ativo=&busca=` (busca por nome/e-mail). |
| GET | `/admin/usuarios/{id}` | Detalha um usuário. |
| POST | `/admin/usuarios` | Cria usuário (e-mail único, senha ≥ 8). |
| PUT | `/admin/usuarios/{id}` | Atualiza nome, e-mail e perfil. |
| PATCH | `/admin/usuarios/{id}/status` | Ativa/desativa (o TI não pode se autodesativar). |
| PUT | `/admin/usuarios/{id}/senha` | Redefine a senha. |

## 3. Unidades — `/unidades`
**Entidade:** `Unidade` (CAHOSP central + hospitais atendidos; porte, conectividade, hub, ativo).
**Para que serve:** catálogo da rede EMSERH; referenciado por FK em estoque, previsão, alertas etc.
**Tela:** usado em filtros/seletores de toda a aplicação; cadastro na Administração.

| Método | Rota | O que faz |
|---|---|---|
| GET | `/unidades` | Lista; filtros `?porte=&conectividade=&hub=&ativo=&busca=`. |
| GET | `/unidades/{id}` | Detalha uma unidade. |
| POST · PUT · PATCH `/status` | `/unidades` · `/{id}` · `/{id}/status` | Cria, atualiza e ativa/desativa **(TI)**. |

## 4. Medicamentos — `/medicamentos`
**Entidade:** `Medicamento` (código `MED-NNN`, família terapêutica, criticidade, essencial, ativo).
**Para que serve:** catálogo de itens; base de estoque/previsão/alerta/recomendação.
**Tela:** seletores em toda a app; cadastro na Administração.

| Método | Rota | O que faz |
|---|---|---|
| GET | `/medicamentos` | Lista; filtros `?familia=&criticidade=&essencial=&ativo=&busca=`. |
| GET | `/medicamentos/{id}` | Detalha um medicamento. |
| POST · PUT · PATCH `/status` | `/medicamentos` · `/{id}` · `/{id}/status` | Cria, atualiza e ativa/desativa **(TI)**. |

## 5. Estoque, Lotes e Movimentações — `/estoque`, `/lotes`, `/movimentacoes`
**Entidades:** `PosicaoEstoque` (saldo + parâmetros), `Lote` (validade/fabricante),
`Movimentacao` (livro-razão **imutável**: Entrada/Saída/Transferência/Ajuste).
**Para que serve:** saber o que tem em cada unidade, por lote/validade, com status ok/atenção/crítico.
**Tela:** Estoque & Lotes (`/estoque`).

| Método | Rota | O que faz |
|---|---|---|
| GET | `/estoque` | Posições com status; filtros `?unidade=&medicamento=&status=&busca=`. |
| GET | `/estoque/resumo` | KPIs: itens críticos, lotes a vencer, lead médio, total. |
| GET | `/estoque/{medicamentoId}/{unidadeId}` | Drill-down: lotes e movimentações recentes do item. |
| GET · POST | `/lotes` | Lista lotes (filtros) e cria lote (gera a Entrada inicial). |
| GET · POST | `/movimentacoes` | Lista o livro-razão (filtros) e registra um lançamento (ajusta saldo). |

## 6. Previsão de Demanda — `/previsoes`
**Entidades:** `Previsao` (MAPE, modelo/versão, `drift`) + `PontoSerie` (realizado/previsto/bandas).
**Para que serve:** prever consumo e mostrar a assertividade do modelo (meta MAPE < 15%).
**Tela:** Previsão de Demanda (`/previsao`).

| Método | Rota | O que faz |
|---|---|---|
| GET | `/previsoes` | Lista; filtros `?unidade=&medicamento=&drift=&busca=`. |
| GET | `/previsoes/resumo` | KPIs: MAPE médio, críticos na meta, com drift. |
| GET | `/previsoes/{medicamentoId}/{unidadeId}` | Série temporal completa do item. |
| POST | `/previsoes/recalibrar` | Recalibra os modelos e estabiliza drift **(Gestor)**. |

## 7. Alertas — `/alertas`
**Entidade:** `Alerta` (Desabastecimento/Vencimento, severidade, status, destinatários por perfil).
**Para que serve:** avisar sobre risco de ruptura e lotes a vencer; gerado por regra a partir do estoque/previsão.
**Tela:** Alertas (`/alertas`).

| Método | Rota | O que faz |
|---|---|---|
| GET | `/alertas` | Lista; filtros `?tipo=&severidade=&status=&unidade=&medicamento=&busca=`. |
| GET | `/alertas/resumo` | KPIs: abertos, desabastecimento, vencimento, tratados. |
| PATCH | `/alertas/{id}/status` | Trata: Aberto → Em tratamento → Resolvido (terminal). |
| POST | `/alertas/gerar` | Regenera pelo motor de regras **(Gestor)**. |

## 8. Reposição & Redistribuição — `/recomendacoes`
**Entidade:** `Recomendacao` (Reposição/Redistribuição, motor Regras/IA, prioridade, `economiaEstimada` R$, status).
**Para que serve:** sugerir mover estoque entre unidades ou repor, com economia estimada; ciclo Pendente → Aprovada → Executada.
**Tela:** Reposição & Redistribuição (`/recomendacoes`).

| Método | Rota | O que faz |
|---|---|---|
| GET | `/recomendacoes` | Lista; filtros `?tipo=&status=&motor=&prioridade=&unidade=&medicamento=&busca=`. |
| GET | `/recomendacoes/resumo` | KPIs: pendentes, economia potencial, geradas por IA, taxa de adesão. |
| POST | `/recomendacoes/{id}/aprovar` | Aprova uma pendente **(Gestor)**. |
| POST | `/recomendacoes/{id}/executar` | Marca uma aprovada como executada **(Gestor)**. |
| POST | `/recomendacoes/gerar` | Regenera pelo motor **(Gestor)**. |

## 9. Indicadores — `/indicadores`
**Entidades:** `IndicadorMeta` (baseline/atual/meta) + `PontoHistorico` (série mensal).
**Para que serve:** acompanhar metas do projeto vs. linha de base (RF-IND-06). Somente leitura.
**Tela:** Indicadores (`/indicadores`); alimenta também Relatórios (`/relatorios`).

| Método | Rota | O que faz |
|---|---|---|
| GET | `/indicadores` | Lista com histórico, progresso, meta atingida e variação. |
| GET | `/indicadores/resumo` | KPIs: total, metas atingidas, em progresso. |
| GET | `/indicadores/{codigo}` | Detalha um indicador (ex.: `ind-ruptura`). |

## 10. Painel / Dashboard — `/painel`
**Para que serve:** agregações prontas para os dashboards (não tem entidade própria; consolida os outros domínios). Somente leitura.
**Tela:** Dashboard Gerencial (`/`) e Painel Operacional (`/operacional`).

| Método | Rota | O que faz |
|---|---|---|
| GET | `/painel` | Gerencial: totais da rede, cobertura, série agregada, alertas e recomendações. |
| GET | `/painel/operacional` | Operacional: situação por unidade, fila de alertas e recomendações em aberto. |

## 11. Ingestão de Dados — `/ingestao`
**Entidades:** `FonteDado` (status/qualidade/procedência) e `QualidadeFamilia` (maturidade por família).
**Para que serve:** mostrar de onde vêm os dados e a qualidade da base. Somente leitura.
**Tela:** Ingestão de Dados (`/ingestao`).

| Método | Rota | O que faz |
|---|---|---|
| GET | `/ingestao/fontes` | Fontes com status, volume, qualidade e procedência. |
| GET | `/ingestao/qualidade` | Maturidade/qualidade por família terapêutica. |
| GET | `/ingestao/resumo` | KPIs: registros, fontes sincronizadas, qualidade média, LGPD. |

## 12. Integração EMSERH — `/integracoes`
**Entidades:** `IntegracaoApi` (status, latência, modo Online/Offline-buffer/Reconciliando) e `ProvedorIa`.
**Para que serve:** painel de saúde das conexões externas (FarmaWeb, SIH…) e dos provedores de IA. Somente leitura. *(integração real é stub por padrão.)*
**Tela:** Integração EMSERH (`/integracao`).

| Método | Rota | O que faz |
|---|---|---|
| GET | `/integracoes` | Conexões com status, latência, modo e buffer offline. |
| GET | `/integracoes/resumo` | KPIs: operacionais, latência média, buffer, provedores. |
| GET | `/integracoes/provedores-ia` | Provedores do AI Gateway (papel, custo, anonimização). |

## 13. IA (AI Gateway) — `/ia`
**Para que serve:** chat com IA externa, **anonimizando os dados antes de enviar** (RF-SEG-04); sem chave de API responde em "modo demo".
**Tela:** transversal (assistente de IA).

| Método | Rota | O que faz |
|---|---|---|
| POST | `/ia/chat` | Recebe `{ mensagens }`, devolve `{ content, model, mode, provider }`. |

## 14. Segurança & Auditoria — `/seguranca/auditoria` *(perfil Gestor/TI)*
**Entidade:** `LogAuditoria` (registro imutável: ação, recurso, base legal LGPD, assistido por IA, IP, usuário/perfil, data).
**Para que serve:** trilha de auditoria para conformidade LGPD. O registro das ações sensíveis (aprovar/executar recomendação, recalibrar previsão, gerar alertas, inferência por IA) é **automático**. Somente leitura.
**Tela:** Segurança & LGPD (`/seguranca`).

| Método | Rota | O que faz |
|---|---|---|
| GET | `/seguranca/auditoria` | Trilha (mais recentes primeiro); filtros `?categoria=&perfil=&assistidoPorIA=&busca=`. |
| GET | `/seguranca/auditoria/resumo` | KPIs: total, assistidos por IA, com base legal, última atividade. |

---

### Telas do front × domínio (referência rápida)

| Tela (rota front) | Domínio(s) da API |
|---|---|
| Dashboard Gerencial `/` | `/painel` |
| Painel Operacional `/operacional` | `/painel/operacional` |
| Previsão de Demanda `/previsao` | `/previsoes` |
| Estoque & Lotes `/estoque` | `/estoque`, `/lotes`, `/movimentacoes` |
| Alertas `/alertas` | `/alertas` |
| Reposição & Redistribuição `/recomendacoes` | `/recomendacoes` |
| Relatórios `/relatorios` | `/indicadores`, `/painel` *(sem endpoint próprio)* |
| Ingestão de Dados `/ingestao` | `/ingestao` |
| Integração EMSERH `/integracao` | `/integracoes` |
| Segurança & LGPD `/seguranca` | `/seguranca/auditoria` |
| Administração `/admin` | `/admin/usuarios`, `/auth` |
| Indicadores `/indicadores` | `/indicadores` |
