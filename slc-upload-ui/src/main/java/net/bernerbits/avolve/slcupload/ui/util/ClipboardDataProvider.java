package net.bernerbits.avolve.slcupload.ui.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.swt.dnd.HTMLTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.google.common.collect.ImmutableMap;

import net.bernerbits.avolve.slcupload.ui.model.ClipboardData;
import net.bernerbits.avolve.slcupload.ui.model.ColoredText;
import net.bernerbits.avolve.slcupload.util.function.IndexFunction;

public class ClipboardDataProvider {

	private Display display;

	public ClipboardDataProvider(Display display) {
		this.display = display;
	}
	
	private static interface TransferDataBuilder<T> {
		public T buildTransferData(TableColumn[] columns, TableItem[] selectedRows);
	}

	private final Map<Transfer, TransferDataBuilder<? extends Object>> transfers = new ImmutableMap.Builder<Transfer, TransferDataBuilder<? extends Object>>()
			.put(TextTransfer.getInstance(), this::textTransfer).put(HTMLTransfer.getInstance(), this::htmlTransfer)
			.build();

	public ClipboardData toClipboardData(TableColumn[] columns, TableItem[] selection) {
		return new ClipboardData(transfers.values().stream().map((v) -> v.buildTransferData(columns, selection))
				.toArray(), transfers.keySet().toArray(new Transfer[0]));
	}

	public String textTransfer(TableColumn[] columns, TableItem[] selectedRows) {
		return createTransferData(columns, selectedRows, ",", "\r\n", (b) -> b, (h) -> h, (r) -> r, (hf) -> "\"" + hf
				+ "\"", (rf) -> "\"" + rf.getText() + "\"");
	}

	public String htmlTransfer(TableColumn[] columns, TableItem[] selectedRows) {
		return createTransferData(
				columns,
				selectedRows,
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

	private String createTransferData(TableColumn[] columns, TableItem[] selectedRows, String colDelim,
			String rowDelim, Function<String, String> bodyDecorator, Function<String, String> headerDecorator,
			Function<String, String> rowDecorator, Function<String, String> headerFieldDecorator,
			Function<ColoredText, String> rowFieldDecorator) {
		List<String> headers = Arrays.stream(columns).map((c) -> sync(() -> c.getText())).collect(Collectors.toList());

		Stream<List<ColoredText>> rowStream = Arrays.stream(selectedRows).map(
				(r) -> Arrays.stream(columns).mapToInt(IndexFunction.function())
						.mapToObj((c) -> new ColoredText(sync(() -> r.getText(c)), sync(() -> r.getForeground(c))))
						.collect(Collectors.toList()));

		return createTransferData(headers, rowStream, colDelim, rowDelim, bodyDecorator, headerDecorator, rowDecorator,
				headerFieldDecorator, rowFieldDecorator);

	}

	private static String createTransferData(List<String> headers, Stream<List<ColoredText>> rowStream,
			String colDelim, String rowDelim, Function<String, String> bodyDecorator,
			Function<String, String> headerDecorator, Function<String, String> rowDecorator,
			Function<String, String> headerFieldDecorator, Function<ColoredText, String> rowFieldDecorator) {
		StringBuilder sb = new StringBuilder();

		sb.append(headerDecorator.apply(headers.stream().map((h) -> headerFieldDecorator.apply(h))
				.reduce("", (h, f) -> h + colDelim + f)));
		sb.append(rowDelim);

		sb.append(rowStream.map(
				(columns) -> rowDecorator.apply(columns.stream().map((c) -> rowFieldDecorator.apply(c))
						.reduce(null, (r, f) -> r == null ? f : r + colDelim + f))).reduce(null,
				(t, r) -> t == null ? r : t + rowDelim + r));

		return bodyDecorator.apply(sb.toString());
	}
	
	@SuppressWarnings("unchecked")
	private <T> T sync(Supplier<T> callToSync) {
		final Object[] ref = new Object[1];
		display.syncExec(() -> ref[0] = callToSync.get());
		return (T) ref[0];
	}
}
