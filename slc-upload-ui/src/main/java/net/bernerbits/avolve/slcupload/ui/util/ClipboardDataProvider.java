package net.bernerbits.avolve.slcupload.ui.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import net.bernerbits.avolve.slcupload.ui.model.ClipboardData;
import net.bernerbits.avolve.slcupload.ui.model.ColoredText;
import net.bernerbits.avolve.slcupload.util.function.IndexFunction;
import net.bernerbits.avolve.slcupload.util.function.NullSafe;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.dnd.HTMLTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.google.common.collect.ImmutableMap;

public class ClipboardDataProvider {

	private Display display;

	public ClipboardDataProvider(Display display) {
		this.display = display;
	}

	private static interface TransferDataBuilder {
		public Object buildTransferData(@NonNull TableColumn[] columns, @NonNull TableItem[] selectedRows);
	}

	@SuppressWarnings("null")
	private final Map<Transfer, TransferDataBuilder> transfers = 
		new ImmutableMap.Builder<Transfer, TransferDataBuilder>()
			.put(TextTransfer.getInstance(), this::textTransfer)
			.put(HTMLTransfer.getInstance(), this::htmlTransfer)
			.build();

	@SuppressWarnings("null")
	public ClipboardData toClipboardData(@NonNull TableColumn[] columns, @NonNull TableItem[] selection) {
		return new ClipboardData(transfers.values().stream().map((v) -> v.buildTransferData(columns, selection))
				.toArray(), transfers.keySet().toArray(new Transfer[0]));
	}

	public String textTransfer(@NonNull TableColumn[] columns, @NonNull TableItem[] selectedRows) {
		return createTransferData(columns, selectedRows, ",", "\r\n", (b) -> b, (h) -> h, (r) -> r, (hf) -> "\"" + hf
				+ "\"", (rf) -> "\"" + rf.getText() + "\"");
	}

	public String htmlTransfer(@NonNull TableColumn[] columns, @NonNull TableItem[] selectedRows) {
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

	private String createTransferData(@NonNull TableColumn[] columns,
			@NonNull TableItem [] selectedRows, String colDelim, String rowDelim,
			Function<String, String> bodyDecorator, Function<String, String> headerDecorator,
			Function<String, String> rowDecorator, Function<String, String> headerFieldDecorator,
			Function<ColoredText, String> rowFieldDecorator) {

		List<String> headers = Arrays.stream(columns).map((c) -> sync(() -> c.getText()))
				.collect(NullSafe.toListCollector());
		
		@SuppressWarnings("null")
		@NonNull Stream<List<ColoredText>> rowStream = Arrays.stream(selectedRows).map(
				(r) -> getData(columns, r));

		return createTransferData(headers, rowStream, colDelim, rowDelim, bodyDecorator, headerDecorator, rowDecorator,
				headerFieldDecorator, rowFieldDecorator);

	}

	private List<@NonNull ColoredText> getData(TableColumn[] columns, TableItem row) {
		return Arrays.stream(columns).mapToInt(IndexFunction.function())
				.mapToObj((c) -> sync(() -> new ColoredText(row.getText(c), row.getForeground(c))))
				.collect(NullSafe.toListCollector());
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
						.reduce("", (r, f) -> r.isEmpty() ? f : r + colDelim + f))).reduce("",
				(t, r) -> t.isEmpty() ? r : t + rowDelim + r));

		return bodyDecorator.apply(NullSafe.toString(sb));
	}

	@SuppressWarnings("unchecked")
	private <T> T sync(Supplier<T> callToSync) {
		final Object[] ref = new Object[1];
		display.syncExec(() -> ref[0] = callToSync.get());
		return (T) ref[0];
	}
}
