package com.example.lolserver.esports;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * esports 컨텍스트의 헥사고날 레이어 + 컨텍스트 경계 규칙.
 *
 * <p>esports 는 다른 컨텍스트(member·community·duo·championstats)에 의존하지 않는다.
 * 소환사 정보는 puuid 만 다루고, 화면이 lol-repository 에서 직접 받는다.
 * 레이어가 아직 비어 있는 단계에서도 규칙이 깨지지 않도록 {@code allowEmptyShould(true)} 를 둔다.
 */
class ArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.example.lolserver.esports");

    @Test
    void domain은_application이나_adapter에_의존하지_않는다() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..esports.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..esports.application..", "..esports.adapter..")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    void application은_adapter에_의존하지_않는다() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..esports.application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..esports.adapter..")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    void domain과_application은_인프라_기술타입에_의존하지_않는다() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..esports.domain..", "..esports.application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "jakarta.persistence..",
                        "org.springframework.web..",
                        "org.springframework.data.jpa..",
                        "org.springframework.jdbc..",
                        "com.querydsl..")
                .as("도메인/애플리케이션은 JPA·웹·JDBC·QueryDSL 같은 인프라 타입을 몰라야 한다")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    void in어댑터는_out어댑터에_직접_의존하지_않는다() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..esports.adapter.in..")
                .should().dependOnClassesThat()
                .resideInAPackage("..esports.adapter.out..")
                .as("driving 어댑터(웹)는 driven 어댑터(영속성)를 거치지 않고 port.in/port.out 으로만 흐른다")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    void 다른_컨텍스트에_의존하지_않는다() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..esports..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.example.lolserver.member..",
                        "com.example.lolserver.community..",
                        "com.example.lolserver.duo..",
                        "com.example.lolserver.championstats..")
                .as("esports 는 다른 컨텍스트에 의존하지 않는다")
                .allowEmptyShould(true);
        rule.check(classes);
    }
}
