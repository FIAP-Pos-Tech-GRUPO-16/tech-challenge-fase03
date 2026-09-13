# Sistema Hospitalar — Tech Challenge Fase 03

Este documento explica o que este projeto faz, como ele foi construído e como qualquer pessoa
pode colocá-lo para rodar na própria máquina — mesmo sem nenhuma experiência técnica prévia com
o projeto. Sempre que um termo mais técnico aparecer, ele vem acompanhado de uma explicação em
linguagem simples.

## O que é este projeto

É o backend (a parte de "bastidores", que não tem tela, só recebe e responde pedidos) de um
sistema para uso dentro de um hospital. Ele resolve três problemas:

1. **Agendar consultas** — médicos e enfermeiros podem marcar e alterar consultas de pacientes.
2. **Avisar o paciente** — sempre que uma consulta é marcada ou alterada, o paciente recebe
   automaticamente um lembrete (aqui, simulado: o sistema registra que "enviaria" o lembrete e
   grava isso no banco de dados, no lugar de realmente disparar um e-mail ou SMS).
3. **Guardar o histórico** — é possível consultar todo o histórico de consultas de um paciente,
   ou só as que ainda vão acontecer.

O sistema tem três tipos de usuário, cada um enxergando e podendo fazer coisas diferentes:

- **Médico**: vê e edita qualquer consulta.
- **Enfermeiro**: registra novas consultas e também consegue ver o histórico.
- **Paciente**: só enxerga as próprias consultas — nunca as de outra pessoa.

Este projeto foi feito para o Tech Challenge da Fase 3 da Pós Tech (curso de Arquitetura e
Desenvolvimento Java), cujo enunciado pedia justamente um sistema assim: seguro, dividido em
partes independentes, e que se comunicasse de forma assíncrona (explico o que isso significa
mais abaixo).

## Como o sistema é organizado

Em vez de construir "um programa só" fazendo tudo, o projeto foi dividido em **três aplicações
independentes**, cada uma responsável por uma parte do problema. Pense nelas como três
funcionários de um hospital que fazem tarefas diferentes e se avisam quando algo importante
acontece, mas cada um trabalha na sua própria sala, com seus próprios arquivos:

- **Agendamento** (`scheduling-service`) — cuida do cadastro de usuários, do login, e de criar e
  editar as consultas. É "o dono" do dado da consulta.
- **Notificações** (`notification-service`) — fica "de olho" esperando avisos de que uma consulta
  foi criada ou alterada, e simula o envio do lembrete para o paciente.
- **Histórico** (`history-service`) — também fica de olho nesses mesmos avisos, e mantém uma
  cópia organizada do histórico de consultas, pronta para ser consultada de formas flexíveis.

```
                     "Oi, marquei uma consulta!"
                                │
                                ▼
                      ┌───────────────────┐
                      │    Agendamento     │  ← médicos, enfermeiros e pacientes
                      │                    │    conversam com este daqui
                      └─────────┬──────────┘
                                │
                     deixa um "recado" no correio
                                │
                                ▼
                      ┌───────────────────┐
                      │   Correio (fila de │
                      │  mensagens/RabbitMQ)│
                      └─────────┬──────────┘
                       ┌────────┴────────┐
                       ▼                 ▼
              ┌────────────────┐ ┌────────────────┐
              │  Notificações   │ │   Histórico     │
              │ (envia lembrete)│ │ (guarda o       │
              │                 │ │  histórico)     │
              └────────────────┘ └────────────────┘
```

Por que não juntar tudo em uma coisa só? Porque o enunciado do desafio pedia explicitamente que
o agendamento e as notificações fossem serviços separados, se comunicando de forma assíncrona —
e decidimos manter o histórico separado também, para deixar cada parte do sistema com uma
responsabilidade bem definida. Assim, se um dia o serviço de notificações precisar de manutenção
ou cair, o resto do sistema continua funcionando normalmente — só o envio de lembretes fica em
espera até ele voltar.

**"Comunicação assíncrona"**, em palavras simples: quando o agendamento cria uma consulta, ele
não fica esperando o serviço de notificações confirmar "ok, recebi, vou avisar o paciente". Ele
só deixa um recado num "correio" (chamado tecnicamente de fila de mensagens) e segue em frente,
respondendo rapidamente para quem pediu a consulta. O serviço de notificações lê esse recado
quando conseguir, no seu próprio tempo. Isso é diferente de uma ligação telefônica (onde as duas
pessoas precisam estar na linha ao mesmo tempo) — é mais parecido com deixar um recado por
mensagem de texto.

