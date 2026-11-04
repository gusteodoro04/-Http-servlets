package twii.http;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet 2 - Manipulação do corpo da requisição (POST /request-body).
 *
 * Recebe o formulário da página inicial (nome, email e mensagem) e exibe o corpo da
 * requisição exatamente como chegou, mais os campos já decodificados.
 */
@WebServlet(name = "RequestBodyServlet", urlPatterns = "/request-body")
public class RequestBodyServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** Tamanho máximo do corpo aceito por este exemplo. */
    private static final int LIMITE_BYTES = 100_000;

    /** Campos esperados, na ordem da tela: {nome do campo, rótulo}. */
    private static final String[][] CAMPOS = {
        {"nome", "Nome"},
        {"email", "E-mail"},
        {"mensagem", "Mensagem"},
    };

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1) Lê o corpo BRUTO. Isso precisa vir antes de qualquer req.getParameter(): o
        //    contêiner só consegue ler o corpo uma vez, e getParameter() consome o corpo de
        //    formulários. Lendo primeiro, mostramos os bytes originais e decodificamos à mão.
        byte[] bytes = req.getInputStream().readNBytes(LIMITE_BYTES + 1);
        if (bytes.length > LIMITE_BYTES) {
            Html.enviar(resp, 413, "Corpo grande demais",
                    "<p>Este exemplo aceita no máximo " + LIMITE_BYTES + " bytes no corpo.</p>\n");
            return;
        }

        Charset charset = charsetDe(req);
        String corpo = new String(bytes, charset);
        String tipo = req.getContentType();
        boolean formulario = tipo != null
                && tipo.toLowerCase(Locale.ROOT).startsWith("application/x-www-form-urlencoded");

        // 2) Se for um formulário, separa "chave=valor&chave=valor" e decodifica (%XX e +).
        Map<String, List<String>> campos = null;
        if (formulario) {
            try {
                campos = decodificar(corpo, charset);
            } catch (IllegalArgumentException e) {
                Html.enviar(resp, HttpServletResponse.SC_BAD_REQUEST, "Corpo inválido",
                        "<p>O corpo não é um formulário URL-encoded válido: "
                        + Html.escape(e.getMessage()) + "</p>\n");
                return;
            }
        }

        // 3) Monta a página.
        StringBuilder html = new StringBuilder();

        html.append("<h2>Requisição recebida</h2>\n<table>\n");
        html.append(Html.linha("Método HTTP", req.getMethod()));
        html.append(Html.linha("URI", req.getRequestURI()));
        html.append(Html.linha("Versão do HTTP", req.getProtocol()));
        html.append(Html.linha("Content-Type", Html.ou(tipo, "(não enviado)")));
        html.append(Html.linha("Content-Length (cabeçalho)",
                Html.ou(req.getHeader("Content-Length"), "(não enviado)")));
        html.append(Html.linha("Bytes lidos do corpo", String.valueOf(bytes.length)));
        html.append(Html.linha("Charset usado na leitura", charset.name()));
        html.append("</table>\n");

        html.append("<h2>Corpo da requisição (bruto)</h2>\n<pre>")
            .append(corpo.isEmpty() ? "(vazio)" : Html.escape(corpo))
            .append("</pre>\n");

        html.append("<h2>Campos do formulário (decodificados)</h2>\n");
        if (campos == null) {
            html.append("<p class=\"nota\">O Content-Type não é "
                      + "<code>application/x-www-form-urlencoded</code>, então o corpo foi mostrado "
                      + "sem decodificação.</p>\n");
        } else {
            html.append("<table>\n");
            for (String[] c : CAMPOS) {
                List<String> valores = campos.remove(c[0]);
                html.append(Html.linha(c[0] + " (" + c[1] + ")",
                        valores == null ? "(não enviado)" : String.join(", ", valores)));
            }
            for (Map.Entry<String, List<String>> extra : campos.entrySet()) {
                html.append(Html.linha(extra.getKey() + " (campo extra)",
                        String.join(", ", extra.getValue())));
            }
            html.append("</table>\n");
            html.append("<p class=\"nota\">No formato URL-encoded o espaço vira <code>+</code> e os "
                      + "caracteres especiais viram <code>%XX</code> (por exemplo, "
                      + "<code>@</code> vira <code>%40</code>).</p>\n");
        }

        Html.enviar(resp, HttpServletResponse.SC_OK,
                "Servlet 2 - Corpo da requisição (POST /request-body)", html.toString());
    }

    /** Charset informado no Content-Type; se não houver (ou for inválido), UTF-8. */
    private static Charset charsetDe(HttpServletRequest req) {
        String nome = req.getCharacterEncoding();
        if (nome != null) {
            try {
                return Charset.forName(nome);
            } catch (IllegalArgumentException e) {
                // charset desconhecido: cai no padrão abaixo
            }
        }
        return StandardCharsets.UTF_8;
    }

    /** Transforma "a=1&b=2%403" em {a=[1], b=[2@3]}. Lança IllegalArgumentException se houver %XX inválido. */
    private static Map<String, List<String>> decodificar(String corpo, Charset charset) {
        Map<String, List<String>> campos = new LinkedHashMap<>();
        if (corpo.isEmpty()) {
            return campos;
        }
        for (String par : corpo.split("&")) {
            if (par.isEmpty()) {
                continue;
            }
            int igual = par.indexOf('=');
            String chave = URLDecoder.decode(igual < 0 ? par : par.substring(0, igual), charset);
            String valor = igual < 0 ? "" : URLDecoder.decode(par.substring(igual + 1), charset);
            campos.computeIfAbsent(chave, k -> new ArrayList<>()).add(valor);
        }
        return campos;
    }
}
