package jadx.api;

import jadx.core.codegen.CodeWriter;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.nodes.LineAttrNode;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 *
 SRX1:jadx.core.dex.visitors.blocksmaker.VM_BlockSplitter
 SRX1:jadx.core.dex.visitors.blocksmaker.VM_BlockProcessor
 SRX1:jadx.core.dex.visitors.blocksmaker.VM_BlockExceptionHandler
 SRX1:jadx.core.dex.visitors.blocksmaker.VM_BlockFinallyExtract
 SRX1:jadx.core.dex.visitors.blocksmaker.VM_BlockFinish
 SRX1:jadx.core.dex.visitors.VM_SSATransform
 SRX1:jadx.core.dex.visitors.VM_DebugInfoVisitor
 SRX1:jadx.core.dex.visitors.VM_TypeInference
 SRX1:jadx.core.dex.visitors.VM_ConstInlineVisitor
 SRX1:jadx.core.dex.visitors.VM_FinishTypeInference
 SRX1:jadx.core.dex.visitors.VM_EliminatePhiNodes
 SRX1:jadx.core.dex.visitors.VM_ModVisitor
 SRX1:jadx.core.dex.visitors.VM_CodeShrinker
 SRX1:jadx.core.dex.visitors.VM_ReSugarCode
 SRX1:jadx.core.dex.visitors.VM_RegionMakerVisitor
 SRX1:jadx.core.dex.visitors.VM_IfRegionVisitor
 SRX1:jadx.core.dex.visitors.VM_ReturnVisitor
 SRX1:jadx.core.dex.visitors.VM_CodeShrinker
 SRX1:jadx.core.dex.visitors.VM_SimplifyVisitor
 SRX1:jadx.core.dex.visitors.VM_CheckRegions
 SRX1:jadx.core.dex.visitors.VM_MethodInlineVisitor
 SRX1:jadx.core.dex.visitors.VC_ExtractFieldInit
 SRX1:jadx.core.dex.visitors.VC_ClassModifier
 SRX1:jadx.core.dex.visitors.VC_EnumVisitor
 SRX1:jadx.core.dex.visitors.VM_PrepareForCodeGen
 SRX1:jadx.core.dex.visitors.VM_LoopRegionVisitor
 SRX1:jadx.core.dex.visitors.VM_ProcessVariables
 SRX1:jadx.core.dex.visitors.VC_DependencyCollector
 SRX1:jadx.core.dex.visitors.VC_RenameVisitor
 */
public final class JavaClass implements JavaNode {

	private final JadxDecompiler decompiler;//解析器
	private final ClassNode cls;
	private final JavaClass parent;//父类

	private List<JavaClass> innerClasses = Collections.emptyList();//内部类集合
	private List<JavaField> fields = Collections.emptyList();//字段集合
	private List<JavaMethod> methods = Collections.emptyList();//方法集合

	/**
	 * 构造方法
	 * @param classNode
	 * @param decompiler
	 */
	JavaClass(ClassNode classNode, JadxDecompiler decompiler) {
		this.decompiler = decompiler;
		this.cls = classNode;
		this.parent = null;
	}

	/**
	 * 内部类构造方法
	 * @param classNode
	 * @param parent
	 */
	JavaClass(ClassNode classNode, JavaClass parent) {
		this.decompiler = null;
		this.cls = classNode;
		this.parent = parent;
	}

	/**
	 * 获得代码
	 * @return
	 */
	public String getCode() {
		CodeWriter code = cls.getCode();
		if (code == null) {
			decompile();
			code = cls.getCode();
			if (code == null) {
				return "";
			}
		}
		return code.getCodeStr();
	}

	/**
	 * 反编译
	 */
	public synchronized void decompile() {
		if (decompiler == null) {
			return;
		}
		if (cls.getCode() == null) {
			decompiler.processClass(cls);//进行作业
			load();//载入
		}
	}

	ClassNode getClassNode() {
		return cls;
	}

