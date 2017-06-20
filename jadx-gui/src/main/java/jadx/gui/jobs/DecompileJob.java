package jadx.gui.jobs;

import jadx.api.JavaClass;
import jadx.gui.JadxWrapper;

/**
 * 解析工作类 从队列中取出任务进行解析 进行反编译作业
 */
public class DecompileJob extends BackgroundJob {

	public DecompileJob(JadxWrapper wrapper, int threadsCount) {
		super(wrapper, threadsCount);
	}

	/**
	 * 进行反编译
	 */
	protected void runJob() {
		for (final JavaClass cls : wrapper.getClasses()) {//迭代类集合
			addTask(new Runnable() {
				@Override
				public void run() {
					cls.decompile();//进行反编译操作
				}
			});
		}
	}

	@Override
	public String getInfoString() {
		return "Decompiling: ";
	}

}
