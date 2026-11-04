package twii.http;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Funções de apoio para montar as páginas HTML devolvidas pelos servlets.
 *
 * Tudo que vem do cliente (cabeçalhos, parâmetros, corpo, cookies) precisa passar por
 * {@link #escape(String)} antes de entrar no HTML. Sem isso, um User-Agent ou um campo
 * de formulário contendo "&lt;script&gt;" seria executado pelo navegador (ataque XSS).
 */
final class Html {

    private static final String CSS =
              "body{font-family:system-ui,sans-serif;max-width:900px;margin:2rem auto;padding:0 1rem;line-height:1.5;color:#1f2328}"
            + "h1{font-size:1.45rem}h2{font-size:1.1rem;margin-top:1.6rem}"
            + "table{border-collapse:collapse;width:100%;margin:.5rem 0}"
            + "th,td{border:1px solid #d1d9e0;padding:.35rem .6rem;text-align:left;vertical-align:top;overflow-wrap:anywhere}"
            + "th{background:#f6f8fa;width:32%}td{white-space:pre-wrap}"
            + "pre{background:#f6f8fa;padding:.75rem;overflow:auto;white-space:pre-wrap;overflow-wrap:anywhere}"
            + "code{background:#f6f8fa;padding:0 .25rem}.nota{color:#59636e}";

    private Html() {
    }

    /** Troca os caracteres especiais do HTML pelas entidades correspondentes. */
    static String escape(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(texto.length() + 16);
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '&':  sb.append("&amp;");  break;
                case '<':  sb.append("&lt;");   break;
                case '>':  sb.append("&gt;");   break;
                case '"':  sb.append("&quot;"); break;
                case '\'': sb.append("&#39;");  break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Devolve o valor, ou o texto padrão quando ele é nulo. */
    static String ou(String valor, String padrao) {
        return valor != null ? valor : padrao;
    }

    /** Linha de tabela "nome | valor" (os dois lados são escapados). */
    static String linha(String nome, String valor) {
        return "<tr><th>" + escape(nome) + "</th><td>" + escape(valor) + "</td></tr>\n";
    }

    /** Página HTML completa. O conteúdo já deve vir escapado pelo chamador. */
    static String pagina(String titulo, String conteudo) {
        return "<!DOCTYPE html>\n"
             + "<html lang=\"pt-BR\">\n<head>\n"
             + "<meta charset=\"UTF-8\">\n"
             + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
             + "<title>" + escape(titulo) + "</title>\n"
             + "<style>" + CSS + "</style>\n"
             + "</head>\n<body>\n"
             + "<h1>" + escape(titulo) + "</h1>\n"
             + conteudo
             + "<p><a href=\"./\">&larr; Voltar ao menu</a></p>\n"
             + "</body>\n</html>\n";
    }

    /** Define o status, o tipo de conteúdo e escreve a página na resposta. */
    static void enviar(HttpServletResponse resp, int status, String titulo, String conteudo)
            throws IOException {
        resp.setStatus(status);
        resp.setContentType("text/html;charset=UTF-8");
        resp.getWriter().write(pagina(titulo, conteudo));
    }
}