	private void load() {
		JadxDecompiler rootDecompiler = getRootDecompiler();
		int inClsCount = cls.getInnerClasses().size();
		if (inClsCount != 0) {
			List<JavaClass> list = new ArrayList<JavaClass>(inClsCount);
			for (ClassNode inner : cls.getInnerClasses()) {
				if (!inner.contains(AFlag.DONT_GENERATE)) {
					JavaClass javaClass = new JavaClass(inner, this);
					javaClass.load();
					list.add(javaClass);
					rootDecompiler.getClassesMap().put(inner, javaClass);
				}
			}
			this.innerClasses = Collections.unmodifiableList(list);
		}

		int fieldsCount = cls.getFields().size();
		if (fieldsCount != 0) {
			List<JavaField> flds = new ArrayList<JavaField>(fieldsCount);
			for (FieldNode f : cls.getFields()) {
				if (!f.contains(AFlag.DONT_GENERATE)) {
					JavaField javaField = new JavaField(f, this);
					flds.add(javaField);
					rootDecompiler.getFieldsMap().put(f, javaField);
				}
			}
			this.fields = Collections.unmodifiableList(flds);
		}

		int methodsCount = cls.getMethods().size();
		if (methodsCount != 0) {
			List<JavaMethod> mths = new ArrayList<JavaMethod>(methodsCount);
			for (MethodNode m : cls.getMethods()) {
				if (!m.contains(AFlag.DONT_GENERATE)) {
					JavaMethod javaMethod = new JavaMethod(this, m);
					mths.add(javaMethod);
					rootDecompiler.getMethodsMap().put(m, javaMethod);
				}
			}
			Collections.sort(mths, new Comparator<JavaMethod>() {
				@Override
				public int compare(JavaMethod o1, JavaMethod o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			this.methods = Collections.unmodifiableList(mths);
		}
	}

	private JadxDecompiler getRootDecompiler() {
		if (parent != null) {
			return parent.getRootDecompiler();
		}
		return decompiler;
	}

	private Map<CodePosition, Object> getCodeAnnotations() {
		decompile();
		return cls.getCode().getAnnotations();
	}

	public Map<CodePosition, JavaNode> getUsageMap() {
		Map<CodePosition, Object> map = getCodeAnnotations();
		if (map.isEmpty() || decompiler == null) {
			return Collections.emptyMap();
		}
		Map<CodePosition, JavaNode> resultMap = new HashMap<CodePosition, JavaNode>(map.size());
		for (Map.Entry<CodePosition, Object> entry : map.entrySet()) {
			CodePosition codePosition = entry.getKey();
			Object obj = entry.getValue();
			if (obj instanceof LineAttrNode) {
				JavaNode node = convertNode(obj);
				if (node != null) {
					resultMap.put(codePosition, node);
				}
			}
		}
		return resultMap;
	}

	@Nullable
	private JavaNode convertNode(Object obj) {
		if (!(obj instanceof LineAttrNode)) {
			return null;
		}
		if (obj instanceof ClassNode) {
			return getRootDecompiler().getClassesMap().get(obj);
		}
		if (obj instanceof MethodNode) {
			return getRootDecompiler().getMethodsMap().get(obj);
		}
		if (obj instanceof FieldNode) {
			return getRootDecompiler().getFieldsMap().get(obj);
		}
		return null;
	}

	@Nullable
	public JavaNode getJavaNodeAtPosition(int line, int offset) {
		Map<CodePosition, Object> map = getCodeAnnotations();
		if (map.isEmpty()) {
			return null;
		}
		Object obj = map.get(new CodePosition(line, offset));
		if (obj == null) {
			return null;
		}
		return convertNode(obj);
	}

	@Nullable
	public CodePosition getDefinitionPosition(int line, int offset) {
		JavaNode javaNode = getJavaNodeAtPosition(line, offset);
		if (javaNode == null) {
			return null;
		}
		return getDefinitionPosition(javaNode);
	}

	@Nullable
	public CodePosition getDefinitionPosition(JavaNode javaNode) {
		JavaClass jCls = javaNode.getTopParentClass();
		jCls.decompile();
		int defLine = javaNode.getDecompiledLine();
		if (defLine == 0) {
			return null;
		}
		return new CodePosition(jCls, defLine, 0);
	}

	public Integer getSourceLine(int decompiledLine) {
		decompile();
		return cls.getCode().getLineMapping().get(decompiledLine);
	}

	@Override
	public String getName() {
		return cls.getShortName();
	}

	@Override
	public String getFullName() {
		return cls.getFullName();
	}

	public String getPackage() {
		return cls.getPackage();
	}

	@Override
	public JavaClass getDeclaringClass() {
		return parent;
	}

	@Override
	public JavaClass getTopParentClass() {
		return parent == null ? this : parent.getTopParentClass();
	}

	public AccessInfo getAccessInfo() {
		return cls.getAccessFlags();
	}

	public List<JavaClass> getInnerClasses() {
		decompile();
		return innerClasses;
	}

	public List<JavaField> getFields() {
		decompile();
		return fields;
	}

	public List<JavaMethod> getMethods() {
		decompile();
		return methods;
	}

	public int getDecompiledLine() {
		return cls.getDecompiledLine();
	}

	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof JavaClass && cls.equals(((JavaClass) o).cls);
	}

	@Override
	public int hashCode() {
		return cls.hashCode();
	}

	@Override
	public String toString() {
		return cls.getFullName() + "[ " + getFullName() + " ]";
	}
}
