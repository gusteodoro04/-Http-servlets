package twii.http;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Servlet 3 - Controle de sessão e cookies (GET /session).
 *
 * Na primeira visita grava no navegador o cookie "usuario" com a identificação do usuário
 * (X). Nas visitas seguintes o navegador devolve o cookie e o servlet responde
 * "Bem-vindo de volta, usuário 'X'". A página também mostra a sessão (HttpSession), que o
 * contêiner controla por conta própria usando o cookie JSESSIONID.
 *
 * Parâmetros opcionais: ?nome=Ana escolhe o nome gravado no cookie; ?acao=sair apaga tudo.
 */
@WebServlet(name = "SessionServlet", urlPatterns = "/session")
public class SessionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String COOKIE_USUARIO = "usuario";
    private static final int UM_MES_EM_SEGUNDOS = 60 * 60 * 24 * 30;
    private static final int TAMANHO_MAXIMO_NOME = 40;
    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setHeader("Cache-Control", "no-store");

        if ("sair".equals(req.getParameter("acao"))) {
            sair(req, resp);
            return;
        }

        // ---- Cookie "usuario" ----------------------------------------------------------
        String usuarioDoCookie = lerCookieUsuario(req);
        String nomeEscolhido = limparNome(req.getParameter("nome"));

        String destaque;
        String explicacao;
        String setCookie = null;   // texto do Set-Cookie enviado nesta resposta (se houver)
        String usuario;

        if (usuarioDoCookie != null && (nomeEscolhido == null || nomeEscolhido.equals(usuarioDoCookie))) {
            // Visita de retorno: o navegador mandou o cookie.
            usuario = usuarioDoCookie;
            destaque = "Bem-vindo de volta, usuário '<strong>" + Html.escape(usuario) + "</strong>'.";
            explicacao = "O navegador enviou o cookie <code>usuario</code> no cabeçalho "
                       + "<code>Cookie</code> da requisição, e o servidor reconheceu você.";
        } else if (nomeEscolhido != null) {
            usuario = nomeEscolhido;
            setCookie = gravarCookie(req, resp, usuario);
            destaque = "Cookie gravado: usuário '<strong>" + Html.escape(usuario) + "</strong>'.";
            explicacao = "O servidor enviou o cabeçalho <code>Set-Cookie</code>. Acesse "
                       + "<a href=\"session\">/session</a> de novo para ver a mensagem de boas-vindas.";
        } else {
            usuario = "visitante-" + UUID.randomUUID().toString().substring(0, 8);
            setCookie = gravarCookie(req, resp, usuario);
            destaque = "Primeira visita! Cookie gravado: usuário '<strong>" + Html.escape(usuario)
                     + "</strong>'.";
            explicacao = "O servidor enviou o cabeçalho <code>Set-Cookie</code>. Recarregue a página "
                       + "(F5) ou acesse <a href=\"session\">/session</a> de novo: agora o navegador "
                       + "vai devolver o cookie e você verá a mensagem de boas-vindas.";
        }

        // ---- Sessão (HttpSession) --------------------------------------------------------
        // O contêiner cria a sessão e envia o cookie JSESSIONID sozinho.
        HttpSession sessao = req.getSession(true);
        boolean nova = sessao.isNew();
        Integer anterior = (Integer) sessao.getAttribute("visitas");
        int visitas = anterior == null ? 1 : anterior + 1;
        sessao.setAttribute("visitas", visitas);

        // ---- Página ----------------------------------------------------------------------
        StringBuilder html = new StringBuilder();
        html.append("<p style=\"font-size:1.2rem\">").append(destaque).append("</p>\n");
        html.append("<p>").append(explicacao).append("</p>\n");

        html.append("<h2>Cookies que o navegador enviou nesta requisição</h2>\n<table>\n");
        Cookie[] recebidos = req.getCookies();
        if (recebidos == null || recebidos.length == 0) {
            html.append(Html.linha("(nenhum)", "Nenhum cookie chegou: é a primeira requisição deste navegador."));
        } else {
            for (Cookie c : recebidos) {
                html.append(Html.linha(c.getName(), c.getValue()));
            }
        }
        html.append("</table>\n");

        html.append("<h2>Cookie enviado nesta resposta</h2>\n<table>\n");
        html.append(Html.linha("Set-Cookie (usuario)",
                setCookie != null ? setCookie : "(nenhum: o cookie já existia, não precisa gravar de novo)"));
        html.append("</table>\n");

        html.append("<h2>Sessão (HttpSession)</h2>\n<table>\n");
        html.append(Html.linha("ID da sessão (cookie JSESSIONID)", sessao.getId()));
        html.append(Html.linha("Sessão nova nesta requisição?", nova ? "sim" : "não"));
        html.append(Html.linha("Criada em", FORMATO.format(Instant.ofEpochMilli(sessao.getCreationTime()))));
        html.append(Html.linha("Último acesso", FORMATO.format(Instant.ofEpochMilli(sessao.getLastAccessedTime()))));
        html.append(Html.linha("Inatividade máxima", sessao.getMaxInactiveInterval() + " segundos"));
        html.append(Html.linha("Visitas nesta sessão", String.valueOf(visitas)));
        html.append("</table>\n");
        html.append("<p class=\"nota\">O cookie <code>usuario</code> dura 30 dias e sobrevive ao fechamento do "
                  + "navegador. A sessão vale enquanto o navegador estiver aberto (e o servidor não a expirar), "
                  + "e os dados dela ficam no servidor.</p>\n");

        html.append("<h2>Testar</h2>\n");
        html.append("<form method=\"get\" action=\"session\">\n"
                  + "<label>Escolher o nome gravado no cookie: "
                  + "<input name=\"nome\" maxlength=\"" + TAMANHO_MAXIMO_NOME + "\" required></label>\n"
                  + "<button type=\"submit\">Gravar cookie</button>\n</form>\n");
        html.append("<p><a href=\"session\">Visitar /session de novo</a> &middot; "
                  + "<a href=\"session?acao=sair\">Apagar cookie e sessão</a></p>\n");

        Html.enviar(resp, HttpServletResponse.SC_OK, "Servlet 3 - Sessão e cookies (GET /session)", html.toString());
    }

    /** Grava o cookie "usuario" e devolve um texto descrevendo o Set-Cookie enviado. */
    private static String gravarCookie(HttpServletRequest req, HttpServletResponse resp, String usuario) {
        // Cookie não aceita espaço, ponto e vírgula nem acentos: o valor é codificado (%XX).
        String valor = URLEncoder.encode(usuario, StandardCharsets.UTF_8);
        String caminho = caminhoDoCookie(req);

        Cookie cookie = new Cookie(COOKIE_USUARIO, valor);
        cookie.setPath(caminho);
        cookie.setMaxAge(UM_MES_EM_SEGUNDOS);
        cookie.setHttpOnly(true);            // o JavaScript da página não consegue ler
        cookie.setSecure(req.isSecure());    // só exige HTTPS quando a requisição já é HTTPS
        resp.addCookie(cookie);

        return COOKIE_USUARIO + "=" + valor + "; Max-Age=" + UM_MES_EM_SEGUNDOS
                + "; Path=" + caminho + "; HttpOnly" + (req.isSecure() ? "; Secure" : "");
    }

    /** Apaga o cookie (Max-Age=0) e encerra a sessão, para repetir o teste do zero. */
    private static void sair(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Cookie apagar = new Cookie(COOKIE_USUARIO, "");
        apagar.setMaxAge(0);
        apagar.setPath(caminhoDoCookie(req));
        resp.addCookie(apagar);

        HttpSession sessao = req.getSession(false);
        if (sessao != null) {
            sessao.invalidate();
        }

        Html.enviar(resp, HttpServletResponse.SC_OK, "Servlet 3 - Cookie apagado",
                "<p>O cookie <code>usuario</code> foi apagado (<code>Max-Age=0</code>) e a sessão foi encerrada.</p>\n"
              + "<p><a href=\"session\">Abrir /session</a> para ver a primeira visita outra vez.</p>\n");
    }

    /** Valor do cookie "usuario" já decodificado, ou null se não existir ou estiver inválido. */
    private static String lerCookieUsuario(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();   // null quando o navegador não mandou nenhum
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (COOKIE_USUARIO.equals(c.getName())) {
                try {
                    return limparNome(URLDecoder.decode(c.getValue(), StandardCharsets.UTF_8));
                } catch (IllegalArgumentException e) {
                    return null;   // valor adulterado: trata como se não existisse
                }
            }
        }
        return null;
    }

    /** Remove espaços das pontas, limita o tamanho e devolve null se ficar vazio. */
    private static String limparNome(String nome) {
        if (nome == null) {
            return null;
        }
        nome = nome.trim();
        if (nome.isEmpty()) {
            return null;
        }
        return nome.length() > TAMANHO_MAXIMO_NOME ? nome.substring(0, TAMANHO_MAXIMO_NOME) : nome;
    }

    private static String caminhoDoCookie(HttpServletRequest req) {
        String contexto = req.getContextPath();
        return contexto.isEmpty() ? "/" : contexto;
    }
}
