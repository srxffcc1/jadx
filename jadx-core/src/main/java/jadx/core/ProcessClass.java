package jadx.core;

import jadx.core.codegen.CodeGen;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.visitors.DepthTraversal;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.utils.ErrorsCounter;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static jadx.core.dex.nodes.ProcessState.*;

public final class ProcessClass {
	private static final Logger LOG = LoggerFactory.getLogger(ProcessClass.class);

	private ProcessClass() {
	}

	/**
	 * 感觉是一个筛选过滤类 根据 载入状态进行判断 做出动作
	 * @param cls
	 * @param passes
	 * @param codeGen
	 */
	public static void process(ClassNode cls, List<IDexTreeVisitor> passes, @Nullable CodeGen codeGen) {
		if (codeGen == null && cls.getState() == PROCESSED) {
			return;
		}
		synchronized (cls) {
			try {
				if (cls.getState() == NOT_LOADED) {//未载入
					cls.load();//执行载入
					cls.setState(STARTED);//置位状态
//					System.out.println("SRX1:"+passes.size());
					for (IDexTreeVisitor visitor : passes) {//迭代所有访问者
//			System.out.println("SRX1:"+visitor.getClass().getName());
						DepthTraversal.visit(visitor, cls);
					}
					cls.setState(PROCESSED);//置位状态
				}
				if (cls.getState() == PROCESSED && codeGen != null) {//已经载入
					processDependencies(cls, passes);
					codeGen.visit(cls);
					cls.setState(GENERATED);
				}
			} catch (Exception e) {
				ErrorsCounter.classError(cls, e.getClass().getSimpleName(), e);
			} finally {
				if (cls.getState() == GENERATED) {
					cls.unload();
					cls.setState(UNLOADED);
				}
			}
		}
	}

	static void processDependencies(ClassNode cls, List<IDexTreeVisitor> passes) {
		for (ClassNode depCls : cls.getDependencies()) {
			process(depCls, passes, null);
		}
	}
}