Cada uma das três aplicações tem seu próprio banco de dados — nenhuma delas nunca lê ou escreve
diretamente no banco de outra. A única forma de uma "conversar" com a outra é através desses
recados no correio. Isso evita que uma mudança em um serviço quebre outro por acidente.

## Tecnologias usadas (e por quê)

| Tecnologia | Para que serve aqui | Por que essa e não outra |
|---|---|---|
| **Java 21** | Linguagem de programação usada em tudo | Pedida pelo curso e pelo desafio |
| **Spring Boot** | Framework que facilita construir uma aplicação Java que recebe pedidos pela internet | É o mais usado no mercado para esse tipo de aplicação, e o pedido pelo desafio |
| **Spring Security** | Cuida de quem pode entrar no sistema e o que cada um pode fazer | Exigido pelo desafio |
| **JWT** (JSON Web Token) | Um "crachá digital": depois que a pessoa faz login, ela recebe esse token e o usa para provar quem é em cada pedido seguinte, sem digitar a senha de novo | Explicado com mais detalhe na seção de Segurança, abaixo |
| **PostgreSQL** | Banco de dados onde as informações ficam guardadas de verdade (mesmo se a aplicação reiniciar) | Banco de dados robusto, gratuito e muito usado no mercado |
| **RabbitMQ** | O "correio" que carrega os recados entre os três serviços | Pedido pelo desafio (a alternativa seria o Kafka; explico a escolha mais abaixo) |
| **GraphQL** | Uma forma de consultar dados em que quem pergunta escolhe exatamente quais informações quer receber, sem precisar de vários endpoints diferentes | Pedido pelo desafio, usado no serviço de Histórico |
| **Docker / Docker Compose** | Empacota cada aplicação (e o banco de dados e o RabbitMQ) dentro de "caixinhas" prontas, que rodam iguais em qualquer computador | Permite rodar tudo sem instalar Java, Maven, Postgres etc. na sua máquina — só o Docker |
| **Maven** | Ferramenta que organiza o código Java e baixa as bibliotecas de que ele precisa | Padrão de mercado para projetos Java |
| **Swagger / OpenAPI** | Gera uma página interativa onde dá pra ver e testar cada rota da API, direto do navegador | Facilita testar e entender a API sem precisar de ferramentas extras |
| **JUnit + Mockito** | Ferramentas para escrever testes automatizados, que verificam se o código continua funcionando corretamente | Padrão de mercado para testes em Java |

## Decisões técnicas explicadas

Aqui explicamos as principais escolhas feitas durante o desenvolvimento, e por quê.

### Por que três aplicações separadas, e não uma só?

O enunciado do desafio pede, no mínimo, duas: uma de agendamento e uma de notificações. A
terceira (histórico) era descrita como opcional — poderia ter ficado dentro da aplicação de
agendamento. Optamos por deixá-la separada também, para reforçar a ideia de que cada parte do
sistema cuida de uma coisa só, e porque isso deixa mais claro, no código, como um sistema real de
hospital (com várias equipes trabalhando em partes diferentes) costuma ser organizado.

### Por que RabbitMQ, e não Kafka?

O desafio permitia usar qualquer um dos dois para o "correio" entre os serviços. O RabbitMQ
combina melhor com o que estamos fazendo aqui: avisar um paciente específico sobre a consulta
dele, um recado de cada vez. O Kafka é mais indicado quando se precisa guardar um histórico
gigante de mensagens para reprocessar depois, ou quando o volume é muito maior — o que não é o
caso deste desafio. Usar o Kafka aqui seria como alugar um caminhão para levar uma sacola de
compras: funcionaria, mas com mais complexidade do que o necessário.

### Por que login com token (JWT), e não com senha em toda requisição?

