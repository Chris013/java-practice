package io.github.chris013.practice.task35;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ForwardingSetTest {

    @Test
    void rejects_a_null_delegate() {
        assertThatThrownBy(() -> new ForwardingSet<String>(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("delegate");
    }

    @Nested
    class Forwarding {

        static Stream<Arguments> set_methods() {
            return Stream.of(
                    Arguments.of("size()", (Consumer<Set<String>>) s -> s.size()),
                    Arguments.of("isEmpty()", (Consumer<Set<String>>) s -> s.isEmpty()),
                    Arguments.of("contains(Object)", (Consumer<Set<String>>) s -> s.contains("A")),
                    Arguments.of("iterator()", (Consumer<Set<String>>) s -> s.iterator()),
                    Arguments.of("toArray()", (Consumer<Set<String>>) s -> s.toArray()),
                    Arguments.of("toArray(Object[])", (Consumer<Set<String>>) s -> s.toArray(new String[0])),
                    Arguments.of("toArray(IntFunction)", (Consumer<Set<String>>) s -> s.toArray(String[]::new)),
                    Arguments.of("add(Object)", (Consumer<Set<String>>) s -> s.add("C")),
                    Arguments.of("remove(Object)", (Consumer<Set<String>>) s -> s.remove("A")),
                    Arguments.of("containsAll(Collection)", (Consumer<Set<String>>) s -> s.containsAll(List.of("A"))),
                    Arguments.of("addAll(Collection)", (Consumer<Set<String>>) s -> s.addAll(List.of("C"))),
                    Arguments.of("retainAll(Collection)", (Consumer<Set<String>>) s -> s.retainAll(List.of("A"))),
                    Arguments.of("removeAll(Collection)", (Consumer<Set<String>>) s -> s.removeAll(List.of("A"))),
                    Arguments.of("removeIf(Predicate)", (Consumer<Set<String>>) s -> s.removeIf("A"::equals)),
                    Arguments.of("clear()", (Consumer<Set<String>>) s -> s.clear()),
                    Arguments.of("forEach(Consumer)", (Consumer<Set<String>>) s -> s.forEach(x -> { })),
                    Arguments.of("spliterator()", (Consumer<Set<String>>) s -> s.spliterator()),
                    Arguments.of("equals(Object)", (Consumer<Set<String>>) s -> s.equals(Set.of())),
                    Arguments.of("hashCode()", (Consumer<Set<String>>) s -> s.hashCode()),
                    Arguments.of("toString()", (Consumer<Set<String>>) s -> s.toString()));
        }

        @ParameterizedTest(name = "forwards {0}")
        @MethodSource("set_methods")
        void forwards_every_set_method_to_the_delegate(String method, Consumer<Set<String>> call) {
            List<String> delegateCalls = new ArrayList<>();
            Set<String> wrapper = new ForwardingSet<>(recording(new LinkedHashSet<>(List.of("A", "B")), delegateCalls));

            call.accept(wrapper);

            assertThat(delegateCalls).contains(method);
        }

        @Test
        void returns_what_the_delegate_returns() {
            Set<String> wrapper = new ForwardingSet<>(new HashSet<>(List.of("A", "B")));

            assertThat(wrapper.size()).isEqualTo(2);
            assertThat(wrapper.contains("A")).isTrue();
            assertThat(wrapper.add("A")).isFalse();
            assertThat(wrapper.add("C")).isTrue();
            assertThat(wrapper.removeAll(List.of("C", "Z"))).isTrue();
            assertThat(wrapper).containsExactlyInAnyOrder("A", "B");
        }

        @Test
        void a_wrapper_equals_and_hashes_like_the_delegate() {
            Set<String> delegate = new HashSet<>(List.of("A", "B"));
            Set<String> wrapper = new ForwardingSet<>(delegate);

            assertThat(wrapper).isEqualTo(delegate);
            assertThat(delegate).isEqualTo(wrapper);
            assertThat(wrapper.hashCode()).isEqualTo(delegate.hashCode());
            assertThat(wrapper.toString()).isEqualTo(delegate.toString());
        }
    }

    @Nested
    class SelfUse {

        /** Counts calls to {@code add} made on the wrapper itself. */
        private static final class AddCountingSet extends ForwardingSet<String> {

            int adds;

            AddCountingSet(Set<String> delegate) {
                super(delegate);
            }

            @Override
            public boolean add(String element) {
                adds++;
                return super.add(element);
            }
        }

        @Test
        void add_all_does_not_call_the_overridden_add_of_the_wrapper() {
            AddCountingSet wrapper = new AddCountingSet(new HashSet<>());

            wrapper.addAll(List.of("A", "B"));

            assertThat(wrapper.adds).isZero();
            assertThat(wrapper).containsExactlyInAnyOrder("A", "B");
        }

        @Test
        void is_empty_does_not_call_the_overridden_size_of_the_wrapper() {
            Set<String> wrapper = new ForwardingSet<>(new HashSet<>()) {
                @Override
                public int size() {
                    throw new AssertionError("self-use: isEmpty must not call size");
                }
            };

            assertThat(wrapper.isEmpty()).isTrue();
        }
    }

    /**
     * A Set that behaves like {@code real} and records the signature of every method called on it, such as
     * {@code toArray(IntFunction)}. The proxy exists so the test can see which delegate method gets called,
     * without a hand-written Set of twenty methods.
     */
    @SuppressWarnings("unchecked") // the proxy implements Set, so the cast cannot fail
    private static Set<String> recording(Set<String> real, Collection<String> calls) {
        return (Set<String>) Proxy.newProxyInstance(
                Set.class.getClassLoader(),
                new Class<?>[] {Set.class},
                (proxy, method, args) -> {
                    calls.add(method.getName() + Arrays.stream(method.getParameterTypes())
                            .map(Class::getSimpleName)
                            .collect(Collectors.joining(",", "(", ")")));
                    try {
                        return method.invoke(real, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }
}
