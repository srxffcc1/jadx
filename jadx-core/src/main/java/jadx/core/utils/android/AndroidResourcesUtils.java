package jadx.core.utils.android;

import jadx.core.LOGS;
import jadx.core.codegen.ClassGen;
import jadx.core.codegen.CodeWriter;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.RootNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Android resources specific handlers
 */
public class AndroidResourcesUtils {
	private static final Logger LOG = LoggerFactory.getLogger(AndroidResourcesUtils.class);

	public static ClassNode searchAppResClass(RootNode root) {
		String appPackage = root.getAppPackage();//获得包名
		String fullName = appPackage != null ? appPackage + ".R" : "R";//获得全量R路径
		ClassNode resCls = root.searchClassByName(fullName);//找到R.class类
		if (resCls != null) {
			return resCls;
		}
		List<ClassNode> candidates = root.searchClassByShortName("R");//还是搜索R文件
		if (candidates.size() == 1) {
			return candidates.get(0);
		}
		if (!candidates.isEmpty()) {
			LOGS.info("Found several 'R' class candidates: {}", candidates);
		}
		LOGS.warn("Unknown 'R' class, create references to '{}'", fullName);
		return makeClass(root, fullName);
	}

	public static boolean handleAppResField(CodeWriter code, ClassGen clsGen, ClassInfo declClass) {
		ClassInfo parentClass = declClass.getParentClass();
		if (parentClass != null && parentClass.getShortName().equals("R")) {
			clsGen.useClass(code, parentClass);
			code.add('.');
			code.add(declClass.getAlias().getShortName());
			return true;
		}
		return false;
	}

	private static ClassNode makeClass(RootNode root, String clsName) {
		DexNode firstDex = root.getDexNodes().get(0);
		ClassInfo r = ClassInfo.fromName(firstDex, clsName);
		return new ClassNode(firstDex, r);
	}
}
