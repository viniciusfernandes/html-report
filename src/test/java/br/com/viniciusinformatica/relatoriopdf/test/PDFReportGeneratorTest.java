package br.com.viniciusinformatica.relatoriopdf.test;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import br.com.viniciusinformatica.relatoriopdf.HTMLBlock;
import br.com.viniciusinformatica.relatoriopdf.PropertyResolver;
import br.com.viniciusinformatica.relatoriopdf.PropriedadeNaoEncontradaException;

public class PDFReportGeneratorTest {

	@Test
	public void test() throws IOException, PropriedadeNaoEncontradaException {
		BufferedReader reader = new BufferedReader(new FileReader(
				"template.html"));
		PropertyResolver resolver = generatePropertyResolver();
		HTMLBlock block = new HTMLBlock(reader, resolver);
		block.read();
		System.out.println(block.generateHTML());
		
		assertEquals("Sub blocks number is not correct", 1, block.subBlocksNumber());
		assertEquals("Lines number is not correct", 4, block.linesNumber());
	}

	private PropertyResolver generatePropertyResolver() {
		PropertyResolver resolver = new PropertyResolver();
		List<Cliente> clients = new ArrayList<Cliente>();
		clients.add(new Cliente("Marcos FErras"));
		clients.add(new Cliente("Vania melo"));
		
		resolver.addProperty("codigo", "6543");
		resolver.addProperty("cliente", new Cliente("Vinicius Fernandes"));
		resolver.addProperty("listaCliente", clients);
		return resolver;
	}

}
