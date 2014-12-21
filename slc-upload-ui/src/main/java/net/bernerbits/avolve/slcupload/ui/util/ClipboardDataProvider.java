package net.bernerbits.avolve.slcupload.ui.util;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import net.bernerbits.avolve.slcupload.ui.model.ClipboardData;
import net.bernerbits.avolve.slcupload.ui.model.ColoredText;
import net.bernerbits.avolve.slcupload.util.function.NullSafe;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.dnd.HTMLTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;

import com.google.common.collect.ImmutableMap;

public class ClipboardDataProvider {

	public ClipboardDataProvider() {
	}

	private static interface TransferDataBuilder {
		public Object buildTransferData(List<String> headers, List<ColumnLabelProvider> columns, List<?> selectedItems);
	}

	private final Map<Transfer, TransferDataBuilder> transfers = new ImmutableMap.Builder<Transfer, TransferDataBuilder>()
			.put(TextTransfer.getInstance(), this::textTransfer).put(HTMLTransfer.getInstance(), this::htmlTransfer)
			.build();

	public ClipboardData toClipboardData(List<String> headers, List<ColumnLabelProvider> columns, List<?> selectedItems) {
		return new ClipboardData(transfers.values().stream()
				.map((v) -> v.buildTransferData(headers, columns, selectedItems)).toArray(), transfers.keySet()
				.toArray(new Transfer[0]));
	}

	public String textTransfer(List<String> headers, List<ColumnLabelProvider> columns, List<?> selectedItems) {
		return createTransferData(headers, columns, selectedItems, ",", "\r\n", (b) -> b, (h) -> h, (r) -> r,
				(hf) -> "\"" + hf + "\"", (rf) -> "\"" + rf.getText() + "\"");
	}

	public String htmlTransfer(List<String> headers, List<ColumnLabelProvider> columns, List<?> selectedItems) {
		return createTransferData(
				headers,
				columns,
				selectedItems,
				"",
				"",
				(b) -> "<table>" + b + "</table>",
				(h) -> "<thead><tr>" + h + "</tr></thead>",
				(r) -> "<tr>" + r + "</tr>",
				(hf) -> "<th>" + hf + "</th>",
				(rf) -> "<td><font color=\"#"
						+ String.format("%02x%02x%02x", rf.getColor().getRed(), rf.getColor().getGreen(), rf.getColor()
								.getBlue()) + "\">" + rf.getText() + "</font></td>");
	}

	private static String createTransferData(List<String> headers, List<ColumnLabelProvider> columns,
			List<?> selectedItems, String colDelim, String rowDelim, Function<String, String> bodyDecorator,
			Function<String, String> headerDecorator, Function<String, String> rowDecorator,
			Function<String, String> headerFieldDecorator, Function<ColoredText, String> rowFieldDecorator) {
		@SuppressWarnings("null")
		@NonNull
		Stream<List<ColoredText>> rowStream = selectedItems.stream().map((r) -> getData(columns, r));

		return createTransferData(headers, rowStream, colDelim, rowDelim, bodyDecorator, headerDecorator, rowDecorator,
				headerFieldDecorator, rowFieldDecorator);
	}

	private static List<@NonNull ColoredText> getData(List<ColumnLabelProvider> columns, Object data) {
		return columns.stream().map((c) -> new ColoredText(c.getText(data), c.getForeground(data)))
				.collect(NullSafe.toListCollector());
	}

	private static String createTransferData(List<String> headers, Stream<List<ColoredText>> rowStream,
			String colDelim, String rowDelim, Function<String, String> bodyDecorator,
			Function<String, String> headerDecorator, Function<String, String> rowDecorator,
			Function<String, String> headerFieldDecorator, Function<ColoredText, String> rowFieldDecorator) {
		StringBuilder sb = new StringBuilder();

		sb.append(headerDecorator.apply(headers.stream().map((h) -> headerFieldDecorator.apply(h))
				.reduce("", (h, f) -> h.isEmpty() ? f : h + colDelim + f)));
		sb.append(rowDelim);

		sb.append(rowStream
				.parallel()
				.map((columns) -> 
					rowDecorator.apply(
						columns.stream()
							.map((c) -> rowFieldDecorator.apply(c))
							.reduce("", (r, f) -> r.isEmpty() ? f : r + colDelim + f)
					)
				).sequential()
				.reduce(new StringBuilder(), (t, r) -> t.length() == 0 ? t.append(r) : t.append(rowDelim).append(r), (t1, t2) -> t1.append(t2)));

		return bodyDecorator.apply(NullSafe.toString(sb));
	}

}
