package twii.http;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet 5 - Métodos suportados (OPTIONS /methods).
 *
 * O método OPTIONS pergunta ao servidor o que ele aceita para um recurso. A resposta traz
 * o cabeçalho "Allow" com a lista de métodos e, no corpo, o mapa de métodos de cada URL
 * desta aplicação.
 *
 * Observação: o navegador só envia OPTIONS por script (fetch) ou ferramentas como o curl.
 * Ao abrir /methods na barra de endereço ele envia GET, por isso o doGet abaixo existe só
 * para explicar como testar.
 */
@WebServlet(name = "MethodsServlet", urlPatterns = "/methods")
public class MethodsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** Métodos aceitos por este recurso (/methods). */
    private static final String METODOS_DESTE_RECURSO = "OPTIONS, GET, HEAD";

    /** Mapa da aplicação: {URL, métodos aceitos}. Mantenha em sincronia com os servlets. */
    private static final String[][] ENDPOINTS = {
        {"/headers", "GET, HEAD, OPTIONS"},
        {"/request-body", "POST, OPTIONS"},
        {"/session", "GET, HEAD, OPTIONS"},
        {"/status", "GET, HEAD, OPTIONS"},
        {"/methods", METODOS_DESTE_RECURSO},
    };

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setHeader("Allow", METODOS_DESTE_RECURSO);   // o cabeçalho que o OPTIONS existe para devolver
        resp.setHeader("Cache-Control", "no-store");
        resp.setContentType("text/plain;charset=UTF-8");

        StringBuilder texto = new StringBuilder();
        texto.append("Métodos HTTP suportados\n\n");
        texto.append("Este recurso (").append(req.getContextPath()).append("/methods): ")
             .append(METODOS_DESTE_RECURSO).append("\n\n");
        texto.append("Todas as URLs da aplicação:\n");
        for (String[] endpoint : ENDPOINTS) {
            texto.append(String.format("  %-15s %s\n", endpoint[0], endpoint[1]));
        }
        resp.getWriter().write(texto.toString());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        StringBuilder html = new StringBuilder();
        html.append("<p>Este recurso responde ao método HTTP <strong>OPTIONS</strong>, que pergunta ao "
                  + "servidor quais métodos ele aceita. O navegador só envia OPTIONS por script, então "
                  + "teste com o botão da página inicial ou com o curl:</p>\n");
        html.append("<pre>curl -i -X OPTIONS ").append(Html.escape(req.getRequestURL().toString())).append("</pre>\n");
        html.append("<p>A resposta traz o cabeçalho <code>Allow</code> e a lista abaixo no corpo.</p>\n");
        html.append("<h2>Métodos aceitos por cada URL desta aplicação</h2>\n<table>\n");
        for (String[] endpoint : ENDPOINTS) {
            html.append(Html.linha(endpoint[0], endpoint[1]));
        }
        html.append("</table>\n");

        Html.enviar(resp, HttpServletResponse.SC_OK,
                "Servlet 5 - Métodos suportados (OPTIONS /methods)", html.toString());
    }
}
