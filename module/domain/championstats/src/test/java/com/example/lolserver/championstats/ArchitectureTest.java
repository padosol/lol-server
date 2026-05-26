package com.example.lolserver.championstats;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * championstats 컨텍스트의 헥사고날 레이어 + 컨텍스트 경계 규칙.
 *
 * <p>championstats 는 도메인 애그리거트가 없는 read-only(OLAP) 컨텍스트이므로
 * domain 패키지가 없다. 따라서 domain 관련 규칙은 제외하고 application/adapter 규칙만 검증한다.
 */
class ArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.example.lolserver.championstats");

    @Test
    void application은_adapter에_의존하지_않는다() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..championstats.application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..championstats.adapter..");
        rule.check(classes);
    }

    @Test
    void in어댑터는_out어댑터에_직접_의존하지_않는다() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..championstats.adapter.in..")
                .should().dependOnClassesThat()
                .resideInAPackage("..championstats.adapter.out..")
                .as("driving 어댑터(웹)는 driven 어댑터(영속성)를 거치지 않고 port.in/port.out 으로만 흐른다");
        rule.check(classes);
    }

    @Test
    void application은_인프라_기술타입에_의존하지_않는다() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..championstats.application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "jakarta.persistence..",
                        "org.springframework.web..")
                .as("애플리케이션은 JPA·웹 같은 인프라 타입을 몰라야 한다");
        rule.check(classes);
    }

    @Test
    void 타_컨텍스트의_내부에_의존하지_않는다() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..championstats..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.example.lolserver.member..",
                        "com.example.lolserver.summoner..",
                        "com.example.lolserver.match..",
                        "com.example.lolserver.community..",
                        "com.example.lolserver.duo..",
                        "com.example.lolserver.gamedata..",
                        "com.example.lolserver.leaderboard..")
                .as("championstats 는 다른 컨텍스트에 의존하지 않는다");
        rule.check(classes);
    }
}
