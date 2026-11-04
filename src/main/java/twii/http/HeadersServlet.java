package twii.http;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet 1 - Análise de cabeçalhos (GET /headers).
 *
 * Mostra tudo o que o agente (navegador, curl, Postman...) enviou na requisição HTTP:
 * linha da requisição, cabeçalhos, IP de origem e versão do protocolo.
 */
@WebServlet(name = "HeadersServlet", urlPatterns = "/headers")
public class HeadersServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** Cabeçalhos que o enunciado pede em destaque: {nome do cabeçalho, descrição}. */
    private static final String[][] PRINCIPAIS = {
        {"User-Agent", "Navegador"},
        {"Referer", "Página de origem"},
        {"Accept-Language", "Linguagem preferida"},
        {"Accept-Encoding", "Codificações aceitas"},
        {"Host", "Servidor de destino"},
        {"Accept", "Tipos de conteúdo aceitos"},
        {"Connection", "Controle da conexão"},
    };

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Cabeçalhos de RESPOSTA definidos por este servlet (veja na aba "Rede" do navegador).
        resp.setHeader("X-Servlet", "HeadersServlet");
        resp.setHeader("Cache-Control", "no-store");

        StringBuilder html = new StringBuilder();

        html.append("<h2>Linha da requisição</h2>\n<table>\n");
        html.append(Html.linha("Método HTTP", req.getMethod()));
        html.append(Html.linha("URL", urlCompleta(req)));
        html.append(Html.linha("Versão do HTTP", req.getProtocol()));
        html.append(Html.linha("Esquema", req.getScheme()));
        html.append("</table>\n");

        html.append("<h2>Cabeçalhos em destaque</h2>\n<table>\n");
        for (String[] item : PRINCIPAIS) {
            html.append(Html.linha(item[0] + " (" + item[1] + ")",
                    Html.ou(req.getHeader(item[0]), "(não enviado)")));
        }
        html.append("</table>\n");

        html.append("<h2>IP e conexão</h2>\n<table>\n");
        html.append(Html.linha("IP do usuário", req.getRemoteAddr()));
        html.append(Html.linha("Porta de origem", String.valueOf(req.getRemotePort())));
        String encaminhado = req.getHeader("X-Forwarded-For");
        if (encaminhado != null) {
            html.append(Html.linha("X-Forwarded-For (IP original, quando há proxy)", encaminhado));
        }
        html.append(Html.linha("Servidor", req.getServerName() + ":" + req.getServerPort()));
        html.append(Html.linha("IP e porta locais", req.getLocalAddr() + ":" + req.getLocalPort()));
        html.append(Html.linha("Software do servidor", getServletContext().getServerInfo()));
        html.append("</table>\n");

        html.append("<h2>Todos os cabeçalhos recebidos</h2>\n<table>\n");
        Enumeration<String> nomes = req.getHeaderNames();
        if (nomes != null) {
            for (String nome : Collections.list(nomes)) {
                // Um mesmo cabeçalho pode chegar mais de uma vez: junta os valores.
                html.append(Html.linha(nome, String.join(", ", Collections.list(req.getHeaders(nome)))));
            }
        }
        html.append("</table>\n");

        html.append("<h2>Cabeçalhos de resposta definidos por este servlet</h2>\n<table>\n");
        for (String nome : resp.getHeaderNames()) {
            html.append(Html.linha(nome, resp.getHeader(nome)));
        }
        html.append("</table>\n");
        html.append("<p class=\"nota\">O servidor ainda acrescenta outros cabeçalhos na hora de enviar "
                  + "(Date, Content-Length ou Transfer-Encoding...). Para ver a resposta completa use "
                  + "<code>curl -i</code> ou a aba Rede (F12) do navegador.</p>\n");

        Html.enviar(resp, HttpServletResponse.SC_OK,
                "Servlet 1 - Cabeçalhos da requisição (GET /headers)", html.toString());
    }

    private static String urlCompleta(HttpServletRequest req) {
        String url = req.getRequestURL().toString();
        String consulta = req.getQueryString();
        return consulta == null ? url : url + "?" + consulta;
    }
}
