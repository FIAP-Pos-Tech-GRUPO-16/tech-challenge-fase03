# Tech Challenge — Fase 03: Sistema Hospitalar

Backend modular para agendamento de consultas, histórico de pacientes e notificações,
desenvolvido para o Tech Challenge da Fase 03 (Pós Tech / Arquitetura e Desenvolvimento Java).

Três serviços Spring Boot independentes, comunicando-se de forma assíncrona via RabbitMQ:

- **scheduling-service** — dono dos usuários e das consultas. Expõe REST, autentica via JWT e
  publica eventos de consulta criada/editada.
- **notification-service** — consome os eventos e simula o envio de um lembrete ao paciente.
- **history-service** — consome os mesmos eventos, mantém um modelo de leitura do histórico e
  expõe consultas flexíveis via **GraphQL**.

## Arquitetura

```
                         POST /auth/login (usuário/senha)
                                    │
                                    ▼
                        ┌─────────────────────┐
                        │  scheduling-service  │  :8081
                        │  (dono de usuários   │
                        │   e consultas)       │◄──── REST (JWT) ─── médico / enfermeiro / paciente
                        └──────────┬───────────┘
                                   │ publica ConsultaEvent (criada/editada)
                                   ▼
                        ┌─────────────────────┐
                        │   RabbitMQ           │
                        │ consultas.exchange    │
                        │ (topic)               │
                        └─────┬───────────┬─────┘
                              │           │
                 notificacao. │           │ historico.
                 consultas.   │           │ consultas.
                 queue        │           │ queue
                              ▼           ▼
                ┌───────────────────┐   ┌───────────────────┐
                │ notification-      │   │ history-service    │
                │ service    :8082   │   │            :8083   │
                │ (envia lembrete)   │   │ (GraphQL /graphql) │
                └───────────────────┘   └───────────────────┘
```

Cada serviço tem seu próprio banco PostgreSQL (`hospital_scheduling`, `hospital_notification`,
`hospital_history`) — nenhum serviço acessa o banco de outro diretamente; a única integração
entre eles é o evento publicado no RabbitMQ. Essa é a fronteira intencional: o agendamento é a
fonte da verdade; notificação e histórico são consumidores independentes e eventualmente
consistentes.

### Módulos compartilhados

- **common-events** — contrato do evento de integração (`ConsultaEvent`) e a topologia do
  RabbitMQ (nomes de exchange, filas, routing keys). Sem dependência de frameworks.
- **common-security** — emissão/validação de JWT e o filtro de autenticação, usados pelos três
  serviços. Apenas o `scheduling-service` emite tokens (é o dono da tabela de usuários); os
  outros dois validam o mesmo token com o segredo compartilhado, sem se comunicar entre si para
  autenticar cada requisição.

## Decisões de arquitetura e trade-offs

- **RabbitMQ em vez de Kafka**: o enunciado permite qualquer um dos dois. RabbitMQ encaixa melhor
  no caso de uso (notificar um paciente por evento, sem necessidade de replay/streaming) e é
  operacionalmente mais simples para o escopo do desafio.
- **Autenticação com JWT**: o enunciado pede "autenticação básica com Spring Security". Optamos
  por JWT em vez de HTTP Basic puro porque o sistema tem três serviços independentes — com JWT,
  notificação e histórico validam a identidade do usuário sem precisar chamar o agendamento a
  cada requisição (nem compartilhar sessão). O segredo de assinatura é compartilhado via
  variável de ambiente (`SECURITY_JWT_SECRET`), não há um servidor OAuth2/OIDC completo — decisão
  deliberada para não introduzir infraestrutura desproporcional ao escopo.
- **Histórico como serviço separado** (em vez de embutido no agendamento): mais fiel à separação
  sugerida no enunciado, ao custo de consistência eventual entre o agendamento e o histórico —
  aceitável porque o histórico é um modelo de leitura, não a fonte da verdade.
- **Sem Transactional Outbox**: o evento é publicado depois que a transação de banco confirma
  (via `@TransactionalEventListener(phase = AFTER_COMMIT)`), o que já evita notificar sobre uma
  consulta que não foi de fato salva. Ainda existe uma janela teórica entre o commit e a
  publicação em que o processo poderia cair — eliminá-la por completo exigiria um Outbox
  (tabela de eventos pendentes + publicador assíncrono), deliberadamente fora do escopo deste
  desafio.
- **Idempotência dos consumidores**: o `notification-service` usa uma constraint única em
  `evento_id` — o mesmo evento nunca gera duas notificações, mesmo em caso de reentrega do
  RabbitMQ (entrega "at-least-once"). O `history-service` só aplica um evento se ele for mais
  recente que a última atualização já processada, protegendo contra entrega fora de ordem.
- **Retry + Dead Letter Queue**: cada fila de consumo tenta reprocessar uma mensagem até 3 vezes
  (com backoff exponencial) antes de desistir e roteá-la para a fila de dead-letter
  correspondente — uma falha transitória não descarta a mensagem, e uma falha persistente não
  trava a fila indefinidamente.

## Segurança

- Cada usuário tem um papel único: `MEDICO`, `ENFERMEIRO` ou `PACIENTE`.
- Login em `POST /auth/login` (scheduling-service) retorna um JWT válido por 120 minutos
  (configurável), com o papel do usuário como claim.
- Regras de autorização:
  - **Médicos e enfermeiros** podem criar e editar consultas, e visualizar qualquer consulta.
  - **Pacientes** só podem visualizar as próprias consultas — em toda rota (REST ou GraphQL)
    que aceita um `pacienteId`, um paciente que tente consultar o de outra pessoa recebe
    `403 Forbidden`.
