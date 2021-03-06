package jadx.tests.integration.variables;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.visitors.DepthTraversal;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.tests.api.IntegrationTest;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestVariablesDefinitions extends IntegrationTest {

	public static class TestCls {
		private static Logger LOG;
		private ClassNode cls;
		private List<IDexTreeVisitor> passes;

		public void run() {
			try {
				cls.load();
				for (IDexTreeVisitor pass : this.passes) {
					DepthTraversal.visit(pass, cls);
				}
			} catch (Exception e) {
				LOGS.error("Decode exception: {}", cls, e);
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne(indent(3) + "for (IDexTreeVisitor pass : this.passes) {"));
		assertThat(code, not(containsString("iterator;")));
	}
}
