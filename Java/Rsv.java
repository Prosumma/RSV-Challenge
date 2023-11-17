/* (C) Stefan John / Stenway / Stenway.com / 2023 */

import java.util.ArrayList;
import java.util.Objects;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

class Rsv {
	static byte[] encodeRsv(String[][] rows) {
		ArrayList<byte[]> parts = new ArrayList<byte[]>();
		byte[] valueTerminatorByte = new byte[]{(byte)0xFF};
		byte[] nullValueByte = new byte[]{(byte)0xFE};
		byte[] rowTerminatorByte = new byte[]{(byte)0xFD};
		CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
		int resultLength = 0;
		for (String[] row : rows) {
			for (String value : row) {
				if (value == null) { parts.add(nullValueByte); resultLength++; }
				else if (value.length() > 0) {
					byte[] valueBytes;
					try {
						ByteBuffer byteBuffer = encoder.encode(CharBuffer.wrap(value));
						valueBytes = new byte[byteBuffer.limit()];
						byteBuffer.get(valueBytes);
					} catch (Exception e) { throw new RuntimeException("Invalid string value", e); }
					parts.add(valueBytes); resultLength += valueBytes.length;
				}
				parts.add(valueTerminatorByte); resultLength++;
			}
			parts.add(rowTerminatorByte); resultLength++;
		}
		byte[] result = new byte[resultLength];		
		ByteBuffer resultBuffer = ByteBuffer.wrap(result);
		for (byte[] part : parts) { resultBuffer.put(part); }
		return result;
	}
	
	static String[][] decodeRsv(byte[] bytes) {
		CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
		decoder.onMalformedInput(CodingErrorAction.REPORT);
		decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		ArrayList<String[]> result = new ArrayList<String[]>();
		ArrayList<String> currentRow = new ArrayList<String>();
		if (bytes.length > 0 && (bytes[bytes.length-1] & 0xFF) != 0xFD) { throw new RuntimeException("Incomplete RSV document"); }
		int valueStartIndex = 0;
		for (int i=0; i<bytes.length; i++) {
			int currentByte = bytes[i] & 0xFF;
			if (currentByte == 0xFF) {
				int length = i-valueStartIndex;
				if (length == 0) { currentRow.add(""); }
				else if (length == 1 && (bytes[valueStartIndex] & 0xFF) == 0xFE) { currentRow.add(null); }
				else {
					ByteBuffer valueBytes = ((ByteBuffer)(byteBuffer.duplicate().position(valueStartIndex).limit(i))).slice();
					try { currentRow.add(decoder.decode(valueBytes).toString()); }
					catch (Exception e) { throw new RuntimeException("Invalid string value", e); }
				}
				valueStartIndex = i+1;
			} else if (currentByte == 0xFD) {
				if (i > 0 && valueStartIndex != i) { throw new RuntimeException("Incomplete RSV row"); }
				result.add(currentRow.toArray(new String[0]));
				currentRow.clear();
				valueStartIndex = i+1;
			}
		}
		return result.toArray(new String[0][]);
	}
	
	// ----------------------------------------------------------------------
	
	static void saveRsv(String[][] rows, String filePath) throws IOException {
		Objects.requireNonNull(filePath);
		Files.write(Paths.get(filePath), encodeRsv(rows));
	}
	
	static String[][] loadRsv(String filePath) throws IOException {
		Objects.requireNonNull(filePath);
		return decodeRsv(Files.readAllBytes(Paths.get(filePath)));
	}
	
	static void appendRsv(String[][] rows, String filePath) throws IOException {
		appendRsv(rows, filePath, false);
	}
	
	static void appendRsv(String[][] rows, String filePath, boolean continueLastRow) throws IOException {
	   	RandomAccessFile file = new RandomAccessFile(filePath, "rw");
	   	try {
			if (continueLastRow && file.length() > 0) {
				file.seek(file.length() - 1);
				if ((file.readByte() & 0xFF) != 0xFD) { throw new RuntimeException("Incomplete RSV document"); }
				if (rows.length == 0) return;
				file.seek(file.length() - 1);
			} else {
				file.seek(file.length());
			}
			
			file.write(encodeRsv(rows));
		} finally {
			file.close();
		}
	}

	// ----------------------------------------------------------------------
	
	static final int[] byteClassLookup = {
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
		3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
		4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
		4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
		0, 0, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
		5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
		6, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 8, 7, 7,
		9, 10, 10, 10, 11, 0, 0, 0, 0, 0, 0, 0, 0, 12, 13, 14
	};

