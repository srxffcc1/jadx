package jadx.core.xmlgen;

import jadx.api.ResourcesLoader;
import jadx.core.codegen.CodeWriter;
import jadx.core.dex.info.ConstStorage;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.StringUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.xmlgen.entry.ValuesParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/* TODO:
	Don't die when error occurs
	Check error cases, maybe checked const values are not always the same
	Better error messages
	What to do, when Binary XML Manifest is > size(int)?
	Check for missing chunk size types
	Implement missing data types
	Use line numbers to recreate EXACT AndroidManifest
	Check Element chunk size
*/

public class BinaryXMLParser extends CommonBinaryParser {

	private static final Logger LOG = LoggerFactory.getLogger(BinaryXMLParser.class);
	private static final String ANDROID_R_STYLE_CLS = "android.R$style";
	private static final boolean ATTR_NEW_LINE = false;

	private CodeWriter writer;
	private String[] strings;

	private String nsPrefix = "ERROR";
	private String nsURI = "ERROR";
	private String currentTag = "ERROR";

	private boolean firstElement;
	private boolean wasOneLiner = false;

	private final Map<Integer, String> styleMap = new HashMap<Integer, String>();
	private final Map<Integer, FieldNode> localStyleMap = new HashMap<Integer, FieldNode>();
	private final Map<Integer, String> resNames;
	private ValuesParser valuesParser;

	private final ManifestAttributes attributes;

	public BinaryXMLParser(RootNode root) {
		try {
			try {
				Class<?> rStyleCls = Class.forName(ANDROID_R_STYLE_CLS);
				for (Field f : rStyleCls.getFields()) {
					styleMap.put(f.getInt(f.getType()), f.getName());
				}
			} catch (Throwable th) {
				//LOGS.error("R class loading failed", th);
			}
			// add application constants
			ConstStorage constStorage = root.getConstValues();
			Map<Object, FieldNode> constFields = constStorage.getGlobalConstFields();
			for (Map.Entry<Object, FieldNode> entry : constFields.entrySet()) {
				Object key = entry.getKey();
				FieldNode field = entry.getValue();
				if (field.getType().equals(ArgType.INT) && key instanceof Integer) {
					localStyleMap.put((Integer) key, field);
				}
			}
			//获得解析的arsc里的数据
			resNames = constStorage.getResourcesNames();//

			attributes = new ManifestAttributes();
			attributes.parseAll();
		} catch (Exception e) {
			throw new JadxRuntimeException("BinaryXMLParser init error", e);
		}
	}

