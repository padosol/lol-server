package com.example.lolserver.community.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 카테고리의 로케일별 라벨. PK 는 (category_id, locale) 복합키다.
 */
@Entity
@Table(name = "community_category_i18n")
@IdClass(CommunityCategoryI18nEntity.Pk.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityCategoryI18nEntity {

    @Id
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Id
    @Column(name = "locale", nullable = false, length = 10)
    private String locale;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 300)
    private String description;

    /**
     * JPA 복합키 식별자. record 는 no-arg 생성자가 없어 IdClass 로 쓸 수 없다.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pk implements java.io.Serializable {

        private Long categoryId;
        private String locale;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Pk other)) {
                return false;
            }
            return java.util.Objects.equals(categoryId, other.categoryId)
                    && java.util.Objects.equals(locale, other.locale);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(categoryId, locale);
        }
    }
}
