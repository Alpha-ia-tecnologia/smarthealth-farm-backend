# Smart Health CAHOSP — Backend (Java / Spring Boot)

API da plataforma **Smart Health CAHOSP** — **gestão preditiva da cadeia farmacêutica** da
Central de Abastecimento Hospitalar (**CAHOSP / EMSERH-MA**). Atende o frontend
[`../smarthealth-farm`](../smarthealth-farm) e cobre os **62 requisitos funcionais** (`RF-*`)
do Edital FAPEMA GovIA — Desafio Tecnológico 2: previsão de demanda de medicamentos, controle
de estoque por lote, alertas de desabastecimento/vencimento, recomendações de
reposição/redistribuição entre unidades, indicadores de projeto, ingestão e qualidade de dados,
integração com sistemas EMSERH e segurança/LGPD.

> **API profissional, construída do zero**, com foco em **segurança real**, **SOLID**, tipos
> corretos, schema versionado e **testes obrigatórios**. O frontend é a única fonte da verdade
> dos requisitos (não há backend legado). Veja [`CLAUDE.md`](CLAUDE.md) para o guia de
> desenvolvimento.

> **Status:** 🚧 em construção — projeto sendo implementado módulo a módulo, guiado pelo front.
>
> **Progresso:**
> - ✅ **Fase 0 — Fundação:** esqueleto Maven + Spring Boot 3.4 / Java 21, Maven Wrapper
>   (`./mvnw`, Maven não precisa estar instalado), `application.yml` lendo de `.env`, Flyway,
>   `ddl-auto: validate`, Swagger, Actuator, envelope `ApiResponse<T>`, base de testes de
>   integração com Testcontainers.
> - ✅ **Fase 1 — Núcleo + Segurança + Autenticação:** `GlobalExceptionHandler` (envelope de
>   erro padronizado), entidade `Usuario` + enum `Perfil` (RBAC Operador/Gestor/TI), migration
>   `V1__usuario.sql`, segurança stateless com **JWT** (login por e-mail, BCrypt), filtro JWT,
>   CORS, Swagger com Bearer, e o **admin inicial idempotente** (`AdminSeeder`, lê do `.env`).
>   Endpoints: `POST /auth/login`, `GET /auth/me`, `POST /auth/logout`. 16 testes verdes.
> - ✅ **Fase 2 — Administração de usuários** (RF-ADM-01): CRUD de usuários sob `/admin/usuarios`,
>   **exclusivo do perfil TI** (`@PreAuthorize("hasRole('TI')")`). Lista com filtros (perfil,
>   ativo, busca por nome/e-mail), criação com e-mail único + senha BCrypt, atualização,
>   redefinição de senha e ativação/desativação (sem DELETE — desligamento por desativação, LGPD).
>   Regra: o TI autenticado não pode desativar a própria conta (422). `Perfil` serializa/aceita o
>   rótulo pt-BR no JSON. Reaproveita a tabela `usuario` (sem nova migration).
> - ✅ **Fase 3 — Catálogo** (RF-DAD-06): `Unidade` (porte, conectividade, hub, ativo) e
>   `Medicamento` (código de negócio `MED-NNN`, família, criticidade, essencial, ativo) com
>   migration `V2__catalogo.sql` e **seeders idempotentes** espelhando o front (8 unidades,
>   30 medicamentos). Endpoints `/unidades` e `/medicamentos`: leitura para qualquer autenticado,
>   escrita restrita a `TI`; filtros por enum aceitam o rótulo pt-BR (`?familia=Antibióticos`).
>   56 testes verdes (27 unitários + 29 integração).
> - ✅ **Fase 4 — Estoque** (RF-EST): `Lote` (validade/fabricante), `Movimentacao` (livro-razão
>   **imutável**: Entrada/Saída/Transferência/Ajuste) e `PosicaoEstoque` (parâmetros de
>   dimensionamento + `quantidade` como projeção do livro-razão). `CalculadoraEstoque` (status
>   ok/atenção/crítico + dias para vencer) como regra pura testável. Endpoints `/estoque`
>   (posições com status, `/resumo` KPIs, drill-down `/{med}/{uni}`), `/lotes` e `/movimentacoes`.
>   Migration `V3__estoque.sql` + seeder demo (210 posições, ~309 lotes, ~917 movimentações).
>   72 testes verdes (32 unitários + 40 integração).
> - ✅ **Fase 5 — Previsão de Demanda** (RF-PRV): `Previsao` (MAPE, modelo/versão, `Drift`,
>   `calibradoEm`) + `PontoSerie` (série realizado/previsto com bandas). `CalculadoraPrevisao`
>   (meta MAPE < 15%) pura/testável. Endpoints `/previsoes` (+ filtros), `/previsoes/resumo`
>   (KPIs), `/previsoes/{med}/{uni}` (série completa) e `POST /previsoes/recalibrar`
>   (**restrito a Gestor** — estabiliza drift e marca calibração). Migration `V4__previsao.sql`
>   + seeder (210 previsões × 15 pontos, com sazonalidade epidemiológica). PRNG determinístico
>   extraído para `comum/GeradorPseudoaleatorio` (reuso entre seeders). 84 testes verdes.
> - ✅ **Fase 6 — Alertas** (RF-ALE): `Alerta` (tipo `Desabastecimento`/`Vencimento`, `Severidade`,
>   `StatusAlerta`, destinatários por `Perfil` via `@ElementCollection`, FK opcional para o `Lote`).
>   **Motor de geração por regra** (`GeradorAlerta`): desabastecimento a partir do estoque/cobertura
>   (medicamento essencial em nível crítico) e vencimento a partir dos lotes na janela de 60 dias;
>   `CalculadoraAlerta` (bandas de severidade) pura/testável. Endpoints `/alertas` (+ filtros tipo/
>   severidade/status/unidade/medicamento/busca), `/alertas/resumo` (KPIs), `PATCH /alertas/{id}/status`
>   (tratamento Aberto→Em tratamento→Resolvido, resolvido é terminal) e `POST /alertas/gerar`
>   (**restrito a Gestor** — regenera renovando os abertos e preservando os já tratados). Migration
>   `V5__alerta.sql` + seeder (106 alertas demo). Handler global para corpo inválido (400). 100 testes verdes.
> - ✅ **Fase 7 — Recomendações** (RF-REC): `Recomendacao` (tipo `Reposição`/`Redistribuição`,
>   `OrigemMotor` Regras/IA, `Prioridade`, `StatusRecomendacao`, `economiaEstimada` em `BigDecimal` R$,
>   FK para medicamento/unidade destino e origem opcional). **Motor de geração** (`GeradorRecomendacao`):
>   redistribuição entre unidade crítica e unidade com excedente, e reposição dimensionada até o
>   estoque máximo; `CalculadoraRecomendacao` (dimensionamento) pura/testável. Endpoints
>   `/recomendacoes` (+ filtros tipo/status/motor/prioridade/unidade/medicamento/busca),
>   `/recomendacoes/resumo` (KPIs: pendentes, economia potencial, geradas por IA, taxa de adesão),
>   `POST /recomendacoes/{id}/aprovar` e `/executar` (ciclo Pendente→Aprovada→Executada) e
>   `POST /recomendacoes/gerar` — **todas as ações restritas a Gestor**. Migration `V6__recomendacao.sql`
>   + seeder (84 recomendações demo). 115 testes verdes.
> - ✅ **Fase 8 — Indicadores** (RF-IND): `IndicadorMeta` (código de negócio, `baseline`/`atual`/`meta`
>   em `BigDecimal`, `metaReducaoPct`, `melhorMenor`) + `PontoHistorico` (série mensal da medição).
>   `CalculadoraIndicador` (progresso até a meta, meta atingida, variação — base do comparativo
>   piloto × sistema atual RF-IND-06) pura/testável. Endpoints `/indicadores` (lista com histórico
>   e derivações), `/indicadores/resumo` (total, atingidas, em progresso) e `/indicadores/{codigo}`
>   (drill-down). Módulo de **governança somente leitura**. Migration `V7__indicador.sql` + seeder
>   (6 indicadores × 12 pontos). 124 testes verdes.
> - ✅ **Fase 9 — Painel/Dashboard** (RF-DASH): agregações de dashboard gerencial e painel
>   operacional sob `/painel` e `/painel/operacional`. `CalculadoraPainel` (cobertura e status
>   por unidade) pura/testável; `PainelService` consolida totais da rede, cobertura por unidade,
>   série agregada de previsão (medicamento mais crítico), filas de alertas e recomendações.
>   Somente leitura — qualquer autenticado. 130 testes verdes.
> - ✅ **Fase 10 — Ingestão de Dados** (RF-DAD): `FonteDado` (status/qualidade/procedência,
>   `ultimaIngestao` como `Instant`) e `QualidadeFamilia` (maturidade/completude/consistência/
>   granularidade/lacunas por família terapêutica). `CalculadoraIngestao` (qualidade média)
>   pura/testável. Endpoints `/ingestao/fontes`, `/ingestao/qualidade` e `/ingestao/resumo`
>   (KPIs: registros, fontes sincronizadas, qualidade média, anonimização LGPD ativa).
>   Migration `V8__ingestao.sql` + seeder (6 fontes × 8 famílias). Somente leitura. 136 testes verdes.
> - ✅ **Fase 11 — Integração EMSERH** (RF-INT): `IntegracaoApi` (status, latência, `ModoIntegracao`
>   Online/Offline-buffer/Reconciliando, buffer offline) e `ProvedorIa` (papel, custo por 1k tokens,
>   anonimização) do AI Gateway. `CalculadoraIntegracao` (latência média das conexões ativas)
>   pura/testável. Endpoints `/integracoes`, `/integracoes/resumo` (operacionais, latência média,
>   buffer, provedores) e `/integracoes/provedores-ia`. Migration `V10__integracao.sql` + seeder
>   (5 integrações, 3 provedores); `V9__check_ingestao.sql` alinha a V8 ao padrão de CHECK
>   constraints. Somente leitura. 143 testes verdes.
> - ✅ **Fase 12 — IA Gateway** (RF-INT-06 · RF-SEG-04): `POST /ia/chat` faz proxy para provedores
>   externos atrás da interface `ClienteIa` (estratégia DeepSeek→OpenAI→Gemini por prioridade) via
>   `RestClient` — DeepSeek/OpenAI compatíveis com OpenAI (`/chat/completions`), Gemini
>   (`:generateContent`). **Anonimização obrigatória antes do envio** (`Anonimizador`/`AnonimizadorRegex`
>   — mascara e-mail/CPF/CNPJ/telefone/números longos, RF-SEG-04) e **modo demo resiliente**: sem
>   chave de API ou em falha, devolve resposta simulada (`mode: "demo"`), nunca quebra. Resposta
>   `{ content, model, mode, provider }`. Sem migration (não persiste). 156 testes verdes.
> - ✅ **Fase 13 — Segurança & Auditoria** (RF-SEG): trilha `LogAuditoria` — **ledger imutável**
>   (data `Instant`, ator em *snapshot* + referência fraca `usuarioId`, `CategoriaAuditoria` tipada,
>   `recurso`, `baseLegal` LGPD, `assistidoPorIA`, `ip`). Porta `RegistradorAuditoria` (interface
>   pequena, DIP) implementada por `AuditoriaService`, que resolve o ator (usuário + IP) pelo
>   `ContextoAuditoria` — assim **as ações sensíveis já existentes passam a registrar auditoria sem
>   acoplar controllers**: aprovar/executar recomendação, recalibrar previsão, gerar alertas e
>   inferência por IA (anonimizada, RF-SEG-04) gravam na trilha na mesma transação da ação. Consulta
>   somente leitura sob `/seguranca/auditoria` (lista com filtros + `/resumo`), **restrita a Gestor/TI**.
>   Filtro dinâmico via JPA `Specification` (evita o `:param IS NULL` de tipo indeterminado no Postgres).
>   Migration `V11__auditoria.sql` (CHECK em categoria/perfil) + seeder idempotente (21 eventos de
>   demonstração, semeia se a trilha estiver vazia). **167 testes verdes.**
>
> 🎉 **Todos os domínios do `CLAUDE.md` concluídos** (Fases 0–13).

