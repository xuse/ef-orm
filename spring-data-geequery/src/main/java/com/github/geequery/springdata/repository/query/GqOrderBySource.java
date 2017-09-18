package com.github.geequery.springdata.repository.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.repository.query.parser.OrderBySource;
import org.springframework.util.StringUtils;

/**
 * Simple helper class to create a {@link Sort} instance from a method name end. It expects the last part of the method
 * name to be given and supports lining up multiple properties ending with the sorting direction. So the following
 * method ends are valid: {@code LastnameUsernameDesc}, {@code LastnameAscUsernameDesc}.
 * 
 * @author Oliver Gierke
 */
public class GqOrderBySource {

    private static final String BLOCK_SPLIT = "(?<=Asc|Desc)(?=\\p{Lu})";
    private static final Pattern DIRECTION_SPLIT = Pattern.compile("(.+?)(Asc|Desc)?$");
    private static final String INVALID_ORDER_SYNTAX = "Invalid order syntax for part %s!";
    private static final Set<String> DIRECTION_KEYWORDS = new HashSet<String>(Arrays.asList("Asc", "Desc"));

    private final List<Order> orders;

    /**
     * Creates a new {@link OrderBySource} for the given String clause not doing any checks whether the referenced
     * property actually exists.
     * 
     * @param clause must not be {@literal null}.
     */
    public GqOrderBySource(String clause) {
        this(clause, null);
    }

    public GqOrderBySource(String[] clause, Class<?> domainClass) {
        this.orders = new ArrayList<Sort.Order>();
        for (String part : clause) {
            String[] desc=part.split(" ");
            String propertyString =desc[0];
            String directionString = desc.length>1?desc[1]:null;
            Direction direction = StringUtils.hasText(directionString) ? Direction.fromString(directionString) : null;
            this.orders.add(createOrder(propertyString, direction, domainClass));
        }
        
    }
    /**
     * Creates a new {@link OrderBySource} for the given clause, checking the property referenced exists on the given
     * type.
     * 
     * @param clause must not be {@literal null}.
     * @param domainClass can be {@literal null}.
     */
    public GqOrderBySource(String clause, Class<?> domainClass) {

        this.orders = new ArrayList<Sort.Order>();

        for (String part : clause.split(BLOCK_SPLIT)) {

            Matcher matcher = DIRECTION_SPLIT.matcher(part);

            if (!matcher.find()) {
                throw new IllegalArgumentException(String.format(INVALID_ORDER_SYNTAX, part));
            }

            String propertyString = matcher.group(1);
            String directionString = matcher.group(2);

            // No property, but only a direction keyword
            if (DIRECTION_KEYWORDS.contains(propertyString) && directionString == null) {
                throw new IllegalArgumentException(String.format(INVALID_ORDER_SYNTAX, part));
            }

            Direction direction = StringUtils.hasText(directionString) ? Direction.fromString(directionString) : null;
            this.orders.add(createOrder(propertyString, direction, domainClass));
        }
    }

    /**
     * Creates an {@link Order} instance from the given property source, direction and domain class. If the domain class
     * is given, we will use it for nested property traversal checks.
     * 
     * @param propertySource
     * @param direction
     * @param domainClass can be {@literal null}.
     * @return
     * @see PropertyPath#from(String, Class)
     */
    private Order createOrder(String propertySource, Direction direction, Class<?> domainClass) {

        if (null == domainClass) {
            return new Order(direction, StringUtils.uncapitalize(propertySource));
        }
        PropertyPath propertyPath = PropertyPath.from(propertySource, domainClass);
        return new Order(direction, propertyPath.toDotPath());
    }

    /**
     * Returns the clause as {@link Sort}.
     * 
     * @return the {@link Sort} or null if no orders found.
     */
    public Sort toSort() {
        return this.orders.isEmpty() ? null : new Sort(this.orders);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Order By " + StringUtils.collectionToDelimitedString(orders, ", ");
    }
}