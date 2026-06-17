# CLAUDE.md — Smart Health CAHOSP · Backend (Java / Spring Boot)

Guia para o agente trabalhar neste repositório.

## Contexto do projeto

Este é o backend **novo, escrito do zero em Java**, da plataforma **Smart Health CAHOSP** —
**gestão preditiva da cadeia farmacêutica** da Central de Abastecimento Hospitalar
(**CAHOSP / EMSERH-MA**). A plataforma cobre os **62 requisitos funcionais** (`RF-*`)
especificados no documento *Requisitos Funcionais Smart Health* (Edital FAPEMA GovIA —
Desafio Tecnológico 2): previsão de demanda de insumos, controle de estoque por lote,
alertas de desabastecimento e vencimento, recomendações de reposição/redistribuição entre
unidades, indicadores de projeto, ingestão/qualidade de dados, integração com sistemas EMSERH
e segurança/LGPD.

**Objetivo: uma API profissional, robusta e segura — qualidade de produção.** É uma construção
limpa, guiada pelo frontend.

### Papéis de cada projeto vizinho

| Projeto | Papel |
|---|---|
| `../smarthealth-farm` | React 19 + Vite + **TypeScript** + Tailwind/shadcn. **Fonte ÚNICA da verdade do que precisa existir.** Cada tela revela entidades, campos e operações. O dono do projeto vai adaptar o front (remover os dados mockados em `src/data` e apontar para esta API) — **o agente NÃO altera o frontend**, só o lê para entender requisitos. |
| `smarthealth-farm-backend` (aqui) | A API nova. |

> **Não existe API de referência (não há backend Node).** Toda a modelagem sai do frontend.
> Os pontos de partida obrigatórios são:
> - `../smarthealth-farm/src/types/index.ts` — **o modelo de domínio quase pronto** (entidades, enums, campos).
> - `../smarthealth-farm/src/data/*.ts` — dados mock + **regras de agregação/derivação** (status de estoque, geração de alertas, recomendações, indicadores) que revelam a lógica de negócio esperada.
> - `../smarthealth-farm/src/pages/*.tsx` + `src/lib/*` — operações por tela, filtros, formatação (pt-BR) e tokens de status.
> - `../smarthealth-farm/src/lib/nav.ts` e o `README.md` do front — mapa de módulos ↔ faixas de `RF-*`.

> **Regra mental:** o front responde *quais dados, regras e operações existem*. "Como vamos
> fazer no back?" é decisão nova, com qualidade de produção (UUID, tipos certos, FK, validação,
> segurança real). Onde o mock simplifica (datas como String, dados derivados no cliente),
> **modele corretamente no servidor.**

---

## Princípios inegociáveis

1. **Idioma: português** em tudo — entidades, atributos, pacotes de domínio, rotas
   (`/api/insumos`, `/api/estoque`, `/api/previsoes`), mensagens de erro e validação.
   (Termos técnicos universais — `id`, `status`, `email` — podem ficar como são.)
2. **IDs: `UUID` em TODAS as entidades** (`@Id UUID`, gerado pela aplicação). Nada de `Long`
   autoincrement nem IDs String atribuídos na mão. (No front os ids são strings tipo `m-001`/
   `u-cahosp`; isso é detalhe de mock — no back use UUID e, quando útil, um **código de negócio**
   legível à parte, ex.: `codigo` do insumo.)
3. **Tipos corretos**: datas/horas são `LocalDate` / `LocalDateTime` / `Instant` — **nunca**
   `String` (o mock usa ISO string; no back tipe). Dinheiro/valores (`economiaEstimada`,
   custos, R$) são `BigDecimal`. Percentuais e métricas numéricas tipados. Enums para todo
   conjunto fechado (status, severidade, prioridade, categoria de insumo, perfil, modo de
   integração, papel de IA, tipo de movimentação, drift…).
4. **Segurança real**: Spring Security + **JWT** de verdade, senha com **BCrypt**, RBAC por
   perfil (`Operador` / `Gestor` / `TI`). Nada de token fake.
5. **Entidades centrais `Insumo` e `Unidade`**: referenciadas por **FK** em todos os
   domínios (estoque, lote, movimentação, previsão, alerta, recomendação…). **Não** repetir
   nome/sigla soltos como String — sempre relacionamento.
