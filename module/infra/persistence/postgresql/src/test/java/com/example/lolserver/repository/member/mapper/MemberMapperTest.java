package com.example.lolserver.repository.member.mapper;

import com.example.lolserver.domain.member.domain.Member;
import com.example.lolserver.domain.member.domain.SocialAccount;
import com.example.lolserver.repository.member.MemberMapper;
import com.example.lolserver.repository.member.MemberMapperImpl;
import com.example.lolserver.repository.member.SocialAccountMapperImpl;
import com.example.lolserver.repository.member.entity.MemberEntity;
import com.example.lolserver.repository.member.entity.SocialAccountEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MemberMapperTest {

    private final MemberMapper mapper;

    MemberMapperTest() {
        MemberMapperImpl impl = new MemberMapperImpl();
        ReflectionTestUtils.setField(
                impl, "socialAccountMapper",
                new SocialAccountMapperImpl());
        this.mapper = impl;
    }

    @DisplayName("MemberEntity를 Member 도메인으로 변환한다 (socialAccounts 미포함)")
    @Test
    void toDomain_validEntity_returnsMember() {
        // given
        LocalDateTime now = LocalDateTime.now();
        MemberEntity entity = MemberEntity.builder()
                .id(1L)
                .uuid("test-uuid")
                .email("test@example.com")
                .nickname("테스터")
                .profileImageUrl("https://example.com/photo.jpg")
                .role("USER")
                .createdAt(now)
                .lastLoginAt(now)
                .build();

        // when
        Member result = mapper.toDomain(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUuid()).isEqualTo("test-uuid");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getNickname()).isEqualTo("테스터");
        assertThat(result.getProfileImageUrl())
                .isEqualTo("https://example.com/photo.jpg");
        assertThat(result.getRole()).isEqualTo("USER");
        assertThat(result.getCreatedAt()).isEqualTo(now);
        assertThat(result.getLastLoginAt()).isEqualTo(now);
        assertThat(result.isSocialAccountsLoaded()).isFalse();
    }

    @DisplayName("MemberEntity를 socialAccounts 포함하여 도메인으로 변환한다")
    @Test
    void toDomainWithSocialAccounts_entityWithAccounts_returnsMemberWithAccounts() {
        // given
        LocalDateTime now = LocalDateTime.now();
        MemberEntity entity = MemberEntity.builder()
                .id(1L)
                .uuid("test-uuid")
                .email("test@example.com")
                .nickname("테스터")
                .role("USER")
                .createdAt(now)
                .lastLoginAt(now)
                .socialAccounts(new ArrayList<>())
                .build();

        SocialAccountEntity saEntity = SocialAccountEntity.builder()
                .id(10L)
                .member(entity)
                .provider("GOOGLE")
                .providerId("g-123")
                .email("test@gmail.com")
                .nickname("GoogleUser")
                .linkedAt(now)
                .build();
        entity.getSocialAccounts().add(saEntity);

        // when
        Member result = mapper.toDomainWithSocialAccounts(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.isSocialAccountsLoaded()).isTrue();
        assertThat(result.getSocialAccounts()).hasSize(1);
        assertThat(result.getSocialAccounts().get(0).getProvider())
                .isEqualTo("GOOGLE");
        assertThat(result.getSocialAccounts().get(0).getMemberId())
                .isEqualTo(1L);
    }

    @DisplayName("socialAccounts가 비어있는 엔티티를 toDomainWithSocialAccounts로 변환하면 빈 리스트를 가진다")
    @Test
    void toDomainWithSocialAccounts_noAccounts_returnsEmptyList() {
        // given
        MemberEntity entity = MemberEntity.builder()
                .id(1L)
                .uuid("test-uuid")
                .nickname("테스터")
                .role("USER")
                .createdAt(LocalDateTime.now())
                .build();

        // when
        Member result = mapper.toDomainWithSocialAccounts(entity);

        // then
        assertThat(result.isSocialAccountsLoaded()).isTrue();
        assertThat(result.getSocialAccounts()).isEmpty();
    }

    @DisplayName("Member 도메인을 MemberEntity로 변환한다 (socialAccounts 미포함)")
    @Test
    void toEntity_validMember_returnsEntity() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Member member = Member.builder()
                .id(1L)
                .uuid("test-uuid")
                .email("test@example.com")
                .nickname("테스터")
                .profileImageUrl("https://example.com/photo.jpg")
                .role("USER")
                .createdAt(now)
                .lastLoginAt(now)
                .build();

        // when
        MemberEntity result = mapper.toEntity(member);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUuid()).isEqualTo("test-uuid");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getNickname()).isEqualTo("테스터");
        assertThat(result.getRole()).isEqualTo("USER");
        assertThat(result.getSocialAccounts()).isEmpty();
    }

    @DisplayName("updateEntityFromDomain은 id와 socialAccounts를 변경하지 않는다")
    @Test
    void updateEntityFromDomain_updatesOnlyMappedFields() {
        // given
        LocalDateTime originalCreatedAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        MemberEntity entity = MemberEntity.builder()
                .id(1L)
                .uuid("original-uuid")
                .email("old@example.com")
                .nickname("원래이름")
                .role("USER")
                .createdAt(originalCreatedAt)
                .build();

        SocialAccountEntity existingSa = SocialAccountEntity.builder()
                .id(10L)
                .member(entity)
                .provider("GOOGLE")
                .providerId("g-123")
                .linkedAt(LocalDateTime.now())
                .build();
        entity.getSocialAccounts().add(existingSa);

        LocalDateTime newLoginAt = LocalDateTime.of(2026, 6, 1, 12, 0);
        Member updatedMember = Member.builder()
                .id(99L)
                .uuid("updated-uuid")
                .email("new@example.com")
                .nickname("새이름")
                .role("ADMIN")
                .createdAt(originalCreatedAt)
                .lastLoginAt(newLoginAt)
                .build();

        // when
        mapper.updateEntityFromDomain(updatedMember, entity);

        // then
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getUuid()).isEqualTo("updated-uuid");
        assertThat(entity.getEmail()).isEqualTo("new@example.com");
        assertThat(entity.getNickname()).isEqualTo("새이름");
        assertThat(entity.getRole()).isEqualTo("ADMIN");
        assertThat(entity.getLastLoginAt()).isEqualTo(newLoginAt);
        assertThat(entity.getSocialAccounts()).hasSize(1);
        assertThat(entity.getSocialAccounts().get(0).getId())
                .isEqualTo(10L);
    }

    @DisplayName("updateEntityFromDomain 호출 시 socialAccounts에 중복 엔티티가 추가되지 않는다")
    @Test
    void updateEntityFromDomain_doesNotDuplicateSocialAccounts() {
        // given
        MemberEntity entity = MemberEntity.builder()
                .id(1L)
                .uuid("test-uuid")
                .nickname("테스터")
                .role("USER")
                .createdAt(LocalDateTime.now())
                .build();

        SocialAccountEntity existingSa = SocialAccountEntity.builder()
                .id(19L)
                .member(entity)
                .provider("GOOGLE")
                .providerId("g-123")
                .linkedAt(LocalDateTime.now())
                .build();
        entity.getSocialAccounts().add(existingSa);

        Member memberWithSocialAccounts = Member.builder()
                .id(1L)
                .uuid("test-uuid")
                .nickname("테스터")
                .role("USER")
                .createdAt(LocalDateTime.now())
                .socialAccounts(List.of(
                        SocialAccount.builder()
                                .id(19L)
                                .memberId(1L)
                                .provider("GOOGLE")
                                .providerId("g-123")
                                .linkedAt(LocalDateTime.now())
                                .build()))
                .build();

        // when
        mapper.updateEntityFromDomain(memberWithSocialAccounts, entity);

        // then - socialAccounts 컬렉션에 중복이 추가되면 안 된다
        assertThat(entity.getSocialAccounts()).hasSize(1);
        assertThat(entity.getSocialAccounts().get(0).getId())
                .isEqualTo(19L);
    }

    @DisplayName("SocialAccount 도메인을 SocialAccountEntity로 변환한다 (member 미포함)")
    @Test
    void toSocialAccountEntity_validDomain_returnsEntity() {
        // given
        LocalDateTime linkedAt = LocalDateTime.of(2026, 3, 1, 10, 0);
        SocialAccount sa = SocialAccount.builder()
                .id(10L)
                .memberId(1L)
                .provider("DISCORD")
                .providerId("discord-789")
                .email("test@discord.com")
                .nickname("DiscordUser")
                .profileImageUrl("https://cdn.discord.com/avatar.png")
                .linkedAt(linkedAt)
                .build();

        // when
        SocialAccountEntity result = mapper.toSocialAccountEntity(sa);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getMember()).isNull();
        assertThat(result.getProvider()).isEqualTo("DISCORD");
        assertThat(result.getProviderId()).isEqualTo("discord-789");
        assertThat(result.getEmail()).isEqualTo("test@discord.com");
        assertThat(result.getLinkedAt()).isEqualTo(linkedAt);
    }

    @DisplayName("setSocialAccountRelationships는 새 엔티티에 socialAccount를 추가하고 양방향 관계를 설정한다")
    @Test
    void setSocialAccountRelationships_addsAccountsWithBidirectionalRelation() {
        // given
        MemberEntity entity = MemberEntity.builder()
                .uuid("test-uuid")
                .nickname("테스터")
                .role("USER")
                .createdAt(LocalDateTime.now())
                .build();

        Member member = Member.builder()
                .uuid("test-uuid")
                .nickname("테스터")
                .role("USER")
                .createdAt(LocalDateTime.now())
                .socialAccounts(List.of(
                        SocialAccount.builder()
                                .provider("GOOGLE")
                                .providerId("g-123")
                                .email("test@gmail.com")
                                .linkedAt(LocalDateTime.now())
                                .build()))
                .build();

        // when
        mapper.setSocialAccountRelationships(member, entity);

        // then
        assertThat(entity.getSocialAccounts()).hasSize(1);
        assertThat(entity.getSocialAccounts().get(0).getProvider())
                .isEqualTo("GOOGLE");
        assertThat(entity.getSocialAccounts().get(0).getMember())
                .isSameAs(entity);
    }

    @DisplayName("setSocialAccountRelationships는 socialAccounts가 로드되지 않은 경우 아무것도 하지 않는다")
    @Test
    void setSocialAccountRelationships_notLoaded_doesNothing() {
        // given
        MemberEntity entity = MemberEntity.builder()
                .uuid("test-uuid")
                .nickname("테스터")
                .role("USER")
                .createdAt(LocalDateTime.now())
                .build();

        Member member = Member.builder()
                .uuid("test-uuid")
                .nickname("테스터")
                .role("USER")
                .createdAt(LocalDateTime.now())
                .build();

        // when
        mapper.setSocialAccountRelationships(member, entity);

        // then
        assertThat(entity.getSocialAccounts()).isEmpty();
    }
}
