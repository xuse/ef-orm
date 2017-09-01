package com.github.geequery.springdata.repository.query;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.data.repository.query.parser.PartTree.OrPart;
import org.springframework.util.StringUtils;

import com.github.geequery.springdata.annotation.Condition;

/**
 * A part of the parsed source that results from splitting up the resource
 * around {@literal Or} keywords. Consists of {@link GqPart}s that have to be
 * concatenated by {@literal And}.
 */
final class GqOrPart implements Iterable<GqPart> {

    private final List<GqPart> children = new ArrayList<GqPart>();

    /**
     * Creates a new {@link OrPart}.
     * 
     * @param source
     *            the source to split up into {@literal And} parts in turn.
     * @param domainClass
     *            the domain class to check the resulting {@link GqPart}s
     *            against.
     * @param alwaysIgnoreCase
     *            if always ignoring case
     */
    GqOrPart(String source, Class<?> domainClass, boolean alwaysIgnoreCase) {

        String[] split = GqPartTree.split(source, "And");
        for (String part : split) {
            if (StringUtils.hasText(part)) {
                children.add(new GqPart(part, domainClass, alwaysIgnoreCase));
            }
        }
    }

    public GqOrPart(Condition[] conditions, Class<?> domainClass) {
        for(Condition c:conditions){
            children.add(new GqPart(c, domainClass));
        }
    }

    public Iterator<GqPart> iterator() {

        return children.iterator();
    }

    @Override
    public String toString() {
        return StringUtils.collectionToDelimitedString(children, " and ");
    }
}