6. **SOLID e arquitetura limpa** (ver seção dedicada abaixo): responsabilidade única por classe,
   serviços coesos, dependências por interface/injeção, controllers finos. Regra de negócio
   vive no `Service`, nunca no controller nem na entidade anêmica exposta.
7. **Validação e tratamento de erro consistentes**: Bean Validation nos DTOs + um
   `@RestControllerAdvice` global. Toda resposta segue o mesmo envelope (ver abaixo).
8. **Schema versionado com Flyway**: nenhuma mudança de schema é feita "na mão" no banco nem
   pelo Hibernate. Toda alteração é uma nova migration `V<n>__*.sql`. `ddl-auto: validate`.
9. **Testes são obrigatórios**: **toda nova funcionalidade ou mudança em funcionalidade existente
   exige testes** (unitários da regra de negócio; integração quando tocar banco/endpoint).
   Nenhum módulo é concluído sem testes verdes (`./mvnw test` / `./mvnw verify`).
10. **LGPD e auditoria de primeira classe** (RF-SEG): trilha de auditoria das ações sensíveis
    (com base legal), e **anonimização antes de qualquer chamada a IA externa** (RF-SEG-04).
11. **Rastreabilidade dos requisitos**: cada domínio/endpoint relevante referencia sua faixa de
    `RF-*` em Javadoc/comentário (ex.: `// RF-EST-04: nível crítico vem da previsão`). Ajuda a
    comprovar a cobertura do edital e a manter o vínculo com o front.
12. **Documentação acompanha o crescimento**: ao adicionar/alterar domínio, endpoint ou regra,
    reflita no `README.md` na mesma entrega. Docs desatualizada é bug.

---

## Stack

| Item | Decisão |
|---|---|
| Linguagem | **Java 21 (LTS)** |
| Framework | **Spring Boot 3.x** — Web, Data JPA, Security, Validation, Actuator |
| Build | **Maven** |
| Banco | **PostgreSQL** |
| Schema | **Flyway** (migrations em `src/main/resources/db/migration`); Hibernate em `ddl-auto: validate` |
| Auth | JWT + Spring Security + BCrypt |
| Docs API | **springdoc-openapi** (Swagger UI em `/api/swagger-ui/index.html`) |
| IA | Proxy HTTP (`RestClient`/`WebClient`) para DeepSeek (primário) / OpenAI (fallback) / Gemini (standby), com **anonimização** e fallback "modo demo" |
| Testes | JUnit 5 + AssertJ + Mockito; MockMvc + Spring Security Test; **Testcontainers (PostgreSQL)** |
| Pacote base | `com.alphatech.cahosp` |
| Porta | **3002**, rotas sob **`/api`** |

> A porta **3002** evita conflito com o backend do projeto irmão (PEC, `3001`), caso rodem juntos.

---

## Comandos

```bash
./mvnw spring-boot:run          # rodar em dev (Windows: .\mvnw.cmd spring-boot:run)
./mvnw clean package            # build → target/*.jar
./mvnw test                     # testes unitários (Surefire, *Test.java) — sem Docker
./mvnw verify                   # + testes de integração (Failsafe, *IT.java) — requer Docker
./mvnw test -Dtest=ClasseTest   # um teste
```

---

## SOLID e arquitetura (boas práticas — exigência do projeto)

A arquitetura é **em camadas, por domínio**. Cada camada tem uma responsabilidade.

- **S — Responsabilidade única**: `Controller` só faz HTTP (entrada/validação/resposta).
  `Service` concentra **uma** área de regra de negócio. `Repository` só persiste. DTOs só
  transportam. Não misture (ex.: nada de query no controller, nada de `HttpServletResponse`
  no service).
- **O — Aberto/fechado**: comportamentos que variam (motor de recomendação por "Regras" vs.
  "Aprendizado de Máquina"; provedores de IA DeepSeek/OpenAI/Gemini; cálculo de severidade)
  ficam atrás de **interfaces + estratégias**, para estender sem editar o existente.
- **L — Substituição de Liskov**: implementações de uma interface (ex.: `ProvedorIa`) são
  intercambiáveis sem quebrar quem chama.
- **I — Segregação de interface**: interfaces pequenas e focadas (ex.: `Anonimizador`,
  `MotorRecomendacao`, `ClienteIa`) em vez de uma "service-deus".
