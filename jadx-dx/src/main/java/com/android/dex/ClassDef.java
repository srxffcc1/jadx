/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.dex;

/**
 *
 * A type definition. 表示某个类的全部信息，包括类类型、访问权限、父类、接口、源文件名、注解和代码等信息。ClassDefs 的大小和文件偏移在 DexHeader 和 map_list 中都有指定。
 */
public final class ClassDef {
    //  typeIndex;          指向 typeIds 的索引，表示类的类型
    //  accessFlags;        类的访问标识
    //  supertypeIndex;     指向 typeIds 的索引，表示父类类型
    //  interfacesOff;      指向 DexTypeList 的文件偏移，表示接口
    //  sourceFileIdx;      指向 stringIds 的索引，表示源文件名
    //  annotationsOff;     指向 annotations_directory_item 的文件偏移，表示注解
    //  classDataOff;       指向 class_data_item 的文件偏移
    //  staticValuesOff;    指向 DexEncodedArray 的文件偏移

    public static final int NO_INDEX = -1;
    private final Dex buffer;
    private final int offset;
    private final int typeIndex;//指向 typeIds 的索引，类类型，必须是一个类类型而不是数组或者基本类型。
    private final int accessFlags;//类的访问标识，如 public、final 等，常量定义如下，包括类、字段和方法的访问标识：
    private final int supertypeIndex;//指向 typeIds 的索引，父类类型；如果没有父类值为 NO_INDEX . 注意 NO_INDEX 值不是0，因为 0 是一个合法的索引，而且 NO_INDEX 是以 uleb128p1 编码的。
    private final int interfacesOffset;//指向 DexTypeList 的文件偏移（在数据段中），表示接口；如果没有接口此值为 0 。DexTypeList 中的值必须是类类型，并且没有重复。
    private final int sourceFileIndex;//指向 stringIds 的索引，表示本类所在的源文件名，如果没有这个信息值为 NO_INDEX
    private final int annotationsOffset;//指向 annotations_directory_item 的文件偏移，表示注解；如果没有注解，此值为 0 。
    private final int classDataOffset;//指向 class_data_item 的文件偏移，如果此类没有数据(如接口)，值为0.除了 DexCode 之外的结构是定义在 /dalvik/libdex/DexCLass.h 文件中的，并且采用的是 uleb128 编码方式。与之前不同
    private final int staticValuesOffset;//指向 DexEncodedArray 的文件偏移，表示静态字段的初始值，值为0表示没有设定静态字段的初始值，静态字段被初始化为0或者null.

    public ClassDef(Dex buffer, int offset, int typeIndex, int accessFlags,
            int supertypeIndex, int interfacesOffset, int sourceFileIndex,
            int annotationsOffset, int classDataOffset, int staticValuesOffset) {
        this.buffer = buffer;
        this.offset = offset;
        this.typeIndex = typeIndex;
        this.accessFlags = accessFlags;
        this.supertypeIndex = supertypeIndex;
        this.interfacesOffset = interfacesOffset;
        this.sourceFileIndex = sourceFileIndex;
        this.annotationsOffset = annotationsOffset;
        this.classDataOffset = classDataOffset;
        this.staticValuesOffset = staticValuesOffset;
    }

    public int getOffset() {
        return offset;
    }

    public int getTypeIndex() {
        return typeIndex;
    }

    public int getSupertypeIndex() {
        return supertypeIndex;
    }

    public int getInterfacesOffset() {
        return interfacesOffset;
    }

    public short[] getInterfaces() {
        return buffer.readTypeList(interfacesOffset).getTypes();
    }

    public int getAccessFlags() {
        return accessFlags;
    }

    public int getSourceFileIndex() {
        return sourceFileIndex;
    }

    public int getAnnotationsOffset() {
        return annotationsOffset;
    }

    public int getClassDataOffset() {
        return classDataOffset;
    }

    public int getStaticValuesOffset() {
        return staticValuesOffset;
    }

    @Override public String toString() {
        if (buffer == null) {
            return typeIndex + " " + supertypeIndex;
        }

        StringBuilder result = new StringBuilder();
        result.append(buffer.typeNames().get(typeIndex));
        if (supertypeIndex != NO_INDEX) {
            result.append(" extends ").append(buffer.typeNames().get(supertypeIndex));
        }
        return result.toString();
    }
}
