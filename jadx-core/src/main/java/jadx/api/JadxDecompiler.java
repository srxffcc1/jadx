package jadx.api;

import jadx.core.Jadx;
import jadx.core.ProcessClass;
import jadx.core.codegen.CodeGen;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.dex.visitors.SaveCode;
import jadx.core.export.ExportGradleProject;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.InputFile;
import jadx.core.xmlgen.BinaryXMLParser;
import jadx.core.xmlgen.ResourcesSaver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Jadx API usage example:
 * <pre><code>
 *  JadxDecompiler jadx = new JadxDecompiler();
 *  jadx.loadFile(new File("classes.dex"));
 *  jadx.setOutputDir(new File("out"));
 *  jadx.save();
 * </code></pre>
 * <p/>
 * Instead of 'save()' you can get list of decompiled classes:
 * <pre><code>
 *  for(JavaClass cls : jadx.getClasses()) {
 *      System.out.println(cls.getCode());
 *  }
 * </code></pre>
 */
public final class JadxDecompiler {
	private static final Logger LOG = LoggerFactory.getLogger(JadxDecompiler.class);

	private final IJadxArgs args;
	private final List<InputFile> inputFiles = new ArrayList<InputFile>();

	private File outDir;

	private RootNode root;
	private List<IDexTreeVisitor> passes;
	private CodeGen codeGen;

	private List<JavaClass> classes;
	private List<ResourceFile> resources;

	private BinaryXMLParser xmlParser;

	private Map<ClassNode, JavaClass> classesMap = new HashMap<ClassNode, JavaClass>();
	private Map<MethodNode, JavaMethod> methodsMap = new HashMap<MethodNode, JavaMethod>();
	private Map<FieldNode, JavaField> fieldsMap = new HashMap<FieldNode, JavaField>();

	public JadxDecompiler() {
		this(new JadxArgs());
	}

	public JadxDecompiler(IJadxArgs jadxArgs) {
		this.args = jadxArgs;
		this.outDir = jadxArgs.getOutDir();
		reset();
		init();
	}

	public void setOutputDir(File outDir) {
		this.outDir = outDir;
		init();
	}

	/**
	 *初始化
	 */
	void init() {
		if (outDir == null) {
			outDir = new JadxArgs().getOutDir();//设置输出位置
		}
		this.passes = Jadx.getPassesList(args, outDir);//添加pass任务
		this.codeGen = new CodeGen(args);//不懂
	}

	/**
	 * 置为null 没啥其他可怕的操作
	 */
	void reset() {
		classes = null;
		resources = null;
		xmlParser = null;
		root = null;
		passes = null;
		codeGen = null;
	}

	/**
	 * jadx版本号
	 * @return
	 */
	public static String getVersion() {
		return Jadx.getVersion();
	}

	/**
	 * 就是返回一个泛型List<File>
	 * @param file apk地址
	 * @throws JadxException
	 */
	public void loadFile(File file) throws JadxException {
		loadFiles(Collections.singletonList(file));//就是返回一个泛型List<File>
	}

	/**
	 *
	 * @param files apk地址
	 * @throws JadxException
	 */
	public void loadFiles(List<File> files) throws JadxException {
		if (files.isEmpty()) {
			throw new JadxException("Empty file list");
		}
		inputFiles.clear();
		for (File file : files) {//看了下好像没有多选功能
			try {
				InputFile.addFilesFrom(file, inputFiles);//静态类遍历文件集合 并把dex文件找出来
			} catch (IOException e) {
				throw new JadxException("Error load file: " + file, e);
			}
		}
		parse();//开始解析-不包括反编译
	}

	/**
	 * 保存
	 */
	public void save() {
		save(!args.isSkipSources(), !args.isSkipResources());
	}

	/**
	 * 保存代码
	 */
	public void saveSources() {
		save(true, false);
	}

	/**
	 * 保存资源
	 */
	public void saveResources() {
		save(false, true);
	}

	/**
	 * 保存
	 * @param saveSources 是否保存代码
	 * @param saveResources 是否保存资源
	 */
	private void save(boolean saveSources, boolean saveResources) {
		try {
			ExecutorService ex = getSaveExecutor(saveSources, saveResources);
			ex.shutdown();
			ex.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			throw new JadxRuntimeException("Save interrupted", e);
		}
	}

	/**
	 * 返回保存线程池 并把保存任务for循环加到线程池 进度条每隔500毫秒检测一次
	 * @return
	 */
	public ExecutorService getSaveExecutor() {
		return getSaveExecutor(!args.isSkipSources(), !args.isSkipResources());
	}

	/**
	 *
	 * @param saveSources 保存代码
	 * @param saveResources 保存资源
	 * @return
	 */
	private ExecutorService getSaveExecutor(boolean saveSources, boolean saveResources) {
		if (root == null) {
			throw new JadxRuntimeException("No loaded files");
		}
		int threadsCount = args.getThreadsCount();
		LOG.debug("processing threads count: {}", threadsCount);

		LOG.info("processing ...");
		ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
		//开始进行线程任务了 下面

		File sourcesOutDir;
		File resOutDir;
		if (args.isExportAsGradleProject()) {
			ExportGradleProject export = new ExportGradleProject(root, outDir);
			export.init();
			sourcesOutDir = export.getSrcOutDir();
			resOutDir = export.getResOutDir();
		} else {
			sourcesOutDir = outDir;
			resOutDir = outDir;
		}
		if (saveSources) {
			appendSourcesSave(executor, sourcesOutDir);
		}
		if (saveResources) {
			appendResourcesSave(executor, resOutDir);
		}
		return executor;
	}

