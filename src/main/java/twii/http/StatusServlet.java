package twii.http;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet 4 - Testes com status HTTP (GET /status?code=404).
 *
 * Responde com o código de status pedido no parâmetro "code". Sem o parâmetro, mostra a
 * lista de códigos de exemplo.
 */
@WebServlet(name = "StatusServlet", urlPatterns = "/status")
public class StatusServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** Códigos de exemplo: código -> {nome padrão, explicação}. */
    private static final Map<Integer, String[]> CODIGOS = new LinkedHashMap<>();

    static {
        registrar(200, "OK", "Resposta normal: a requisição foi atendida com sucesso.");
        registrar(201, "Created", "Um recurso novo foi criado (típico de um POST bem-sucedido).");
        registrar(204, "No Content", "Deu certo, mas a resposta não tem corpo.");
        registrar(301, "Moved Permanently", "O recurso mudou de endereço para sempre; o novo endereço vem no cabeçalho Location.");
        registrar(302, "Found", "Redirecionamento temporário; o destino vem no cabeçalho Location.");
        registrar(304, "Not Modified", "A cópia em cache do cliente continua válida; a resposta não tem corpo.");
        registrar(400, "Bad Request", "O servidor não entendeu a requisição (sintaxe ou parâmetros inválidos).");
        registrar(401, "Unauthorized", "É preciso se autenticar; o cabeçalho WWW-Authenticate diz como.");
        registrar(403, "Forbidden", "O servidor entendeu, mas recusa atender: o usuário não tem permissão.");
        registrar(404, "Not Found", "Página não encontrada: não existe recurso neste endereço.");
        registrar(405, "Method Not Allowed", "O método HTTP usado não é aceito para este recurso; o cabeçalho Allow lista os aceitos.");
        registrar(418, "I'm a teapot", "Piada do RFC 2324: o servidor é um bule de chá e se recusa a fazer café.");
        registrar(429, "Too Many Requests", "O cliente mandou requisições demais; Retry-After diz quando tentar de novo.");
        registrar(500, "Internal Server Error", "Erro interno: algo deu errado no servidor.");
        registrar(502, "Bad Gateway", "Um servidor intermediário recebeu uma resposta inválida do servidor de origem.");
        registrar(503, "Service Unavailable", "Servidor temporariamente indisponível (sobrecarga ou manutenção).");
        registrar(504, "Gateway Timeout", "Um servidor intermediário não recebeu resposta do servidor de origem a tempo.");
    }

    private static void registrar(int codigo, String nome, String explicacao) {
        CODIGOS.put(codigo, new String[] {nome, explicacao});
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setHeader("Cache-Control", "no-store");

        String parametro = req.getParameter("code");
        if (parametro == null || parametro.isBlank()) {
            mostrarMenu(resp);
            return;
        }

        int codigo = converter(parametro);
        if (codigo < 200 || codigo > 599) {
            // 1xx são respostas provisórias (não finais), então só aceitamos de 200 a 599.
            Html.enviar(resp, HttpServletResponse.SC_BAD_REQUEST, "Parâmetro code inválido",
                    "<p>O valor <code>" + Html.escape(parametro) + "</code> não serve. Use um número de "
                  + "200 a 599, por exemplo <code>status?code=404</code>.</p>\n"
                  + "<p><a href=\"status\">Ver a lista de códigos</a></p>\n");
            return;
        }

        responder(req, resp, codigo);
    }

    /** Envia a resposta com o status pedido (e os cabeçalhos que o protocolo exige para ele). */
    private void responder(HttpServletRequest req, HttpServletResponse resp, int codigo)
            throws IOException {

        // Aqui está o ponto central do servlet: setStatus define a linha de status da resposta.
        resp.setStatus(codigo);

        String destino = req.getContextPath() + "/status?code=200";
        String extra = "(nenhum)";

        switch (codigo) {
            case 301: case 302: case 303: case 307: case 308:
                resp.setHeader("Location", destino);
                extra = "Location: " + destino;
                break;
            case 401:
                // Bearer (e não Basic) para o navegador não abrir a janela de login.
                resp.setHeader("WWW-Authenticate", "Bearer realm=\"status-teste\"");
                extra = "WWW-Authenticate: Bearer realm=\"status-teste\"";
                break;
            case 405:
                resp.setHeader("Allow", "POST");
                extra = "Allow: POST";
                break;
            case 429: case 503:
                resp.setHeader("Retry-After", "30");
                extra = "Retry-After: 30";
                break;
            default:
                break;
        }

        // 204, 205 e 304 não podem ter corpo.
        if (codigo == 204 || codigo == 205 || codigo == 304) {
            return;
        }

        String[] info = CODIGOS.get(codigo);
        String nome = info != null ? info[0] : "(fora da lista de exemplos)";
        String explicacao = info != null ? info[1] : "Código sem texto de exemplo neste servlet.";

        StringBuilder html = new StringBuilder();
        html.append("<p>").append(Html.escape(explicacao)).append("</p>\n<table>\n");
        html.append(Html.linha("Código de status", String.valueOf(codigo)));
        html.append(Html.linha("Nome padrão", nome));
        html.append(Html.linha("Classe", classe(codigo)));
        html.append(Html.linha("Cabeçalho extra enviado", extra));
        html.append("</table>\n");
        html.append("<p class=\"nota\">Para ver a linha de status da resposta (<code>HTTP/1.1 ")
            .append(codigo)
            .append("</code>) abra a aba Rede (F12) do navegador ou use <code>curl -i \"...status?code=")
            .append(codigo)
            .append("\"</code>. Nos redirecionamentos (3xx) o navegador segue o Location sozinho.</p>\n");
        html.append("<p><a href=\"status\">Ver a lista de códigos</a></p>\n");

        Html.enviar(resp, codigo,
                "Servlet 4 - Status HTTP " + codigo + (info != null ? " " + info[0] : ""),
                html.toString());
    }

    private void mostrarMenu(HttpServletResponse resp) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<p>Informe o código desejado no parâmetro <code>code</code> da URL, por exemplo "
                  + "<code>status?code=404</code>, e o servidor responde com aquele status HTTP.</p>\n");
        html.append("<table>\n");
        for (Map.Entry<Integer, String[]> item : CODIGOS.entrySet()) {
            int codigo = item.getKey();
            String[] info = item.getValue();
            html.append("<tr><th><a href=\"status?code=").append(codigo).append("\">")
                .append(codigo).append(" ").append(Html.escape(info[0])).append("</a></th><td>")
                .append(Html.escape(info[1])).append("</td></tr>\n");
        }
        html.append("</table>\n");
        html.append("<form method=\"get\" action=\"status\">\n"
                  + "<label>Outro código (200 a 599): "
                  + "<input type=\"number\" name=\"code\" min=\"200\" max=\"599\" required></label>\n"
                  + "<button type=\"submit\">Enviar</button>\n</form>\n");

        Html.enviar(resp, HttpServletResponse.SC_OK,
                "Servlet 4 - Testes com status HTTP (GET /status)", html.toString());
    }

    private static int converter(String texto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String classe(int codigo) {
        switch (codigo / 100) {
            case 2:  return "2xx - Sucesso";
            case 3:  return "3xx - Redirecionamento";
            case 4:  return "4xx - Erro do cliente";
            default: return "5xx - Erro do servidor";
        }
    }
}
