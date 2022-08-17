/*
 * Copyright (c) 2010-2022. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.tracing;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.annotation.MessageHandlingMember;
import org.junit.jupiter.api.*;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TracingHandlerEnhancerDefinitionTest {

    private final MessageHandlingMember<TracingHandlerEnhancerDefinitionTest> original = mock(MessageHandlingMember.class);
    private final SpanFactory spanFactory = mock(SpanFactory.class);
    private final Span span = mock(Span.class);

    private boolean invoked = false;

    @BeforeEach
    void setUp() throws Exception {
        when(spanFactory.createInternalSpan(anyString())).thenReturn(span);
        when(span.runCallable(any())).thenCallRealMethod();

        Method executable = this.getClass().getDeclaredMethod("executable", MyEvent.class, CommandGateway.class);
        when(original.unwrap(Executable.class)).thenReturn(Optional.of(executable));
    }

    private void setupOriginal(boolean eventSourcingHandler) {
        if (eventSourcingHandler) {
            when(original.attribute("EventSourcingHandler.payloadType")).thenReturn(Optional.of("value"));
        } else {
            when(original.attribute("EventSourcingHandler.payloadType")).thenReturn(Optional.empty());
        }
    }

    @Test
    void showsWhenNotEventSourcingHandler() throws Exception {
        setupOriginal(false);

        TracingHandlerEnhancerDefinition definition = new TracingHandlerEnhancerDefinition(spanFactory, false);
        MessageHandlingMember<TracingHandlerEnhancerDefinitionTest> messageHandlingMember = definition.wrapHandler(
                original);
        Message<?> message = mock(Message.class);
        when(original.handle(any(), any())).thenAnswer(invocationOnMock -> {
            verify(span).start();
            invoked = true;
            return null;
        });
        messageHandlingMember.handle(message, this);

        assertTrue(invoked);
        verify(span).end();
        verify(spanFactory).createInternalSpan("TracingHandlerEnhancerDefinitionTest.executable(MyEvent,CommandGateway)");
    }

    @Test
    void doesNotShowWhenEventSourcingHandler() throws Exception {
        setupOriginal(true);

        TracingHandlerEnhancerDefinition definition = new TracingHandlerEnhancerDefinition(spanFactory, false);
        MessageHandlingMember<TracingHandlerEnhancerDefinitionTest> messageHandlingMember = definition.wrapHandler(original);
        assertSame(original, messageHandlingMember);
    }

    @Test
    void showsWhenEventSourcingHandlerButOptionIsTrue() throws Exception {
        setupOriginal(true);

        TracingHandlerEnhancerDefinition definition = new TracingHandlerEnhancerDefinition(spanFactory, true);
        MessageHandlingMember<TracingHandlerEnhancerDefinitionTest> messageHandlingMember = definition.wrapHandler(
                original);
        Message<?> message = mock(Message.class);
        when(original.handle(any(), any())).thenAnswer(invocationOnMock -> {
            verify(span).start();
            invoked = true;
            return null;
        });
        messageHandlingMember.handle(message, this);

        assertTrue(invoked);
        verify(span).end();
        verify(spanFactory).createInternalSpan("TracingHandlerEnhancerDefinitionTest.executable(MyEvent,CommandGateway)");
    }

    private class MyEvent {

    }

    private void executable(MyEvent event, CommandGateway commandGateway) {
    }
}
