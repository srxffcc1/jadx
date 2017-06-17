package jadx.core.utils.files;

import com.android.dex.Dex;

/**
 * Dex文件的包装类
 */
public class DexFile {
	private final InputFile inputFile;
	private final String name;
	private final Dex dexBuf;

	public DexFile(InputFile inputFile, String name, Dex dexBuf) {
		this.inputFile = inputFile;
		this.name = name;
		this.dexBuf = dexBuf;
	}

	public String getName() {
		return name;
	}

	public Dex getDexBuf() {
		return dexBuf;
	}

	public InputFile getInputFile() {
		return inputFile;
	}

	@Override
	public String toString() {
		return inputFile.toString() + (name.isEmpty() ? "" : ":" + name);
	}
}
