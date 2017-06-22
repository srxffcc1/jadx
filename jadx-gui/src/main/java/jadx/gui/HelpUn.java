package jadx.gui;

import jadx.core.dex.visitors.*;

/**
 * Created by Administrator on 2017/6/20.
 */
public class HelpUn {
    public static void main(String[] args) {
        VM_BlockSplitter visit1=new VM_BlockSplitter();
        VM_BlockProcessor visit2=new VM_BlockProcessor();
        VM_BlockExceptionHandler visit3=new VM_BlockExceptionHandler();
        VM_BlockFinallyExtract visit4=new VM_BlockFinallyExtract();
        VM_BlockFinish visit5=new VM_BlockFinish();
        VM_SSATransform visit6=new VM_SSATransform();
        VM_DebugInfoVisitor visit7=new VM_DebugInfoVisitor();
        VM_TypeInference visit8=new VM_TypeInference();
        VM_ConstInlineVisitor visit9=new VM_ConstInlineVisitor();
        VM_FinishTypeInference visit10=new VM_FinishTypeInference();
        VM_EliminatePhiNodes visit11=new VM_EliminatePhiNodes();
        VM_ModVisitor visit12=new VM_ModVisitor();
        VM_CodeShrinker visit13=new VM_CodeShrinker();
        VM_ReSugarCode visit14=new VM_ReSugarCode();
        VM_RegionMakerVisitor visit15=new VM_RegionMakerVisitor();
        VM_IfRegionVisitor visit16=new VM_IfRegionVisitor();
        VM_ReturnVisitor visit17=new VM_ReturnVisitor();
        VM_CodeShrinker visit18=new VM_CodeShrinker();
        VM_SimplifyVisitor visit19=new VM_SimplifyVisitor();
        VM_CheckRegions visit20=new VM_CheckRegions();
        VM_MethodInlineVisitor visit21=new VM_MethodInlineVisitor();
        VC_ExtractFieldInit visit22=new VC_ExtractFieldInit();
        VC_ClassModifier visit23=new VC_ClassModifier();
        VC_EnumVisitor visit24=new VC_EnumVisitor();
        VM_PrepareForCodeGen visit25=new VM_PrepareForCodeGen();
        VM_LoopRegionVisitor visit26=new VM_LoopRegionVisitor();
        VM_ProcessVariables visit27=new VM_ProcessVariables();
        VC_DependencyCollector visit28=new VC_DependencyCollector();
        VC_RenameVisitor visit29=new VC_RenameVisitor();
    }
}
