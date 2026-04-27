package com.toanlv.flashsale.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "com.toanlv.flashsale")
class ArchitectureTest {

    // ----------------------------------------------------------------
    // Bounded context dependency rules
    // ----------------------------------------------------------------

    @ArchTest
    static final ArchRule auth_does_not_depend_on_domain_packages =
            noClasses()
                    .that().resideInAPackage("..auth..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.toanlv.flashsale.flashsale..",
                            "..inventory..",
                            "..order.."
                    )
                    .as("auth must not depend on flashsale, inventory, or order");

    @ArchTest
    static final ArchRule product_does_not_depend_on_flashsale_or_order =
            noClasses()
                    .that().resideInAPackage("..product..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.toanlv.flashsale.flashsale..",
                            "..order.."
                    )
                    .as("product must not depend on flashsale or order");

    @ArchTest
    static final ArchRule common_does_not_depend_on_domain_packages =
            noClasses()
                    .that().resideInAPackage("..common..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..auth..",
                            "..product..",
                            "com.toanlv.flashsale.flashsale..",
                            "..inventory..",
                            "..order.."
                    )
                    .as("common must not depend on any domain package");

    // ----------------------------------------------------------------
    // Layer rules
    // ----------------------------------------------------------------

    @ArchTest
    static final ArchRule controllers_reside_in_controller_package =
            classes()
                    .that().areAnnotatedWith(RestController.class)
                    .should().resideInAPackage("..controller..")
                    .as("@RestController classes must reside in controller packages");

    @ArchTest
    static final ArchRule services_reside_in_service_or_worker_package =
            classes()
                    .that().areAnnotatedWith(Service.class)
                    .should().resideInAnyPackage(
                            "..service..",
                            "..strategy..",
                            "..outbox.."
                    )
                    .as("@Service classes must reside in service, strategy, or outbox packages");

    @ArchTest
    static final ArchRule repositories_reside_in_repository_package =
            classes()
                    .that().areAnnotatedWith(Repository.class)
                    .should().resideInAPackage("..repository..")
                    .as("@Repository classes must reside in repository packages");

    @ArchTest
    static final ArchRule controllers_do_not_access_repositories_directly =
            noClasses()
                    .that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..repository..")
                    .as("controllers must not access repositories directly — go through service layer");

    // ----------------------------------------------------------------
    // No circular dependencies between slices
    // ----------------------------------------------------------------

    @ArchTest
    static final ArchRule no_cyclic_dependencies =
            slices()
                    .matching("com.toanlv.flashsale.(*)..")
                    .should().beFreeOfCycles()
                    .as("bounded contexts must not have cyclic dependencies");
}
