package com.mahitotsu.tsukumogami.apl.tools.websearch;

import com.mahitotsu.tsukumogami.apl.tools.ActionGroupAttributes;
import com.mahitotsu.tsukumogami.apl.tools.FunctionAttributes;
import com.mahitotsu.tsukumogami.apl.tools.ParameterAttributes;

@ActionGroupAttributes(name = "WebSearchTool", description = """
        ユーザーからの要求に対してインターネット検索した結果に基づいた内容で回答を作成して返すツールです。
        """)
public interface WebSearchTool {

    @FunctionAttributes(name = "search", description = """
            指定された要求を満たすための検索を指定されたドメインに限定して実行し、検索結果に基づいた回答を作成して返します。
             """)
    String search(
        @ParameterAttributes(name="request", description = """
                ユーザーからの要求。取得したい情報、調査したい内容、など具体的に記述します。
                また、回答に含めたい内容やフォーマットについても指定可能です。
                """, type = "string", required = true)
        String request, 
        @ParameterAttributes(name = "domains", description = """
                情報を検索する範囲をドメインの単位で指定します。
                複数の指定が可能です。
                指定しなかった場合は任意のドメインを対象に検索を行います。
                """, type = "string", required = true)
        String... domains);
}
