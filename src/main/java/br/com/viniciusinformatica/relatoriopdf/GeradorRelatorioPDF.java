package br.com.viniciusinformatica.relatoriopdf;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GeradorRelatorioPDF {

    private final String regex = "\\{[a-zA-Z]+(\\.[a-zA-Z]+)*\\}";
    private final PropertyResolver resolver;
    private final ConversorHTML2PDF conversor;
    private final Charset charset;
    private StringBuilder html;
    private StringBuilder css;
    private StringBuilder cabecalho;
    private boolean layoutConfigurado;

    public GeradorRelatorioPDF() {
        charset = Charset.forName("UTF-8");
        conversor = new ConversorHTML2PDF(charset);
        resolver = new PropertyResolver();
    }

    public String gerarHTML() {
        return html == null ? "" : html.toString();
    }

    public byte[] gerarPDF() throws ConversaoHTML2PDFException {
        return html == null || html.length() == 0 ? new byte[0] : conversor.converter(new ByteArrayInputStream(this
                .gerarHTML().getBytes()));
    }

    public void processar(File arquivo) throws ConversaoHTML2PDFException {

        configurarLayout(arquivo);

        this.html = new StringBuilder();

        final Pattern p = Pattern.compile(regex);
        String linha = null;
        String lista = null;
        String grupo = null;
        boolean encontrou = false;

        Matcher m = null;
        List<String> listaGrupo = new ArrayList<String>();
        BufferedReader reader = null;
        // Variavel utilizada para apontar qual eh o numero da linha que teve
        // problemas durante o processamento.
        int numLinha = 1;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(arquivo)));

            while ((linha = reader.readLine()) != null) {

                encontrou = false;
                lista = null;
                listaGrupo.clear();

                m = p.matcher(linha);
                while (m.find()) {
                    grupo = m.group(0);
                    listaGrupo.add(grupo);
                    // Verificando se o padrao de iteracao "lista" foi
                    // encontrado
                    if (lista == null && grupo.contains("lista")) {
                        lista = grupo;
                    }

                    // Indicacao de que foi encontrado uma marcacao para
                    // substituir por valores dos objetos
                    encontrou = true;
                }

                // Verificando se a marcacao encontrada sera uma iteracao de uma
                // lista
                if (encontrou && lista != null) {
                    Collection<?> colecao = (Collection<?>) resolver.getValue(limpar(lista));

                    if (colecao != null && !colecao.isEmpty()) {
                        Object valor = null;
                        String linhaParseada = null;

                        // Varrendo os objetos da lista
                        for (Object o : colecao) {
                            linhaParseada = linha;

                            /*
                             * Convencionamos que os atributos que serao
                             * preenchidos foram encontrados apos a "lista", por
                             * exemplo: {listaCliente} ... {nome}, {idade},
                             * {documento}, onde nome, idade, etc, sao atributos
                             * dos objetos contidos em listaCliente. Sendo
                             * assim, ao encontrarmos a listaCliente, pulamos
                             * para os proximos grupos encontrados, ou seja,
                             * nome, idade e documento. Alem disso, todos os
                             * atributos que devem ser repetidos deverao estar
                             * na mesma linha logo apos a marcacao
                             * "listaCliente".
                             */
                            for (String parametro : listaGrupo) {

                                if (parametro.contains("lista")) {
                                    linhaParseada = linhaParseada.replace(parametro, "");
                                    // Pulando para os proximos grupos
                                    // encontrados na linha lida.
                                    continue;
                                }
                                valor = resolver.getValue(limpar(parametro), o);
                                linhaParseada = linhaParseada.replace(parametro, valor == null ? "" : valor.toString());
                            }
                            html.append(linhaParseada).append("\n");
                        }
                    }

                } else if (encontrou) {
                    Object valor = null;
                    for (String re : listaGrupo) {
                        valor = resolver.getValue(limpar(re));
                        linha = linha.replace(re, valor == null ? "" : valor.toString());
                    }
                    html.append(linha).append("\n");
                }

                // Se nao encontrou uma marcacao devemos apenas copiar a linha
                if (!encontrou) {
                    html.append(linha).append("\n");
                }

                numLinha++;
            }
        } catch (Exception e) {
            throw new ConversaoHTML2PDFException("Falha ao popular os valores do relatorio. Problemas na linha: "
                    + numLinha, e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                throw new ConversaoHTML2PDFException("Falha ao popular os valor do relatorio."
                        + "Nao foi possivel liberar recursos de leitura do arquivo.", e);
            }
        }
    }

    public void addAtributo(String property, Object value) {
        this.resolver.addProperty(property, value);
    }

    private void configurarLayout(File arquivo) throws ConversaoHTML2PDFException {
        // Vamos configuraro o layout apenas 1 vez pois foi definido que o CSS e
        // o cabecalho do relatorio sera o mesmo para todos os arquivos gerados,
        // independente do tipo de relatorio requisitado. Entao, se tivermos um
        // laco para gerar varios relatorios em um lote teremos um ganho de
        // performance ao carregar o CSS.
        if (!this.layoutConfigurado) {
            this.addCss(new File(arquivo.getParentFile(), "/css/relatorio.css"));
            this.addCabecalho(new File(arquivo.getParentFile(), "/cabecalho.html"));
            layoutConfigurado = true;
        }
    }

    private void addCss(File arquivoCSS) throws ConversaoHTML2PDFException {
        this.css = new StringBuilder();
        copiarConteudo(arquivoCSS, this.css);

        // Temos que adicionar o conteudo CSS nas propriedades que serao
        // populadas no momento de converter o arquivo para o o CSS sea incluido
        // no HTML resultante da conversao.
        this.addAtributo("conteudoCss", this.css.toString());
    }

    private void addCabecalho(File arquivoCabecalho) throws ConversaoHTML2PDFException {
        this.cabecalho = new StringBuilder();
        copiarConteudo(arquivoCabecalho, this.cabecalho);

        // Temos que adicionar o conteudo do CABECALHO nas propriedades que
        // serao
        // populadas no momento de converter o arquivo para o CABECALHO seja
        // incluido
        // no HTML resultante da conversao.
        this.addAtributo("conteudoCabecalho", this.cabecalho.toString());
    }

    private void copiarConteudo(File arquivoCSS, StringBuilder conteudo) throws ConversaoHTML2PDFException {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(arquivoCSS));
        } catch (FileNotFoundException e1) {
            throw new ConversaoHTML2PDFException("Nao foi possivel encontrar o arquivo " + arquivoCSS.getName(), e1);
        }
        String linha = null;
        try {
            while ((linha = reader.readLine()) != null) {
                conteudo.append(linha).append("\n");
            }
        } catch (IOException e) {
            throw new ConversaoHTML2PDFException("Falha na leitura do arquivo " + arquivoCSS.getName(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    throw new ConversaoHTML2PDFException("Falha na liberacao dos recursos na leitura do arquivo "
                            + arquivoCSS.getName(), e);
                }
            }
        }
    }

    /*
     * Os grupos encontrados em cada linha sao recuperados contendo as chaves
     * como {cliente.nome}, {pedido.id}, etc. Sendo que para recuperarmos os
     * valores dos objetos atraves de reflection devemos remover as chaves,
     * ficando assim: {cliente.nome} => cliente.nome, {pedido.id} => pedido.id
     */
    private static String limpar(String property) {
        return property.replace("{", "").replace("}", "");
    }
}
