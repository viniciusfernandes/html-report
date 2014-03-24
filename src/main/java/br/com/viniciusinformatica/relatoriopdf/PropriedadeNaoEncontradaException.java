package br.com.viniciusinformatica.relatoriopdf;


public class PropriedadeNaoEncontradaException extends Exception {

    private static final long serialVersionUID = 2460863798253857753L;

    public PropriedadeNaoEncontradaException(String mensagem) {
        super(mensagem);
    }

    public PropriedadeNaoEncontradaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