---

## Stack

| Item | Decisão |
|---|---|
| Linguagem | Java 21 (LTS) |
| Framework | Spring Boot 3.x (Web, Data JPA, Security, Validation, Actuator) |
| Build | Maven |
| Banco | PostgreSQL |
| Schema | Flyway (migrations versionadas) · Hibernate `ddl-auto: validate` |
| Auth | JWT + Spring Security + BCrypt · RBAC por perfil (Operador/Gestor/TI) |
| Docs | springdoc-openapi (Swagger UI) |
| IA | Proxy para DeepSeek/OpenAI/Gemini com anonimização (LGPD) e modo demo |
| Testes | JUnit 5 · AssertJ · Mockito · MockMvc · Testcontainers (PostgreSQL) |
| Pacote base | `com.alphatech.cahosp` |
| Porta | **3002** · rotas sob `/api` |

---

## Como rodar

Pré-requisitos: **JDK 21**, **PostgreSQL** e (para os testes de integração) **Docker**.

```bash
# 1) configurar ambiente
cp .env.example .env        # preencha DB_*, JWT_SECRET, etc.

# 2) rodar em desenvolvimento
c      # Windows: .\mvnw.cmd spring-boot:run

# 3) build de produção
./mvnw clean package        # → target/*.jar

# 4) testes
./mvnw test                 # unitários (rápidos, sem Docker)
./mvnw verify               # + integração (Testcontainers, requer Docker)
```

