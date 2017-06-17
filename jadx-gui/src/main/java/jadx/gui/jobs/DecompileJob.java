package jadx.gui.jobs;

import jadx.api.JavaClass;
import jadx.gui.JadxWrapper;

/**
 * 解析工作类 从队列中取出任务进行解析
 */
public class DecompileJob extends BackgroundJob {

	public DecompileJob(JadxWrapper wrapper, int threadsCount) {
		super(wrapper, threadsCount);
	}

	protected void runJob() {
		for (final JavaClass cls : wrapper.getClasses()) {
			addTask(new Runnable() {
				@Override
				public void run() {
					cls.decompile();
				}
			});
		}
	}

	@Override
	public String getInfoString() {
		return "Decompiling: ";
	}

}
