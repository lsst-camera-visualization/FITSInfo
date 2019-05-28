package org.lsst.fits.fitsinfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

/**
 *
 * @author tonyj
 */
public abstract class Filter {

    private enum Operation {
        EQUALS("="), GT(">"), LT("<"), NOTEQUALS("<>"), CONTAINS("contains"), NOT("!"), AND("and"), OR("or");

        private static Operation fromRep(String rep) {
            for (Operation op : Operation.values()) {
                if (op.rep.equals(rep)) {
                    return op;
                }
            }
            throw new IllegalArgumentException("Unknown operation rep " + rep);
        }
        private final String rep;

        Operation(String rep) {
            this.rep = rep;
        }
    }

    public abstract <T> Expression<Boolean> buildQuery(CriteriaBuilder builder, Root<T> root);

    public static Filter fromObjects(List<Object> input) {
        if (input.isEmpty()) {
            return null;
        } else if (input.size() == 3 && input.get(0) instanceof String && input.get(1) instanceof String) {
            return new SimpleFilter((String) input.get(0), Operation.fromRep((String) input.get(1)), input.get(2));
        } else if (input.size() == 2 && input.get(0) instanceof String && input.get(1) instanceof List) {
            return new UnaryFilter(Operation.fromRep((String) input.get(0)), Filter.fromObjects((List) input.get(1)));
        } else if (input.size() > 2 && input.size() % 2 == 1) {
            return new ComplexFilter(input);
        } else {
            throw new IllegalArgumentException("Cannot construct filter from " + input);
        }
    }

    public static Filter fromString(String input) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<Object> readValue = mapper.readValue(input,List.class);        
        return Filter.fromObjects(readValue);
    }

    static class SimpleFilter extends Filter {

        private final String column;
        private final Operation op;
        private final Object value;

        SimpleFilter(String column, Operation op, Object value) {
            this.column = column;
            this.op = op;
            this.value = value;
        }

        @Override
        public String toString() {
            return "SimpleFilter{" + "column=" + column + ", op=" + op + ", value=" + value + '}';
        }

        @Override
        public <T> Expression<Boolean> buildQuery(CriteriaBuilder builder, Root<T> root) {
            switch (op) {
                case EQUALS:
                    if (value == null) {
                        return builder.isNull(root.get(column));
                    } else {
                        return builder.equal(root.get(column), value);
                    }
                case NOTEQUALS:
                    if (value == null) {
                        return builder.isNotNull(root.get(column));
                    } else {
                        return builder.notEqual(root.get(column), value);
                    }
                case GT:
                    return builder.greaterThan(root.get(column), (Comparable) value);
                case LT:
                    return builder.lessThan(root.get(column), (Comparable) value);
                case CONTAINS:
                    return builder.like(root.<String>get(column), "%" + value + "%");
                default:
                    throw new IllegalArgumentException("Invalid binary operator: " + op);
            }
        }
    }

    static class UnaryFilter extends Filter {

        private final Operation op;
        private final Filter filter;

        UnaryFilter(Operation op, Filter filter) {
            this.op = op;
            this.filter = filter;
        }

        @Override
        public String toString() {
            return "UnaryFilter{" + "op=" + op + ", filter=" + filter + '}';
        }

        @Override
        public <T> Expression<Boolean> buildQuery(CriteriaBuilder builder, Root<T> root) {
            switch (op) {
                case NOT:
                    return builder.not(filter.buildQuery(builder, root));
                default:
                    throw new IllegalArgumentException("Invalid unary operator: " + op);
            }
        }
    }

    static class ComplexFilter extends Filter {

        private final List<Filter> filters = new ArrayList<>();
        private final List<Operation> ops = new ArrayList<>();

        ComplexFilter(List<Object> input) {
            if (!(input.get(0) instanceof List)) {
                throw new IllegalArgumentException("Cannot build filter from: " + input);
            }
            filters.add(Filter.fromObjects((List<Object>) (input.get(0))));
            for (int i = 1; i < input.size(); i += 2) {
                if (!(input.get(i) instanceof String)) {
                    throw new IllegalArgumentException("Cannot build filter from: " + input);
                }
                ops.add(Operation.fromRep((String) input.get(i)));
                if (!(input.get(i + 1) instanceof List)) {
                    throw new IllegalArgumentException("Cannot build filter from: " + input);
                }
                filters.add(Filter.fromObjects((List<Object>) (input.get(i + 1))));
            }
        }

        @Override
        public String toString() {
            return "ComplexFilter{" + "filters=" + filters + ", ops=" + ops + '}';
        }

        @Override
        public <T> Expression<Boolean> buildQuery(CriteriaBuilder builder, Root<T> root) {
            Expression<Boolean> result = filters.get(0).buildQuery(builder, root);
            for (int i = 0; i < ops.size(); i++) {
                switch (ops.get(i)) {
                    case AND:
                        result = builder.and(result, filters.get(i + 1).buildQuery(builder, root));
                        break;
                    case OR:
                        result = builder.or(result, filters.get(i + 1).buildQuery(builder, root));
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid boolean operator: " + ops.get(i));
                }
            }
            return result;
        }
    }
}