- API: `http://localhost:3002/api`
- Swagger UI: `http://localhost:3002/api/swagger-ui/index.html`

O schema é aplicado automaticamente pelo **Flyway** no startup. Nenhuma alteração de schema é
feita à mão: cada mudança é uma migration `V<n>__*.sql`.

### Deploy com Docker (EasyPanel)

A imagem de produção é construída pelo [`Dockerfile`](Dockerfile) multi-stage (build com Maven/JDK 21
→ runtime em JRE 21 enxuto, usuário não-root). **Os testes não rodam no build da imagem** — os `*IT`
usam Testcontainers/Docker e ficam no CI (`./mvnw verify`); o [`docker-compose.yml`](docker-compose.yml)
existe só para subir um PostgreSQL local de apoio aos testes.

No EasyPanel, crie um serviço **App** apontando para este repositório com build via **Dockerfile** e
**porta 3002**. Injete as variáveis de ambiente (ver [`.env.example`](.env.example)) — no mínimo:
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_SENHA` e `CORS_ALLOWED_ORIGINS`.

```bash
# build e run local da imagem (equivalente ao EasyPanel)
docker build -t cahosp-backend .
docker run -p 3002:3002 --env-file .env cahosp-backend
```

- Health check (usado pelo container e pelo painel): `GET /api/actuator/health`
- `JAVA_OPTS` pode ser usado para passar flags da JVM (ex.: `-Xmx512m`).

---

## Convenção de resposta

Toda a API usa **um envelope único**:

```json
// sucesso
{ "success": true, "data": { }, "total": 12 }   // "total" apenas em listas

