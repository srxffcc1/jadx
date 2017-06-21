package jadx.core;

import jadx.api.IJadxArgs;
import jadx.core.dex.visitors.*;
import jadx.core.dex.visitors.VC_ExtractFieldInit;
import jadx.core.dex.visitors.blocksmaker.*;
import jadx.core.dex.visitors.blocksmaker.VM_BlockExceptionHandler;
import jadx.core.dex.visitors.VM_CheckRegions;
import jadx.core.dex.visitors.VM_EliminatePhiNodes;
import jadx.core.dex.visitors.VM_SSATransform;
import jadx.core.dex.visitors.VM_FinishTypeInference;
import jadx.core.dex.visitors.VM_TypeInference;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Jadx {
	private static final Logger LOG = LoggerFactory.getLogger(Jadx.class);

	static {
		if (Consts.DEBUG) {
			LOG.info("debug enabled");
		}
	}

	public static List<IDexTreeVisitor> getPassesList(IJadxArgs args, File outDir) {
		List<IDexTreeVisitor> passes = new ArrayList<IDexTreeVisitor>();
		if (args.isFallbackMode()) {
			passes.add(new VM_FallbackModeVisitor());
		} else {
			passes.add(new VM_BlockSplitter());
			passes.add(new VM_BlockProcessor());
			passes.add(new VM_BlockExceptionHandler());
			passes.add(new VM_BlockFinallyExtract());
			passes.add(new VM_BlockFinish());

			passes.add(new VM_SSATransform());
			passes.add(new VM_DebugInfoVisitor());
			passes.add(new VM_TypeInference());

			if (args.isRawCFGOutput()) {
				passes.add(VM_DotGraphVisitor.dumpRaw(outDir));
			}

			passes.add(new VM_ConstInlineVisitor());
			passes.add(new VM_FinishTypeInference());
			passes.add(new VM_EliminatePhiNodes());

			passes.add(new VM_ModVisitor());

			passes.add(new VM_CodeShrinker());
			passes.add(new VM_ReSugarCode());

			if (args.isCFGOutput()) {
				passes.add(VM_DotGraphVisitor.dump(outDir));
			}

			passes.add(new VM_RegionMakerVisitor());
			passes.add(new VM_IfRegionVisitor());
			passes.add(new VM_ReturnVisitor());

			passes.add(new VM_CodeShrinker());
			passes.add(new VM_SimplifyVisitor());
			passes.add(new VM_CheckRegions());

			if (args.isCFGOutput()) {
				passes.add(VM_DotGraphVisitor.dumpRegions(outDir));
			}

			passes.add(new VM_MethodInlineVisitor());
			passes.add(new VC_ExtractFieldInit());
			passes.add(new VC_ClassModifier());
			passes.add(new VC_EnumVisitor());
			passes.add(new VM_PrepareForCodeGen());
			passes.add(new VM_LoopRegionVisitor());
			passes.add(new VM_ProcessVariables());

			passes.add(new VC_DependencyCollector());

			passes.add(new VC_RenameVisitor());
		}
		return passes;
	}

	public static String getVersion() {
		try {
			ClassLoader classLoader = Jadx.class.getClassLoader();
			if (classLoader != null) {
				Enumeration<URL> resources = classLoader.getResources("META-INF/MANIFEST.MF");
				while (resources.hasMoreElements()) {
					Manifest manifest = new Manifest(resources.nextElement().openStream());
					String ver = manifest.getMainAttributes().getValue("jadx-version");
					if (ver != null) {
						return ver;
					}
				}
			}
		} catch (Exception e) {
			LOG.error("Can't get manifest file", e);
		}
		return "dev";
	}
}
