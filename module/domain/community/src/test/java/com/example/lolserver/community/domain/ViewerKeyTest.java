package com.example.lolserver.community.domain;

import com.example.lolserver.community.domain.vo.ViewerKey;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ViewerKeyTest {

    @Test
    void 회원키는_m_접두사() {
        assertThat(ViewerKey.ofMember(7L).value()).isEqualTo("m:7");
    }

    @Test
    void 방문자키는_v_접두사() {
        UUID id = UUID.fromString("0b7c6d7e-1f2a-4b3c-8d4e-5f6a7b8c9d0e");
        assertThat(ViewerKey.ofVisitor(id).value()).isEqualTo("v:0b7c6d7e-1f2a-4b3c-8d4e-5f6a7b8c9d0e");
    }

    @Test
    void null_인자는_NPE() {
        assertThatThrownBy(() -> ViewerKey.ofMember(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ViewerKey.ofVisitor(null)).isInstanceOf(NullPointerException.class);
    }
}