	/**
	 * 附加导出源码任务
	 * @param executor
	 * @param outDir
	 */
	private void appendResourcesSave(ExecutorService executor, File outDir) {
		for (ResourceFile resourceFile : getResources()) {
			executor.execute(new ResourcesSaver(outDir, resourceFile));
		}
	}

	/**
	 * 附加导出源码任务
	 * @param executor
	 * @param outDir
	 */
	private void appendSourcesSave(ExecutorService executor, final File outDir) {
		for (final JavaClass cls : getClasses()) {
			if (cls.getClassNode().contains(AFlag.DONT_GENERATE)) {
				continue;
			}
			executor.execute(new Runnable() {
				@Override
				public void run() {
					cls.decompile();
					SaveCode.save(outDir, args, cls.getClassNode());
				}
			});
		}
	}

	/**
	 * 迭代classnode 获得javaclass对象集合
	 * @return
	 */
	public List<JavaClass> getClasses() {
		if (root == null) {
			return Collections.emptyList();
		}
		if (classes == null) {
			List<ClassNode> classNodeList = root.getClasses(false);
			List<JavaClass> clsList = new ArrayList<JavaClass>(classNodeList.size());
			classesMap.clear();
			for (ClassNode classNode : classNodeList) {
				JavaClass javaClass = new JavaClass(classNode, this);
				clsList.add(javaClass);
				classesMap.put(classNode, javaClass);
			}
			classes = Collections.unmodifiableList(clsList);
		}
		return classes;
	}

	/**
	 * 获得资源集合
	 * @return
	 */
	public List<ResourceFile> getResources() {
		if (resources == null) {
			if (root == null) {
				return Collections.emptyList();
			}
			resources = new ResourcesLoader(this).load(inputFiles);
		}
		return resources;
	}

	/**
	 * 获得包集合
	 * @return
	 */
	public List<JavaPackage> getPackages() {
		List<JavaClass> classList = getClasses();
		if (classList.isEmpty()) {
			return Collections.emptyList();
		}
		Map<String, List<JavaClass>> map = new HashMap<String, List<JavaClass>>();
		for (JavaClass javaClass : classList) {
			String pkg = javaClass.getPackage();
			List<JavaClass> clsList = map.get(pkg);
			if (clsList == null) {
				clsList = new ArrayList<JavaClass>();
				map.put(pkg, clsList);
			}
			clsList.add(javaClass);
		}
		List<JavaPackage> packages = new ArrayList<JavaPackage>(map.size());
		for (Map.Entry<String, List<JavaClass>> entry : map.entrySet()) {
			packages.add(new JavaPackage(entry.getKey(), entry.getValue()));
		}
		Collections.sort(packages);
		for (JavaPackage pkg : packages) {
			Collections.sort(pkg.getClasses(), new Comparator<JavaClass>() {
				@Override
				public int compare(JavaClass o1, JavaClass o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
		}
		return Collections.unmodifiableList(packages);
	}

	/**
	 * 获得错误数
	 * @return
	 */
	public int getErrorsCount() {
		if (root == null) {
			return 0;
		}
		return root.getErrorsCounter().getErrorCount();
	}

	/**
	 * 获得错误报告
	 */
	public void printErrorsReport() {
		if (root == null) {
			return;
		}
		root.getClsp().printMissingClasses();
		root.getErrorsCounter().printReport();
	}

	/**
	 * 开始解析
	 * @throws DecodeException
	 */
	void parse() throws DecodeException {
		reset();//重置null
		init();//初始化

		root = new RootNode(args);//设置根节点
		LOG.info("loading ...");
		root.load(inputFiles);//载入文件 获得了最开始的文件结构和内部类父类的集合结构

		root.initClassPath();//初始化了底层classpath
		root.loadResources(getResources());//读取res文件
		root.initAppResClass();//获得资源R

		initVisitors();//初始化访问 仅仅是初始化
	}

	/**
	 * 初始化访问者
	 */
	private void initVisitors() {
		for (IDexTreeVisitor pass : passes) {
			try {
				pass.init(root);//迭代passes 对root进行访问 此处应该是进行改名操作 RenameVisitor
			} catch (Exception e) {
				LOG.error("Visitor init failed: {}", pass.getClass().getSimpleName(), e);
			}
		}
	}

	/**
	 * 进程反编译
	 * @param cls
	 */
	void processClass(ClassNode cls) {
//		System.out.println("SRX:"+cls.getFullName());
		ProcessClass.process(cls, passes, codeGen);
	}

	RootNode getRoot() {
		return root;
	}

	synchronized BinaryXMLParser getXmlParser() {
		if (xmlParser == null) {
			xmlParser = new BinaryXMLParser(root);
		}
		return xmlParser;
	}

	Map<ClassNode, JavaClass> getClassesMap() {
		return classesMap;
	}

	Map<MethodNode, JavaMethod> getMethodsMap() {
		return methodsMap;
	}

	Map<FieldNode, JavaField> getFieldsMap() {
		return fieldsMap;
	}

	public IJadxArgs getArgs() {
		return args;
	}

	@Override
	public String toString() {
		return "jadx decompiler " + getVersion();
	}

}
