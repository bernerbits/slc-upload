package net.bernerbits.avolve.slcupload.util.function;

import java.lang.reflect.Array;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.jdt.annotation.NonNull;

/* A utility class for suppressing warnings 
 * around known problems in the Eclipse null 
 * analyzer.
 */
public final class NullSafe {
	private NullSafe() {
	}

	@SuppressWarnings({ "unchecked", "null" })
	public static <T> T[] toArray(Collection<T> list, Class<T> t) {
		Object arrayObj = Array.newInstance(t, list.size());
		return list.toArray((T[]) arrayObj);
	}

	@SuppressWarnings("null")
	public static Path getPath(String path, String... more) {
		return Paths.get(path, more);
	}

	@SuppressWarnings("null")
	public static <T> Collector<T, @NonNull ?, List<T>> toListCollector() {
		return Collectors.toList();
	}

	@SuppressWarnings("null")
	public static String toString(StringBuilder sb) {
		return sb.toString();
	}

	@SuppressWarnings("null")
	public static <T> Stream<T> streamArray(T[] array) {
		return Arrays.stream(array);
	}

	@SuppressWarnings("null")
	public static <T> Stream<T> stream(Collection<T> c) {
		return StreamSupport.stream(c.spliterator(), false);
	}

	@SuppressWarnings("null")
	public static <T> Collection<T> values(Map<?, T> m) {
		return m.values();
	}

	@SuppressWarnings("null")
	public static <T> Collection<T> keys(Map<T, ?> m) {
		return m.keySet();
	}
}