// erro
{ "success": false, "error": "Mensagem clara em português", "codigo": "VALIDACAO" }
```

As chaves do envelope (`success`/`data`/`error`) ficam em inglês por convenção técnica; o
conteúdo é em português. Status HTTP corretos (200/201/204, 400, 401, 403, 404, 409, 422, 500).

### Paginação (listas)

Listas de grande volume são **paginadas no servidor** (parâmetros padrão do Spring Data):

```
GET /estoque?page=0&size=10&sort=quantidade,desc&busca=ceftri
```

- `page` (base 0, default 0) · `size` (default 10) · `sort=campo,(asc|desc)` (ex.: `sort=medicamento.nome`).
- O envelope devolve **a página em `data`** e o **total de elementos do conjunto em `total`** (não o
  tamanho da página) — o cliente calcula o número de páginas a partir de `total`.
- Filtros e ordenação são aplicados **no banco** junto com a paginação. Para campos derivados
  (ex.: `status` de estoque, calculado de `quantidade` vs. `nivelCritico`), a regra é empurrada para
  a query via `Specification` (`EspecificacoesPosicao`), garantindo contagem de páginas correta.
- Hoje paginados: `/estoque`, `/lotes`, `/movimentacoes`, `/alertas`. Demais listas recebem o mesmo
  padrão conforme as telas consumidoras migram.

---

## Autenticação

- Login por **e-mail**: `POST /api/auth/login` → `{ email, password }` → `{ success, data: { usuario, token } }`.
- Use o token em `Authorization: Bearer <token>`. `GET /api/auth/me` retorna o usuário atual.
- **RBAC por perfil**: `Operador`, `Gestor`, `TI`. Senhas com BCrypt; JWT stateless.

---

## Administração de usuários (RF-ADM-01)

CRUD de usuários sob `/api/admin/usuarios`, **restrito ao perfil `TI`** (401 sem token,
403 `ACESSO_NEGADO` para outros perfis). O campo `perfil` aceita/serializa o rótulo pt-BR
(`Operador`/`Gestor`/`TI`).

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/admin/usuarios` | Lista; filtros opcionais `?perfil=&ativo=&busca=` (busca casa nome **ou** e-mail, ignore-case). |
| `GET` | `/admin/usuarios/{id}` | Detalha um usuário (404 se inexistente). |
| `POST` | `/admin/usuarios` | Cria `{nome,email,perfil,senha}` → 201. E-mail único (409); senha ≥ 8, gravada com BCrypt. |
| `PUT` | `/admin/usuarios/{id}` | Atualiza `{nome,email,perfil}` (409 se o novo e-mail já for de outro usuário). |
| `PATCH` | `/admin/usuarios/{id}/status` | `{ativo}` ativa/desativa. O TI **não** pode desativar a própria conta (422). |
| `PUT` | `/admin/usuarios/{id}/senha` | `{novaSenha}` redefine a senha (BCrypt). |