	static final int[] stateTransitionLookup = {
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 2, 0, 0, 0, 3, 4, 6, 5, 7, 8, 9, 1, 10, 11,
		0, 2, 0, 0, 0, 3, 4, 6, 5, 7, 8, 9, 0, 0, 11,
		0, 0, 2, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 3, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 3, 3, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 6, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 6, 6, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 11,
		0, 2, 0, 0, 0, 3, 4, 6, 5, 7, 8, 9, 1, 10, 11
	};

	static boolean isValidRsv(byte[] bytes) {
		int lastState = 1;
		for (int i=0; i<bytes.length; i++) {
			int currentByte = bytes[i] & 0xFF;
			int currentByteClass = byteClassLookup[currentByte];
			int newStateLookupIndex = lastState*15+currentByteClass;
			lastState = stateTransitionLookup[newStateLookupIndex];
			if (lastState == 0) { return false; }
		}
		return (lastState == 1);
	}

	// ----------------------------------------------------------------------

	static String escapeJsonString(String str) {
		StringBuilder result = new StringBuilder();
		result.append("\"");
		for (int i = 0; i < str.length(); i++) {
			int c = str.charAt(i);
			if (c == 0x08) { result.append("\\b"); }
			else if (c == 0x09) { result.append("\\t"); }
			else if (c == 0x0A) { result.append("\\n"); }
			else if (c == 0x0C) { result.append("\\f"); }
			else if (c == 0x0D) { result.append("\\r"); }
			else if (c == 0x22) { result.append("\\\""); }
			else if (c == 0x5C) { result.append("\\\\"); }
			else if (c >= 0x00 && c <= 0x1F) { result.append("\\u00" + String.format("%02x", c)); }
			else { result.append((char)c); }
		}
		result.append("\"");
		return result.toString();
	}
	
	static String rsvToJson(String[][] rows) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		boolean isFirstRow = true;
		for (String[] row : rows) {
			if (!isFirstRow) { sb.append(","); }
			isFirstRow = false;
			sb.append("\n  [");
			boolean isFirstValue = true;
			for (String value : row) {
				if (!isFirstValue) { sb.append(", "); }
				isFirstValue = false;
				if (value == null) { sb.append("null"); }
				else { sb.append(escapeJsonString(value)); }
			}
			sb.append("]");
		}
		sb.append("\n]");
		return sb.toString();
	}
	
	// ----------------------------------------------------------------------

	static void checkTestFiles(PrintWriter stdout) throws IOException {
		for (int i=1; i<=79; i++) {
			String filePath = "./../TestFiles/Valid_" + String.format("%03d", i);
			stdout.println("Checking valid test file: " + filePath);
			String[][] loadedRows = loadRsv(filePath + ".rsv");
			String jsonStr = rsvToJson(loadedRows);
			
			String loadedJsonStr = new String(Files.readAllBytes(Paths.get(filePath + ".json")), StandardCharsets.UTF_8);
			if (!jsonStr.equals(loadedJsonStr)) {
				throw new RuntimeException("JSON mismatch");
			}
			
			if (!isValidRsv(Files.readAllBytes(Paths.get(filePath + ".rsv")))) {
				throw new RuntimeException("Validation mismatch");
			}
		}
		
		for (int i=1; i<=29; i++) {
			String filePath = "./../TestFiles/Invalid_" + String.format("%03d", i);
			stdout.println("Checking invalid test file: " + filePath);
			boolean wasError = false;
			try {
				loadRsv(filePath + ".rsv");
			} catch(Exception e) {
				wasError = true;
			}
			if (!wasError) {
				throw new RuntimeException("RSV document is valid");
			}
			if (isValidRsv(Files.readAllBytes(Paths.get(filePath + ".rsv")))) {
				throw new RuntimeException("Validation mismatch");
			}
		}
	}
	
	// ----------------------------------------------------------------------

	public static void main(String[] args) {
		try {
			PrintWriter stdout = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
			String[][] rows = {{"Hello", "\uD83C\uDF0E", null, ""}, {"A\0B\nC", "Test \uD834\uDD1E"}, {}, {""}};
			stdout.println(rsvToJson(rows));
			saveRsv(rows, "Test.rsv");
			
			String[][] loadedRows = loadRsv("Test.rsv");
			stdout.println(rsvToJson(loadedRows));
			
			saveRsv(loadedRows, "TestResaved.rsv");
			
			appendRsv(new String[][]{{"ABC"}}, "Append.rsv", true);
			
			checkTestFiles(stdout);
		} catch(Exception e) {
			System.out.println(e.toString());
		}
	}
}