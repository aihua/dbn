package com.dci.intellij.dbn.common.util;

import com.dci.intellij.dbn.language.common.psi.IdentifierPsiElement;
import com.dci.intellij.dbn.object.common.DBObject;
import com.dci.intellij.dbn.object.type.DBObjectType;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@UtilityClass
public class Naming {

    public static String nextNumberedIdentifier(String identifier, boolean insertWhitespace) {
        StringBuilder text = new StringBuilder();
        StringBuilder number = new StringBuilder();
        for (int i=identifier.length() -1; i >= 0; i--) {
            char chr = identifier.charAt(i);
            if ('0' <= chr && chr <= '9') {
                number.insert(0, chr);
            } else {
                text.append(identifier, 0, i+1);
                break;
            }
        }
        int nr = number.length() == 0 ? 0 : Integer.parseInt(number.toString());
        nr++;
        if (insertWhitespace && nr == 1) text.append(" ");
        return text.toString() + nr;
    }

    public static String nextNumberedIdentifier(String identifier, boolean insertWhitespace, Supplier<Set<String>> taken) {
        Set<String> takenIdentifiers = taken.get();
        while (takenIdentifiers.contains(identifier)) {
            identifier = nextNumberedIdentifier(identifier, insertWhitespace);
        }
        return identifier;
    }

    public static String createNamesList(Set<IdentifierPsiElement> identifiers, int maxItems) {
        boolean partial = false;
        Set<String> names = new HashSet<>();
        for (IdentifierPsiElement identifier : identifiers) {
            names.add(identifier.getUnquotedText().toString().toUpperCase());
            if (names.size() >= maxItems) {
                partial = identifiers.size() > maxItems;
                break;
            }
        }

        StringBuilder buffer = new StringBuilder();
        for (String name : names) {
            if (buffer.length() > 0) buffer.append(", ");
            buffer.append(name);
        }

        if (partial) {
            buffer.append("...");
        }
        return buffer.toString();
    }

    public static String[] createAliasNames(DBObject object) {
        if (object != null) {
            return createAliasNames(object.getName());
        }
        return new String[0];
    }

    public static String[] createAliasNames(CharSequence  objectName) {
        return new String[]{createAliasName(objectName)};
    }

    public static String createAliasName(CharSequence objectName) {
        StringBuilder camelBuffer = new StringBuilder();

        camelBuffer.append(objectName.charAt(0));

        for (int i = 1; i < objectName.length(); i++) {
            char previous = objectName.charAt(i - 1);
            char current = objectName.charAt(i);
            if (!Character.isLetter(previous) && Character.isLetter(current)) {
                camelBuffer.append(current);
            }
        }
        return camelBuffer.toString().toLowerCase();
    }

    public static String createCommaSeparatedList(DBObject[] objects) {
        StringBuilder buffer = new StringBuilder();
        for (DBObject column : objects) {
            buffer.append(column.getName());
            if (column != objects[objects.length-1]) {
                buffer.append(", ");
            }
        }
        return buffer.toString();
    }

    public static String createCommaSeparatedList(List<? extends DBObject> objects) {
        StringBuilder buffer = new StringBuilder();
        for (DBObject column : objects) {
            buffer.append(column.getName());
            if (column != objects.get(objects.size()-1)) {
                buffer.append(", ");
            }
        }
        return buffer.toString();
    }

    public static String createFriendlyName(String name) {
        StringBuilder friendlyName = new StringBuilder(name.replace('_', ' '));
        for (int i=0; i<friendlyName.length(); i++) {
            if (i>0 && Character.isLetter(friendlyName.charAt(i-1))){
                char chr = friendlyName.charAt(i);
                chr = Character.toLowerCase(chr);
                friendlyName.setCharAt(i, chr);
            }
        }
        return friendlyName.toString();
    }

    private static String duplicateCharacter(String name, char chr) {
        if (name != null) {
            int index = name.indexOf(chr);
            if (index > -1) {
                int startIndex = 0;
                StringBuilder buffer  = new StringBuilder();
                while(index > -1) {
                    buffer.append(name, startIndex, index+1);
                    buffer.append(chr);
                    startIndex = index + 1;
                    index = name.indexOf(chr, startIndex);
                }
                buffer.append(name.substring(startIndex));
                return buffer.toString();
            }
        }
        return name;
    }

    public static String capitalize(String string) {
        string = string.toLowerCase();
        string = Character.toUpperCase(string.charAt(0)) + string.substring(1);
        return string;
    }

    public static String capitalizeWords(String string) {
        StringBuilder result = new StringBuilder(string.toLowerCase());
        for (int i=0; i<result.length(); i++) {
            if (i == 0 || !Character.isLetter(result.charAt(i-1))) {
                result.setCharAt(i, Character.toUpperCase(result.charAt(i)));
            }
        }
        return result.toString();
    }

    public static String unquote(String string) {
        if (string.length() > 1) {
            char firstChar = string.charAt(0);
            char lastChar = string.charAt(string.length() - 1);
            if ((firstChar =='"' && lastChar == '"') || (firstChar == '`' && lastChar == '`') || (firstChar == '\'' && lastChar == '\'')) {
                return string.substring(1, string.length() - 1);
            }
            return string;
        } else {
            return string;
        }
    }

    public static CharSequence unquote(CharSequence charSequence) {
        if (charSequence.length() > 1) {
            char firstChar = charSequence.charAt(0);
            char lastChar = charSequence.charAt(charSequence.length() - 1);
            if ((firstChar =='"' && lastChar == '"') || (firstChar == '`' && lastChar == '`') || (firstChar == '\'' && lastChar == '\'')) {
                return charSequence.subSequence(1, charSequence.length() - 1);
            }
            return charSequence;
        } else {
            return charSequence;
        }
    }

    public static String getQualifiedObjectName(DBObject object) {
        return object.getTypeName() + " \"" + object.ref().getPath() + "\"";
    }

    public static String getQualifiedObjectName(DBObjectType objectType, String objectName, @Nullable DBObject parentObject) {
        return objectType.getName() + " \"" + (parentObject == null ? "" : parentObject.ref().getPath()) + "." + objectName + "\"";
    }
}
