package jadx.gui.utils;

import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.ResourceBundle;

public class NLS {

	private static ResourceBundle messages;

	static {
//		load(new Locale("zh", "CN"));
		load(new Locale("en", "US"));
	}

	private NLS() {
	}

	private static void load(Locale locale) {
		messages = ResourceBundle.getBundle("i18n/Messages", locale);
	}

	public static String str(String key) {
		String ms=messages.getString(key);
//		try {
//			ms=new String(ms.getBytes("ISO8859-1"),"GB2312");
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		}

		return ms;
	}
}