O desafio pedia "autenticação básica com Spring Security". Existe uma forma bem literal de fazer
isso (chamada HTTP Basic, onde o navegador pede usuário e senha em toda requisição), mas como
temos três aplicações separadas, seria estranho pedir para o usuário digitar a senha de novo
toda vez que uma delas precisasse confirmar quem ele é. Por isso, escolhemos o modelo de token:
a pessoa faz login **uma vez** no serviço de Agendamento, recebe um token (o "crachá digital"
mencionado antes), e usa esse mesmo crachá para se identificar nos outros dois serviços também —
sem que eles precisem perguntar ao Agendamento "essa senha está certa?" a cada pedido. Isso deixa
os serviços mais independentes entre si.

O token contém, de forma criptografada, o papel do usuário (médico, enfermeiro ou paciente) e
expira depois de um tempo (120 minutos, por padrão) — depois disso, é preciso fazer login de
novo.

### Como o sistema decide quem pode fazer o quê

- Criar ou editar uma consulta: só médico ou enfermeiro.
- Ver qualquer consulta: médico ou enfermeiro.
- Ver uma consulta: um paciente só consegue ver as próprias — se tentar ver a de outra pessoa,
  o sistema recusa (retorna um erro "Forbidden", ou seja, "proibido").
- A mesma regra vale para notificações e para o histórico em GraphQL: um paciente nunca enxerga
  dado de outro paciente.

#### Do enunciado até o teste que garante a regra

Cada linha de permissão do enunciado, onde ela vive no código e qual teste quebra se alguém
remover a proteção:

| Enunciado | Onde é garantido | Teste que protege |
|---|---|---|
| Médicos podem visualizar e editar o histórico de consultas | `@PreAuthorize("hasAnyRole('MEDICO','ENFERMEIRO')")` em `PUT /consultas/{id}` | `ConsultaSecurityIntegrationTest#medicoPodeEditarConsulta` |
| Enfermeiros podem registrar consultas e acessar o histórico | `@PreAuthorize("hasAnyRole('MEDICO','ENFERMEIRO')")` em `POST /consultas` | `ConsultaSecurityIntegrationTest#enfermeiroPodeCriarConsulta` |
| Pacientes podem visualizar apenas as suas consultas | `@PreAuthorize` nos endpoints de leitura **+** checagem de posse em `ConsultaService#garantirAcesso` | `#pacienteNaoPodeVerConsultaDeOutroPaciente` e `#listagemDoPacienteIgnoraFiltroEDevolveSomenteAsProprias` |
| Paciente não registra nem edita consulta | ausência do papel `PACIENTE` no `@PreAuthorize` | `#pacienteNaoPodeCriarConsulta` e `#pacienteNaoPodeEditarConsulta` |
| Histórico respeita a mesma posse do dado | `HistoricoService#garantirAcesso` | `HistoricoGraphQlSchemaTest#pacienteNaoPodeConsultarHistoricoDeOutroPaciente` |
| Só quem tem token válido entra | `SecurityFilterChain` + `JwtAuthenticationFilter` | `#semTokenDeveRetornar401`, `#tokenDeOutroEmissorDeveRetornar401` |

#### Uma decisão de interpretação, dita explicitamente

O enunciado atribui o *editar* ao médico e o *registrar* ao enfermeiro. **Neste projeto, médico
e enfermeiro podem fazer as duas coisas.**

A escolha foi deliberada. O enunciado enumera o que cada perfil *pode* fazer, não declara
exclusividade entre os dois; e num fluxo hospitalar real o reagendamento de uma consulta é
rotina da enfermagem — restringir a edição ao médico criaria uma regra que nenhum hospital
teria. A separação que o enunciado de fato exige, e que está implementada e testada, é entre
**paciente e equipe clínica**: um paciente não cria, não edita, e não enxerga dado de ninguém
além dele mesmo.

### O que acontece se uma mensagem "se perder" ou chegar duas vezes?

Como o agendamento e os outros dois serviços se comunicam por recados assíncronos (e não por uma
"ligação" onde dá pra confirmar na hora), pensamos em dois problemas que podem acontecer de
verdade em qualquer sistema parecido com este:

1. **A mesma mensagem chegar duas vezes.** Isso pode acontecer porque o RabbitMQ garante que a
   mensagem "chegue pelo menos uma vez" — em caso de falha, ele prefere reenviar (o que pode
   gerar uma repetição) a arriscar perder a mensagem de vez. Para não gerar dois lembretes
   duplicados para a mesma consulta, o serviço de notificações guarda o identificador de cada
   mensagem já processada, e ignora qualquer repetição.
