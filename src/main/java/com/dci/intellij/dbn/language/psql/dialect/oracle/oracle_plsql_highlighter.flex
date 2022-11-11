package com.dci.intellij.dbn.language.psql.dialect.oracle;

import com.dci.intellij.dbn.language.common.SharedTokenTypeBundle;
import com.dci.intellij.dbn.language.common.TokenTypeBundle;
import com.intellij.psi.tree.IElementType;

%%

%class OraclePLSQLHighlighterFlexLexer
%implements FlexLexer
%final
%unicode
%ignorecase
%function advance
%type IElementType
%eof{ return;
%eof}


%{
    private TokenTypeBundle tt;
    private SharedTokenTypeBundle stt;
    public OraclePLSQLHighlighterFlexLexer(TokenTypeBundle tt) {
        this.tt = tt;
        this.stt = tt.getSharedTokenTypes();
    }
%}

eol = \r|\n|\r\n
wsc = [ \t\f]
wso = ({eol}|{wsc})*
ws  = ({eol}|{wsc})+
WHITE_SPACE = {ws}


BLOCK_COMMENT="/"{wsc}*"*"(~"*/")?
LINE_COMMENT = ("--"[^\r\n]*{eol}?) | ("rem"({wsc}+[^\r\n]*{eol}?|{eol}?))