	/**
	 * 好像是解析啊
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
	public synchronized CodeWriter parse(InputStream inputStream) throws IOException {
		is = new ParserStream(inputStream);//构造输入流
		if (!isBinaryXml()) {//是否为2进制xml？不是就走下面
			return ResourcesLoader.loadToCodeWriter(inputStream);//从来没进去过  因为都是2进制xml
		}//以下为是2进制xml
		writer = new CodeWriter();//代码绘制板
		writer.add("<?xml version=\"1.0\" encoding=\"utf-8\"?>");//xml头
		firstElement = true;//第一个元素
		decode();//解析xml
		writer.finish();//解析完毕大概是保存吧
		return writer;
	}

	private boolean isBinaryXml() throws IOException {
		is.mark(4);
		int v = is.readInt16(); // version
		int h = is.readInt16(); // header size
		if (v == 0x0003 && h == 0x0008) {
			return true;
		}
		is.reset();
		return false;
	}
//	enum
//	{
//				RES_NULL_TYPE               = 0x0000,
//				RES_STRING_POOL_TYPE        = 0x0001,
//				RES_TABLE_TYPE              = 0x0002,
//
//				// Chunk types in RES_XML_TYPE
//				RES_XML_TYPE                = 0x0003,
//				RES_XML_FIRST_CHUNK_TYPE    = 0x0100,
//				RES_XML_START_NAMESPACE_TYPE= 0x0100,
//				RES_XML_END_NAMESPACE_TYPE  = 0x0101,
//				RES_XML_START_ELEMENT_TYPE  = 0x0102,
//				RES_XML_END_ELEMENT_TYPE    = 0x0103,
//				RES_XML_CDATA_TYPE          = 0x0104,
//				RES_XML_LAST_CHUNK_TYPE     = 0x017f,
//
//				// This contains a uint32_t array mapping strings in the string
//				// pool back to resource identifiers.  It is optional.
//				RES_XML_RESOURCE_MAP_TYPE   = 0x0180,
//
//				// Chunk types in RES_TABLE_TYPE
//				RES_TABLE_PACKAGE_TYPE      = 0x0200,
//				RES_TABLE_TYPE_TYPE         = 0x0201,
//				RES_TABLE_TYPE_SPEC_TYPE    = 0x0202
//	};
	void decode() throws IOException {
//		System.out.println("进入解析");//一个xml就是一个parse
		int size = is.readInt32();
		while (is.getPos() < size) {
			int type = is.readInt16();
			switch (type) {//判断chunk类型
				case RES_NULL_TYPE:
					// NullType is just doing nothing
					break;
				case RES_STRING_POOL_TYPE:
					strings = parseStringPoolNoType();
//					System.out.println("string+value:"+strings+","+resNames);
					valuesParser = new ValuesParser(strings, resNames);
					break;
				case RES_XML_RESOURCE_MAP_TYPE:
					parseResourceMap();
					break;
				case RES_XML_START_NAMESPACE_TYPE:
					parseNameSpace();
					break;
				case RES_XML_CDATA_TYPE:
					parseCData();
					break;
				case RES_XML_END_NAMESPACE_TYPE:
					parseNameSpaceEnd();
					break;
				case RES_XML_START_ELEMENT_TYPE:
					parseElement();
					break;
				case RES_XML_END_ELEMENT_TYPE:
					parseElementEnd();
					break;

				default:
					die("Type: 0x" + Integer.toHexString(type) + " not yet implemented");
					break;
			}
		}
	}

	private void parseResourceMap() throws IOException {
		if (is.readInt16() != 0x8) {
			die("Header size of resmap is not 8!");
		}
		int rhsize = is.readInt32();
		int[] ids = new int[(rhsize - 8) / 4];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = is.readInt32();
		}
	}

	private void parseNameSpace() throws IOException {
		if (is.readInt16() != 0x10) {
			die("NAMESPACE header is not 0x0010");
		}
		if (is.readInt32() != 0x18) {
			die("NAMESPACE header chunk is not 0x18 big");
		}
		int beginLineNumber = is.readInt32();
		int comment = is.readInt32();
		int beginPrefix = is.readInt32();
		nsPrefix = strings[beginPrefix];
		int beginURI = is.readInt32();
		nsURI = strings[beginURI];
	}

	private void parseNameSpaceEnd() throws IOException {
		if (is.readInt16() != 0x10) {
			die("NAMESPACE header is not 0x0010");
		}
		if (is.readInt32() != 0x18) {
			die("NAMESPACE header chunk is not 0x18 big");
		}
		int endLineNumber = is.readInt32();
		int comment = is.readInt32();
		int endPrefix = is.readInt32();
		nsPrefix = strings[endPrefix];
		int endURI = is.readInt32();
		nsURI = strings[endURI];
	}

	private void parseCData() throws IOException {
		if (is.readInt16() != 0x10) {
			die("CDATA header is not 0x10");
		}
		if (is.readInt32() != 0x1C) {
			die("CDATA header chunk is not 0x1C");
		}
		int lineNumber = is.readInt32();
		is.skip(4);

		int strIndex = is.readInt32();
		String str = strings[strIndex];

		writer.startLine().addIndent();
		writer.attachSourceLine(lineNumber);
		writer.add(StringUtils.escapeXML(str.trim())); // TODO: wrap into CDATA for easier reading

		int size = is.readInt16();
		is.skip(size - 2);
	}

	private void parseElement() throws IOException {
		if (firstElement) {
			firstElement = false;
		} else {
			writer.incIndent();
		}
		if (is.readInt16() != 0x10) {
			die("ELEMENT HEADER SIZE is not 0x10");
		}
		// TODO: Check element chunk size
		is.readInt32();
		int elementBegLineNumber = is.readInt32();
		int comment = is.readInt32();
		int startNS = is.readInt32();
		int startNSName = is.readInt32(); // actually is elementName...
		if (!wasOneLiner && !"ERROR".equals(currentTag)
				&& !currentTag.equals(strings[startNSName])) {
			writer.add(">");
		}
		wasOneLiner = false;
		currentTag = strings[startNSName];
		writer.startLine("<").add(currentTag);
		writer.attachSourceLine(elementBegLineNumber);
		int attributeStart = is.readInt16();
		if (attributeStart != 0x14) {
			die("startNS's attributeStart is not 0x14");
		}
		int attributeSize = is.readInt16();
		if (attributeSize != 0x14) {
			die("startNS's attributeSize is not 0x14");
		}
		int attributeCount = is.readInt16();
		int idIndex = is.readInt16();
		int classIndex = is.readInt16();
		int styleIndex = is.readInt16();
		if ("manifest".equals(currentTag) || writer.getIndent() == 0) {
			writer.add(" xmlns:android=\"").add(nsURI).add("\"");
		}
		boolean attrNewLine = attributeCount == 1 ? false : ATTR_NEW_LINE;
		for (int i = 0; i < attributeCount; i++) {
			parseAttribute(i, attrNewLine);
		}
	}

	private void parseAttribute(int i, boolean newLine) throws IOException {
		int attributeNS = is.readInt32();
		int attributeName = is.readInt32();
		int attributeRawValue = is.readInt32();
		int attrValSize = is.readInt16();
		if (attrValSize != 0x08) {
			die("attrValSize != 0x08 not supported");
		}
		if (is.readInt8() != 0) {
			die("res0 is not 0");
		}
		int attrValDataType = is.readInt8();
		int attrValData = is.readInt32();

		String attrName = strings[attributeName];
		if (newLine) {
			writer.startLine().addIndent();
		} else {
			writer.add(' ');
		}
		if (attributeNS != -1) {
			writer.add(nsPrefix).add(':');
		}
		writer.add(attrName).add("=\"");
		String decodedAttr = attributes.decode(attrName, attrValData);
		if (decodedAttr != null) {
			writer.add(decodedAttr);
		} else {
			decodeAttribute(attributeNS, attrValDataType, attrValData);
		}
		writer.add('"');
	}

	private void decodeAttribute(int attributeNS, int attrValDataType, int attrValData) {
		if (attrValDataType == TYPE_REFERENCE) {
			// reference custom processing
			String name = styleMap.get(attrValData);
			if (name != null) {
				writer.add("@*");
				if (attributeNS != -1) {
					writer.add(nsPrefix).add(':');
				}
				writer.add("style/").add(name.replaceAll("_", "."));
			} else {
				FieldNode field = localStyleMap.get(attrValData);
				if (field != null) {
					String cls = field.getParentClass().getShortName().toLowerCase();
					writer.add("@");
					if ("id".equals(cls)) {
						writer.add('+');
					}
					writer.add(cls).add("/").add(field.getName());
				} else {
					String resName = resNames.get(attrValData);
					if (resName != null) {
						writer.add("@").add(resName);
					} else {
						writer.add("0x").add(Integer.toHexString(attrValData));
					}
				}
			}
		} else {
			String str = valuesParser.decodeValue(attrValDataType, attrValData);
			writer.add(str != null ? str : "null");
		}
	}

	private void parseElementEnd() throws IOException {
		if (is.readInt16() != 0x10) {
			die("ELEMENT END header is not 0x10");
		}
		if (is.readInt32() != 0x18) {
			die("ELEMENT END header chunk is not 0x18 big");
		}
		int endLineNumber = is.readInt32();
		int comment = is.readInt32();
		int elementNS = is.readInt32();
		int elementName = is.readInt32();
		if (currentTag.equals(strings[elementName])) {
			writer.add(" />");
			wasOneLiner = true;
		} else {
			writer.startLine("</");
			writer.attachSourceLine(endLineNumber);
			if (elementNS != -1) {
				writer.add(strings[elementNS]).add(':');
			}
			writer.add(strings[elementName]).add(">");
		}
		if (writer.getIndent() != 0) {
			writer.decIndent();
		}
	}
}