2. **Uma mensagem falhar ao ser processada.** Se der um erro passageiro (por exemplo, o banco de
   dados estar temporariamente indisponível), o sistema tenta de novo automaticamente até 3
   vezes, indicando um erro que possa ser passageiro. Se mesmo assim continuar falhando, a
   mensagem é movida para uma "fila de mensagens com problema" (chamada tecnicamente de dead
   letter queue), em vez de travar todas as mensagens que vieram depois dela.

### O lembrete só é enviado se a consulta realmente foi salva

O sistema só avisa o serviço de notificações depois que a consulta é gravada com sucesso no
banco de dados do agendamento — nunca antes. Assim, evitamos o cenário de um paciente receber um
lembrete de uma consulta que, por algum erro, nunca chegou a ser salva de verdade.

## Segurança: usuários de teste já vêm prontos

Para facilitar os testes, quatro usuários (um de cada papel, mais um segundo paciente) são
criados automaticamente na primeira vez que o serviço de Agendamento sobe — não é preciso
cadastrar ninguém manualmente:

| Usuário (login) | Papel | Senha |
|---|---|---|
| `medica.ana` | Médico | `Senha@123` |
| `enfermeiro.bruno` | Enfermeiro | `Senha@123` |
| `paciente.joao` | Paciente | `Senha@123` |
| `paciente.maria` | Paciente | `Senha@123` |

## Como rodar o projeto (só precisa do Docker)

