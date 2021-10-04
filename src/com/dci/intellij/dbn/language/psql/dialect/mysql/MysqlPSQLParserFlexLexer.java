/* The following code was generated by JFlex 1.7.0-SNAPSHOT tweaked for IntelliJ platform */

package com.dci.intellij.dbn.language.psql.dialect.mysql;

import com.dci.intellij.dbn.language.common.TokenTypeBundle;
import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;


/**
 * This class is a scanner generated by 
 * <a href="http://www.jflex.de/">JFlex</a> 1.7.0-SNAPSHOT
 * from the specification file <tt>D:/workspaces/DBN-14.0/src/com/dci/intellij/dbn/language/psql/dialect/mysql/mysql_psql_parser.flex</tt>
 */
public final class MysqlPSQLParserFlexLexer implements FlexLexer {

  /** This character denotes the end of file */
  public static final int YYEOF = -1;

  /** initial size of the lookahead buffer */
  private static final int ZZ_BUFFERSIZE = 16384;

  /** lexical states */
  public static final int YYINITIAL = 0;
  public static final int DIV = 2;

  /**
   * ZZ_LEXSTATE[l] is the state in the DFA for the lexical state l
   * ZZ_LEXSTATE[l+1] is the state in the DFA for the lexical state l
   *                  at the beginning of a line
   * l is of the form l = 2*k, k a non negative integer
   */
  private static final int ZZ_LEXSTATE[] = { 
     0,  0,  1, 1
  };

  /** 
   * Translates characters to character classes
   * Chosen bits are [12, 6, 3]
   * Total runtime size is 14816 bytes
   */
  public static int ZZ_CMAP(int ch) {
    return ZZ_CMAP_A[(ZZ_CMAP_Y[(ZZ_CMAP_Z[ch>>9]<<6)|((ch>>3)&0x3f)]<<3)|(ch&0x7)];
  }

  /* The ZZ_CMAP_Z table has 2176 entries */
  static final char ZZ_CMAP_Z[] = zzUnpackCMap(
    "\1\0\1\1\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\1\12\1\13\1\14\1\15\1\16\1\17\1"+
    "\20\5\21\1\22\1\23\1\24\1\21\14\25\1\26\50\25\1\27\2\25\1\30\1\31\1\32\1\33"+
    "\25\25\1\34\20\21\1\35\1\36\1\37\1\40\1\41\1\42\1\43\1\21\1\44\1\45\1\46\1"+
    "\21\1\47\2\21\1\50\4\21\1\25\1\51\1\52\5\21\2\25\1\53\31\21\1\25\1\54\1\21"+
    "\1\55\40\21\1\56\17\21\1\57\1\60\1\61\1\62\13\21\1\63\10\21\123\25\1\64\7"+
    "\25\1\65\1\66\37\21\1\25\1\66\u0582\21\1\67\u017f\21");

