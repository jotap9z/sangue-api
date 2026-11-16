# Sangue+ — API

API do Sangue+, escrita em Kotlin com Spring Boot e PostgreSQL. O [aplicativo Android](https://github.com/jotap9z/Sangue-) usa esta API para cadastro, login, agendamento, check-in, atendimento e gestão do hemocentro.

> **Instalação em uma máquina nova:** comece por este guia para configurar o PostgreSQL, aplicar os scripts SQL e iniciar a API. Depois de confirmar que o servidor está respondendo, siga o [guia do Android](https://github.com/jotap9z/Sangue-/blob/main/README.md) e mantenha a API rodando durante os testes.

## 1. Preparar a máquina

- Instale Git, um **JDK 25** e PostgreSQL com o cliente `psql` ou pgAdmin.
- Configure o `JAVA_HOME` para o JDK 25 e coloque a pasta `bin` desse JDK no `PATH`. Confira com `java -version`. O Gradle deste repositório pede JDK 25 para o daemon; o código da API tem Java 17 como alvo.
- Use o **Gradle Wrapper** do projeto. A primeira execução precisa de acesso à internet para baixar o Gradle e as dependências.

```sh
git clone https://github.com/jotap9z/sangue-api.git
cd sangue-api
```

## 2. Configurar o PostgreSQL

1. Inicie o serviço PostgreSQL na máquina onde vai rodar a API. A porta padrão é **5432**.
2. Crie um banco chamado `sangue`. No pgAdmin, conecte ao servidor PostgreSQL local e escolha **Databases → Create → Database**, informe `sangue` e salve. No terminal, se o cliente estiver no PATH, a alternativa é:

   ```sh
   createdb -h localhost -p 5432 -U postgres -W sangue
   ```

3. Configure as variáveis abaixo no **mesmo terminal** em que vai iniciar a API. Substitua o texto de exemplo pela senha do PostgreSQL desta máquina. Se usar outro usuário, ele precisa poder criar e alterar as tabelas no schema `public`.

**Windows (PowerShell):**

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/sangue'
$env:DB_USERNAME = 'postgres'
$env:DB_PASSWORD = 'SUBSTITUA_PELA_SENHA_LOCAL'
$env:PORT = '8080'
```

**Linux/macOS (Bash/Zsh):**

```bash
export DB_URL='jdbc:postgresql://localhost:5432/sangue'
export DB_USERNAME='postgres'
export DB_PASSWORD='SUBSTITUA_PELA_SENHA_LOCAL'
export PORT='8080'
```

`localhost` na URL JDBC aponta para o ambiente em que **a API** está rodando. Se API e PostgreSQL estiverem na mesma máquina, essa URL pode ser igual em todas as instalações; o usuário, a senha e os dados do banco são locais. Use a porta real do serviço PostgreSQL se ela for diferente de 5432. Não coloque a senha real no README nem no Git.

## 3. Inicializar as tabelas e aplicar o SQL

Para um **banco novo e vazio**, execute a API pela primeira vez. O Hibernate está configurado com `ddl-auto=update` e cria as tabelas mapeadas no código. Espere o log de inicialização do Spring Boot (linha que começa com `Started`) e pare com **Ctrl+C** antes de rodar o SQL.

| Sistema | Comando na raiz de `sangue-api` |
| --- | --- |
| Windows (PowerShell) | `.\gradlew.bat bootRun` |
| Linux/macOS | `bash ./gradlew bootRun` |

Aplique os arquivos abaixo no **banco `sangue`**, nesta ordem:

1. `sql/002_completar_status_atendimento.sql`
2. `sql/003_administrador_principal_unico.sql`
3. `sql/004_recursos_complementares.sql`

O script `002` substitui a restrição de status criada pelo `001` e inclui todos os estados atuais. O `001` é histórico e não precisa ser executado neste procedimento. O `004` cria, entre outras estruturas, as tabelas de chat, notificações e recuperação de senha; elas não são todas criadas pelo Hibernate.

No pgAdmin, selecione o banco `sangue`, abra o **Query Tool** e execute cada arquivo. Com `psql` no PATH, rode a partir da raiz do projeto e informe a senha quando for solicitada:

```sh
psql -h localhost -p 5432 -U postgres -d sangue -v ON_ERROR_STOP=1 -f sql/002_completar_status_atendimento.sql
psql -h localhost -p 5432 -U postgres -d sangue -v ON_ERROR_STOP=1 -f sql/003_administrador_principal_unico.sql
psql -h localhost -p 5432 -U postgres -d sangue -v ON_ERROR_STOP=1 -f sql/004_recursos_complementares.sql
```

Se for usar **uma base existente**, faça uma cópia antes. Em um banco novo nesta máquina, restaure o backup antes de aplicar os scripts que faltarem. Confira se as tabelas e colunas de base existem; se o backup for de uma versão antiga, use o Hibernate para atualizá-las na cópia de teste antes dos scripts. Resolva qualquer erro do SQL antes de prosseguir. Para uma instalação limpa, não é necessário importar dados de outra máquina.

## 4. Iniciar e verificar a API

Rode novamente o comando `bootRun` da seção anterior e deixe o terminal aberto. Em outro terminal:

**Windows (PowerShell):**

```powershell
curl.exe -i http://localhost:8080/api/auth/me
```

**Linux/macOS:**

```bash
curl -i http://localhost:8080/api/auth/me
```

A resposta esperada **sem login** é `HTTP 401`, com a mensagem “Entre na sua conta para continuar.” Isso confirma que o servidor respondeu. Para testar o banco e o login juntos, inicie o [app Android](https://github.com/jotap9z/Sangue-), crie uma conta de teste e entre com ela. Um banco vazio não traz as contas, os hemocentros ou as vagas de outra instalação.

A aplicação está configurada para escutar em `0.0.0.0`, na porta **8080** por padrão. Para um celular na mesma rede acessar a API, use o IPv4 do computador no app e, se necessário, libere apenas a porta da API no firewall para a rede local confiável. O Android não usa a porta **5432** do PostgreSQL.

### Recuperação de senha por e-mail

O envio real de e-mail depende de um servidor SMTP. Configure `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM` e, conforme o provedor, `SMTP_AUTH`, `SMTP_STARTTLS` e `SMTP_SSL`. Cadastro, login e o fluxo de agendamento podem ser testados sem configurar o SMTP.

## Problemas comuns

| Sintoma | O que verificar |
| --- | --- |
| PostgreSQL: conexão recusada | Serviço iniciado, host e porta em `DB_URL`. |
| PostgreSQL: senha inválida ou banco inexistente | Senha do usuário **desta instalação**, usuário em `DB_USERNAME` e existência de `sangue`. |
| Erro de tabela inexistente em chat, notificações ou preferências | Execute o `004` no mesmo banco da API, depois de criar as tabelas de base. |
| Gradle não encontra o Java solicitado | Confira o JDK 25 e rode `gradlew --version` com o Wrapper apropriado ao sistema. |
| Porta 8080 já está em uso | Pare o processo conflitante ou altere `PORT`; use a nova porta também no teste HTTP e no app. |
| API responde no PC, mas não no celular | Confira a URL no app, a rede e o firewall. Veja no README do Android as alternativas por emulador e USB. |
