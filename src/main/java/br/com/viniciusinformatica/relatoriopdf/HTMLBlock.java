package br.com.viniciusinformatica.relatoriopdf;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTMLBlock {

	private final String regex = "\\{[a-zA-Z]+(\\.[a-zA-Z]+)*\\}";
	private List<String> lines = new ArrayList<String>();
	private String listName;
	private BufferedReader reader;
	private PropertyResolver resolver;
	private HTMLBlock parentBlock;
	private Queue<HTMLBlock> subBlocks;
	private static int number;
	private int sequence;

	final Pattern p = Pattern.compile(regex);

	private Matcher m = null;

	public HTMLBlock(BufferedReader reader, PropertyResolver resolver) throws IOException, PropriedadeNaoEncontradaException {
		this.reader = reader;
		this.resolver = resolver;
		this.sequence = ++number;
	}


	public void read() throws IOException, PropriedadeNaoEncontradaException {
		read(this);
	}
	
	public int linesNumber() {
		return lines.size();
	}
	
	public int subBlocksNumber() {
		return subBlocks == null ? 0 : subBlocks.size();
	}
	
	private void read(HTMLBlock block) throws IOException, PropriedadeNaoEncontradaException {
		String line = null;
		String groupName = null;
		List<String>  groupListName = new ArrayList<String>();
		boolean startBlockFinded = false;
		boolean endBlockFinded = false;
		while ((line = block.reader.readLine()) != null) {
			m = p.matcher(line);
			groupListName.clear();
			
			startBlockFinded = false;
			endBlockFinded = false;
			
			while (m.find()) {
				 groupName = m.group(0);
				 groupListName.add(groupName);
                 // Verificando se o padrao de iteracao "lista" foi
                 // encontrado
				startBlockFinded = groupName != null && groupName.contains("start.lista");
				endBlockFinded = groupName != null && groupName.contains("end.lista");
				if (startBlockFinded || endBlockFinded) {
					break;
				}
             }
			 
			 if (startBlockFinded) {
				 HTMLBlock subBlock = new HTMLBlock(reader, resolver);
				 subBlock.listName = groupName;
				 block.addSubBlock(subBlock);
				 read(subBlock);
			 } else if (endBlockFinded) {
				 read(block.parentBlock);
			 } else {
					 block.lines.add(line+"\n");
			 }
		}
	}

	public String generateHTML() throws PropriedadeNaoEncontradaException {
		return generateHTML(this, new StringBuilder());
	}
	
	public String generateHTML(HTMLBlock block, StringBuilder html) throws PropriedadeNaoEncontradaException {
		String groupName = null;
		List<String>  groupListName = new ArrayList<String>();
		for (String line : lines) {
			m = p.matcher(line);
			groupListName.clear();
			
			while (m.find()) {
				 groupName = m.group(0);
				 groupListName.add(groupName);
             }
			
			Object valor = null;
            for (String re : groupListName) {
                valor = resolver.getValue(removeBrackets(re, false));
                line = line.replace(re, valor == null ? "" : valor.toString());
            }
			html.append(line);
		}
		if (block.isRoot() && block.hasSubBlocks()) {
			for (HTMLBlock subBlock : block.subBlocks) {
				generateHTML(subBlock, html);
			}
			
		} else if (!block.isRoot() && block.hasSubBlocks()) {
			Collection<?> collection = (Collection<?>) resolver.getValue(removeBrackets(block.listName, true));
			for (Object object : collection) {
				for (HTMLBlock subBlock : block.subBlocks) {
					generateHTML(subBlock, html);
				}
			}
			
		}
		return html.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder string = new StringBuilder("block sequence: ");
		string.append(sequence).append("\n");
		
		for (String str : lines) {
			string.append(str);
		}
		return string.toString();
	}
	
	
	private void addSubBlock(HTMLBlock subBlock) {
		if (subBlocks == null) {
			subBlocks = new LinkedList<HTMLBlock>();
		}
		this.subBlocks.add(subBlock);
		subBlock.parentBlock = this;
	}
	
	private boolean isRoot() {
		return parentBlock == null;
	}
	
	private boolean hasSubBlocks() {
		return subBlocks != null && !subBlocks.isEmpty();
	}
	

    /*
     * Os grupos encontrados em cada linha sao recuperados contendo as chaves
     * como {cliente.nome}, {pedido.id}, etc. Sendo que para recuperarmos os
     * valores dos objetos atraves de reflection devemos remover as chaves,
     * ficando assim: {cliente.nome} => cliente.nome, {pedido.id} => pedido.id
     */
    private String removeBrackets(String property, boolean isList) {
    	property = property.replace("{", "").replace("}", "");
    	if (isList) {
    		property = property.split("\\.")[1]; 
    	}
        return property;
    }
}
