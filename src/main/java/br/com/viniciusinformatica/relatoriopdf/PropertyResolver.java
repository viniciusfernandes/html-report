package br.com.viniciusinformatica.relatoriopdf;


import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class PropertyResolver {
    private Map<String, Object> propss = new HashMap<String, Object>();

    public void addProperty(String property, Object value) {
        this.propss.put(property, value);
    }

    public Object getValue(String property) throws PropriedadeNaoEncontradaException {
        if (property == null || property.trim().length() == 0) {
            return null;
        }

        String[] val = property.split("\\.");
        Object v = propss.get(property);

        if (val.length <= 1) {
            return v;
        } else {
            v = propss.get(val[0]);
        }
        property = property.substring(property.indexOf(".") + 1);
        v = getValue(property, v);

        return v;
    }

    public Object getValue(String property, Object o) throws PropriedadeNaoEncontradaException {

        if (o == null) {
            return "";
        }

        String[] val = property.split("\\.");

        if (val.length > 1) {
            return getValue(property.substring(property.indexOf(".") + 1), getProperty(val[0], o));
        }

        return getProperty(property, o);
    }

    private Object getProperty(String property, Object o) throws PropriedadeNaoEncontradaException {
        Method[] m = o.getClass().getMethods();
        final String get = "get" + property.substring(0, 1).toUpperCase() + property.substring(1);
        for (Method method : m) {
            if (get.equals(method.getName())) {
                try {
                    return method.invoke(o);
                } catch (Exception e) {
                    throw new PropriedadeNaoEncontradaException("Falha ao tentar recuperar o atributo " + property
                            + " do objeto " + o.getClass().getName(), e);
                }
            }
        }

        throw new IllegalStateException("Nao exite o metodo " + get + " na classe " + o.getClass().getName());
    }
}