  /* The ZZ_CMAP_Y table has 3584 entries */
  static final char ZZ_CMAP_Y[] = zzUnpackCMap(
    "\1\0\1\1\1\0\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\1\12\1\13\1\10\1\11\1\14\1"+
    "\15\3\0\1\16\1\17\1\20\1\21\2\11\1\22\3\11\1\22\71\11\1\23\1\11\1\24\1\25"+
    "\1\26\1\27\2\25\16\0\1\30\1\16\1\31\1\32\2\11\1\33\11\11\1\34\21\11\1\35\1"+
    "\36\23\11\1\25\1\37\3\11\1\22\1\40\1\37\4\11\1\41\1\42\4\0\1\43\1\44\1\25"+
    "\3\11\2\45\1\25\1\46\1\47\1\0\1\50\5\11\1\51\3\0\1\52\1\53\13\11\1\54\1\43"+
    "\1\55\1\56\1\0\1\57\1\25\1\60\1\61\3\11\3\0\1\62\12\11\1\63\1\0\1\64\1\25"+
    "\1\0\1\65\3\11\1\51\1\66\1\21\2\11\1\63\1\67\1\70\1\71\2\25\3\11\1\72\10\25"+
    "\1\73\1\26\6\25\1\74\2\0\1\75\1\76\6\11\1\77\2\0\1\100\1\11\1\101\1\0\2\37"+
    "\1\102\1\103\1\104\2\11\1\73\1\105\1\106\1\107\1\110\1\60\1\111\1\101\1\0"+
    "\1\112\1\47\1\102\1\113\1\104\2\11\1\73\1\114\1\115\1\116\1\117\1\120\1\121"+
    "\1\122\1\0\1\123\1\25\1\102\1\34\1\33\2\11\1\73\1\124\1\106\1\43\1\125\1\126"+
    "\1\25\1\101\1\0\1\40\1\25\1\102\1\103\1\104\2\11\1\73\1\124\1\106\1\107\1"+
    "\117\1\122\1\111\1\101\1\0\1\40\1\25\1\127\1\130\1\131\1\132\1\133\1\130\1"+
    "\11\1\134\1\135\1\136\1\137\1\25\1\122\1\0\1\25\1\40\1\102\1\30\1\73\2\11"+
    "\1\73\1\140\1\141\1\142\1\136\1\143\1\24\1\101\1\0\2\25\1\144\1\30\1\73\2"+
    "\11\1\73\1\140\1\106\1\142\1\136\1\143\1\31\1\101\1\0\1\145\1\25\1\144\1\30"+
    "\1\73\4\11\1\146\1\142\1\147\1\60\1\25\1\101\1\0\1\25\1\36\1\144\1\11\1\22"+
    "\1\36\2\11\1\33\1\150\1\22\1\151\1\152\1\0\2\25\1\153\1\25\1\37\5\11\1\154"+
    "\1\155\1\156\1\75\1\0\1\157\4\25\1\160\1\161\1\162\1\37\1\163\1\164\1\154"+
    "\1\165\1\166\1\167\1\0\1\170\4\25\1\126\2\25\1\157\1\0\1\157\1\171\1\172\1"+
    "\11\1\37\3\11\1\26\1\42\1\0\1\142\1\173\1\0\1\42\3\0\1\46\1\174\7\25\5\11"+
    "\1\51\1\0\1\175\1\0\1\157\1\63\1\176\1\177\1\200\1\201\1\11\1\202\1\203\1"+
    "\0\1\167\4\11\1\34\1\20\5\11\1\204\51\11\1\131\1\22\1\131\5\11\1\131\4\11"+
    "\1\131\1\22\1\131\1\11\1\22\7\11\1\131\10\11\1\205\4\25\2\11\2\25\12\11\1"+
    "\26\1\25\1\37\114\11\1\103\2\11\1\37\2\11\1\45\11\11\1\130\1\126\1\25\1\11"+
    "\1\30\1\206\1\25\2\11\1\206\1\25\2\11\1\207\1\25\1\11\1\30\1\210\1\25\6\11"+
    "\1\211\3\0\1\212\1\213\1\0\1\157\3\25\1\214\1\0\1\157\13\11\1\25\5\11\1\215"+
    "\10\11\1\216\1\25\3\11\1\26\1\0\1\2\1\0\1\2\1\122\1\0\3\11\1\216\1\26\1\25"+
    "\5\11\1\112\2\0\1\53\1\157\1\0\1\157\4\25\2\11\1\156\1\2\6\11\1\173\1\75\3"+
    "\0\1\107\1\0\1\157\1\0\1\157\1\41\13\25\1\217\5\11\1\211\1\0\1\217\1\112\1"+
    "\0\1\157\1\25\1\220\1\2\1\25\1\221\3\11\1\100\1\200\1\0\1\65\4\11\1\63\1\0"+
    "\1\2\1\25\4\11\1\211\2\0\1\25\1\0\1\222\1\0\1\65\3\11\1\216\12\25\1\223\2"+
    "\0\1\224\1\225\1\25\30\11\4\0\1\75\2\25\1\74\42\11\2\216\4\11\2\216\1\11\1"+
    "\226\3\11\1\216\6\11\1\30\1\166\1\227\1\26\1\230\1\112\1\11\1\26\1\227\1\26"+
    "\1\25\1\220\3\25\1\231\1\25\1\41\1\126\1\25\1\232\1\25\1\46\1\233\1\40\1\41"+
    "\2\25\1\11\1\26\3\11\1\45\2\25\1\0\1\46\1\234\1\0\1\235\1\25\1\236\1\36\1"+
    "\150\1\237\1\27\1\240\1\11\1\241\1\242\1\243\2\25\5\11\1\126\116\25\5\11\1"+
    "\22\5\11\1\22\20\11\1\26\1\244\1\245\1\25\4\11\1\34\1\20\7\11\1\41\1\25\1"+
    "\60\2\11\1\22\1\25\10\22\4\0\5\25\1\41\72\25\1\242\3\25\1\37\1\202\1\237\1"+
    "\26\1\37\11\11\1\22\1\246\1\37\12\11\1\204\1\242\4\11\1\216\1\37\12\11\1\22"+
    "\2\25\3\11\1\45\6\25\170\11\1\216\11\25\71\11\1\26\6\25\21\11\1\26\10\25\5"+
    "\11\1\216\41\11\1\26\2\11\1\0\1\245\2\25\5\11\1\156\1\74\1\247\3\11\1\60\12"+
    "\11\1\157\3\25\1\41\1\11\1\36\14\11\1\250\1\112\1\25\1\11\1\45\11\25\1\11"+
    "\1\251\1\252\2\11\1\51\2\25\1\126\6\11\1\112\1\25\1\65\5\11\1\211\1\0\1\46"+
    "\1\25\1\0\1\157\2\0\1\65\1\47\1\0\1\65\2\11\1\63\1\167\2\11\1\156\1\0\1\2"+
    "\1\25\3\11\1\26\1\76\5\11\1\51\1\0\1\235\1\41\1\0\1\157\4\25\5\11\1\100\1"+
    "\75\1\25\1\252\1\253\1\0\1\157\2\11\1\22\1\254\6\11\1\177\1\255\1\215\2\25"+
    "\1\256\1\11\1\51\1\257\1\25\3\260\1\25\2\22\22\25\4\11\1\51\1\261\1\0\1\157"+
    "\64\11\1\112\1\25\2\11\1\22\1\262\5\11\1\112\40\25\55\11\1\216\15\11\1\24"+
    "\4\25\1\22\1\25\1\262\1\263\1\11\1\73\1\22\1\166\1\264\15\11\1\24\3\25\1\262"+
    "\54\11\1\216\2\25\10\11\1\36\6\11\5\25\1\11\1\26\2\0\2\25\1\75\1\25\1\133"+
    "\2\25\1\242\3\25\1\40\1\30\20\11\1\265\1\232\1\25\1\0\1\157\1\37\2\11\1\113"+
    "\1\37\2\11\1\45\1\266\12\11\1\22\3\36\1\267\1\270\2\25\1\271\1\11\1\140\2"+
    "\11\1\22\2\11\1\272\1\11\1\216\1\11\1\216\4\25\17\11\1\45\10\25\6\11\1\26"+
    "\20\25\1\273\20\25\3\11\1\26\6\11\1\126\5\25\3\11\1\22\2\25\3\11\1\45\6\25"+
    "\3\11\1\216\4\11\1\112\1\11\1\237\5\25\23\11\1\216\1\0\1\157\52\25\1\216\1"+
    "\73\4\11\1\34\1\274\2\11\1\216\25\25\2\11\1\216\1\25\3\11\1\24\10\25\7\11"+
    "\1\266\10\25\1\275\1\74\1\140\1\37\2\11\1\112\1\116\4\25\3\11\1\26\20\25\6"+
    "\11\1\216\1\25\2\11\1\216\1\25\2\11\1\45\21\25\11\11\1\126\66\25\1\221\6\11"+
    "\1\0\1\75\3\25\1\122\1\0\2\25\1\221\5\11\1\0\1\276\2\25\3\11\1\126\1\0\1\157"+
    "\1\221\3\11\1\156\1\0\1\142\1\0\10\25\1\221\5\11\1\51\1\0\1\277\1\25\1\0\1"+
    "\157\24\25\5\11\1\51\1\0\1\25\1\0\1\157\46\25\55\11\1\22\22\25\14\11\1\45"+
    "\63\25\5\11\1\22\72\25\7\11\1\126\130\25\10\11\1\26\1\25\1\100\4\0\1\75\1"+
    "\25\1\60\1\221\1\11\14\25\1\24\153\25\1\300\1\301\2\0\1\302\1\2\3\25\1\303"+
    "\22\25\1\304\67\25\12\11\1\30\10\11\1\30\1\305\1\306\1\11\1\307\1\140\7\11"+
    "\1\34\1\310\2\30\3\11\1\311\1\166\1\36\1\73\51\11\1\216\3\11\1\73\2\11\1\204"+
    "\3\11\1\204\2\11\1\30\3\11\1\30\2\11\1\22\3\11\1\22\3\11\1\73\3\11\1\73\2"+
    "\11\1\204\1\312\6\0\1\140\3\11\1\160\1\37\1\204\1\313\1\236\1\314\1\160\1"+
    "\226\1\160\2\204\1\121\1\11\1\33\1\11\1\112\1\315\1\33\1\11\1\112\50\25\32"+
    "\11\1\22\5\25\106\11\1\26\1\25\33\11\1\216\74\25\1\120\3\25\14\0\20\25\36"+
    "\0\2\25");

