package jadx.api;

import jadx.api.ResourceFile.ZipRef;
import jadx.core.codegen.CodeWriter;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.files.InputFile;
import jadx.core.xmlgen.ResContainer;
import jadx.core.xmlgen.ResTableParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static jadx.core.utils.files.FileUtils.*;

// TODO: move to core package
public final class ResourcesLoader {
	private static final Logger LOG = LoggerFactory.getLogger(ResourcesLoader.class);

	private static final int LOAD_SIZE_LIMIT = 10 * 1024 * 1024;

	private final JadxDecompiler jadxRef;

	ResourcesLoader(JadxDecompiler jadxRef) {
		this.jadxRef = jadxRef;
	}

	List<ResourceFile> load(List<InputFile> inputFiles) {
		List<ResourceFile> list = new ArrayList<ResourceFile>(inputFiles.size());
		for (InputFile file : inputFiles) {
//			System.out.println("资源载入:");
			loadFile(list, file.getFile());
		}
		return list;
	}

	public interface ResourceDecoder {
		ResContainer decode(long size, InputStream is) throws IOException;
	}

	/**
	 * 解码文件
	 * @param rf 资源
	 * @param decoder 解析器
	 * @return
	 * @throws JadxException
	 */
	public static ResContainer decodeStream(ResourceFile rf, ResourceDecoder decoder) throws JadxException {
		ZipRef zipRef = rf.getZipRef();
		if (zipRef == null) {
			return null;
		}
		ZipFile zipFile = null;
		InputStream inputStream = null;
		ResContainer result = null;//容器
		try {
			zipFile = new ZipFile(zipRef.getZipFile());
			ZipEntry entry = zipFile.getEntry(zipRef.getEntryName());
			if (entry == null) {
				throw new IOException("Zip entry not found: " + zipRef);
			}
			inputStream = new BufferedInputStream(zipFile.getInputStream(entry));//通过zip获得读取流
//			System.out.println(entry.getName());
			result = decoder.decode(entry.getSize(), inputStream);//把文件流进行解析
		} catch (Exception e) {
			throw new JadxException("Error decode: " + zipRef.getEntryName(), e);
		} finally {
			try {
				if (zipFile != null) {
					zipFile.close();
				}
			} catch (Exception e) {
				LOG.error("Error close zip file: {}", zipRef, e);
			}
			close(inputStream);
		}
		return result;
	}

	/**
	 * 加载内容
	 * @param jadxRef 解析器
	 * @param rf 资源文件
	 * @return 资源容器
	 */
	static ResContainer loadContent(final JadxDecompiler jadxRef, final ResourceFile rf) {
		try {
			return decodeStream(rf, new ResourceDecoder() {
				@Override
				public ResContainer decode(long size, InputStream is) throws IOException {
					return loadContent(jadxRef, rf, is, size);
				}
			});
		} catch (JadxException e) {
			LOG.error("Decode error", e);
			CodeWriter cw = new CodeWriter();
			cw.add("Error decode ").add(rf.getType().toString().toLowerCase());
			cw.startLine(Utils.getStackTrace(e.getCause()));
			return ResContainer.singleFile(rf.getName(), cw);
		}
	}

	/**
	 * 载入内容
	 * @param jadxRef  解析器
	 * @param rf 资源路劲
	 * @param inputStream 输入流
	 * @param size 大小
	 * @return
	 * @throws IOException
	 */
	private static ResContainer loadContent(JadxDecompiler jadxRef, ResourceFile rf,
			InputStream inputStream, long size) throws IOException {
		switch (rf.getType()) {//分容器存放
			case MANIFEST:
			case XML:
				return ResContainer.singleFile(rf.getName(),
						jadxRef.getXmlParser().parse(inputStream));//获得解析 生成容器

			case ARSC:
				return new ResTableParser().decodeFiles(inputStream);

			case IMG:
				return ResContainer.singleImageFile(rf.getName(), inputStream);
		}
		if (size > LOAD_SIZE_LIMIT) {//超内存了的意思
			return ResContainer.singleFile(rf.getName(),
					new CodeWriter().add("File too big, size: " + String.format("%.2f KB", size / 1024.)));
		}
		return ResContainer.singleFile(rf.getName(), loadToCodeWriter(inputStream));
	}

	private void loadFile(List<ResourceFile> list, File file) {
		if (file == null) {
			return;
		}
		ZipFile zip = null;
		try {
			zip = new ZipFile(file);
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
//				System.out.println("加入");
				ZipEntry entry = entries.nextElement();
				addEntry(list, file, entry);
			}
		} catch (IOException e) {
			LOG.debug("Not a zip file: {}", file.getAbsolutePath());
		} finally {
			if (zip != null) {
				try {
					zip.close();
				} catch (Exception e) {
					LOG.error("Zip file close error: {}", file.getAbsolutePath(), e);
				}
			}
		}
	}

	/**
	 *
	 * @param list 用于进行add的对象
	 * @param zipFile zip文件地址 应该是不变的
	 * @param entry zip注册表
	 */
	private void addEntry(List<ResourceFile> list, File zipFile, ZipEntry entry) {
		if (entry.isDirectory()) {
			return;
		}
//		System.out.println("资源:"+list.size()+":"+zipFile.getName());
		String name = entry.getName();
//		System.out.println(name);
		ResourceType type = ResourceType.getFileType(name);
//		System.out.println(type.name());
		ResourceFile rf = new ResourceFile(jadxRef, name, type);
		rf.setZipRef(new ZipRef(zipFile, name));
		list.add(rf);
//		System.out.println(list.size());
	}

	public static CodeWriter loadToCodeWriter(InputStream is) throws IOException {
		CodeWriter cw = new CodeWriter();
		ByteArrayOutputStream baos = new ByteArrayOutputStream(READ_BUFFER_SIZE);
		copyStream(is, baos);
		cw.add(baos.toString("UTF-8"));
		return cw;
	}
}
