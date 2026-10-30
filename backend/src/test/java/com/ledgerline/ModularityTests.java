package com.ledgerline;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Architecture tests for the module structure. These run without Spring or a database: Spring
 * Modulith reads the compiled classes and checks how the packages depend on each other.
 */
class ModularityTests {

    private static final ApplicationModules MODULES = ApplicationModules.of(LedgerlineApplication.class);

    /**
     * Fails if a module uses another module's internal (sub-package) types, depends on a module that
     * is not in its {@code allowedDependencies}, or if module dependencies form a cycle.
     */
    @Test
    void modulesRespectTheirBoundaries() {
        MODULES.verify();
    }

    /**
     * Writes a component diagram of all modules and one per module (PlantUML and AsciiDoc) to {@code
     * target/spring-modulith-docs}, so the real structure can be compared with the design.
     */
    @Test
    void writesModuleDocumentation() {
        new Documenter(MODULES).writeDocumentation();
    }
}