> Sem `DELETE`: o desligamento é por **desativação** (`ativo=false`), preservando auditoria/LGPD.

---

## Ingestão de Dados (RF-DAD)

Governança de dados somente leitura sob `/api/ingestao`, disponível para **qualquer autenticado**.

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/ingestao/fontes` | Lista fontes de dados (status, volume, qualidade, procedência, última ingestão). |
| `GET` | `/ingestao/qualidade` | Maturidade e qualidade da base histórica por família terapêutica. |
| `GET` | `/ingestao/resumo` | KPIs: registros ingeridos, fontes sincronizadas, qualidade média, anonimização LGPD. |

---

## Painel / Dashboard (RF-DASH)

Agregações somente leitura sob `/api/painel`, disponível para **qualquer autenticado** (401 sem token).

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/painel` | Dashboard gerencial: totais da rede, cobertura por unidade, série agregada de previsão (medicamento mais crítico), alertas recentes e recomendações pendentes. |
| `GET` | `/painel/operacional` | Painel operacional: situação por unidade (cobertura, críticos, alertas, conectividade), fila de alertas ativos e recomendações em aberto. |

---

## Segurança & Auditoria (RF-SEG)

Trilha de auditoria e conformidade LGPD sob `/api/seguranca/auditoria`. Módulo de governança
**somente leitura**, **restrito a `Gestor`/`TI`** (401 sem token, 403 `ACESSO_NEGADO` para `Operador`).

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/seguranca/auditoria` | Lista a trilha (mais recentes primeiro); filtros `?categoria=&perfil=&assistidoPorIA=&busca=` (busca casa ação/usuário/recurso). |
| `GET` | `/seguranca/auditoria/resumo` | KPIs: total de eventos, assistidos por IA, com base legal e última atividade. |

O registro das ações sensíveis é **automático e desacoplado**: os serviços de domínio chamam a porta
`RegistradorAuditoria` (não os controllers). Hoje gravam na trilha: aprovar/executar recomendação,
recalibrar previsão, gerar alertas e cada inferência por IA (anonimizada antes do envio, RF-SEG-04).
Cada registro é um **ledger imutável** com a base legal LGPD aplicável e o IP de origem; o ator é
resolvido do contexto de segurança da requisição.

---

## Módulos / Domínios

Mapeados 1:1 com as telas do frontend e suas faixas de requisitos funcionais (`RF-*`):

| Domínio | Rotas `/api` | Tela do front | RF |
|---|---|---|---|
| Painel & Indicadores | `/painel`, `/indicadores` | Dashboard, Operacional, Indicadores | RF-DASH, RF-IND |
| Medicamentos | `/medicamentos` | (catálogo, usado em todas) | — |
| Unidades | `/unidades` | (rede EMSERH) | RF-DAD-06 |
| Estoque & Lotes | `/estoque`, `/lotes`, `/movimentacoes` | Estoque & Lotes | RF-EST-01..06 |
| Previsão de Demanda | `/previsoes` | Previsão de Demanda | RF-PRV-01..09 |
| Alertas | `/alertas` | Alertas | RF-ALE-01..05 |
| Reposição & Redistribuição | `/recomendacoes` | Recomendações | RF-REC-01..05 |
| Ingestão de Dados | `/ingestao/fontes`, `/ingestao/qualidade` | Ingestão de Dados | RF-DAD-01..08 |
| Integração EMSERH | `/integracoes` | Integração | RF-INT-01..06 |
| Segurança & LGPD | `/seguranca/auditoria` | Segurança & LGPD | RF-SEG-01..06 |
| Administração | `/admin/usuarios` | Administração | RF-ADM-01..04 |
| IA (gateway) | `/ia/chat` | (transversal) | RF-INT, RF-SEG-04 |

**Entidades centrais:** `Medicamento` e `Unidade` (incluindo a CAHOSP central) — referenciadas
por FK em estoque, previsão, alertas e recomendações.

### Ordem de implementação sugerida

Por dependência (base antes dos consumidores):

1. `comum` (envelope + tratamento de erro) → `seguranca` (JWT/RBAC) → `usuario` (auth/admin)
2. `unidade` → `medicamento`
3. `estoque` (lote/movimentação/posição) → `previsao` → `alerta` → `recomendacao`
4. `indicador` → `painel` → `ingestao` → `integracao` → `ia` → `seguranca/auditoria`

---

## Princípios do projeto

- **Português** em domínio, rotas, mensagens e validação (envelope técnico em inglês).
- **UUID** em todas as entidades; **tipos corretos** (`LocalDate`/`Instant`, `BigDecimal`, enums).
- **SOLID** e camadas claras: controller fino · service com a regra · repository de persistência ·
  DTOs (`record`); entidades nunca expostas direto.
- **Segurança real** (JWT/BCrypt/RBAC), **auditoria** e **LGPD** (anonimização antes de IA externa).
- **Flyway** versiona o schema (`ddl-auto: validate`).
- **Testes obrigatórios** em toda mudança (unitários + integração com Testcontainers).
- **Rastreabilidade `RF-*`** mantida no código e na documentação.

Detalhes e convenções completas em [`CLAUDE.md`](CLAUDE.md).

---

## Variáveis de ambiente

| Env | Uso |
|---|---|
| `PORT` | porta (default 3002) |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | PostgreSQL (JDBC) |
| `CORS_ALLOWED_ORIGINS` | origens do CORS, separadas por vírgula (default `http://localhost:5173`) |
| `JWT_SECRET` / `JWT_EXPIRES_IN` | JWT |
| `ADMIN_NOME` / `ADMIN_EMAIL` / `ADMIN_SENHA` | administrador inicial (criado se não existir) |
| `DEEPSEEK_API_KEY` / `OPENAI_API_KEY` / `GEMINI_API_KEY` | IA (opcionais → modo demo) |
| `INTEGRACAO_ENABLED` | `true` liga integração EMSERH real; `false` (default) = stub |

Nunca commitar segredos — `application.yml` lê de env (via `.env`).

---

## Estrutura

```
src/main/java/com/alphatech/cahosp/
├── CahospApplication.java
├── comum/          # ApiResponse<T>, exceções, GlobalExceptionHandler
├── config/         # CORS, OpenAPI, beans de IA, JPA Auditing
├── seguranca/      # SecurityConfig, JWT, RBAC, auditoria/LGPD
└── <dominio>/      # unidade, medicamento, estoque, previsao, alerta,
                    # recomendacao, indicador, ingestao, integracao,
                    # ia, painel, usuario
    ├── <X>Controller.java
    ├── <X>Service.java
    ├── <X>Repository.java
    ├── dominio/    # @Entity + enums
    └── dto/        # records + validação

src/main/resources/
├── application.yml
└── db/migration/   # V<n>__*.sql (Flyway)
```
