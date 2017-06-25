package jadx.dxapi;

import com.android.dex.Dex;
import com.android.dex.TableOfContents;
import jadx.core.utils.AsmUtils;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;
import jadx.core.utils.files.InputFile;
import jadx.core.utils.files.JavaToDex;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static jadx.core.utils.files.FileUtils.close;


public class DexUtil {
	public static void main(String[] args) throws Exception {
		List<Dex> result=DexUtil.path2dexlist("D:\\Apk_Need\\WPSOffice_197.apk");
		for (int i = 0; i < result.size(); i++) {
			Dex dex=result.get(i);
			TableOfContents tableOfContents=dex.getTableOfContents();
			tableOfContents.readFrom(dex);
			System.out.println(tableOfContents.annotations.off);


		}
	}
	public static List<Dex> path2dexlist(String filepath) throws IOException, DecodeException{
		return path2dexlist(new File(filepath));
	}
	public static List<Dex> path2dexlist(File file) throws IOException, DecodeException {
		List<Dex> result=new ArrayList<>();
		String fileName = file.getName();
		if (fileName.endsWith(".dex")) {
			result.add(new Dex(file));
		}
		if (fileName.endsWith(".class")) {
			result.addAll(loadFromClassFile(file));
		}
		if (fileName.endsWith(".apk") || fileName.endsWith(".zip")) {
			result.addAll(loadFromZip(file,".dex"));
		}
		if (fileName.endsWith(".jar")) {
			// check if jar contains '.dex' files
			result.addAll(loadFromJar(file));
		}
		if (fileName.endsWith(".aar")) {
			result.addAll(loadFromZip(file,".jar"));
		}
		return result;
	}

	private static List<Dex> loadFromZip(File file,String ext) throws IOException, DecodeException {
		List<Dex> result=new ArrayList<>();
		ZipFile zf = new ZipFile(file);
		int index = 0;
		while (true) {
			String entryName = "classes" + (index == 0 ? "" : index) + ext;
			ZipEntry entry = zf.getEntry(entryName);
			if (entry == null) {
				break;
			}
			InputStream inputStream = zf.getInputStream(entry);
			try {
				if (ext.equals(".dex")) {
					result.add(new Dex(inputStream));
				} else if (ext.equals(".jar")) {
					File jarFile = FileUtils.createTempFile(entryName);
					FileOutputStream fos = new FileOutputStream(jarFile);
					try {
						IOUtils.copy(inputStream, fos);
					} finally {
						close(fos);
					}
					result.addAll(loadFromJar(jarFile));
				} else {
					throw new JadxRuntimeException("Unexpected extension in zip: " + ext);
				}
			} finally {
				close(inputStream);
			}
			index++;
			if (index == 1) {
				index = 2;
			}
		}
		zf.close();
		return result;
	}
	private static List<Dex> loadFromJar(File jarFile) throws DecodeException {
		List<Dex> result=new ArrayList<>();
		try {
			//LOG.info("converting to dex: {} ...", jarFile.getName());
			JavaToDex j2d = new JavaToDex();
			byte[] ba = j2d.convert(jarFile.getAbsolutePath());
			if (ba.length == 0) {
				throw new JadxException(j2d.isError() ? j2d.getDxErrors() : "Empty dx output");
			}
			if (j2d.isError()) {
				//LOG.warn("dx message: {}", j2d.getDxErrors());
			}
			result.add(new Dex(ba));
		} catch (Throwable e) {
			throw new DecodeException("java class to dex conversion error:\n " + e.getMessage(), e);
		}
		return result;
	}
	private static List<Dex> loadFromClassFile(File file) throws IOException, DecodeException {
		List<Dex> result=new ArrayList<>();
		File outFile = FileUtils.createTempFile("cls.jar");
		FileOutputStream out = null;
		JarOutputStream jo = null;
		try {
			out = new FileOutputStream(outFile);
			jo = new JarOutputStream(out);
			String clsName = AsmUtils.getNameFromClassFile(file);
			if (clsName == null) {
				throw new IOException("Can't read class name from file: " + file);
			}
			FileUtils.addFileToJar(jo, file, clsName + ".class");
		} finally {
			close(jo);
			close(out);
		}
		result.addAll(loadFromJar(outFile));
		return result;
	}

}
