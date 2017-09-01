package jadx.gui;

import jadx.api.*;
import jadx.core.LOGS;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * jadx包装类 帮gui进行具体的操作
 */
public class JadxWrapper {
	private static final Logger LOG = LoggerFactory.getLogger(JadxWrapper.class);

	private final JadxDecompiler decompiler;
	private File openFile;
	/**
	 * jadx包装类 帮gui进行具体的操作
	 */
	public JadxWrapper(IJadxArgs jadxArgs) {
		this.decompiler = new JadxDecompiler(jadxArgs);
	}

	/**
	 * 总起简单解析文件
	 * @param file
	 */
	public void openFile(File file) {
		this.openFile = file;
		try {
			this.decompiler.loadFile(file);
		} catch (DecodeException e) {
			LOGS.error("Error decode file: {}", file, e);
		} catch (JadxException e) {
			LOGS.error("Error open file: {}", file, e);
		}
	}

	/**
	 * 保存
	 * @param dir
	 * @param progressMonitor 进度条
	 */
	public void saveAll(final File dir, final ProgressMonitor progressMonitor) {
		Runnable save = new Runnable() {
			@Override
			public void run() {
				try {
					decompiler.setOutputDir(dir);
					ThreadPoolExecutor ex = (ThreadPoolExecutor) decompiler.getSaveExecutor();//获得保存的执行线程 其实这里已经开始执行了
					ex.shutdown();//关闭线程池 为导出做准备
					while (ex.isTerminating()) {//意思是线程池准备完毕
						long total = ex.getTaskCount();//获得线程任务数
						long done = ex.getCompletedTaskCount();//返回已经完成的任务数
						progressMonitor.setProgress((int) (done * 100.0 / (double) total));//初始化进度条
						Thread.sleep(500);
					}
					progressMonitor.close();
					LOGS.info("done");
				} catch (InterruptedException e) {
					LOGS.error("Save interrupted", e);
				}
			}
		};
		new Thread(save).start();
	}

	public List<JavaClass> getClasses() {
		return decompiler.getClasses();
	}

	public List<JavaPackage> getPackages() {
		return decompiler.getPackages();
	}

	public List<ResourceFile> getResources() {
		return decompiler.getResources();
	}

	public File getOpenFile() {
		return openFile;
	}
}
