package com.anderk.ecommerce.shared.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Marks a class as an inbound port implementation — a use case.
 *
 * <p>The stereotype makes the hexagonal shape explicit and greppable: every
 * use case is annotated with it, so a reviewer (or the ArchUnit test) can see
 * at a glance which classes sit in the application layer.
 *
 * <p>It is meta-annotated with {@link Service} and {@link Transactional}, so a
 * use case is a Spring bean and runs inside a transaction without repeating
 * the annotations. A use case that must not be transactional (a read-only
 * query) overrides it with {@code @Transactional(readOnly = true)}.
 *
 * <p>Declared once, in {@code shared}, because it is a cross-cutting technical
 * concern — it carries no business meaning.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Service
@Transactional
public @interface UseCase {
}