- **D — Inversão de dependência**: o domínio depende de **abstrações**; integrações concretas
  (HTTP de IA, exportação de relatório, gateway EMSERH) implementam interfaces e entram por
  injeção. Facilita o "modo demo"/stub e os testes com mock.

Regras práticas:
- **Controllers finos**; regra no **Service**; persistência no **Repository** (`JpaRepository<T, UUID>`).
- **Nunca exponha `@Entity`** no controller — sempre mapeie para DTO (`record`).
- Cálculos derivados (status de estoque, dias para vencer, cobertura, dimensionamento de
  reposição) vivem em **serviços de domínio testáveis** (lógica pura → teste unitário fácil).
  Veja a lógica de referência no front em `src/data/index.ts` e `src/lib/status.ts`.
- Evite duplicação (DRY) e mantenha métodos curtos e nomeados em português do domínio.

---

## Domínios e modelagem (a partir do front)

Um pacote por domínio. As entidades saem direto de `../smarthealth-farm/src/types/index.ts`
(tradução fiel → JPA, com UUID/tipos corretos/FK). Resumo do que existe:

| Domínio (pacote) | Entidades principais | Rotas `/api` | RF |
|---|---|---|---|
| `unidade` | `Unidade` (inclui a CAHOSP central + unidades atendidas; `porte`, `conectividade`, `leitos`, `perfilDemografico`) | `/unidades` | RF-DAD-06 |
| `insumo` | `Insumo` (`categoria`, `criticidade`, `essencial`, `apresentacao`, `unidadeMedida`) | `/insumos` | base do catálogo |
| `usuario` | `Usuario` (`perfil` Operador/Gestor/TI, `unidade` FK, `ativo`), auth + admin | `/auth`, `/admin/usuarios` | RF-ADM, RF-SEG |
| `estoque` | `PosicaoEstoque` (derivada), `Lote` (validade, fabricante), `Movimentacao` (Entrada/Saída/Transferência/Ajuste) | `/estoque`, `/lotes`, `/movimentacoes` | RF-EST-01..06 |
| `previsao` | `Previsao` (`mape`, `modelo`, `versaoModelo`, `drift`) + `PontoSerie` (realizado/previsto/limites) | `/previsoes` | RF-PRV-01..09 |
| `alerta` | `Alerta` (`Desabastecimento`/`Vencimento`, `severidade`, `status`, `destinatarios`) | `/alertas` | RF-ALE-01..05 |
| `recomendacao` | `Recomendacao` (`Reposição`/`Redistribuição`, `origemMotor`, `prioridade`, `economiaEstimada` R$, `status`) | `/recomendacoes` | RF-REC-01..05 |
| `indicador` | `IndicadorMeta` (`baseline`/`atual`/`meta`, `historico`) | `/indicadores` | RF-IND-01..06 |
| `ingestao` | `FonteDado` (status/qualidade/procedência), `QualidadeCategoria` | `/ingestao/fontes`, `/ingestao/qualidade` | RF-DAD-01..08 |
| `integracao` | `IntegracaoAPI` (modo Online/Offline-buffer/Reconciliando, latência, buffer), `ProvedorIA` | `/integracoes` | RF-INT-01..06 |
| `seguranca` | `LogAuditoria` (ação, recurso, `baseLegal`, `assistidoPorIA`, ip) | `/seguranca/auditoria` | RF-SEG-01..06 |
| `ia` | gateway de IA (proxy + anonimização + modo demo) | `/ia/chat` | RF-INT, RF-SEG-04 |
| `painel` | agregações de dashboard (totais, resumo por unidade, série agregada) | `/painel`, `/painel/operacional` | RF-DASH-01..07 |

**Observações de modelagem importantes:**
- `PosicaoEstoque` no front é **derivada** (`nivelCritico` calculado da previsão, `consumoMedioDiario`,
  `tempoMedioRessuprimentoDias`). Decida: materializar (tabela atualizada por serviço) **ou**
  calcular sob demanda — documente a escolha. O `statusEstoque` (ok/atenção/crítico) é regra de
  negócio (ver `src/data/index.ts`) → serviço testável.
