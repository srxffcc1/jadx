package jadx.core.dex.nodes;

import jadx.api.IJadxArgs;
import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.api.ResourcesLoader;
import jadx.core.LOGS;
import jadx.core.clsp.ClspGraph;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.ConstStorage;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.StringUtils;
import jadx.core.utils.android.AndroidResourcesUtils;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.files.DexFile;
import jadx.core.utils.files.InputFile;
import jadx.core.xmlgen.ResContainer;
import jadx.core.xmlgen.ResTableParser;
import jadx.core.xmlgen.ResourceStorage;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RootNode {
	private static final Logger LOG = LoggerFactory.getLogger(RootNode.class);

	private final ErrorsCounter errorsCounter = new ErrorsCounter();
	private final IJadxArgs args;
	private final StringUtils stringUtils;
	private final ConstStorage constValues;

	private List<DexNode> dexNodes;
	@Nullable
	private String appPackage;
	private ClassNode appResClass;
	private ClspGraph clsp;

	public RootNode(IJadxArgs args) {
		this.args = args;
		this.stringUtils = new StringUtils(args);//不懂 不过看了下影响不大
		this.constValues = new ConstStorage(args);//不懂 不过看了下影响不大
	}

	public void load(List<InputFile> inputFiles) throws DecodeException {
		dexNodes = new ArrayList<DexNode>();
		for (InputFile input : inputFiles) {//其实只有一个apk文件啊
			for (DexFile dexFile : input.getDexFiles()) {//有几个dex就遍历几次
				try {
					LOGS.debug("Load: {}", dexFile);
					DexNode dexNode = new DexNode(this, dexFile);
					dexNodes.add(dexNode);//加入所有dex节点 还是有几个dex就加多少
				} catch (Exception e) {
					throw new DecodeException("Error decode file: " + dexFile, e);
				}
			}
		}
		for (DexNode dexNode : dexNodes) {//遍历每个dex 载入dex含有的class文件
			dexNode.loadClasses();
		}
		initInnerClasses();//初始化内部类相关
	}

	/**
	 * 载入资源 arsc进行解析
	 * @param resources
	 */
	public void loadResources(List<ResourceFile> resources) {
		ResourceFile arsc = null;
		for (ResourceFile rf : resources) {
			if (rf.getType() == ResourceType.ARSC) {//判断arsc是不是存在
				arsc = rf;
				break;
			}
		}

		if (arsc == null) {//检验arsc文件是否存在 存在说明有res资源
			LOGS.debug("'.arsc' file not found");
			return;
		}
		final ResTableParser parser = new ResTableParser();//res解析器
		try {
			ResourcesLoader.decodeStream(arsc, new ResourcesLoader.ResourceDecoder() {//解析arsc
				@Override
				public ResContainer decode(long size, InputStream is) throws IOException {
					parser.decode(is);
					return null;
				}
			});
		} catch (JadxException e) {
			LOGS.error("Failed to parse '.arsc' file", e);
			return;
		}

		ResourceStorage resStorage = parser.getResStorage();
		constValues.setResourcesNames(resStorage.getResourcesNames());
		appPackage = resStorage.getAppPackage();
	}

	public void initAppResClass() {
		appResClass = AndroidResourcesUtils.searchAppResClass(this);
	}

	public void initClassPath() throws DecodeException {//整合类树
		try {
			if (this.clsp == null) {
				ClspGraph clsp = new ClspGraph();
				clsp.load();

				List<ClassNode> classes = new ArrayList<ClassNode>();
				for (DexNode dexNode : dexNodes) {
					classes.addAll(dexNode.getClasses());
				}
				clsp.addApp(classes);

				this.clsp = clsp;
			}
		} catch (IOException e) {
			throw new DecodeException("Error loading classpath", e);
		}
	}

	private void initInnerClasses() {
		for (DexNode dexNode : dexNodes) {
			dexNode.initInnerClasses();
		}
	}

	public List<ClassNode> getClasses(boolean includeInner) {
		List<ClassNode> classes = new ArrayList<ClassNode>();
		for (DexNode dex : dexNodes) {
			if (includeInner) {
				classes.addAll(dex.getClasses());
			} else {
				for (ClassNode cls : dex.getClasses()) {
					if (!cls.getClassInfo().isInner()) {
						classes.add(cls);
					}
				}
			}
		}
		return classes;
	}

	public ClassNode searchClassByName(String fullName) {
		for (DexNode dexNode : dexNodes) {
			ClassInfo clsInfo = ClassInfo.fromName(dexNode, fullName);
			ClassNode cls = dexNode.resolveClass(clsInfo);
			if (cls != null) {
				return cls;
			}
		}
		return null;
	}

	public List<ClassNode> searchClassByShortName(String shortName) {
		List<ClassNode> list = new ArrayList<ClassNode>();
		for (DexNode dexNode : dexNodes) {
			for (ClassNode cls : dexNode.getClasses()) {
				if (cls.getClassInfo().getShortName().equals(shortName)) {
					list.add(cls);
				}
			}
		}
		return list;
	}

	public List<DexNode> getDexNodes() {
		return dexNodes;
	}

	public ClspGraph getClsp() {
		return clsp;
	}

	public ErrorsCounter getErrorsCounter() {
		return errorsCounter;
	}

	@Nullable
	public String getAppPackage() {
		return appPackage;
	}

	public ClassNode getAppResClass() {
		return appResClass;
	}

	public IJadxArgs getArgs() {
		return args;
	}

	public StringUtils getStringUtils() {
		return stringUtils;
	}

	public ConstStorage getConstValues() {
		return constValues;
	}
}
