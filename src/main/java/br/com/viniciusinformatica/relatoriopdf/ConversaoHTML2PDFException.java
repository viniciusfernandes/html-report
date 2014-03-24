package br.com.viniciusinformatica.relatoriopdf;


public class ConversaoHTML2PDFException extends Exception {
    private static final long serialVersionUID = 7021878033141035308L;

    public ConversaoHTML2PDFException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
