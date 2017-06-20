package jadx.core.dex.visitors;

import jadx.api.IJadxArgs;
import jadx.core.codegen.ClassGen;
import jadx.core.codegen.CodeWriter;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.exceptions.CodegenException;

public class VC_CodeGen extends AbstractVisitor {

	private final IJadxArgs args;

	public VC_CodeGen(IJadxArgs args) {
		this.args = args;
	}

	@Override
	public boolean visit(ClassNode cls) throws CodegenException {
		ClassGen clsGen = new ClassGen(cls, args);
		CodeWriter clsCode = clsGen.makeClass();
		clsCode.finish();
		cls.setCode(clsCode);
		return false;
	}

}