- Requisição sem token ou com token inválido/expirado recebe `401 Unauthorized`; requisição
  autenticada mas sem permissão recebe `403 Forbidden`.

### Usuários de demonstração

Criados automaticamente na primeira subida do `scheduling-service` (senha igual para todos,
apenas para facilitar os testes):

| username           | papel      | senha       |
|---------------------|------------|-------------|
| `medica.ana`         | MEDICO     | `Senha@123` |
| `enfermeiro.bruno`   | ENFERMEIRO | `Senha@123` |
| `paciente.joao`      | PACIENTE   | `Senha@123` |
| `paciente.maria`     | PACIENTE   | `Senha@123` |

## Como executar

Pré-requisitos: Docker e Docker Compose.

```bash
docker compose up -d --build
```

Isso sobe PostgreSQL, RabbitMQ e os três serviços. Endpoints:

| Serviço              | URL base                     |
|-----------------------|-------------------------------|
| scheduling-service     | http://localhost:8081         |
| notification-service   | http://localhost:8082         |
| history-service        | http://localhost:8083         |
| GraphiQL (history)     | http://localhost:8083/graphiql |
| RabbitMQ management    | http://localhost:15672 (guest/guest) |

Para rodar localmente sem Docker (requer Java 21+, Maven e PostgreSQL/RabbitMQ já disponíveis):

```bash
./mvnw clean install
./mvnw -pl scheduling-service spring-boot:run
```

### Variáveis de ambiente principais

| Variável                        | Descrição                                  | Padrão (dev)                                  |
|----------------------------------|---------------------------------------------|------------------------------------------------|
| `DB_HOST`, `DB_PORT`, `DB_NAME`  | Conexão com o PostgreSQL do serviço          | `localhost` / `5432` / `hospital_<serviço>`    |
| `DB_USERNAME`, `DB_PASSWORD`     | Credenciais do PostgreSQL                    | `hospital` / `hospital`                        |
| `RABBITMQ_HOST`, `RABBITMQ_PORT` | Conexão com o broker                         | `localhost` / `5672`                           |
| `SECURITY_JWT_SECRET`            | Segredo HMAC do JWT (mínimo 32 caracteres), **igual nos três serviços** | valor de desenvolvimento no `application.yml` |
| `SECURITY_JWT_EXPIRATION_MINUTES`| Validade do token                            | `120`                                          |

## API

### REST — scheduling-service (`:8081`)

| Método | Rota              | Papéis                        | Descrição                          |
|--------|-------------------|--------------------------------|-------------------------------------|
| POST   | `/auth/login`      | público                        | Autentica e retorna o JWT           |
| POST   | `/consultas`        | MEDICO, ENFERMEIRO             | Registra uma nova consulta          |
| PUT    | `/consultas/{id}`   | MEDICO, ENFERMEIRO             | Edita uma consulta existente        |
| GET    | `/consultas/{id}`   | MEDICO, ENFERMEIRO, PACIENTE*  | Busca uma consulta pelo id          |
| GET    | `/consultas?pacienteId=` | MEDICO, ENFERMEIRO, PACIENTE* | Lista consultas (todas, ou de um paciente) |

\* paciente só acessa as próprias consultas — o filtro `pacienteId` é forçado ao próprio id, e
tentar acessar a consulta de outro paciente resulta em `403`.

### REST — notification-service (`:8082`)

| Método | Rota                       | Papéis                        | Descrição                              |
|--------|------------------------------|--------------------------------|------------------------------------------|
| GET    | `/notificacoes?pacienteId=`  | MEDICO, ENFERMEIRO, PACIENTE*  | Lista os lembretes já enviados          |

### GraphQL — history-service (`:8083/graphql`)

```graphql
type Consulta {
    id: ID!
    pacienteId: ID!
    medicoId: ID!
    dataHora: String!
    status: String!
    atualizadoEm: String!
}

type Query {
    consultasPorPaciente(pacienteId: ID!): [Consulta!]!
    consultasFuturasPorPaciente(pacienteId: ID!): [Consulta!]!
    minhasConsultas: [Consulta!]!            # restrito a PACIENTE
    minhasConsultasFuturas: [Consulta!]!     # restrito a PACIENTE
}
```

Exemplo de consulta (com header `Authorization: Bearer <token>`):

```graphql
query {
  consultasPorPaciente(pacienteId: "9f9027d4-2499-4c27-9a5a-a34a95d3bdd4") {
    id
    status
    dataHora
  }
}
```

Uma collection do Postman com todos os fluxos (login, criação/edição de consulta, consulta de
notificações, queries GraphQL, e os casos de erro de autorização) está em `postman/`.

## Testes

Cada módulo tem sua própria suíte de testes unitários (JUnit 5 + Mockito + AssertJ), cobrindo
toda a lógica de negócio: regras de autorização e posse, publicação/consumo de eventos,
idempotência, emissão e validação de JWT, e o tratamento de erros. Os testes não sobem contexto
Spring nem dependem de banco/broker reais — dependências são mockadas, o que os torna rápidos e
determinísticos.

```bash
./mvnw test
```

## Estrutura do repositório

```
tech-challenge-fase03/
├── common/
│   ├── common-events/        # contrato do evento de integração + topologia RabbitMQ
│   └── common-security/      # JWT: emissão, validação, filtro de autenticação
├── scheduling-service/       # usuários, consultas, login, publica eventos
├── notification-service/     # consome eventos, envia (simula) lembretes
├── history-service/          # consome eventos, expõe histórico via GraphQL
├── docker/                   # script de inicialização do Postgres (múltiplos bancos)
├── postman/                  # collection para testar a API manualmente
├── docker-compose.yml
├── Dockerfile                 # multi-stage, um target por serviço
└── pom.xml                    # POM agregador (multi-módulo Maven)
```
