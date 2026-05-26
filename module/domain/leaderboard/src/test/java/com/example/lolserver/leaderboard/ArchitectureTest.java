package com.example.lolserver.leaderboard;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * leaderboard 컨텍스트의 헥사고날 레이어 + 컨텍스트 경계 규칙.
 *
 * <p>이 테스트는 모든 수직 컨텍스트 모듈에 복제하거나, 추후 공유 규칙 라이브러리(common testFixtures
 * 또는 별도 architecture-test 모듈)로 승격할 템플릿이다. CONTEXT 상수와 타 컨텍스트 목록만 바꾸면 된다.
 */
class ArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.example.lolserver.leaderboard");

    @Test
    void domain은_application이나_adapter에_의존하지_않는다() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..leaderboard.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..leaderboard.application..", "..leaderboard.adapter..");
        rule.check(classes);
    }

    @Test
    void application은_adapter에_의존하지_않는다() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..leaderboard.application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..leaderboard.adapter..");
        rule.check(classes);
    }

    @Test
    void domain과_application은_인프라_기술타입에_의존하지_않는다() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..leaderboard.domain..", "..leaderboard.application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "jakarta.persistence..",
                        "org.springframework.web..",
                        "org.springframework.data.jpa..",
                        "com.querydsl..")
                .as("도메인/애플리케이션은 JPA·웹·QueryDSL 같은 인프라 타입을 몰라야 한다");
        rule.check(classes);
    }

    @Test
    void in어댑터는_out어댑터에_직접_의존하지_않는다() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..leaderboard.adapter.in..")
                .should().dependOnClassesThat()
                .resideInAPackage("..leaderboard.adapter.out..")
                .as("driving 어댑터(웹)는 driven 어댑터(영속성)를 거치지 않고 port.in/port.out 으로만 흐른다");
        rule.check(classes);
    }

    @Test
    void 타_컨텍스트의_내부에_의존하지_않는다() {
        // 규칙: 타 컨텍스트는 application.port.in(UseCase) + application.model(ReadModel) 로만 의존 가능.
        // leaderboard 는 리프 컨텍스트이므로 타 컨텍스트 패키지에 전혀 의존하지 않아야 한다.
        ArchRule rule = noClasses()
                .that().resideInAPackage("..leaderboard..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.example.lolserver.member..",
                        "com.example.lolserver.summoner..",
                        "com.example.lolserver.match..",
                        "com.example.lolserver.community..",
                        "com.example.lolserver.duo..",
                        "com.example.lolserver.gamedata..",
                        "com.example.lolserver.championstats..")
                .as("리프 컨텍스트 leaderboard 는 다른 컨텍스트에 의존하지 않는다");
        rule.check(classes);
    }
}
