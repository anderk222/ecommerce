package com.anderk.ecommerce.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * The hexagonal rules, as executable facts.
 *
 * <p>They follow the shape the tree has now: inside every feature, a
 * {@code domain}, an {@code application}, an {@code adapter/in} and an
 * {@code adapter/out}. The rule is one line — <em>the dependencies point
 * inward</em> — and these tests are that line, written where the build can
 * check it.
 *
 * <p>Two things keep the suite honest:
 *
 * <ul>
 *   <li>{@link #theFrameworkRuleRejectsADeliberateViolation()} evaluates the
 *       framework rule against a fixture class that leaks a framework import
 *       and asserts that the rule reports a violation. A rule that cannot fail
 *       is not a rule.
 *   <li>{@link #noDomainClassLeaksAFrameworkImport()} walks every domain class
 *       and asserts that none of them imports a framework. There is no
 *       exception left to grant: the whole tree has to comply.
 * </ul>
 *
 * <p>Pending: the rule that forbids a feature to use the <em>outbound</em> port
 * of another one. Today {@code order} reads {@code catalog}'s
 * {@code CatalogRepository}, and {@code lead} reads the repositories of
 * {@code catalog}, {@code order} and {@code tracking}. The features have to
 * talk through the inbound port of the other one before that rule can be
 * written down.
 */
class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.anderk.ecommerce");
    }

    @Test
    @DisplayName("1. the domain knows nothing about Spring, JPA, Validation or Hibernate")
    void domainIsFrameworkFree() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..",
                        "jakarta.persistence..",
                        "jakarta.validation..",
                        "org.hibernate..")
                .because("the domain is the hexagon: it must be exercisable from a plain unit test")
                .allowEmptyShould(true);

        rule.check(classes);
    }

    @Test
    @DisplayName("2. the application never depends on an adapter")
    void applicationDoesNotKnowAdapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..adapter..")
                .because("a use case depends on ports, never on implementations")
                .allowEmptyShould(true);

        rule.check(classes);
    }

    @Test
    @DisplayName("3. an inbound adapter never talks to an outbound adapter")
    void inboundDoesNotTalkToOutbound() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..adapter.in..")
                .should().dependOnClassesThat().resideInAPackage("..adapter.out..")
                .because("a controller calls a use case; it never reaches the database directly")
                .allowEmptyShould(true);

        rule.check(classes);
    }

    @Test
    @DisplayName("4. the domain never depends outward")
    void domainDoesNotKnowTheOutside() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..application..", "..adapter..")
                .because("the domain does not know that it is used by an application")
                .allowEmptyShould(true);

        rule.check(classes);
    }

    @Test
    @DisplayName("5. an inbound adapter never touches the persistence")
    void inboundDoesNotTouchThePersistence() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..adapter.in..")
                .should().dependOnClassesThat().resideInAPackage("..adapter.out.persistence..")
                .because("the entities of the database do not travel to the HTTP layer")
                .allowEmptyShould(true);

        rule.check(classes);
    }

    @Test
    @DisplayName("6. every implementation of the application implements an inbound port of its feature")
    void applicationImplementationsImplementAnInboundPort() {
        ArchRule rule = classes()
                .that().resideInAPackage("..application..")
                .and().haveSimpleNameEndingWith("Impl")
                .and().areNotInterfaces()
                .should(implementAnInboundPortOfItsOwnFeature())
                .because("a use case is the implementation of a contract, not a class on its own")
                .allowEmptyShould(true);

        rule.check(classes);
    }

    @Test
    @DisplayName("7. no repository port is split by read/write")
    void repositoriesAreNotSplitByReadWrite() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should(haveNameEndingWithAny("ReadRepository", "WriteRepository"))
                .because("one outbound port per aggregate, reads and writes together (tech-stack 2.2.3)")
                .allowEmptyShould(true);

        rule.check(classes);
    }

    @Test
    @DisplayName("8. the shared package holds no business logic")
    void sharedHoldsNoBusinessLogic() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..shared..")
                .should(haveNameEndingWithAny("Policy", "ServiceImpl"))
                .because("shared holds cross-cutting technical utilities only, never business rules")
                .allowEmptyShould(true);

        rule.check(classes);
    }

    @Test
    @DisplayName("the framework rule rejects a deliberate violation")
    void theFrameworkRuleRejectsADeliberateViolation() {
        JavaClasses violating = new ClassFileImporter()
                .importPackages("com.anderk.ecommerce.architecture.fixture");

        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..",
                        "jakarta.persistence..",
                        "jakarta.validation..",
                        "org.hibernate..");

        EvaluationResult result = rule.evaluate(violating);
        org.junit.jupiter.api.Assertions.assertTrue(result.hasViolation(),
                "the framework rule must reject a domain class that leaks a framework import");
    }

    @Test
    @DisplayName("no domain class leaks a framework import — there is no exception left")
    void noDomainClassLeaksAFrameworkImport() {
        for (JavaClass javaClass : classes) {
            if (!isInADomainPackage(javaClass)) {
                continue;
            }
            for (var dependency : javaClass.getDirectDependenciesFromSelf()) {
                String targetPackage = dependency.getTargetClass().getPackageName();
                if (!isForbiddenForTheDomain(targetPackage)) {
                    continue;
                }
                org.junit.jupiter.api.Assertions.fail(
                        "a domain class leaks a framework import: "
                                + javaClass.getName() + " -> " + targetPackage);
            }
        }
    }

    private static boolean isInADomainPackage(JavaClass javaClass) {
        String packageName = javaClass.getPackageName();
        return packageName.contains(".domain.") || packageName.endsWith(".domain");
    }

    private static boolean isForbiddenForTheDomain(String packageName) {
        return packageName.startsWith("org.springframework")
                || packageName.startsWith("jakarta.persistence")
                || packageName.startsWith("jakarta.validation")
                || packageName.startsWith("org.hibernate");
    }

    // --- helpers -----------------------------------------------------------

    private static ArchCondition<JavaClass> haveNameEndingWithAny(String... suffixes) {
        return new ArchCondition<>("have a name ending with " + String.join(" or ", suffixes)) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                for (String suffix : suffixes) {
                    if (item.getSimpleName().endsWith(suffix)) {
                        events.add(SimpleConditionEvent.violated(item,
                                item.getName() + " must not exist: the port is not split by read/write"));
                        return;
                    }
                }
            }
        };
    }

    /** The feature segment of a package name: {@code ...catalog.domain.model} -> {@code catalog}. */
    private static String featureOf(JavaClass javaClass) {
        String packageName = javaClass.getPackageName();
        for (String segment : packageName.split("\\.")) {
            if (FEATURES.contains(segment)) {
                return segment;
            }
        }
        return "";
    }

    private static final Set<String> FEATURES =
            Set.of("catalog", "lead", "order", "tracking", "auth");

    /**
     * The class implements an interface of the application layer of its own
     * feature — the inbound port of the use case.
     */
    private static ArchCondition<JavaClass> implementAnInboundPortOfItsOwnFeature() {
        return new ArchCondition<>("implement an inbound port of its own feature") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                String ownFeature = featureOf(item);
                boolean ok = item.getAllRawInterfaces().stream()
                        .anyMatch(candidate -> {
                            String packageName = candidate.getPackageName();
                            return packageName.startsWith("com.anderk.ecommerce")
                                    && packageName.contains(".application")
                                    && featureOf(candidate).equals(ownFeature);
                        });

                if (!ok) {
                    events.add(SimpleConditionEvent.violated(item,
                            item.getName() + " does not implement an inbound port of its own feature ("
                                    + ownFeature + ")"));
                }
            }
        };
    }
}
