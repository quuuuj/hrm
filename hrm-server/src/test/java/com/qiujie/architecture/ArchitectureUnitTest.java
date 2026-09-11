package com.qiujie.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * 架构规范守护测试（package-by-feature）。
 * 遵循 ADR 0001 原则：白名单必须为空，硬失败。
 */
public class ArchitectureUnitTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void init() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.qiujie");
    }

    /**
     * 规则 1：分层方向约束
     * - service 可以依赖 mapper/entity/dto/vo/enums，禁止 service 依赖 controller
     * - mapper 可以依赖 entity/enums/dto/vo，禁止 mapper 依赖 service 或 controller
     */
    @Test
    void services_should_not_depend_on_controllers() {
        ArchRule rule = noClasses().that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAPackage("..controller..");
        rule.check(importedClasses);
    }

    @Test
    void mappers_should_not_depend_on_services_or_controllers() {
        ArchRule rule = noClasses().that().resideInAPackage("..mapper..")
                .should().dependOnClassesThat().resideInAnyPackage("..service..", "..controller..");
        rule.check(importedClasses);
    }

    /**
     * 规则 2：模块边界与专属内部子包保护
     * 模块 A 不得访问模块 B 的 controller 及专属内部子包：
     * - leave/approval/
     * - salary/calculation/
     * - filetask/engine/
     * - filetask/store/
     */
    @Test
    void leave_approval_internal_package_access() {
        ArchRule rule = noClasses().that().resideInAPackage("com.qiujie..")
                .and().resideOutsideOfPackages("com.qiujie.leave..")
                .should().dependOnClassesThat().resideInAPackage("com.qiujie.leave.approval..");
        rule.check(importedClasses);
    }

    @Test
    void salary_calculation_internal_package_access() {
        ArchRule rule = noClasses().that().resideInAPackage("com.qiujie..")
                .and().resideOutsideOfPackages("com.qiujie.salary..")
                .should().dependOnClassesThat().resideInAPackage("com.qiujie.salary.calculation..");
        rule.check(importedClasses);
    }

    @Test
    void filetask_engine_internal_package_access() {
        ArchRule rule = noClasses().that().resideInAPackage("com.qiujie..")
                .and().resideOutsideOfPackages("com.qiujie.filetask..")
                .should().dependOnClassesThat().resideInAPackage("com.qiujie.filetask.engine..");
        rule.check(importedClasses);
    }

    @Test
    void filetask_store_internal_package_access() {
        ArchRule rule = noClasses().that().resideInAPackage("com.qiujie..")
                .and().resideOutsideOfPackages("com.qiujie.filetask..")
                .should().dependOnClassesThat().resideInAPackage("com.qiujie.filetask.store..");
        rule.check(importedClasses);
    }

    /**
     * 规则 3：模块间零环（业务模块平铺切片无循环依赖）
     */
    @Test
    void business_modules_should_be_free_of_cycles() {
        ArchRule rule = slices().matching("com.qiujie.(*)..")
                .should().beFreeOfCycles();
        rule.check(importedClasses);
    }

    /**
     * 规则 4：横切包约束
     * 横切包（util/、config/、common/、security/）不得依赖任何业务模块的 controller 或 service。
     */
    @Test
    void infrastructure_packages_should_not_depend_on_business_services_or_controllers() {
        ArchRule rule = noClasses().that().resideInAnyPackage(
                        "com.qiujie.common..",
                        "com.qiujie.config..",
                        "com.qiujie.security..",
                        "com.qiujie.util.."
                )
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.qiujie.staff.controller..", "com.qiujie.staff.service..",
                        "com.qiujie.dept.controller..", "com.qiujie.dept.service..",
                        "com.qiujie.role.controller..", "com.qiujie.role.service..",
                        "com.qiujie.menu.controller..", "com.qiujie.menu.service..",
                        "com.qiujie.auth.controller..", "com.qiujie.auth.service..",
                        "com.qiujie.salary.controller..", "com.qiujie.salary.service..",
                        "com.qiujie.attendance.controller..", "com.qiujie.attendance.service..",
                        "com.qiujie.leave.controller..", "com.qiujie.leave.service..",
                        "com.qiujie.overtime.controller..", "com.qiujie.overtime.service..",
                        "com.qiujie.insurance.controller..", "com.qiujie.insurance.service..",
                        "com.qiujie.city.controller..", "com.qiujie.city.service..",
                        "com.qiujie.notification.controller..", "com.qiujie.notification.service..",
                        "com.qiujie.docs.controller..", "com.qiujie.docs.service..",
                        "com.qiujie.filetask.controller..", "com.qiujie.filetask.service..",
                        "com.qiujie.chat.controller..", "com.qiujie.chat.service..",
                        "com.qiujie.knowledge.controller..", "com.qiujie.knowledge.service..",
                        "com.qiujie.home.controller..", "com.qiujie.home.service.."
                );
        rule.check(importedClasses);
    }
}
