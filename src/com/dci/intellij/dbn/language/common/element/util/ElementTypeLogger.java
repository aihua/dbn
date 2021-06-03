package com.dci.intellij.dbn.language.common.element.util;

import com.dci.intellij.dbn.environment.Environment;
import com.dci.intellij.dbn.language.common.element.ElementType;
import com.dci.intellij.dbn.language.common.element.impl.IdentifierElementType;
import com.dci.intellij.dbn.language.common.element.impl.IterationElementType;
import com.dci.intellij.dbn.language.common.element.impl.NamedElementType;
import com.dci.intellij.dbn.language.common.element.impl.OneOfElementType;
import com.dci.intellij.dbn.language.common.element.impl.QualifiedIdentifierElementType;
import com.dci.intellij.dbn.language.common.element.impl.SequenceElementType;
import com.dci.intellij.dbn.language.common.element.impl.TokenElementType;
import com.dci.intellij.dbn.language.common.element.parser.ParseResultType;
import com.dci.intellij.dbn.language.common.element.parser.ParserBuilder;

public class ElementTypeLogger {
    private ElementType elementType;

    public ElementTypeLogger(ElementType elementType) {
        this.elementType = elementType;
    }

    public void logBegin(ParserBuilder builder, boolean optional, int depth) {
        // GTK enable disable debug
        if (Environment.DEBUG_MODE) {
            StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < depth; i++) buffer.append('\t');
            buffer.append('"').append(elementType.getId()).append("\" [");
            buffer.append(getTypeDescription());
            buffer.append(": ");
            buffer.append(optional ? "optional" : "mandatory");
            buffer.append("] '").append(builder.getTokenText()).append("'");
            if (elementType.isLeaf()) System.out.print(buffer.toString());
            else System.out.println(buffer.toString());
            //log.info(msg);
        }
    }

    public void logEnd(ParseResultType resultType, int depth) {
        if (Environment.DEBUG_MODE) {
            StringBuilder buffer = new StringBuilder();
            if (!elementType.isLeaf()) {
                for (int i = 0; i < depth; i++) buffer.append('\t');
                buffer.append('"').append(elementType.getId()).append('"');
            }
            buffer.append(" >> ");
            switch (resultType) {
                case FULL_MATCH: buffer.append("Matched"); break;
                case PARTIAL_MATCH: buffer.append("Partially matched"); break;
                case NO_MATCH: buffer.append("Not matched"); break;
            }
            System.out.println(buffer.toString());
            //log.info(msg);
        }
    }

    private String getTypeDescription(){
        if (elementType instanceof TokenElementType) return "token";
        if (elementType instanceof NamedElementType) return "element";
        if (elementType instanceof SequenceElementType) return "sequence";
        if (elementType instanceof IterationElementType) return "iteration";
        if (elementType instanceof QualifiedIdentifierElementType) return "qualified-identifier";
        if (elementType instanceof OneOfElementType) return "one-of";
        if (elementType instanceof IdentifierElementType) {
            IdentifierElementType iet = (IdentifierElementType) elementType;
            return  iet.getId();
        }
        return null;
    }

    public void logErr(ParserBuilder builder, boolean optional, int depth) {
        logBegin(builder, optional, depth);
        logEnd(ParseResultType.NO_MATCH, depth);
    }
}
