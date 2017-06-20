package jadx.core.dex.visitors;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.ErrorsCounter;

/**
 * 深层遍历
 */
public class DepthTraversal {

	public static void visit(IDexTreeVisitor visitor, ClassNode cls) {
		try {
//			System.out.println("SRX2:"+visitor.getClass().getName());
			if (visitor.visit(cls)) {
				for (ClassNode inCls : cls.getInnerClasses()) {//递归内部类
//					System.out.println("SRX2:"+visitor.getClass().getName());
					visit(visitor, inCls);
				}
				for (MethodNode mth : cls.getMethods()) {//迭代方法
					visit(visitor, mth);
				}
			}
		} catch (Throwable e) {
			ErrorsCounter.classError(cls,
					e.getClass().getSimpleName() + " in pass: " + visitor.getClass().getSimpleName(), e);
		}
	}

	public static void visit(IDexTreeVisitor visitor, MethodNode mth) {
		if (mth.contains(AType.JADX_ERROR)) {
			return;
		}
		try {
			visitor.visit(mth);
		} catch (Throwable e) {
			ErrorsCounter.methodError(mth,
					e.getClass().getSimpleName() + " in pass: " + visitor.getClass().getSimpleName(), e);
		}
	}
}