IDENTIFIER = [:jletter:] ([:jletterdigit:]|"#")*
QUOTED_IDENTIFIER = "\""[^\"]*"\""?

string_simple_quoted      = "'"([^']|"''")*"'"?
string_alternative_quoted =
    "q'["(~"]'")? |
    "q'("(~")'")? |
    "q'{"(~"}'")? |
    "q'<"(~">'")? |
    "q'!"(~"!'")? |
    "q'?"(~"?'")? |
    "q'|"(~"|'")? |
    "q'/"(~"/'")? |
    "q'\\"(~"\\'")? |
    "q'+"(~"+'")? |
    "q'-"(~"-'")? |
    "q'*"(~"*'")? |
    "q'="(~"='")? |
    "q'~"(~"~'")? |
    "q'^"(~"^'")? |
    "q'#"(~"#'")? |
    "q'%"(~"%'")? |
    "q'$"(~"$'")? |
    "q'&"(~"&'")? |
    "q':"(~":'")? |
    "q';"(~";'")? |
    "q'."(~".'")? |
    "q',"(~",'")?
STRING = "n"?({string_alternative_quoted}|{string_simple_quoted})

sign = "+"|"-"
digit = [0-9]
INTEGER = {digit}+("e"{sign}?{digit}+)?
NUMBER = {INTEGER}?"."{digit}+(("e"{sign}?{digit}+)|(("f"|"d"){ws}))?

OPERATOR = ("!"|"^"|"<"|">"){wso}"="|"<"{wso}">"|"<"|">"|"="


KEYWORD     = "a set"|"absent"|"accessible"|"after"|"agent"|"aggregate"|"all"|"alter"|"analyze"|"and"|"any"|"apply"|"array"|"as"|"asc"|"associate"|"at"|"audit"|"authid"|"automatic"|"autonomous_transaction"|"before"|"begin"|"between"|"block"|"body"|"both"|"bulk"|"bulk_exceptions"|"bulk_rowcount"|"by"|"c"|"call"|"canonical"|"case"|"char_base"|"char_cs"|"charsetform"|"charsetid"|"check"|"chisq_df"|"chisq_obs"|"chisq_sig"|"clone"|"close"|"cluster"|"coalesce"|"coefficient"|"cohens_k"|"collation"|"collect"|"columns"|"comment"|"commit"|"committed"|"compatibility"|"compound"|"compress"|"conditional"|"connect"|"constant"|"constraint"|"constructor"|"cont_coefficient"|"container"|"content"|"context"|"conversion"|"count"|"cramers_v"|"create"|"cross"|"crossedition"|"cube"|"current"|"current_user"|"currval"|"cursor"|"database"|"day"|"db_role_change"|"ddl"|"declare"|"decrement"|"default"|"defaults"|"definer"|"delete"|"deleting"|"dense_rank"|"deprecate"|"desc"|"deterministic"|"df"|"df_between"|"df_den"|"df_num"|"df_within"|"dimension"|"disable"|"disassociate"|"distinct"|"do"|"document"|"drop"|"dump"|"duration"|"each"|"editionable"|"else"|"elsif"|"empty"|"enable"|"encoding"|"end"|"entityescaping"|"equals_path"|"error"|"error_code"|"error_index"|"errors"|"escape"|"evalname"|"exact_prob"|"except"|"exception"|"exception_init"|"exceptions"|"exclude"|"exclusive"|"execute"|"exists"|"exit"|"extend"|"extends"|"external"|"f_ratio"|"fetch"|"final"|"first"|"following"|"follows"|"for"|"forall"|"force"|"forward"|"found"|"from"|"format"|"full"|"function"|"goto"|"grant"|"group"|"hash"|"having"|"heap"|"hide"|"hour"|"if"|"ignore"|"immediate"|"in"|"include"|"increment"|"indent"|"index"|"indicator"|"indices"|"infinite"|"inline"|"inner"|"insert"|"inserting"|"instantiable"|"instead"|"interface"|"intersect"|"interval"|"into"|"is"|"isolation"|"isopen"|"iterate"|"java"|"join"|"json"|"keep"|"key"|"keys"|"language"|"last"|"leading"|"left"|"level"|"library"|"like"|"like2"|"like4"|"likec"|"limit"|"limited"|"local"|"lock"|"locked"|"log"|"logoff"|"logon"|"loop"|"main"|"map"|"matched"|"maxlen"|"maxvalue"|"mean_squares_between"|"mean_squares_within"|"measures"|"member"|"merge"|"metadata"|"minus"|"minute"|"minvalue"|"mismatch"|"mlslabel"|"mode"|"model"|"month"|"multiset"|"name"|"nan"|"natural"|"naturaln"|"nav"|"nchar_cs"|"nested"|"new"|"next"|"nextval"|"no"|"noaudit"|"nocopy"|"nocycle"|"none"|"noentityescaping"|"noneditionable"|"noschemacheck"|"not"|"notfound"|"nowait"|"null"|"nulls"|"number_base"|"object"|"ocirowid"|"of"|"offset"|"oid"|"old"|"on"|"one_sided_prob_or_less"|"one_sided_prob_or_more"|"one_sided_sig"|"only"|"opaque"|"open"|"operator"|"option"|"or"|"order"|"ordinality"|"organization"|"others"|"out"|"outer"|"over"|"overflow"|"overlaps"|"overriding"|"package"|"parallel_enable"|"parameters"|"parent"|"partition"|"passing"|"path"|"pctfree"|"percent"|"phi_coefficient"|"pipe"|"pipelined"|"pivot"|"pluggable"|"positive"|"positiven"|"power"|"pragma"|"preceding"|"precedes"|"present"|"pretty"|"prior"|"private"|"procedure"|"public"|"raise"|"range"|"read"|"record"|"ref"|"reference"|"referencing"|"regexp_like"|"reject"|"release"|"relies_on"|"remainder"|"rename"|"replace"|"restrict_references"|"result"|"result_cache"|"return"|"returning"|"reverse"|"revoke"|"right"|"rnds"|"rnps"|"rollback"|"rollup"|"row"|"rowcount"|"rownum"|"rows"|"rowtype"|"rules"|"sample"|"save"|"savepoint"|"schema"|"schemacheck"|"scn"|"second"|"seed"|"segment"|"select"|"self"|"separate"|"sequential"|"serializable"|"serially_reusable"|"servererror"|"set"|"sets"|"share"|"sharing"|"show"|"shutdown"|"siblings"|"sig"|"single"|"size"|"skip"|"some"|"space"|"sql"|"sqlcode"|"sqlerrm"|"standalone"|"start"|"startup"|"statement"|"static"|"statistic"|"statistics"|"strict"|"struct"|"submultiset"|"subpartition"|"subtype"|"successful"|"sum_squares_between"|"sum_squares_within"|"suspend"|"synonym"|"table"|"tdo"|"then"|"ties"|"time"|"timezone_abbr"|"timezone_hour"|"timezone_minute"|"timezone_region"|"to"|"trailing"|"transaction"|"trigger"|"truncate"|"trust"|"two_sided_prob"|"two_sided_sig"|"type"|"u_statistic"|"unbounded"|"unconditional"|"under"|"under_path"|"union"|"unique"|"unlimited"|"unpivot"|"unplug"|"until"|"update"|"updated"|"updating"|"upsert"|"use"|"user"|"using"|"validate"|"value"|"values"|"variable"|"varray"|"varying"|"version"|"versions"|"view"|"wait"|"wellformed"|"when"|"whenever"|"where"|"while"|"with"|"within"|"without"|"wnds"|"wnps"|"work"|"write"|"wrapped"|"wrapper"|"xml"|"xmlnamespaces"|"year"|"yes"|"zone"|"false"|"true"
FUNCTION    = "abs"|"acos"|"add_months"|"appendchildxml"|"ascii"|"asciistr"|"asin"|"atan"|"atan2"|"avg"|"bfilename"|"bin_to_num"|"bitand"|"cardinality"|"cast"|"ceil"|"chartorowid"|"chr"|"compose"|"concat"|"convert"|"corr"|"corr_k"|"corr_s"|"cos"|"cosh"|"covar_pop"|"covar_samp"|"cume_dist"|"current_date"|"current_timestamp"|"cv"|"dbtimezone"|"dbtmezone"|"decode"|"decompose"|"deletexml"|"depth"|"deref"|"empty_blob"|"empty_clob"|"existsnode"|"exp"|"extract"|"extractvalue"|"first_value"|"floor"|"from_tz"|"greatest"|"group_id"|"grouping"|"grouping_id"|"hextoraw"|"initcap"|"insertchildxml"|"insertchildxmlafter"|"insertchildxmlbefore"|"insertxmlafter"|"insertxmlbefore"|"instr"|"instr2"|"instr4"|"instrb"|"instrc"|"iteration_number"|"json_array"|"json_arrayagg"|"json_dataguide"|"json_object"|"json_objectagg"|"json_query"|"json_table"|"json_value"|"lag"|"last_day"|"last_value"|"lateral"|"lead"|"least"|"length"|"length2"|"length4"|"lengthb"|"lengthc"|"listagg"|"ln"|"lnnvl"|"localtimestamp"|"lower"|"lpad"|"ltrim"|"make_ref"|"max"|"median"|"min"|"mod"|"months_between"|"nanvl"|"nchr"|"new_time"|"next_day"|"nls_charset_decl_len"|"nls_charset_id"|"nls_charset_name"|"nls_initcap"|"nls_lower"|"nls_upper"|"nlssort"|"ntile"|"nullif"|"numtodsinterval"|"numtoyminterval"|"nvl"|"nvl2"|"ora_hash"|"percent_rank"|"percentile_cont"|"percentile_disc"|"powermultiset"|"powermultiset_by_cardinality"|"presentnnv"|"presentv"|"previous"|"rank"|"ratio_to_report"|"rawtohex"|"rawtonhex"|"reftohex"|"regexp_instr"|"regexp_replace"|"regexp_substr"|"regr_avgx"|"regr_avgy"|"regr_count"|"regr_intercept"|"regr_r2"|"regr_slope"|"regr_sxx"|"regr_sxy"|"regr_syy"|"round"|"row_number"|"rowidtochar"|"rowidtonchar"|"rpad"|"rtrim"|"scn_to_timestamp"|"sessiontimezone"|"sign"|"sin"|"sinh"|"soundex"|"sqrt"|"stats_binomial_test"|"stats_crosstab"|"stats_f_test"|"stats_ks_test"|"stats_mode"|"stats_mw_test"|"stats_one_way_anova"|"stats_t_test_indep"|"stats_t_test_indepu"|"stats_t_test_one"|"stats_t_test_paired"|"stats_wsr_test"|"stddev"|"stddev_pop"|"stddev_samp"|"substr"|"substr2"|"substr4"|"substrb"|"substrc"|"sum"|"sys_connect_by_path"|"sys_context"|"sys_dburigen"|"sys_extract_utc"|"sys_guid"|"sys_typeid"|"sys_xmlagg"|"sys_xmlgen"|"sysdate"|"systimestamp"|"tan"|"tanh"|"timestamp_to_scn"|"to_binary_double"|"to_binary_float"|"to_char"|"to_clob"|"to_date"|"to_dsinterval"|"to_lob"|"to_multi_byte"|"to_nchar"|"to_nclob"|"to_number"|"to_single_byte"|"to_timestamp"|"to_timestamp_tz"|"to_yminterval"|"translate"|"treat"|"trim"|"trunc"|"tz_offset"|"uid"|"unistr"|"updatexml"|"upper"|"userenv"|"validate_conversion"|"var_pop"|"var_samp"|"variance"|"vsize"|"width_bucket"|"xmlagg"|"xmlattributes"|"xmlcast"|"xmlcdata"|"xmlcolattval"|"xmlcomment"|"xmlconcat"|"xmldiff"|"xmlelement"|"xmlforest"|"xmlisvalid"|"xmlparse"|"xmlpatch"|"xmlpi"|"xmlquery"|"xmlroot"|"xmlsequence"|"xmlserialize"|"xmltable"|"xmltransform"
PARAMETER   = "composite_limit"|"connect_time"|"cpu_per_call"|"cpu_per_session"|"create_stored_outlines"|"current_schema"|"cursor_sharing"|"db_block_checking"|"db_create_file_dest"|"db_create_online_log_dest_"{digit}+|"db_file_multiblock_read_count"|"db_file_name_convert"|"ddl_wait_for_locks"|"error_on_overlap_time"|"failed_login_attempts"|"filesystemio_options"|"flagger"|"global_names"|"hash_area_size"|"idle_time"|"instance"|"isolation_level"|"log_archive_dest_"{digit}+|"log_archive_dest_state_"{digit}+|"log_archive_min_succeed_dest"|"logical_reads_per_call"|"logical_reads_per_session"|"max_dump_file_size"|"nls_calendar"|"nls_comp"|"nls_currency"|"nls_date_format"|"nls_date_language"|"nls_dual_currency"|"nls_iso_currency"|"nls_language"|"nls_length_semantics"|"nls_nchar_conv_excp"|"nls_numeric_characters"|"nls_sort"|"nls_territory"|"nls_timestamp_format"|"nls_timestamp_tz_format"|"object_cache_max_size_percent"|"object_cache_optimal_size"|"olap_page_pool_size"|"optimizer_dynamic_sampling"|"optimizer_features_enable"|"optimizer_index_caching"|"optimizer_index_cost_adj"|"optimizer_mode"|"osm_power_limit"|"parallel_instance_group"|"parallel_min_percent"|"password_grace_time"|"password_life_time"|"password_lock_time"|"password_reuse_max"|"password_reuse_time"|"password_verify_function"|"plsql_code_type"|"plsql_compiler_flags"|"plsql_debug"|"plsql_optimize_level"|"plsql_v2_compatibility"|"plsql_warnings"|"private_sga"|"query_rewrite_enabled"|"query_rewrite_integrity"|"remote_dependencies_mode"|"resumable_timeout"|"session_cached_cursors"|"sessions_per_user"|"skip_unusable_indexes"|"sort_area_retained_size"|"sort_area_size"|"sql_trace"|"sqltune_category"|"star_transformation_enabled"|"statistics_level"|"timed_os_statistics"|"timed_statistics"|"tracefile_identifier"|"use_private_outlines"|"using_nls_comp"|"use_stored_outlines"|"workarea_size_policy"
DATA_TYPE   = "varchar2"|"with"{ws}"time"{ws}"zone"|"with"{ws}"local"{ws}"time"{ws}"zone"|"varchar"|"urowid"|"to"{ws}"second"|"to"{ws}"month"|"timestamp"|"string"|"smallint"|"rowid"|"real"|"raw"|"pls_integer"|"nvarchar2"|"numeric"|"number"|"nclob"|"nchar"{ws}"varying"|"nchar"|"national"{ws}"character"{ws}"varying"|"national"{ws}"character"|"national"{ws}"char"{ws}"varying"|"national"{ws}"char"|"long"{ws}"varchar"|"long"{ws}"raw"|"long"|"integer"|"int"|"float"|"double"{ws}"precision"|"decimal"|"date"|"clob"|"character"{ws}"varying"|"character"|"char"|"byte"|"boolean"|"blob"|"binary_integer"|"binary_float"|"binary_double"|"bfile"
EXCEPTION   = "access_into_null"|"case_not_found"|"collection_is_null"|"cursor_already_open"|"dup_val_on_index"|"invalid_cursor"|"invalid_number"|"login_denied"|"no_data_found"|"not_logged_on"|"program_error"|"rowtype_mismatch"|"self_is_null"|"storage_error"|"subscript_beyond_count"|"subscript_outside_limit"|"sys_invalid_rowid"|"timeout_on_resource"|"too_many_rows"|"value_error"|"zero_divide"

VARIABLE = ":"({IDENTIFIER}|{INTEGER})
SQLP_VARIABLE = "&""&"?{IDENTIFIER}

%state WRAPPED
%%

<WRAPPED> {
    .*               { return stt.getLineComment(); }
    .                { return stt.getLineComment(); }
}


//{VARIABLE}           {return stt.getVariable(); }
{SQLP_VARIABLE}      { return stt.getVariable(); }


{BLOCK_COMMENT}      { return stt.getBlockComment(); }
{LINE_COMMENT}       { return stt.getLineComment(); }

"wrapped"            { yybegin(WRAPPED); return tt.getTokenType("KEYWORD");}

{INTEGER}            { return stt.getInteger(); }
{NUMBER}             { return stt.getNumber(); }
{STRING}             { return stt.getString(); }

{FUNCTION}           { return tt.getTokenType("FUNCTION");}
{PARAMETER}          { return tt.getTokenType("PARAMETER");}
{EXCEPTION}          { return tt.getTokenType("EXCEPTION");}

{DATA_TYPE}          { return tt.getTokenType("DATA_TYPE"); }
{KEYWORD}            { return tt.getTokenType("KEYWORD"); }
{OPERATOR}           { return tt.getTokenType("OPERATOR"); }



{IDENTIFIER}         { return stt.getIdentifier(); }
{QUOTED_IDENTIFIER}  { return stt.getQuotedIdentifier(); }


"("                  { return stt.getChrLeftParenthesis(); }
")"                  { return stt.getChrRightParenthesis(); }
"["                  { return stt.getChrLeftBracket(); }
"]"                  { return stt.getChrRightBracket(); }

{WHITE_SPACE}        { return stt.getWhiteSpace(); }
.                    { return stt.getIdentifier(); }

