package com.example.lolserver.community;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * community 컨텍스트의 헥사고날 레이어 + 컨텍스트 경계 규칙.
 *
 * <p>community 는 member 컨텍스트에 의존하는 하위(다운스트림) 컨텍스트다.
 * 경계 정책(순수 경계): 상위 컨텍스트는 application.port.in(UseCase) + application.model(ReadModel) 로만
 * 의존 가능하며, 상위의 domain / adapter / application.port.out 에는 의존하지 않는다.
 */
class ArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.example.lolserver.community");

    @Test
    void domain은_application이나_adapter에_의존하지_않는다() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..community.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..community.application..", "..community.adapter..");
        rule.check(classes);
    }

    @Test
    void application은_adapter에_의존하지_않는다() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..community.application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..community.adapter..");
        rule.check(classes);
    }

    @Test
    void domain과_application은_인프라_기술타입에_의존하지_않는다() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..community.domain..", "..community.application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "jakarta.persistence..",
                        "org.springframework.web..",
                        "org.springframework.data.jpa..",
                        "com.querydsl..")
                .as("도메인/애플리케이션은 JPA·웹·QueryDSL 같은 인프라 타입을 몰라야 한다");
        rule.check(classes);
    }

    /**
     * 위 규칙의 목록에 없는 기술이라도 스토리지·이미지 처리 SDK 는 애플리케이션에 새어 들어오면
     * 안 된다. {@code ImageStoragePort}/{@code ImageProcessorPort} 가 바이트만 주고받도록
     * 설계한 이유가 여기 있고, 이 규칙이 없으면 "포트를 우회해 S3Client 를 바로 쓰는" 지름길이
     * 조용히 생긴다.
     */
    @Test
    void domain과_application은_스토리지_이미지_SDK에_의존하지_않는다() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..community.domain..", "..community.application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "software.amazon.awssdk..",
                        "org.apache.tika..",
                        "javax.imageio..",
                        "java.awt..")
                .as("이미지 저장·디코딩 기술은 adapter.out 안에만 있어야 한다");
        rule.check(classes);
    }

    @Test
    void in어댑터는_out어댑터에_직접_의존하지_않는다() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..community.adapter.in..")
                .should().dependOnClassesThat()
                .resideInAPackage("..community.adapter.out..")
                .as("driving 어댑터(웹)는 driven 어댑터(영속성)를 거치지 않고 port.in/port.out 으로만 흐른다");
        rule.check(classes);
    }

    @Test
    void 상위컨텍스트_member의_내부에_의존하지_않는다() {
        // 규칙: member 는 application.port.in(UseCase) + application.model(ReadModel) 로만 의존 가능.
        // member.domain / member.adapter / member.application.port.out 직접 의존은 금지한다.
        ArchRule rule = noClasses()
                .that().resideInAPackage("..community..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.example.lolserver.member.domain..",
                        "com.example.lolserver.member.adapter..",
                        "com.example.lolserver.member.application.port.out..")
                .as("community 는 member 의 내부(domain/adapter/port.out)가 아닌 port.in + model 로만 의존한다");
        rule.check(classes);
    }

    @Test
    void 그외_컨텍스트에는_의존하지_않는다() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..community..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.example.lolserver.summoner..",
                        "com.example.lolserver.match..",
                        "com.example.lolserver.leaderboard..",
                        "com.example.lolserver.gamedata..",
                        "com.example.lolserver.championstats..",
                        "com.example.lolserver.duo..")
                .as("community 는 member 외 다른 컨텍스트에 의존하지 않는다");
        rule.check(classes);
    }
}
