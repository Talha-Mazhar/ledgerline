package com.ledgerline;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

/**
 * NullAway only checks code in {@code @NullMarked} packages, and the annotation does not apply to
 * sub-packages. This test closes the gap: a new package without a null-marked {@code
 * package-info.java} fails the build, so no production code escapes the null checks.
 */
class NullSafetyTests {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.ledgerline");

    @Test
    void everyProductionPackageIsNullMarked() {
        classes().should(beInANullMarkedPackage()).check(PRODUCTION_CLASSES);
    }

    private static ArchCondition<JavaClass> beInANullMarkedPackage() {
        return new ArchCondition<>("be in a package annotated with @NullMarked") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                String packageName = javaClass.getPackageName();
                if (!javaClass.getPackage().isAnnotatedWith(NullMarked.class)) {
                    events.add(SimpleConditionEvent.violated(
                            javaClass, packageName + " needs a package-info.java annotated with @NullMarked"));
                }
            }
        };
    }
}
