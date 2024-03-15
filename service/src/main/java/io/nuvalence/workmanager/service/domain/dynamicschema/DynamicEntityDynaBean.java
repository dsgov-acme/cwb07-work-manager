package io.nuvalence.workmanager.service.domain.dynamicschema;

import io.nuvalence.workmanager.service.config.exceptions.UnexpectedException;
import org.apache.commons.beanutils.BasicDynaBean;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaClass;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * DynaBean that supports computed properties.
 */
public class DynamicEntityDynaBean extends BasicDynaBean {

    public static final String COMPUTED_PROPERTIES_CANNOT_BE_MODIFIED =
            "Computed properties cannot be modified";
    private static final long serialVersionUID = -8792549507122331685L;

    private final transient ExpressionParser expressionParser;
    private final transient StandardEvaluationContext evaluationContext;

    /**
     * Construct a new <code>DynaBean</code> associated with the specified
     * <code>DynaClass</code> instance.
     *
     * @param dynaClass The DynaClass we are associated with
     * @throws UnexpectedException if the DynaBean cannot be created
     */
    public DynamicEntityDynaBean(DynaClass dynaClass) {
        super(dynaClass);
        expressionParser = new SpelExpressionParser();
        evaluationContext = new StandardEvaluationContext();
        evaluationContext.setRootObject(this);
        try {
            evaluationContext.registerFunction(
                    "concat",
                    ComputedAttributeFunctions.class.getDeclaredMethod(
                            "concat", String.class, String[].class));
        } catch (NoSuchMethodException e) {
            throw new UnexpectedException(e);
        }
        evaluationContext.setPropertyAccessors(
                List.of(
                        new PropertyAccessor() {
                            @Override
                            public Class<?>[] getSpecificTargetClasses() {
                                return new Class[] {DynaBean.class};
                            }

                            @Override
                            public boolean canRead(
                                    EvaluationContext context, Object target, String name)
                                    throws AccessException {
                                return true;
                            }

                            @Override
                            public TypedValue read(
                                    EvaluationContext context, Object target, String name)
                                    throws AccessException {
                                try {
                                    return new TypedValue(PropertyUtils.getProperty(target, name));
                                } catch (IllegalAccessException
                                        | InvocationTargetException
                                        | NoSuchMethodException e) {
                                    throw new AccessException(
                                            "Unable to access bean property: " + name, e);
                                }
                            }

                            @Override
                            public boolean canWrite(
                                    EvaluationContext context, Object target, String name)
                                    throws AccessException {
                                return false;
                            }

                            @Override
                            public void write(
                                    EvaluationContext context,
                                    Object target,
                                    String name,
                                    Object newValue)
                                    throws AccessException {
                                // Do nothing because writing is not necessary yet
                            }
                        }));
        // disable access to data and methods outside of this bean.
        evaluationContext.setTypeLocator(
                typeName -> {
                    throw new IllegalStateException(
                            "Computed Attribute is misconfigured. Access to static methods and"
                                    + " types disabled in SpEL evaluator.");
                });
    }

    @Override
    public Object get(String name) {
        if (isComputed(name)) {
            return compute(name);
        }

        return super.get(name);
    }

    @Override
    public Object get(String name, int index) {
        if (isComputed(name)) {
            throw new UnsupportedOperationException("Computed properties cannot be indexed");
        }

        return super.get(name, index);
    }

    @Override
    public Object get(String name, String key) {
        if (isComputed(name)) {
            throw new UnsupportedOperationException("Computed properties cannot be indexed");
        }

        return super.get(name, key);
    }

    @Override
    public void set(String name, Object value) {
        if (isComputed(name)) {
            throw new UnsupportedOperationException(COMPUTED_PROPERTIES_CANNOT_BE_MODIFIED);
        }

        super.set(name, value);
    }

    @Override
    public void set(String name, int index, Object value) {
        if (isComputed(name)) {
            throw new UnsupportedOperationException(COMPUTED_PROPERTIES_CANNOT_BE_MODIFIED);
        }

        super.set(name, index, value);
    }

    @Override
    public void set(String name, String key, Object value) {
        if (isComputed(name)) {
            throw new UnsupportedOperationException(COMPUTED_PROPERTIES_CANNOT_BE_MODIFIED);
        }

        super.set(name, key, value);
    }

    private boolean isComputed(String name) {
        return getDynaProperty(name) instanceof ComputedDynaProperty;
    }

    private Object compute(String name) {
        ComputedDynaProperty property = (ComputedDynaProperty) getDynaProperty(name);

        return expressionParser
                .parseExpression(property.getExpression())
                .getValue(evaluationContext);
    }
}