  /* The ZZ_CMAP_A table has 1648 entries */
  static final char ZZ_CMAP_A[] = zzUnpackCMap(
    "\11\7\1\2\1\1\1\41\1\2\1\1\6\7\4\0\1\2\1\25\1\11\1\10\1\6\1\34\1\0\1\12\1"+
    "\33\1\37\1\3\1\14\1\30\1\5\1\17\1\4\12\15\1\26\1\40\1\23\1\21\1\24\1\0\1\27"+
    "\3\6\1\20\1\16\1\20\7\6\1\13\14\6\1\31\1\0\1\35\1\0\1\6\1\0\3\6\1\20\1\16"+
    "\1\20\4\6\1\32\1\22\1\36\1\0\6\7\1\42\2\7\2\0\4\6\4\0\1\6\2\0\1\7\7\0\1\6"+
    "\4\0\1\6\5\0\7\6\1\0\2\6\4\0\4\6\16\0\5\6\7\0\1\6\1\0\1\6\1\0\5\6\1\0\2\6"+
    "\6\0\1\6\1\0\3\6\1\0\1\6\1\0\4\6\1\0\13\6\1\0\3\6\1\0\5\7\2\0\6\6\1\0\7\6"+
    "\1\0\1\6\15\0\1\6\1\0\15\7\1\0\1\7\1\0\2\7\1\0\2\7\1\0\1\7\3\6\5\0\5\7\6\0"+
    "\1\6\4\0\3\7\5\0\3\6\7\7\4\0\2\6\1\7\13\6\1\0\1\6\7\7\2\6\2\7\1\0\4\7\2\6"+
    "\2\7\3\6\2\0\1\6\7\0\1\7\1\6\1\7\6\6\3\7\2\0\11\6\3\7\1\6\6\0\2\7\6\6\4\7"+
    "\2\6\2\0\2\7\1\6\11\7\1\6\3\7\1\6\5\7\2\0\1\6\3\7\4\0\1\6\1\0\6\6\4\0\13\7"+
    "\1\0\4\7\6\6\3\7\1\6\2\7\1\6\7\7\2\6\2\7\2\0\2\7\1\0\3\7\1\0\10\6\2\0\2\6"+
    "\2\0\6\6\1\0\1\6\3\0\4\6\2\0\1\7\1\6\7\7\2\0\2\7\2\0\3\7\1\6\5\0\2\6\1\0\5"+
    "\6\4\0\3\6\4\0\2\6\1\0\2\6\1\0\2\6\1\0\2\6\2\0\1\7\1\0\5\7\4\0\2\7\2\0\3\7"+
    "\3\0\1\7\7\0\4\6\1\0\1\6\7\0\4\7\3\6\1\7\2\0\1\6\1\0\2\6\1\0\3\6\2\7\1\0\3"+
    "\7\2\0\1\6\11\0\1\7\1\6\1\0\6\6\3\0\3\6\1\0\4\6\3\0\2\6\1\0\1\6\1\0\2\6\3"+
    "\0\2\6\3\0\2\6\4\0\5\7\3\0\3\7\1\0\4\7\2\0\1\6\6\0\1\7\4\6\1\0\5\6\3\0\1\6"+
    "\7\7\1\0\2\7\5\0\2\7\3\0\2\7\1\0\3\6\1\0\2\6\5\0\3\6\2\0\1\6\3\7\1\0\4\7\1"+
    "\6\1\0\4\6\1\0\1\6\4\0\1\7\4\0\6\7\1\0\1\7\3\0\2\7\4\0\1\6\1\7\2\6\7\7\4\0"+
    "\10\6\3\7\7\0\2\6\1\0\1\6\2\0\2\6\1\0\1\6\2\0\1\6\6\0\4\6\1\0\3\6\1\0\1\6"+
    "\1\0\1\6\2\0\2\6\1\0\3\6\2\7\1\0\2\7\1\6\2\0\5\6\1\0\1\6\1\0\6\7\2\0\2\7\2"+
    "\0\4\6\5\0\1\7\1\0\1\7\1\0\1\7\4\0\2\7\5\6\3\7\6\0\1\7\1\0\7\7\1\6\2\7\4\6"+
    "\3\7\1\6\3\7\2\6\7\7\3\6\4\7\5\6\14\7\1\6\1\7\3\6\1\0\7\6\2\0\3\7\2\6\3\7"+
    "\3\0\2\6\2\7\4\0\1\6\1\0\2\7\4\0\4\6\10\7\3\0\1\6\3\0\2\6\1\7\5\0\3\7\2\0"+
    "\1\6\1\7\1\6\5\0\6\6\2\0\5\7\3\6\3\0\10\7\5\6\2\7\3\0\3\6\3\7\1\0\5\7\4\6"+
    "\1\7\4\6\3\7\2\6\2\0\1\6\1\0\1\6\1\0\1\6\1\0\1\6\2\0\3\6\1\0\6\6\2\0\2\6\2"+
    "\41\5\7\5\0\1\6\5\0\6\7\1\0\1\7\3\0\4\7\11\0\1\6\4\0\1\6\1\0\5\6\2\0\1\6\1"+
    "\0\4\6\1\0\3\6\2\0\4\6\5\0\5\6\4\0\1\6\4\0\4\6\3\7\2\6\5\0\2\7\2\0\3\6\6\7"+
    "\1\0\2\6\2\0\4\6\1\0\2\6\1\7\3\6\1\7\4\6\1\7\10\6\2\7\4\0\1\6\1\7\4\0\1\7"+
    "\5\6\2\7\3\0\3\6\4\0\3\6\2\7\2\0\6\6\1\0\3\7\1\0\2\7\5\0\5\6\5\0\1\6\1\7\3"+
    "\6\1\0\2\6\1\0\7\6\2\0\1\7\6\0\2\6\2\0\3\6\3\0\2\6\3\0\2\6\2\0\3\7\4\0\3\6"+
    "\1\0\2\6\1\0\1\6\5\0\1\7\2\0\1\6\3\0\1\6\2\0\2\6\3\7\1\0\2\7\1\0\3\7\2\0\1"+
    "\7\2\0\1\7\4\6\10\0\5\7\3\0\6\7\2\0\3\7\2\0\4\7\4\0\3\7\5\0\1\6\2\0\2\6\2"+
    "\0\4\6\1\0\4\6\1\0\1\6\1\0\6\6\2\0\5\6\1\0\4\6\1\0\4\6\2\0\2\7\1\0\1\6\1\0"+
    "\1\6\5\0\1\6\1\0\1\6\1\0\3\6\1\0\3\6\1\0\3\6");