Você **não precisa ter Java, Maven, PostgreSQL ou RabbitMQ instalados na sua máquina**. A única
coisa necessária é o [Docker](https://www.docker.com/products/docker-desktop/) instalado e
aberto. Todo o resto — compilar o código Java, baixar as bibliotecas, subir o banco de dados —
acontece dentro de containers Docker, isolado do seu computador.

1. Abra o Docker (Docker Desktop, no Windows/Mac, ou o serviço do Docker, no Linux).
2. Na pasta raiz do projeto, rode:

   ```bash
   docker compose up -d --build
   ```

   Na primeira vez, isso demora alguns minutos (ele baixa as imagens necessárias e compila as
   três aplicações). Nas próximas vezes é bem mais rápido.
3. Pronto — os três serviços, o banco de dados e o sistema de filas já estão rodando. Para
   conferir se subiu tudo certo:

   ```bash
   docker compose ps
   ```

   As quatro linhas (`postgres`, `rabbitmq`, `scheduling-service`, `notification-service`,
   `history-service`) devem aparecer com status "Up".

4. Para parar tudo:

   ```bash
   docker compose down
   ```

### Onde acessar cada coisa depois de subir

| O quê | Endereço | Para quê serve |
|---|---|---|
| Swagger — Agendamento | http://localhost:8081/swagger-ui.html | Testar login e as rotas de consulta pela tela, sem precisar de outra ferramenta |
| Swagger — Notificações | http://localhost:8082/swagger-ui.html | Testar a consulta de lembretes enviados |
| GraphiQL — Histórico | http://localhost:8083/graphiql | Tela interativa para testar as consultas GraphQL do histórico |
| RabbitMQ (painel de administração) | http://localhost:15672 | Ver as filas e mensagens passando (login: `guest` / senha: `guest`) |

**Como testar pelo Swagger, passo a passo:**

1. Abra http://localhost:8081/swagger-ui.html.
2. Abra a rota `POST /auth/login`, clique em "Try it out", cole um dos usuários da tabela acima
   (por exemplo `{"username": "enfermeiro.bruno", "password": "Senha@123"}`) e clique em
   "Execute".
3. Copie o valor do campo `"token"` que aparecer na resposta.
4. Clique no botão "Authorize" (no topo da página, com um cadeado), cole o token ali (sem
   escrever a palavra "Bearer" antes) e confirme.
5. Agora todas as outras rotas dessa página já vão usar esse token automaticamente — pode testar
   criar uma consulta, por exemplo.
6. O mesmo token funciona também no Swagger de Notificações (porta 8082) e no GraphiQL do
   Histórico (porta 8083, colando `Bearer <token>` no cabeçalho `Authorization`, na aba
   "Headers" do GraphiQL).

Também preparamos uma **collection do Postman** pronta (pasta `postman/`), que já faz login e
testa todos os fluxos automaticamente, incluindo os casos em que o sistema deve recusar o acesso.

## Os endpoints da API

### Agendamento — `http://localhost:8081`

| Método | Rota | Quem pode usar | O que faz |
|---|---|---|---|
| POST | `/auth/login` | qualquer pessoa | Faz login e devolve o token |
| POST | `/consultas` | médico, enfermeiro | Cria uma consulta |
| PUT | `/consultas/{id}` | médico, enfermeiro | Edita uma consulta |
| GET | `/consultas/{id}` | médico, enfermeiro, paciente* | Busca uma consulta pelo id |
| GET | `/consultas?pacienteId=` | médico, enfermeiro, paciente* | Lista consultas |

\* um paciente só vê as próprias consultas, mesmo que tente informar o id de outro paciente.

### Notificações — `http://localhost:8082`

| Método | Rota | Quem pode usar | O que faz |
|---|---|---|---|
| GET | `/notificacoes?pacienteId=` | médico, enfermeiro, paciente* | Lista os lembretes já enviados |

### Histórico (GraphQL) — `http://localhost:8083/graphql`

Diferente de uma API REST comum, no GraphQL existe uma única rota (`/graphql`), e quem consulta
escolhe exatamente quais informações quer receber. As consultas disponíveis são:

- `consultasPorPaciente(pacienteId)` — todas as consultas (passadas e futuras) de um paciente.
- `consultasFuturasPorPaciente(pacienteId)` — só as consultas futuras de um paciente.
- `minhasConsultas` — todas as consultas do paciente que fez login (só funciona para pacientes).
- `minhasConsultasFuturas` — o mesmo, mas só as futuras.

Exemplo de consulta (testável direto no GraphiQL, em http://localhost:8083/graphiql):

```graphql
query {
  consultasPorPaciente(pacienteId: "9f9027d4-2499-4c27-9a5a-a34a95d3bdd4") {
    id
    status
    dataHora
  }
}
```

## Testes automatizados

O projeto tem 91 testes automatizados, que verificam se cada regra do sistema continua
funcionando corretamente (por exemplo: "um paciente não consegue ver a consulta de outra
pessoa", ou "a mesma notificação nunca é enviada duas vezes"). Esses testes rodam sozinhos, sem
precisar do banco de dados nem do RabbitMQ ligados — o que os torna rápidos e confiáveis.

Para rodar os testes (isso já roda automaticamente todo `docker compose up --build`, mas também
dá pra rodar à parte se você tiver Java e Maven instalados):

```bash
./mvnw test
```

## Estrutura de pastas do projeto

```
tech-challenge-fase03/
├── common/
│   ├── common-events/        # o "idioma" comum que os serviços usam para trocar recados
│   └── common-security/      # como o token de login é criado e conferido
├── scheduling-service/       # serviço de Agendamento
├── notification-service/     # serviço de Notificações
├── history-service/          # serviço de Histórico (com GraphQL)
├── docker/                   # script que prepara os bancos de dados na primeira subida
├── postman/                  # collection para testar a API pelo Postman
├── docker-compose.yml        # descreve como subir tudo de uma vez
├── Dockerfile                # como cada serviço é empacotado
└── pom.xml                   # arquivo raiz do projeto Java (Maven)
```

## Variáveis de ambiente (para quem quiser customizar)

Ao rodar via `docker compose up`, tudo já vem configurado com valores padrão prontos para uso —
você não precisa mexer em nada. Estas variáveis existem caso você queira, por exemplo, apontar
para um banco de dados diferente:

| Variável | Para que serve | Valor padrão |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | Onde fica o banco de dados de cada serviço | já configurado no `docker-compose.yml` |
| `DB_USERNAME`, `DB_PASSWORD` | Credenciais do banco de dados | `hospital` / `hospital` |
| `RABBITMQ_HOST`, `RABBITMQ_PORT` | Onde fica o RabbitMQ | já configurado no `docker-compose.yml` |
| `SECURITY_JWT_SECRET` | Chave usada para gerar e conferir o token de login — precisa ser **igual** nos três serviços | um valor de exemplo, só para desenvolvimento |
| `SECURITY_JWT_EXPIRATION_MINUTES` | Por quantos minutos o token continua valendo | `120` |
