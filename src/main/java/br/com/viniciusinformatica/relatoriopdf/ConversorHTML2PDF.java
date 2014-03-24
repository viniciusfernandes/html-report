package br.com.viniciusinformatica.relatoriopdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;

final class ConversorHTML2PDF {

    private Charset charset;

    public ConversorHTML2PDF(Charset charset) {
        this.charset = charset;
    }

    public ConversorHTML2PDF() {

    }

    public byte[] converter(InputStream arquivoHTML) throws ConversaoHTML2PDFException {
        final Document document = new Document();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = null;
        try {
            writer = PdfWriter.getInstance(document, outputStream);
        } catch (DocumentException e) {
            throw new ConversaoHTML2PDFException("Não foi possivel gerar o outputstream do arquivo PDF", e);
        }
        document.open();
        try {

            if (this.charset == null) {
                XMLWorkerHelper.getInstance().parseXHtml(writer, document, arquivoHTML);
            } else {
                XMLWorkerHelper.getInstance().parseXHtml(writer, document, arquivoHTML, charset);
            }

        } catch (IOException e) {
            throw new ConversaoHTML2PDFException(
                    "Não foi possivel parsear o conteudo de HTML para formato PDF do arquivo", e);
        }

        document.close();
        writer.close();

        final byte[] conteudo = outputStream.toByteArray();
        try {
            outputStream.close();
        } catch (IOException e) {
            throw new ConversaoHTML2PDFException("Falha ao finalizar a geracao do arquivo PDF. "
                    + "Nao foi possivel fechar o arquivo apos a leitura.", e);
        }
        return conteudo;
    }
}