- `Alerta` e `Recomendacao` são **gerados por regra** a partir do estoque/previsão (o front mostra
  o algoritmo). No back, isso é um serviço de domínio (com testes), não dado solto.
- `destinatarios` do alerta é um conjunto de `Perfil` → `@ElementCollection` de enum ou tabela de
  associação.
- `serie`/`historico` (séries temporais) → entidade filha (`PontoSerie`) ou `@ElementCollection`,
  conforme a necessidade de consulta.
- `Movimentacao` é o livro-razão do estoque (rastreabilidade por lote, RF-EST). Imutável após criada.

---

## Migrations (Flyway) — o schema do banco

**O schema é versionado com Flyway.** As entidades JPA definem o modelo; o Flyway aplica o SQL
versionado; o Hibernate (`ddl-auto: validate`) só confere a paridade — **nunca altera o banco**.

- Migrations em `src/main/resources/db/migration/`, nomeadas `V<n>__descricao.sql`
  (ex.: `V1__init.sql`, `V2__add_previsao.sql`). Rodam **automaticamente no startup**, em ordem.
- Tabela de controle: `flyway_schema_history`.

**Fluxo ao adicionar/alterar entidade:**
1. Criar/editar a entidade `@Entity`.
2. Criar a próxima migration `V<n>__*.sql` com o DDL correspondente.
   - Para obter o DDL exato esperado pelo Hibernate, gere o script:
     `java -jar target/*.jar --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=schema-export.sql --spring.main.web-application-type=none`
     e copie só o trecho novo para a migration (não commitar `schema-export.sql`).
3. Subir a app → Flyway aplica; `validate` confirma a paridade.

> Regra: **nunca** volte `ddl-auto` para `update`/`create`. Schema só muda via migration.

---

## Testes (OBRIGATÓRIO em toda mudança)

Nenhuma funcionalidade nova nem alteração é concluída sem testes verdes.

**Convenção de execução (separa rápido de lento):**
- **Unitários** → `*Test.java`, rodam no `./mvnw test` (Surefire). **Sem Docker, sem banco.**
  Regra de negócio com dependências mockadas (Mockito) ou lógica pura.
- **Integração** → `*IT.java`, rodam no `./mvnw verify` (Failsafe). Sobem **PostgreSQL real via
  Testcontainers** (requer Docker), aplicam as migrations Flyway e exercitam os endpoints.
  Estendem uma base comum (ex.: `suporte/BaseIntegracaoPostgres`).

**O que testar:**
- Regra de negócio nos `Service` (status de estoque, geração de alertas/recomendações,
  dimensionamento de reposição, dias-para-vencer, cobertura, severidade) → **unitário com Mockito/lógica pura**.
- Contrato HTTP (status, envelope, validação 400, 401/403 por perfil) → `@WebMvcTest` + MockMvc +
  Spring Security Test.
- Fluxo completo + migrations + queries reais → `*IT` com Testcontainers.

> Não teste em banco de produção via curl. Use os testes (`*IT`) — repetíveis e não sujam o banco.

---

## Estrutura de pacotes (por domínio)

```
src/main/java/com/alphatech/cahosp/
├── CahospApplication.java
├── comum/             # ApiResponse<T>, exceções de negócio, GlobalExceptionHandler, tipos base
├── config/            # CorsConfig, OpenAPI, beans HTTP p/ IA, JpaAuditing
├── seguranca/         # SecurityConfig, JwtService, filtro JWT, UserDetails, auditoria/LGPD
└── <dominio>/         # um pacote por domínio (ver tabela acima)
    ├── <X>Controller.java   # @RestController, rotas /api/<dominio> — fino
    ├── <X>Service.java      # regra de negócio (SOLID)
    ├── <X>Repository.java   # JpaRepository<T, UUID>
    ├── dominio/             # entidades @Entity + enums
    └── dto/                 # records de request/response + validação
```

---

## Convenção de resposta (uniforme em TODA a API)

Um envelope só, via `comum/ApiResponse<T>`:

```json
// sucesso
{ "success": true, "data": <payload>, "total": 12 }      // "total" só em listas
// erro
{ "success": false, "error": "Mensagem clara em português", "codigo": "VALIDACAO" }
```

