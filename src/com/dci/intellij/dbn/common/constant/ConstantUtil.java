package com.dci.intellij.dbn.common.constant;

import com.dci.intellij.dbn.common.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class ConstantUtil {
    public static List<String> getIdList(Class<? extends Constant> constantClass) {
        return toIdList(Arrays.asList(constantClass.getEnumConstants()));
    }

    public static String[] getIdArray(Class<? extends Constant> constantClass) {
        List<String> idList = toIdList(Arrays.asList(constantClass.getEnumConstants()));
        return idList.toArray(new String[0]);
    }


    public static List<String> toIdList(List<? extends Constant> constants) {
        List<String> ids = new ArrayList<String>(constants.size());
        for (Constant constant : constants) {
            ids.add(constant.id());
        }
        Collections.sort(ids);
        return ids;
    }

    public static <T extends Constant> List<T> toConstantsList(List<String> ids, Class<T> constantClass) {
        List<T> constants = new ArrayList<T>(ids.size());
        T[] allConstants = constantClass.getEnumConstants();
        for(String id : ids) {
            for (T t : allConstants) {
                if (Objects.equals(t.id(), id)) {
                    constants.add(t);
                    break;
                }

            }
        }
        return constants;
    }

    public static <T extends Constant> boolean isOneOf(T constant, T ... constants) {
        if (constants != null && constants.length > 0) {
            for (T c : constants) {
                if (c == constant) {
                    return true;
                }
            }
        }

        return false;
    }

    public static <T extends Constant> T get(T[] constants, String id, boolean throwException) {
        T constant = get(constants, id);
        if (!StringUtil.isEmpty(id) && constant == null && throwException) {
            String className = constants[0].getClass().getSimpleName();
            throw new IllegalArgumentException("Invalid " + className + ": '" + id + "'.");
        }
        return constant;
    }

    public static <T extends Constant> T get(T[] constants, String id, T defaultConstant) {
        T constant = get(constants, id);
        return constant == null ? defaultConstant : constant;
    }

    public static <T extends Constant> T get(T[] constants, String id) {
        if (StringUtil.isNotEmpty(id)){
            for (T constant : constants) {
                if (Objects.equals(constant.id(), id)) {
                    return constant;
                }
            }
        }
        return null;
    }

    public static String toId(Constant constant) {
        return constant == null ? null : constant.id();
    }

    public static <T extends Constant> List<T> fromCsv(Class<T> clazz, String csvIds) {
        List<T> constants = new ArrayList<T>();

        if (StringUtil.isNotEmpty(csvIds)) {
            String[] ids = csvIds.split(",");
            boolean isPseudoConstant = PseudoConstant.class.isAssignableFrom(clazz);

            T[] enumConstants = isPseudoConstant ? null : clazz.getEnumConstants();

            for (String id : ids) {
                id = id.trim();
                if (id.length() > 0) {
                    T constant;
                    if (isPseudoConstant) {
                        Class<PseudoConstant> pseudoConstantClass = (Class<PseudoConstant>) clazz;
                        constant = (T) PseudoConstant.get(pseudoConstantClass, id);
                    } else {
                        constant = get(enumConstants, id);
                    }
                    constants.add(constant);
                }
            }
        }
        return constants;
    }

    public static <T extends Constant> boolean isValid(String id, T[] constants) {
        T constant = get(constants, id);
        return constant != null;
    }
}
