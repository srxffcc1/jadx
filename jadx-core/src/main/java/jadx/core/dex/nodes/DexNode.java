package jadx.core.dex.nodes;

import com.android.dex.*;
import com.android.dex.ClassData.Method;
import com.android.dex.Dex.Section;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.InfoStorage;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.files.DexFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DexNode implements IDexNode {

	public static final int NO_INDEX = -1;

	private final RootNode root;
	private final Dex dexBuf;
	private final DexFile file;

	private final List<ClassNode> classes = new ArrayList<ClassNode>();
	private final Map<ClassInfo, ClassNode> clsMap = new HashMap<ClassInfo, ClassNode>();

	private final InfoStorage infoStorage = new InfoStorage();

	public DexNode(RootNode root, DexFile input) {//单纯的构造函数
		this.root = root;
		this.file = input;
		this.dexBuf = input.getDexBuf();
	}

	public void loadClasses() throws DecodeException {
		for (ClassDef cls : dexBuf.classDefs()) {//获得一个class的迭代 具体方法是android类提供的以后再说明
			ClassNode clsNode = new ClassNode(this, cls);//此处进行了一些解析操作
			classes.add(clsNode);
			clsMap.put(clsNode.getClassInfo(), clsNode);//生成类集
		}
	}

	void initInnerClasses() {
		// move inner classes
		List<ClassNode> inner = new ArrayList<ClassNode>();
		for (ClassNode cls : classes) {
			if (cls.getClassInfo().isInner()) {
				inner.add(cls);//构造内部类集合
			}
		}
		for (ClassNode cls : inner) {//遍历内部类集合
			ClassInfo clsInfo = cls.getClassInfo();
			ClassNode parent = resolveClass(clsInfo.getParentClass());//从集合中取出查找到的父类
			if (parent == null) {
				clsMap.remove(clsInfo);//父类不存在就删除
				clsInfo.notInner(cls.dex());//设置无父类
				clsMap.put(clsInfo, cls);//重新put回去
			} else {
				parent.addInnerClass(cls);//给父类加内部类
			}
		}
	}

	public List<ClassNode> getClasses() {
		return classes;
	}

	@Nullable
	public ClassNode resolveClass(ClassInfo clsInfo) {
		return clsMap.get(clsInfo);
	}

	@Nullable
	public ClassNode resolveClass(@NotNull ArgType type) {
		if (type.isGeneric()) {
			type = ArgType.object(type.getObject());
		}
		return resolveClass(ClassInfo.fromType(this, type));
	}

	@Nullable
	public MethodNode resolveMethod(@NotNull MethodInfo mth) {
		ClassNode cls = resolveClass(mth.getDeclClass());
		if (cls != null) {
			return cls.searchMethod(mth);
		}
		return null;
	}

	/**
	 * Search method in class hierarchy.
	 */
	@Nullable
	public MethodNode deepResolveMethod(@NotNull MethodInfo mth) {
		ClassNode cls = resolveClass(mth.getDeclClass());
		if (cls == null) {
			return null;
		}
		return deepResolveMethod(cls, mth.makeSignature(false));
	}

	@Nullable
	private MethodNode deepResolveMethod(@NotNull ClassNode cls, String signature) {
		for (MethodNode m : cls.getMethods()) {
			if (m.getMethodInfo().getShortId().startsWith(signature)) {
				return m;
			}
		}
		MethodNode found;
		ArgType superClass = cls.getSuperClass();
		if (superClass != null) {
			ClassNode superNode = resolveClass(superClass);
			if (superNode != null) {
				found = deepResolveMethod(superNode, signature);
				if (found != null) {
					return found;
				}
			}
		}
		for (ArgType iFaceType : cls.getInterfaces()) {
			ClassNode iFaceNode = resolveClass(iFaceType);
			if (iFaceNode != null) {
				found = deepResolveMethod(iFaceNode, signature);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	@Nullable
	public FieldNode resolveField(FieldInfo field) {
		ClassNode cls = resolveClass(field.getDeclClass());
		if (cls != null) {
			return cls.searchField(field);
		}
		return null;
	}

	public InfoStorage getInfoStorage() {
		return infoStorage;
	}

	public DexFile getDexFile() {
		return file;
	}

	// DexBuffer wrappers

	public String getString(int index) {//id
//		System.out.println(dexBuf.strings().get(index));
		return dexBuf.strings().get(index);
	}

	public ArgType getType(int index) {//此处就是new了一个ArgType
		return ArgType.parse(getString(dexBuf.typeIds().get(index)));
	}

	public MethodId getMethodId(int mthIndex) {
		return dexBuf.methodIds().get(mthIndex);
	}

	public FieldId getFieldId(int fieldIndex) {
		return dexBuf.fieldIds().get(fieldIndex);
	}

	public ProtoId getProtoId(int protoIndex) {
		return dexBuf.protoIds().get(protoIndex);
	}

	public ClassData readClassData(ClassDef cls) {
		return dexBuf.readClassData(cls);
	}

	public List<ArgType> readParamList(int parametersOffset) {
		TypeList paramList = dexBuf.readTypeList(parametersOffset);
		List<ArgType> args = new ArrayList<ArgType>(paramList.getTypes().length);
		for (short t : paramList.getTypes()) {
			args.add(getType(t));
		}
		return Collections.unmodifiableList(args);
	}

	public Code readCode(Method mth) {
		return dexBuf.readCode(mth);
	}

	public Section openSection(int offset) {
		return dexBuf.open(offset);
	}

	@Override
	public RootNode root() {
		return root;
	}

	@Override
	public DexNode dex() {
		return this;
	}

	@Override
	public String toString() {
		return "DEX";
	}
}