> As chaves do envelope (`success`/`data`/`error`) ficam em inglês de propósito: é um wrapper
> técnico convencional que o frontend consome. O **conteúdo** (campos de `data`, mensagens) é em
> português. Status HTTP corretos: 200/201/204, 400, 401, 403, 404, 409, 422, 500.

---

## Convenções de código

- **Entidades** (`@Entity`): `@Id UUID id`; nomes de tabela/coluna em snake_case português via
  `@Table`/`@Column` quando útil. Campos obrigatórios `nullable=false`. Relacionamentos por FK
  (`@ManyToOne`/`@OneToMany`), nunca String solta para referenciar outra entidade.
- **Enums** (`@Enumerated(EnumType.STRING)`) para conjuntos fechados. Mapeie fielmente os do front:
  `CategoriaInsumo`, `Criticidade`, `Porte`, `Conectividade`, `Prioridade`, `Perfil`,
  `TipoMovimentacao`, `TipoAlerta`, `Severidade`, `StatusAlerta`, `TipoRecomendacao`, `OrigemMotor`,
  `Drift`, `ModoIntegracao`, `PapelIa`, etc.
- **DTOs**: `record` Java. Validação com `jakarta.validation` (`@NotBlank`, `@NotNull`, `@Positive`…).
  Nunca exponha `@Entity` no controller.
- **Datas**: `LocalDate`/`LocalDateTime`/`Instant`. **Auditoria**: `criadoEm`/`atualizadoEm` com
  `@CreatedDate`/`@LastModifiedDate` (habilitar `@EnableJpaAuditing`).
- **Dinheiro/percentual**: `BigDecimal`. Métricas inteiras: tipos numéricos, nunca String.
- **Service** concentra a regra; **Controller** é fino; **Repository** estende `JpaRepository<T, UUID>`.

---

## Segurança / Autenticação (JWT) + LGPD

- Spring Security **stateless** + filtro `OncePerRequestFilter` que valida `Authorization: Bearer`.
- `JwtService` gera/valida o token. Segredo e expiração vêm de env (`JWT_SECRET`, `JWT_EXPIRES_IN`).
- `Usuario` tem `senhaHash` (**BCrypt**); **login por e-mail** (sem `username`). O subject do JWT
  é o e-mail. `POST /api/auth/login` recebe `{ email, password }` e devolve
  `{ success, data: { usuario: {...}, token } }`. `GET /api/auth/me` valida o JWT; `POST /api/auth/logout`
  é stateless (o front descarta o token).
- **RBAC por `Perfil`** (`Operador`, `Gestor`, `TI`). O claim de papel no JWT controla o acesso
  (`@PreAuthorize` ou regras no `SecurityConfig`). Ex.: aprovar recomendação/recalibrar modelo =
  `Gestor`; administração de usuários/parâmetros = perfil com permissão; leitura operacional = `Operador`.
- **CORS** liberado para o frontend (`http://localhost:5173` em dev), métodos GET/POST/PUT/DELETE/PATCH,
  headers `Content-Type`/`Authorization`. Configurável por `CORS_ALLOWED_ORIGINS` (uma ou várias
  origens separadas por vírgula).
- **Auditoria (RF-SEG-01..03)**: ações sensíveis (aprovar recomendação, recalibrar previsão,
  alterar limiar de alerta, exportar relatório, CRUD de usuário, inferência por IA) geram
  `LogAuditoria` com `usuario`, `perfil`, `acao`, `recurso`, **`baseLegal`** (LGPD) e `assistidoPorIA`.
- **LGPD (RF-SEG-04..06)**: dados pessoais minimizados; **anonimização obrigatória antes de
  enviar qualquer payload a provedores de IA externos** (ver domínio `ia`).

---

## Integração com IA (domínio `ia`) + Integração EMSERH (`integracao`)

`POST /api/ia/chat` faz **proxy** para provedores externos. Comportamento (refletindo o front —
`ProvedorIA`, RF-INT/RF-SEG-04):
- Provedores: **DeepSeek** (primário), **OpenAI** (fallback), **Gemini** (standby) — selecionáveis,
  atrás de uma interface (`ProvedorIa`/`ClienteIa`) e estratégia (princípio O/L/D do SOLID).
