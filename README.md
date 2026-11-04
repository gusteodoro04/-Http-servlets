# Explorando o protocolo HTTP com Java Servlets

Tecnologias de Desenvolvimento para Internet III (IFSULDEMINAS, Campus Machado) — Prof. Dr. Emerson A. Carvalho
Atividade: **Fundamentos do HTTP** — entrega via GitHub até 04/11/2026
Aluno: Gustavo Teodoro

Cinco Servlets que exploram na prática os métodos HTTP, os cabeçalhos, os códigos de status e o corpo da requisição.

| # | Servlet | Método | URL | O que faz |
|---|---------|--------|-----|-----------|
| 1 | `HeadersServlet` | GET | `/headers` | Exibe os cabeçalhos da requisição, o IP do usuário e a versão do HTTP |
| 2 | `RequestBodyServlet` | POST | `/request-body` | Recebe o formulário (nome, email, mensagem) e exibe o corpo da requisição |
| 3 | `SessionServlet` | GET | `/session` | Grava um cookie com o usuário e, nas visitas seguintes, mostra "Bem-vindo de volta, usuário 'X'" |
| 4 | `StatusServlet` | GET | `/status?code=404` | Responde com o código de status indicado no parâmetro `code` |
| 5 | `MethodsServlet` | OPTIONS | `/methods` | Responde com os métodos HTTP suportados (cabeçalho `Allow` e lista no corpo) |

## Estrutura

```
http-servlets/
├── pom.xml
└── src/main
    ├── java/twii/http/
    │   ├── Html.java                 (escape de HTML e montagem das páginas)
    │   ├── HeadersServlet.java
    │   ├── RequestBodyServlet.java
    │   ├── SessionServlet.java
    │   ├── StatusServlet.java
    │   └── MethodsServlet.java
    └── webapp/
        └── index.html                (menu, formulário do Servlet 2 e botão do OPTIONS)
```

Os servlets usam `@WebServlet`, então **não precisa de `web.xml`**.

## Como rodar

Requisitos: JDK 11 ou superior e **Tomcat 10.1 ou 11** (a API usa o pacote `jakarta.servlet`; o Tomcat 9 não serve).

**Eclipse (Maven)**

1. `File > Import > Maven > Existing Maven Projects` e escolha a pasta do projeto.
2. Botão direito no projeto > `Run As > Run on Server` e escolha o seu Tomcat.
3. Abra `http://localhost:8080/http-servlets/` (o contexto é o nome do projeto no Eclipse).

**Linha de comando**

```
mvn clean package
```

Copie `target/http-servlets.war` para a pasta `webapps` do Tomcat, inicie o Tomcat e abra `http://localhost:8080/http-servlets/`.

**Num projeto Eclipse que você já tem (Dynamic Web Project)**

Copie a pasta `twii/http` (os 6 arquivos `.java`) para `src/main/java` e o `index.html` para `src/main/webapp`.

## Como testar

No navegador, use o menu da página inicial. Pela linha de comando (no PowerShell do Windows use `curl.exe`):

```
curl -i http://localhost:8080/http-servlets/headers
curl -i -X POST -d "nome=Ana&email=ana%40exemplo.com&mensagem=Ola+mundo" http://localhost:8080/http-servlets/request-body
curl -i -c cookies.txt -b cookies.txt http://localhost:8080/http-servlets/session
curl -i "http://localhost:8080/http-servlets/status?code=404"
curl -i -X OPTIONS http://localhost:8080/http-servlets/methods
```

- Rode o comando do `/session` duas vezes: na primeira aparece "Primeira visita" e na segunda "Bem-vindo de volta, usuário 'X'". Para recomeçar, apague o `cookies.txt` ou abra `/session?acao=sair`.
- O navegador só envia `OPTIONS` por script. Na página inicial, o botão "Enviar OPTIONS /methods" faz isso com `fetch`.
- Em `/status`, o navegador mostra o corpo da página, mas o código de status aparece na aba **Rede** (F12) ou na primeira linha da resposta do `curl -i`.

## Decisões de implementação

- **Escape de HTML (`Html.escape`)**: tudo que vem do cliente (cabeçalhos, campos, cookies) é escapado antes de entrar na página. Sem isso, um `User-Agent` ou uma mensagem com `<script>` seria executado pelo navegador (XSS).
- **Servlet 2 lê o corpo bruto primeiro**: o corpo só pode ser lido uma vez e `getParameter()` o consome. Por isso o servlet lê os bytes com `getInputStream()` antes, mostra o corpo como chegou (`nome=Ana&email=ana%40...`) e depois decodifica os campos (`+` e `%XX`) por conta própria. Corpos acima de 100 000 bytes recebem 413, e `%XX` inválido recebe 400.
- **Servlet 3 usa cookie e sessão**: o cookie `usuario` guarda a identificação (30 dias, `HttpOnly`) e o valor é codificado em URL, porque cookie não aceita espaço, `;` nem acentos. O `HttpSession` (cookie `JSESSIONID`) conta as visitas da sessão. Use `?nome=Ana` para escolher o nome; sem ele o servlet gera `visitante-xxxxxxxx`.
- **Servlet 4 usa `setStatus`, não `sendError`**: assim a página explica o código. Os cabeçalhos que o protocolo exige vão junto: `Location` nos 3xx, `WWW-Authenticate` no 401, `Allow` no 405 e `Retry-After` no 429/503. Os códigos 204 e 304 saem sem corpo. Só aceita 200 a 599; fora disso, 400.
- **Servlet 5 sobrescreve `doOptions`**: devolve 200, o cabeçalho `Allow` e a lista de métodos de cada URL da aplicação em texto puro. O `doGet` existe só para explicar como testar quando alguém abre `/methods` na barra de endereço.

## Entrega no GitHub

Crie um repositório vazio no GitHub e, na pasta do projeto:

```
git init
git add .
git commit -m "Atividade HTTP com Servlets"
git branch -M main
git remote add origin https://github.com/gusteodoro04/-Http-servlets.git
git push -u origin main
```
