package com.example.lolserver.duo.adapter.in.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SseEmitterRegistryTest {

    private SseEmitterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SseEmitterRegistry();
    }

    @DisplayName("register 하면 해당 멤버의 emitter 가 보관된다")
    @Test
    void register_storesEmitter() {
        SseEmitter emitter = mock(SseEmitter.class);

        SseEmitter returned = registry.register(1L, emitter);

        assertThat(returned).isSameAs(emitter);
        assertThat(registry.contains(1L)).isTrue();
    }

    @DisplayName("send 는 등록된 멤버의 emitter 로 이벤트를 전송한다")
    @Test
    void send_registered_sendsEvent() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        registry.register(1L, emitter);

        registry.send(1L, "duo-notification", "payload");

        then(emitter).should().send(any(SseEmitter.SseEventBuilder.class));
    }

    @DisplayName("send 는 등록되지 않은 멤버면 아무 일도 하지 않는다")
    @Test
    void send_unregistered_doesNothing() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        registry.register(1L, emitter);

        assertThatCode(() -> registry.send(999L, "duo-notification", "payload"))
                .doesNotThrowAnyException();
        then(emitter).should(never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @DisplayName("send 중 IOException 이 나면 죽은 emitter 를 제거한다")
    @Test
    void send_ioException_removesDeadEmitter() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        willThrow(new IOException("broken pipe"))
                .given(emitter).send(any(SseEmitter.SseEventBuilder.class));
        registry.register(1L, emitter);

        registry.send(1L, "duo-notification", "payload");

        assertThat(registry.contains(1L)).isFalse();
        then(emitter).should().completeWithError(any(IOException.class));
    }

    @DisplayName("emitter 완료 콜백이 호출되면 보관소에서 제거된다")
    @Test
    void register_completionCallback_removesEmitter() {
        SseEmitter emitter = mock(SseEmitter.class);
        ArgumentCaptor<Runnable> completionCaptor = ArgumentCaptor.forClass(Runnable.class);
        registry.register(1L, emitter);
        then(emitter).should().onCompletion(completionCaptor.capture());

        completionCaptor.getValue().run();

        assertThat(registry.contains(1L)).isFalse();
    }
}