- DeepSeek/OpenAI usam API compatível (`/chat/completions`); Gemini usa `:generateContent`.
- **Anonimização antes do envio** (RF-SEG-04): nunca vazar dado pessoal/identificável para fora.
- **Fallback "modo demo"**: sem API key (`DEEPSEEK_API_KEY` etc.) ou em 401/403, responder texto
  simulado com `mode: "demo"`. Resposta: `{ success, data: { content, model, mode, provider } }`.
- Use `RestClient`/`WebClient`.

**Integração EMSERH (`integracao`, RF-INT)**: APIs externas (FarmaWeb, SIH, CNES, Compras…) com
estados `Operacional`/`Degradada`/`Indisponível` e **modo** `Online`/`Offline (buffer)`/
`Reconciliando` para unidades de borda com conectividade precária. Modele o **buffer offline** e a
**reconciliação** como conceito de domínio. Comece com **stub/modo demo** (`INTEGRACAO_ENABLED=false`).

---

## Variáveis de ambiente

| Env | Uso |
|---|---|
| `PORT` | porta (default 3002) |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | PostgreSQL (URL no formato JDBC) |
| `CORS_ALLOWED_ORIGINS` | Origens permitidas no CORS; uma ou várias separadas por vírgula (default `http://localhost:5173`) |
| `JWT_SECRET` / `JWT_EXPIRES_IN` | JWT |
| `ADMIN_NOME` / `ADMIN_EMAIL` / `ADMIN_SENHA` | Administrador inicial criado no deploy (idempotente) |
| `DEEPSEEK_API_KEY` / `OPENAI_API_KEY` / `GEMINI_API_KEY` | IA (opcionais → modo demo) |
| `INTEGRACAO_ENABLED` | `true` liga integração EMSERH real; `false` (default) = stub/modo demo |

> **Administrador inicial:** um `DataSeeder` (`CommandLineRunner`) cria o usuário admin
> (perfil com permissão total) **se ainda não existir** — idempotente em redeploys. Sem usuários
> de senha fixa/fraca no seed. Nunca commitar segredos; `application.yml` lê de env (via `.env`).

---

## Fluxo de trabalho (tela por tela)

O desenvolvimento é guiado pelo frontend, **na ordem de dependência** (entidades-base antes das
telas que as consomem). Ordem sugerida (ver tabela de domínios e o `README.md`):

1. **Base/segurança**: `comum` (envelope + handler) → `seguranca` (JWT/RBAC) → `usuario` (auth/admin).
2. **Catálogo**: `unidade` → `insumo`.
3. **Operação**: `estoque` (lote/movimentação/posição) → `previsao` → `alerta` → `recomendacao`.
4. **Governança/visão**: `indicador` → `painel` → `ingestao` → `integracao` → `ia` → `seguranca/auditoria`.

Ciclo de cada item:
1. Identificar o domínio e as entidades que a tela precisa (consultar o `.tsx`, `src/types`, `src/data`).
2. Verificar o que já existe **neste backend**.
3. Modelar com qualidade de produção (UUID, tipos certos, FK, validação, SOLID).
4. Implementar: entidade(s) → migration `V<n>__*.sql` → repository → service → controller/endpoints → **testes**.
5. Atualizar a documentação (`README.md`); manter as tags `RF-*` no código.

**Template de prompt por tela** (o dono usa assim):
> "Tela `<caminho.tsx>` do front `../smarthealth-farm`. Veja o que ela precisa de dados reais,
> confira o que já existe neste back, e implemente as entidades + endpoints que faltam, seguindo
> o CLAUDE.md (SOLID, segurança, testes, Flyway). O front é a fonte da verdade."

Ao tocar um domínio pela primeira vez, olhe **todas** as telas/tipos daquele domínio antes de
modelar, para a entidade já nascer completa e evitar retrabalho.

---

## Referência rápida (onde olhar no front)

| O quê | Caminho (`../smarthealth-farm/`) |
|---|---|
| Modelo de domínio (entidades/campos/enums) | `src/types/index.ts` |
| Regras de negócio / derivações / agregações | `src/data/index.ts` |
| Catálogos base (unidades, insumos) | `src/data/units.ts`, `src/data/medicines.ts` |
| Operações por tela | `src/pages/*.tsx` |
| Mapa de módulos ↔ `RF-*` | `src/lib/nav.ts`, `README.md` |
| Status/format/utilitários (pt-BR) | `src/lib/status.ts`, `src/lib/format.ts` |
