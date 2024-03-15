package io.nuvalence.workmanager.service.domain.dynamicschema;

import org.apache.commons.beanutils.BasicDynaClass;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaProperty;

/**
 * Dynaclass for dynamic entities.
 */
public class SchemaDynaClass extends BasicDynaClass {
    private static final long serialVersionUID = -526257978592799516L;

    public SchemaDynaClass() {
        super();
    }

    public SchemaDynaClass(String name, Class<?> dynaBeanClass) {
        super(name, dynaBeanClass);
    }

    public SchemaDynaClass(String name, Class<?> dynaBeanClass, DynaProperty[] properties) {
        super(name, dynaBeanClass, properties);
    }

    @Override
    public DynaBean newInstance() {
        return new DynamicEntityDynaBean(this);
    }
}