  /** 
   * Translates DFA states to action switch labels.
   */
  private static final int [] ZZ_ACTION = zzUnpackAction();

  private static final String ZZ_ACTION_PACKED_0 =
    "\2\0\1\1\1\2\1\3\1\4\1\5\1\1\1\6"+
    "\1\7\1\10\1\1\1\11\1\12\1\13\1\14\1\15"+
    "\1\16\1\17\1\20\1\21\1\22\1\23\1\24\1\25"+
    "\1\26\1\27\1\30\1\31\1\32\1\33\1\34\1\35"+
    "\1\7\1\10\2\0\1\36\1\37\1\0\1\40\1\41"+
    "\1\0\1\42\1\0\1\43\1\44\1\0\1\45\1\0"+
    "\1\46\1\0\1\47\1\50\1\34\1\0\1\12\4\0"+
    "\2\36\1\34";

  private static int [] zzUnpackAction() {
    int [] result = new int[64];
    int offset = 0;
    offset = zzUnpackAction(ZZ_ACTION_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAction(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /** 
   * Translates a state to a row index in the transition table
   */
  private static final int [] ZZ_ROWMAP = zzUnpackRowMap();

  private static final String ZZ_ROWMAP_PACKED_0 =
    "\0\0\0\43\0\106\0\151\0\106\0\214\0\257\0\322"+
    "\0\106\0\365\0\u0118\0\u013b\0\106\0\u015e\0\u0181\0\u01a4"+
    "\0\u01c7\0\u01ea\0\u020d\0\u0230\0\u0253\0\106\0\106\0\106"+
    "\0\106\0\106\0\106\0\106\0\106\0\106\0\106\0\u0276"+
    "\0\u0299\0\106\0\u02bc\0\u02df\0\u0302\0\u0325\0\106\0\u01a4"+
    "\0\106\0\106\0\u01c7\0\106\0\u01ea\0\106\0\106\0\u020d"+
    "\0\106\0\u0230\0\106\0\u0348\0\106\0\106\0\u036b\0\u038e"+
    "\0\u03b1\0\u03d4\0\u03f7\0\u041a\0\u043d\0\u043d\0\u03f7\0\106";

  private static int [] zzUnpackRowMap() {
    int [] result = new int[64];
    int offset = 0;
    offset = zzUnpackRowMap(ZZ_ROWMAP_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackRowMap(String packed, int offset, int [] result) {
    int i = 0;  /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int high = packed.charAt(i++) << 16;
      result[j++] = high | packed.charAt(i++);
    }
    return j;
  }

  /** 
   * The transition table of the DFA
   */
  private static final int [] ZZ_TRANS = zzUnpackTrans();

  private static final String ZZ_TRANS_PACKED_0 =
    "\1\3\2\4\1\5\1\6\1\7\1\10\1\3\1\11"+
    "\1\12\1\13\1\14\1\15\1\16\1\10\1\17\1\10"+
    "\1\20\1\21\1\22\1\23\1\24\1\25\1\26\1\27"+
    "\1\30\1\31\1\32\1\33\1\34\1\35\1\36\1\37"+
    "\3\0\2\4\1\5\1\6\1\7\1\10\1\0\1\11"+
    "\1\12\1\13\1\14\1\15\1\16\1\10\1\17\1\10"+
    "\1\20\1\21\1\22\1\23\1\24\1\25\1\26\1\27"+
    "\1\30\1\31\1\32\1\33\1\34\1\35\1\36\1\37"+
    "\46\0\2\4\43\0\1\40\44\0\1\41\43\0\3\10"+
    "\2\0\1\10\1\0\2\10\1\0\1\10\21\0\1\10"+
    "\11\12\1\42\31\12\12\13\1\43\30\13\6\0\3\10"+
    "\1\0\1\13\1\10\1\0\2\10\1\0\1\10\21\0"+
    "\1\10\15\0\1\16\1\44\1\45\40\0\1\46\1\0"+
    "\1\47\24\0\2\50\16\0\1\51\2\0\1\52\17\0"+
    "\2\53\17\0\1\54\21\0\2\55\16\0\1\56\2\0"+
    "\1\57\17\0\2\60\16\0\1\61\22\0\2\62\16\0"+
    "\1\63\22\0\2\64\16\0\1\65\4\0\1\66\14\0"+
    "\43\67\1\41\1\0\41\41\12\0\1\13\35\0\1\70"+
    "\6\0\1\70\1\71\42\0\1\46\42\0\1\46\1\72"+
    "\1\0\1\73\23\0\2\64\16\0\1\65\21\0\3\67"+
    "\1\74\37\67\15\0\1\71\42\0\1\71\1\0\1\45"+
    "\30\0\1\75\6\0\1\75\1\76\26\0\2\77\40\0"+
    "\3\67\1\74\1\100\36\67\15\0\1\76\25\0";

  private static int [] zzUnpackTrans() {
    int [] result = new int[1120];
    int offset = 0;
    offset = zzUnpackTrans(ZZ_TRANS_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackTrans(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      value--;
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /* error codes */
  private static final int ZZ_UNKNOWN_ERROR = 0;
  private static final int ZZ_NO_MATCH = 1;
  private static final int ZZ_PUSHBACK_2BIG = 2;

  /* error messages for the codes above */
  private static final String[] ZZ_ERROR_MSG = {
    "Unknown internal scanner error",
    "Error: could not match input",
    "Error: pushback value was too large"
  };

  /**
   * ZZ_ATTRIBUTE[aState] contains the attributes of state <code>aState</code>
   */
  private static final int [] ZZ_ATTRIBUTE = zzUnpackAttribute();

  private static final String ZZ_ATTRIBUTE_PACKED_0 =
    "\2\0\1\11\1\1\1\11\3\1\1\11\3\1\1\11"+
    "\10\1\12\11\2\1\1\11\1\1\2\0\1\1\1\11"+
    "\1\0\2\11\1\0\1\11\1\0\2\11\1\0\1\11"+
    "\1\0\1\11\1\0\2\11\1\1\1\0\1\1\4\0"+
    "\2\1\1\11";

  private static int [] zzUnpackAttribute() {
    int [] result = new int[64];
    int offset = 0;
    offset = zzUnpackAttribute(ZZ_ATTRIBUTE_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAttribute(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }

  /** the input device */
  private java.io.Reader zzReader;

  /** the current state of the DFA */
  private int zzState;

  /** the current lexical state */
  private int zzLexicalState = YYINITIAL;

  /** this buffer contains the current text to be matched and is
      the source of the yytext() string */
  private CharSequence zzBuffer = "";

  /** the textposition at the last accepting state */
  private int zzMarkedPos;

  /** the current text position in the buffer */
  private int zzCurrentPos;

  /** startRead marks the beginning of the yytext() string in the buffer */
  private int zzStartRead;

  /** endRead marks the last character in the buffer, that has been read
      from input */
  private int zzEndRead;

  /**
   * zzAtBOL == true <=> the scanner is currently at the beginning of a line
   */
  private boolean zzAtBOL = true;

  /** zzAtEOF == true <=> the scanner is at the EOF */
  private boolean zzAtEOF;

  /** denotes if the user-EOF-code has already been executed */
  private boolean zzEOFDone;

  /* user code: */
    private TokenTypeBundle tt;
    public MysqlPSQLParserFlexLexer(TokenTypeBundle tt) {
        this.tt = tt;
    }


  /**
   * Creates a new scanner
   *
   * @param   in  the java.io.Reader to read input from.
   */
  public MysqlPSQLParserFlexLexer(java.io.Reader in) {
    this.zzReader = in;
  }


  /** 
   * Unpacks the compressed character translation table.
   *
   * @param packed   the packed character translation table
   * @return         the unpacked character translation table
   */
  private static char [] zzUnpackCMap(String packed) {
    int size = 0;
    for (int i = 0, length = packed.length(); i < length; i += 2) {
      size += packed.charAt(i);
    }
    char[] map = new char[size];
    int i = 0;  /* index in packed string  */
    int j = 0;  /* index in unpacked array */
    while (i < packed.length()) {
      int  count = packed.charAt(i++);
      char value = packed.charAt(i++);
      do map[j++] = value; while (--count > 0);
    }
    return map;
  }

  public final int getTokenStart() {
    return zzStartRead;
  }

  public final int getTokenEnd() {
    return getTokenStart() + yylength();
  }

  public void reset(CharSequence buffer, int start, int end, int initialState) {
    zzBuffer = buffer;
    zzCurrentPos = zzMarkedPos = zzStartRead = start;
    zzAtEOF  = false;
    zzAtBOL = true;
    zzEndRead = end;
    yybegin(initialState);
  }

  /**
   * Refills the input buffer.
   *
   * @return      {@code false}, iff there was new input.
   *
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  private boolean zzRefill() throws java.io.IOException {
    return true;
  }


  /**
   * Returns the current lexical state.
   */
  public final int yystate() {
    return zzLexicalState;
  }


  /**
   * Enters a new lexical state
   *
   * @param newState the new lexical state
   */
  public final void yybegin(int newState) {
    zzLexicalState = newState;
  }


  /**
   * Returns the text matched by the current regular expression.
   */
  public final CharSequence yytext() {
    return zzBuffer.subSequence(zzStartRead, zzMarkedPos);
  }


  /**
   * Returns the character at position {@code pos} from the
   * matched text.
   *
   * It is equivalent to yytext().charAt(pos), but faster
   *
   * @param pos the position of the character to fetch.
   *            A value from 0 to yylength()-1.
   *
   * @return the character at position pos
   */
  public final char yycharat(int pos) {
    return zzBuffer.charAt(zzStartRead+pos);
  }


  /**
   * Returns the length of the matched text region.
   */
  public final int yylength() {
    return zzMarkedPos-zzStartRead;
  }


  /**
   * Reports an error that occurred while scanning.
   *
   * In a wellformed scanner (no or only correct usage of
   * yypushback(int) and a match-all fallback rule) this method
   * will only be called with things that "Can't Possibly Happen".
   * If this method is called, something is seriously wrong
   * (e.g. a JFlex bug producing a faulty scanner etc.).
   *
   * Usual syntax/scanner level error handling should be done
   * in error fallback rules.
   *
   * @param   errorCode  the code of the errormessage to display
   */
  private void zzScanError(int errorCode) {
    String message;
    try {
      message = ZZ_ERROR_MSG[errorCode];
    }
    catch (ArrayIndexOutOfBoundsException e) {
      message = ZZ_ERROR_MSG[ZZ_UNKNOWN_ERROR];
    }

    throw new Error(message);
  }


  /**
   * Pushes the specified amount of characters back into the input stream.
   *
   * They will be read again by then next call of the scanning method
   *
   * @param number  the number of characters to be read again.
   *                This number must not be greater than yylength()!
   */
  public void yypushback(int number)  {
    if ( number > yylength() )
      zzScanError(ZZ_PUSHBACK_2BIG);

    zzMarkedPos -= number;
  }


  /**
   * Contains user EOF-code, which will be executed exactly once,
   * when the end of file is reached
   */
  private void zzDoEOF() {
    if (!zzEOFDone) {
      zzEOFDone = true;
    
    }
  }


  /**
   * Resumes scanning until the next regular expression is matched,
   * the end of input is encountered or an I/O-Error occurs.
   *
   * @return      the next token
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  public IElementType advance() throws java.io.IOException {
    int zzInput;
    int zzAction;

    // cached fields:
    int zzCurrentPosL;
    int zzMarkedPosL;
    int zzEndReadL = zzEndRead;
    CharSequence zzBufferL = zzBuffer;

    int [] zzTransL = ZZ_TRANS;
    int [] zzRowMapL = ZZ_ROWMAP;
    int [] zzAttrL = ZZ_ATTRIBUTE;

    while (true) {
      zzMarkedPosL = zzMarkedPos;

      zzAction = -1;

      zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;

      zzState = ZZ_LEXSTATE[zzLexicalState];

      // set up zzAction for empty match case:
      int zzAttributes = zzAttrL[zzState];
      if ( (zzAttributes & 1) == 1 ) {
        zzAction = zzState;
      }


      zzForAction: {
        while (true) {

          if (zzCurrentPosL < zzEndReadL) {
            zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL/*, zzEndReadL*/);
            zzCurrentPosL += Character.charCount(zzInput);
          }
          else if (zzAtEOF) {
            zzInput = YYEOF;
            break zzForAction;
          }
          else {
            // store back cached positions
            zzCurrentPos  = zzCurrentPosL;
            zzMarkedPos   = zzMarkedPosL;
            boolean eof = zzRefill();
            // get translated positions and possibly new buffer
            zzCurrentPosL  = zzCurrentPos;
            zzMarkedPosL   = zzMarkedPos;
            zzBufferL      = zzBuffer;
            zzEndReadL     = zzEndRead;
            if (eof) {
              zzInput = YYEOF;
              break zzForAction;
            }
            else {
              zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL/*, zzEndReadL*/);
              zzCurrentPosL += Character.charCount(zzInput);
            }
          }
          int zzNext = zzTransL[ zzRowMapL[zzState] + ZZ_CMAP(zzInput) ];
          if (zzNext == -1) break zzForAction;
          zzState = zzNext;

          zzAttributes = zzAttrL[zzState];
          if ( (zzAttributes & 1) == 1 ) {
            zzAction = zzState;
            zzMarkedPosL = zzCurrentPosL;
            if ( (zzAttributes & 8) == 8 ) break zzForAction;
          }

        }
      }

      // store back cached position
      zzMarkedPos = zzMarkedPosL;

      if (zzInput == YYEOF && zzStartRead == zzCurrentPos) {
        zzAtEOF = true;
        zzDoEOF();
        return null;
      }
      else {
        switch (zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {
          case 1: 
            { yybegin(YYINITIAL); return tt.getSharedTokenTypes().getIdentifier();
            }
          case 41: break;
          case 2: 
            { return tt.getSharedTokenTypes().getWhiteSpace();
            }
          case 42: break;
          case 3: 
            { return tt.getCharacterTokenType(20);
            }
          case 43: break;
          case 4: 
            { return tt.getCharacterTokenType(19);
            }
          case 44: break;
          case 5: 
            { return tt.getCharacterTokenType(12);
            }
          case 45: break;
          case 6: 
            { return tt.getCharacterTokenType(7);
            }
          case 46: break;
          case 7: 
            { yybegin(YYINITIAL); return tt.getSharedTokenTypes().getQuotedIdentifier();
            }
          case 47: break;
          case 8: 
            { return tt.getSharedTokenTypes().getString();
            }
          case 48: break;
          case 9: 
            { return tt.getCharacterTokenType(14);
            }
          case 49: break;
          case 10: 
            { return tt.getSharedTokenTypes().getInteger();
            }
          case 50: break;
          case 11: 
            { return tt.getCharacterTokenType(3);
            }
          case 51: break;
          case 12: 
            { return tt.getCharacterTokenType(4);
            }
          case 52: break;
          case 13: 
            { return tt.getCharacterTokenType(21);
            }
          case 53: break;
          case 14: 
            { return tt.getCharacterTokenType(11);
            }
          case 54: break;
          case 15: 
            { return tt.getCharacterTokenType(6);
            }
          case 55: break;
          case 16: 
            { return tt.getCharacterTokenType(5);
            }
          case 56: break;
          case 17: 
            { return tt.getCharacterTokenType(1);
            }
          case 57: break;
          case 18: 
            { return tt.getCharacterTokenType(0);
            }
          case 58: break;
          case 19: 
            { return tt.getCharacterTokenType(2);
            }
          case 59: break;
          case 20: 
            { return tt.getCharacterTokenType(8);
            }
          case 60: break;
          case 21: 
            { return tt.getCharacterTokenType(9);
            }
          case 61: break;
          case 22: 
            { return tt.getCharacterTokenType(10);
            }
          case 62: break;
          case 23: 
            { return tt.getCharacterTokenType(13);
            }
          case 63: break;
          case 24: 
            { return tt.getCharacterTokenType(15);
            }
          case 64: break;
          case 25: 
            { return tt.getCharacterTokenType(16);
            }
          case 65: break;
          case 26: 
            { return tt.getCharacterTokenType(17);
            }
          case 66: break;
          case 27: 
            { return tt.getCharacterTokenType(18);
            }
          case 67: break;
          case 28: 
            { return tt.getSharedTokenTypes().getBlockComment();
            }
          case 68: break;
          case 29: 
            { return tt.getSharedTokenTypes().getLineComment();
            }
          case 69: break;
          case 30: 
            { return tt.getSharedTokenTypes().getNumber();
            }
          case 70: break;
          case 31: 
            { return tt.getOperatorTokenType(8);
            }
          case 71: break;
          case 32: 
            { return tt.getOperatorTokenType(0);
            }
          case 72: break;
          case 33: 
            { return tt.getOperatorTokenType(7);
            }
          case 73: break;
          case 34: 
            { return tt.getOperatorTokenType(1);
            }
          case 74: break;
          case 35: 
            { return tt.getOperatorTokenType(2);
            }
          case 75: break;
          case 36: 
            { return tt.getOperatorTokenType(4);
            }
          case 76: break;
          case 37: 
            { return tt.getOperatorTokenType(3);
            }
          case 77: break;
          case 38: 
            { return tt.getOperatorTokenType(5);
            }
          case 78: break;
          case 39: 
            { return tt.getOperatorTokenType(6);
            }
          case 79: break;
          case 40: 
            { return tt.getOperatorTokenType(9);
            }
          case 80: break;
          default:
            zzScanError(ZZ_NO_MATCH);
          }
      }
    }
  }


}
