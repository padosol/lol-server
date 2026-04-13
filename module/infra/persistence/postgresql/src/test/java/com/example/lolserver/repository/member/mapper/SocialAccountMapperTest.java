package com.example.lolserver.repository.member.mapper;

import com.example.lolserver.domain.member.domain.SocialAccount;
import com.example.lolserver.repository.member.SocialAccountMapper;
import com.example.lolserver.repository.member.SocialAccountMapperImpl;
import com.example.lolserver.repository.member.entity.MemberEntity;
import com.example.lolserver.repository.member.entity.SocialAccountEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SocialAccountMapperTest {

    private final SocialAccountMapper mapper = new SocialAccountMapperImpl();

    @DisplayName("SocialAccountEntity를 SocialAccount 도메인으로 변환한다")
    @Test
    void toDomain_validEntity_returnsSocialAccount() {
        // given
        MemberEntity memberEntity = MemberEntity.builder()
                .id(1L)
                .build();
        LocalDateTime linkedAt = LocalDateTime.of(2026, 1, 15, 10, 0);
        SocialAccountEntity entity = SocialAccountEntity.builder()
                .id(10L)
                .member(memberEntity)
                .provider("GOOGLE")
                .providerId("google-123")
                .email("test@gmail.com")
                .nickname("TestUser")
                .profileImageUrl("https://example.com/photo.jpg")
                .puuid("test-puuid-123")
                .linkedAt(linkedAt)
                .build();

        // when
        SocialAccount result = mapper.toDomain(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getMemberId()).isEqualTo(1L);
        assertThat(result.getProvider()).isEqualTo("GOOGLE");
        assertThat(result.getProviderId()).isEqualTo("google-123");
        assertThat(result.getEmail()).isEqualTo("test@gmail.com");
        assertThat(result.getNickname()).isEqualTo("TestUser");
        assertThat(result.getProfileImageUrl())
                .isEqualTo("https://example.com/photo.jpg");
        assertThat(result.getPuuid()).isEqualTo("test-puuid-123");
        assertThat(result.getLinkedAt()).isEqualTo(linkedAt);
    }

    @DisplayName("member가 null인 엔티티를 변환하면 memberId는 null이다")
    @Test
    void toDomain_nullMember_memberIdIsNull() {
        // given
        SocialAccountEntity entity = SocialAccountEntity.builder()
                .id(10L)
                .member(null)
                .provider("GOOGLE")
                .providerId("google-123")
                .linkedAt(LocalDateTime.now())
                .build();

        // when
        SocialAccount result = mapper.toDomain(entity);

        // then
        assertThat(result.getMemberId()).isNull();
    }

    @DisplayName("SocialAccount 도메인을 SocialAccountEntity로 변환한다")
    @Test
    void toEntity_validDomain_returnsEntity() {
        // given
        LocalDateTime linkedAt = LocalDateTime.of(2026, 1, 15, 10, 0);
        SocialAccount domain = SocialAccount.builder()
                .id(10L)
                .memberId(1L)
                .provider("DISCORD")
                .providerId("discord-456")
                .email("test@discord.com")
                .nickname("DiscordUser")
                .profileImageUrl("https://cdn.discord.com/avatar.png")
                .puuid("discord-puuid-456")
                .linkedAt(linkedAt)
                .build();

        // when
        SocialAccountEntity result = mapper.toEntity(domain);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getMember()).isNull();
        assertThat(result.getProvider()).isEqualTo("DISCORD");
        assertThat(result.getProviderId()).isEqualTo("discord-456");
        assertThat(result.getEmail()).isEqualTo("test@discord.com");
        assertThat(result.getPuuid()).isEqualTo("discord-puuid-456");
        assertThat(result.getLinkedAt()).isEqualTo(linkedAt);
    }

    @DisplayName("updateEntityFromDomain은 id, member, linkedAt을 변경하지 않는다")
    @Test
    void updateEntityFromDomain_updatesOnlyMappedFields() {
        // given
        MemberEntity memberEntity = MemberEntity.builder()
                .id(1L)
                .build();
        LocalDateTime originalLinkedAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        SocialAccountEntity entity = SocialAccountEntity.builder()
                .id(10L)
                .member(memberEntity)
                .provider("GOOGLE")
                .providerId("google-old")
                .email("old@gmail.com")
                .nickname("OldName")
                .profileImageUrl("https://old.com/photo.jpg")
                .linkedAt(originalLinkedAt)
                .build();

        SocialAccount updated = SocialAccount.builder()
                .id(99L)
                .memberId(99L)
                .provider("GOOGLE_UPDATED")
                .providerId("google-new")
                .email("new@gmail.com")
                .nickname("NewName")
                .profileImageUrl("https://new.com/photo.jpg")
                .puuid("new-puuid")
                .linkedAt(LocalDateTime.of(2026, 12, 31, 23, 59))
                .build();

        // when
        mapper.updateEntityFromDomain(updated, entity);

        // then
        assertThat(entity.getId()).isEqualTo(10L);
        assertThat(entity.getMember()).isEqualTo(memberEntity);
        assertThat(entity.getLinkedAt()).isEqualTo(originalLinkedAt);
        assertThat(entity.getProvider()).isEqualTo("GOOGLE_UPDATED");
        assertThat(entity.getProviderId()).isEqualTo("google-new");
        assertThat(entity.getEmail()).isEqualTo("new@gmail.com");
        assertThat(entity.getNickname()).isEqualTo("NewName");
        assertThat(entity.getProfileImageUrl())
                .isEqualTo("https://new.com/photo.jpg");
        assertThat(entity.getPuuid()).isEqualTo("new-puuid");
    }

    @DisplayName("SocialAccountEntity 리스트를 도메인 리스트로 변환한다")
    @Test
    void toDomainList_entityList_returnsDomainList() {
        // given
        MemberEntity memberEntity = MemberEntity.builder()
                .id(1L)
                .build();
        SocialAccountEntity entity1 = SocialAccountEntity.builder()
                .id(1L)
                .member(memberEntity)
                .provider("GOOGLE")
                .providerId("g-1")
                .linkedAt(LocalDateTime.now())
                .build();
        SocialAccountEntity entity2 = SocialAccountEntity.builder()
                .id(2L)
                .member(memberEntity)
                .provider("DISCORD")
                .providerId("d-1")
                .linkedAt(LocalDateTime.now())
                .build();

        // when
        List<SocialAccount> result = mapper.toDomainList(
                List.of(entity1, entity2));

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProvider()).isEqualTo("GOOGLE");
        assertThat(result.get(1).getProvider()).isEqualTo("DISCORD");
    }

    @DisplayName("빈 엔티티 리스트를 변환하면 빈 리스트를 반환한다")
    @Test
    void toDomainList_emptyList_returnsEmptyList() {
        // when
        List<SocialAccount> result = mapper.toDomainList(List.of());

        // then
        assertThat(result).isEmpty();
    }
}
